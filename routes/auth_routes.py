from flask import Blueprint, current_app, flash, redirect, render_template, request, url_for
from flask_login import current_user, login_required, login_user, logout_user
from pymongo.errors import PyMongoError

from models.user_model import create_user, find_user_by_email, get_user_object_by_id, verify_password


auth_bp = Blueprint("auth", __name__)


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
        if len(password) < 6:
            flash("Password must be at least 6 characters.", "danger")
            return render_template("signup.html")

        db = current_app.db
        try:
            if find_user_by_email(db, email):
                flash("Email already registered.", "warning")
                return render_template("signup.html")

            role = "admin" if request.form.get("admin_code") == "ASSISTLY_ADMIN" else "user"
            created = create_user(db, name, email, password, role)
            user = get_user_object_by_id(db, str(created.inserted_id))
            login_user(user)
            flash("Account created successfully.", "success")
            return redirect(url_for("dashboard.dashboard"))
        except PyMongoError:
            flash("Database connection failed. Please check MongoDB Atlas network access and TLS settings.", "danger")
            return render_template("signup.html")

    return render_template("signup.html")


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
