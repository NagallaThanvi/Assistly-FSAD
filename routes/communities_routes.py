from flask import Blueprint, current_app, flash, redirect, render_template, request, url_for
from flask_login import current_user, login_required

from models.community_model import (
    create_community,
    delete_community,
    ensure_default_communities,
    join_community,
    list_communities,
)


communities_bp = Blueprint("communities", __name__, url_prefix="/communities")


@communities_bp.route("/")
@login_required
def communities_page():
    db = current_app.db
    ensure_default_communities(db)
    search = request.args.get("q", "").strip()
    communities = list_communities(db, search or None)
    return render_template("communities.html", communities=communities, query=search)


@communities_bp.route("/<community_id>/join", methods=["POST"])
@login_required
def join(community_id):
    join_community(current_app.db, community_id, current_user.id)
    flash("Community joined.", "success")
    return redirect(url_for("communities.communities_page"))


@communities_bp.route("/create", methods=["POST"])
@login_required
def create():
    if current_user.role != "admin":
        flash("Admin access required.", "danger")
        return redirect(url_for("communities.communities_page"))

    name = request.form.get("name", "").strip()
    location = request.form.get("location", "").strip()
    if not name or not location:
        flash("Name and location are required.", "warning")
        return redirect(url_for("communities.communities_page"))

    create_community(current_app.db, name, location)
    flash("Community created.", "success")
    return redirect(url_for("communities.communities_page"))


@communities_bp.route("/<community_id>/delete", methods=["POST"])
@login_required
def delete(community_id):
    if current_user.role != "admin":
        flash("Admin access required.", "danger")
        return redirect(url_for("communities.communities_page"))

    delete_community(current_app.db, community_id)
    flash("Community deleted.", "info")
    return redirect(url_for("communities.communities_page"))
