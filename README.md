# Assistly - Role-Based Community Service Platform

Assistly is a Flask + MongoDB platform where users can switch between Resident and Volunteer modes, while Admin users manage users, requests, and communities.

## Core Upgrades

- Flask-Login authentication (Signup/Login/Logout)
- Role-aware routing:
  - `admin` -> Admin Dashboard
  - `user` -> User Dashboard
- User mode toggle inside user dashboard:
  - Resident Mode
  - Volunteer Mode
- Leaflet map integration:
  - click to select location
  - detect current location
  - save coordinates to DB
- Resident requests system:
  - create/view/update/delete
  - status tracking (`Open`, `In Progress`, `Completed`)
- Volunteer workflow:
  - view all requests except own
  - accept request
  - mark accepted request completed
  - self-accept blocked at backend
- Communities module:
  - list/search communities
  - join community
  - admin create/delete communities
- Admin dashboard:
  - view users
  - view/delete requests
  - manage communities
  - analytics charts
- Bonus:
  - modal request form
  - polling notifications without refresh

## Tech Stack

- Framework: Django
- Database: MongoDB
- Graph-related Database: SurrealDB
- API: GraphQL
- AI/ML/NLP: Python Libraries (such as scikit-learn, TensorFlow/PyTorch, spaCy, NLTK, Transformers)
- Frontend: Jinja2/HTML, Bootstrap 5, JavaScript, Leaflet.js
- Deployment: Gunicorn + Render

## Updated Structure

```text
assistly/
  app.py
  config.py
  requirements.txt
  routes/
    auth_routes.py
    dashboard_routes.py
    requests_routes.py
    communities_routes.py
  models/
    user_model.py
    request_model.py
    community_model.py
  templates/
    base.html
    index.html
    login.html
    signup.html
    dashboard.html
    admin_dashboard.html
    communities.html
  static/
    css/style.css
    js/main.js
  analytics/
    analytics.py
```

## Run Locally

1. Open project folder.

```powershell
cd C:\Users\nagal\OneDrive\Desktop\PFSD\assistly
```

2. Create and activate venv (Python 3.12 recommended).

```powershell
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
```

3. Install dependencies.

```powershell
pip install -r requirements.txt
```

4. Create/update `.env`.

```env
SECRET_KEY=replace-with-a-secure-random-string
MONGO_URI=mongodb+srv://<username>:<password>@<cluster-url>/?retryWrites=true&w=majority
DB_NAME=assistly_db
```

5. Start the app.

```powershell
python app.py
```

6. Open:

`http://127.0.0.1:5000`

## Default Role Behavior

- New users are created with role `user`.
- To create an admin via signup, use admin code: `ASSISTLY_ADMIN`.
- Admin accounts always use Admin Dashboard.

## Key Endpoints

- Auth: `/signup`, `/login`, `/logout`
- Dashboard: `/dashboard`, `/dashboard/user`, `/dashboard/admin`
- Mode toggle API: `POST /dashboard/mode`
- Location save API: `POST /dashboard/location`
- Request CRUD + actions:
  - `POST /requests/create`
  - `POST /requests/<id>/update`
  - `POST /requests/<id>/status`
  - `POST /requests/<id>/delete`
  - `POST /requests/<id>/accept`
  - `POST /requests/<id>/complete`
- Communities:
  - `GET /communities/`
  - `POST /communities/<id>/join`
  - `POST /communities/create` (admin)
  - `POST /communities/<id>/delete` (admin)

## Deployment (Render)

- Build command: `pip install -r requirements.txt`
- Start command: `gunicorn app:create_app()`
- Env vars: `SECRET_KEY`, `MONGO_URI`, `DB_NAME`
