import bcrypt
from bson.objectid import ObjectId
from flask_login import UserMixin


ALLOWED_ROLES = {"admin", "user"}


class UserAccount(UserMixin):
    def __init__(self, doc: dict):
        self.doc = doc
        self.id = str(doc["_id"])
        self.name = doc.get("name", "")
        self.email = doc.get("email", "")
        self.role = doc.get("role", "user")

    @property
    def is_admin(self) -> bool:
        return self.role == "admin"


def _users(db):
    return db["users"]


def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")


def verify_password(password: str, hashed_password: str) -> bool:
    return bcrypt.checkpw(password.encode("utf-8"), hashed_password.encode("utf-8"))


def create_user(db, name: str, email: str, password: str, role: str = "user"):
    role = role if role in ALLOWED_ROLES else "user"
    user_doc = {
        "name": name.strip(),
        "email": email.strip().lower(),
        "password": hash_password(password),
        "role": role,
        "mode": "resident",
        "email_verified": True,
        "auth_provider": "local",
    }
    return _users(db).insert_one(user_doc)


def create_user_with_hash(db, name: str, email: str, password_hash: str, role: str = "user"):
    role = role if role in ALLOWED_ROLES else "user"
    user_doc = {
        "name": name.strip(),
        "email": email.strip().lower(),
        "password": password_hash,
        "role": role,
        "mode": "resident",
        "email_verified": True,
        "auth_provider": "local",
    }
    return _users(db).insert_one(user_doc)


def create_google_user(db, name: str, email: str, role: str = "user"):
    role = role if role in ALLOWED_ROLES else "user"
    user_doc = {
        "name": name.strip() or email.split("@")[0],
        "email": email.strip().lower(),
        "password": hash_password(email + "-google-oauth"),
        "role": role,
        "mode": "resident",
        "email_verified": True,
        "auth_provider": "google",
    }
    return _users(db).insert_one(user_doc)


def find_user_by_email(db, email: str):
    return _users(db).find_one({"email": email.strip().lower()})


def find_user_by_id(db, user_id: str):
    try:
        return _users(db).find_one({"_id": ObjectId(user_id)})
    except Exception:
        return None


def get_user_object_by_id(db, user_id: str):
    doc = find_user_by_id(db, user_id)
    if not doc:
        return None
    return UserAccount(doc)


def list_users(db):
    return list(_users(db).find({}, {"password": 0}).sort("name", 1))


def count_total_users(db) -> int:
    return _users(db).count_documents({})
