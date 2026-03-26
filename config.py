import os
import certifi
from pymongo import MongoClient


class Config:
    SECRET_KEY = os.getenv("SECRET_KEY", "change-this-in-production")
    MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017/")
    DB_NAME = os.getenv("DB_NAME", "assistly_db")


def _to_bool(value: str | None) -> bool:
    if not value:
        return False
    return value.strip().lower() in {"1", "true", "yes", "y", "on"}


def get_db():
    """Create a Mongo client and return the Assistly database handle."""
    kwargs = {
        "serverSelectionTimeoutMS": 10000,
        "connectTimeoutMS": 10000,
        "socketTimeoutMS": 20000,
        "tls": True,
        "tlsCAFile": certifi.where(),
    }

    # Temporary fallback for managed/campus networks that intercept TLS.
    # Keep this disabled in production.
    if _to_bool(os.getenv("MONGO_TLS_ALLOW_INVALID_CERTS")):
        kwargs["tlsAllowInvalidCertificates"] = True

    client = MongoClient(Config.MONGO_URI, **kwargs)
    return client[Config.DB_NAME]
