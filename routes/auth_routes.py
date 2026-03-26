import random
import re
import smtplib
from datetime import datetime, timedelta
from email.mime.text import MIMEText

from flask import Blueprint, current_app, flash, redirect, render_template, request, url_for
from flask_login import current_user, login_required, login_user, logout_user
from pymongo.errors import PyMongoError

from models.user_model import (
    create_google_user,
    create_user_with_hash,
    find_user_by_email,
    get_user_object_by_id,
    hash_password,
    verify_password,
)


auth_bp = Blueprint("auth", __name__)

EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")


def _is_valid_email(email: str) -> bool:
    return bool(EMAIL_RE.match(email))


def _send_verification_email(email: str, otp: str):
    cfg = current_app.config
    if not cfg.get("SMTP_HOST") or not cfg.get("SMTP_USER") or not cfg.get("SMTP_PASSWORD"):
        raise RuntimeError("SMTP is not configured. Set SMTP_HOST, SMTP_USER and SMTP_PASSWORD in .env")

    sender = cfg.get("EMAIL_FROM") or cfg.get("SMTP_USER")
    msg = MIMEText(
        (
            "Your Assistly verification code is: "
            f"{otp}\n\n"
            "This code expires in 10 minutes."
        ),
        "plain",
        "utf-8",
    )
    msg["Subject"] = "Assistly Email Verification"
    msg["From"] = sender
    msg["To"] = email

    with smtplib.SMTP(cfg["SMTP_HOST"], cfg["SMTP_PORT"], timeout=20) as smtp:
        if cfg.get("SMTP_USE_TLS", True):
            smtp.starttls()
        smtp.login(cfg["SMTP_USER"], cfg["SMTP_PASSWORD"])
        smtp.sendmail(sender, [email], msg.as_string())


@auth_bp.route("/signup", methods=["GET", "POST"])
def signup():
    if current_user.is_authenticated:
        return redirect(url_for("dashboard.dashboard"))

    if request.method == "POST":
        name = request.form.get("name", "").strip()
        email = request.form.get("email", "").strip().lower()
        password = request.form.get("password", "")

        if not name or not email or not password:
            flash("All fields are required.", "danger")
            return render_template("signup.html")
        if not _is_valid_email(email):
            flash("Please enter a valid email address.", "danger")
            return render_template("signup.html")
        if len(password) < 6:
            flash("Password must be at least 6 characters.", "danger")
            return render_template("signup.html")

        db = current_app.db
        try:
            if find_user_by_email(db, email):
                flash("Email already registered.", "warning")
                return render_template("signup.html")

            role = "admin" if request.form.get("admin_code") == "ASSISTLY_ADMIN" else "user"
            otp = f"{random.randint(0, 999999):06d}"
            verification_doc = {
                "name": name,
                "email": email,
                "password_hash": hash_password(password),
                "role": role,
                "otp": otp,
                "expires_at": datetime.utcnow() + timedelta(minutes=10),
                "created_at": datetime.utcnow(),
            }
            db["email_verifications"].update_one(
                {"email": email},
                {"$set": verification_doc},
                upsert=True,
            )
            _send_verification_email(email, otp)
            flash("Verification code sent to your email.", "info")
            return redirect(url_for("auth.verify_email", email=email))
        except RuntimeError as ex:
            flash(str(ex), "danger")
            return render_template("signup.html")
        except PyMongoError:
            flash("Database connection failed. Please check MongoDB Atlas network access and TLS settings.", "danger")
            return render_template("signup.html")

    return render_template("signup.html")


@auth_bp.route("/verify-email", methods=["GET", "POST"])
def verify_email():
    if current_user.is_authenticated:
        return redirect(url_for("dashboard.dashboard"))

    email = request.args.get("email", "").strip().lower()
    if request.method == "POST":
        email = request.form.get("email", "").strip().lower()
        otp = request.form.get("otp", "").strip()
        db = current_app.db
        record = db["email_verifications"].find_one({"email": email})
        if not record:
            flash("No verification request found. Please sign up again.", "warning")
            return redirect(url_for("auth.signup"))

        if datetime.utcnow() > record["expires_at"]:
            db["email_verifications"].delete_one({"_id": record["_id"]})
            flash("Verification code expired. Please sign up again.", "warning")
            return redirect(url_for("auth.signup"))

        if otp != record.get("otp"):
            flash("Invalid verification code.", "danger")
            return render_template("verify_email.html", email=email)

        try:
            existing = find_user_by_email(db, email)
            if existing:
                db["email_verifications"].delete_one({"_id": record["_id"]})
                flash("Email already registered. Please log in.", "info")
                return redirect(url_for("auth.login"))

            created = create_user_with_hash(
                db,
                record.get("name", "User"),
                email,
                record["password_hash"],
                record.get("role", "user"),
            )
            db["email_verifications"].delete_one({"_id": record["_id"]})
            user = get_user_object_by_id(db, str(created.inserted_id))
            login_user(user)
            flash("Email verified. Account created successfully.", "success")
            return redirect(url_for("dashboard.dashboard"))
        except PyMongoError:
            flash("Database connection failed. Please try again.", "danger")
            return render_template("verify_email.html", email=email)

    return render_template("verify_email.html", email=email)


@auth_bp.route("/login/google")
def login_google():
    if current_user.is_authenticated:
        return redirect(url_for("dashboard.dashboard"))
    google = getattr(getattr(current_app, "oauth", None), "google", None)
    if not google:
        flash("Google login is not configured.", "warning")
        return redirect(url_for("auth.login"))
    # Explicitly pass redirect_uri to authorize_redirect
    redirect_uri = url_for("auth.google_callback", _external=True)
    return google.authorize_redirect(redirect_uri=redirect_uri)


@auth_bp.route("/auth/google/callback")
def google_callback():
    google = getattr(getattr(current_app, "oauth", None), "google", None)
    if not google:
        flash("Google login is not configured.", "warning")
        return redirect(url_for("auth.login"))

    try:
        # Authorize and get token
        token = google.authorize_access_token()
        # Get user profile using the token
        profile = google.get("https://www.googleapis.com/oauth2/v1/userinfo", token=token).json()
    except Exception as ex:
        current_app.logger.exception("Google OAuth callback failed")
        if current_app.debug:
            flash(f"Google authentication failed: {ex}", "danger")
        else:
            flash("Google authentication failed. Please try again.", "danger")
        return redirect(url_for("auth.login"))

    email = str(profile.get("email", "")).strip().lower()
    if not email:
        flash("Google account did not provide an email.", "danger")
        return redirect(url_for("auth.login"))

    db = current_app.db
    try:
        user_doc = find_user_by_email(db, email)
        if not user_doc:
            name = str(profile.get("name") or profile.get("given_name") or email.split("@")[0])
            created = create_google_user(db, name, email, role="user")
            user = get_user_object_by_id(db, str(created.inserted_id))
        else:
            user = get_user_object_by_id(db, str(user_doc["_id"]))

        login_user(user)
        flash("Logged in with Google.", "success")
        return redirect(url_for("dashboard.dashboard"))
    except PyMongoError:
        flash("Database connection failed. Please try again.", "danger")
        return redirect(url_for("auth.login"))


@auth_bp.route("/login", methods=["GET", "POST"])
def login():
    if current_user.is_authenticated:
        return redirect(url_for("dashboard.dashboard"))

    if request.method == "POST":
        email = request.form.get("email", "").strip().lower()
        password = request.form.get("password", "")

        db = current_app.db
        try:
            user_doc = find_user_by_email(db, email)
            if not user_doc or not verify_password(password, user_doc["password"]):
                flash("Invalid credentials.", "danger")
                return render_template("login.html")

            user = get_user_object_by_id(db, str(user_doc["_id"]))
            login_user(user)
            flash("Logged in successfully.", "success")
            return redirect(url_for("dashboard.dashboard"))
        except PyMongoError:
            flash("Database connection failed. Please check MongoDB Atlas network access and TLS settings.", "danger")
            return render_template("login.html")

    return render_template("login.html")


@auth_bp.route("/logout")
@login_required
def logout():
    logout_user()
    flash("You have been logged out.", "info")
    return redirect(url_for("dashboard.index"))
