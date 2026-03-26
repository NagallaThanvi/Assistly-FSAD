from bson.objectid import ObjectId


def _communities(db):
    return db["communities"]


def ensure_default_communities(db):
    if _communities(db).count_documents({}) > 0:
        return

    _communities(db).insert_many(
        [
            {"name": "Green Meadows", "location": "North Zone", "members": []},
            {"name": "Lakeside Heights", "location": "East Zone", "members": []},
            {"name": "Central Harmony", "location": "City Center", "members": []},
            {"name": "Sunrise Colony", "location": "South Zone", "members": []},
        ]
    )


def list_communities(db, search: str | None = None):
    query = {}
    if search:
        query = {"name": {"$regex": search, "$options": "i"}}
    return list(_communities(db).find(query).sort("name", 1))


def join_community(db, community_id: str, user_id: str):
    _communities(db).update_one(
        {"_id": ObjectId(community_id)},
        {"$addToSet": {"members": user_id}},
    )


def create_community(db, name: str, location: str):
    return _communities(db).insert_one({"name": name.strip(), "location": location.strip(), "members": []})


def delete_community(db, community_id: str):
    return _communities(db).delete_one({"_id": ObjectId(community_id)})
