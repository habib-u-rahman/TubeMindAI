# TubeMind AI - Backend API

FastAPI backend for TubeMind AI application.

## Features

- User Authentication (JWT)
- OTP Verification
- Video Notes Generation
- Chat with AI about videos
- User Management
- PostgreSQL Database

## Setup Instructions

### 1. Install Dependencies

```bash
pip install -r requirements.txt
```

### 2. Setup PostgreSQL Database

Create a PostgreSQL database:

```sql
CREATE DATABASE tubemindai_db;
```

### 3. Initialize Database Tables

Run the initialization script to create all tables:

```bash
python init_db.py
```

This will create the `users` and `otps` tables in your PostgreSQL database.

### 5. Run the Server

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

The API will be available at `http://localhost:8000`

API Documentation: `http://localhost:8000/docs`

## Project Structure

```
backend/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI app entry point
│   ├── config.py            # Configuration settings
│   ├── database.py          # Database connection
│   ├── models/              # SQLAlchemy models
│   ├── schemas/             # Pydantic schemas
│   ├── api/                 # API routes
│   ├── core/                # Core utilities (auth, security)
│   └── services/            # Business logic
├── alembic/                 # Database migrations
├── requirements.txt
├── .env.example
└── README.md
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/verify-otp` - Verify OTP
- `POST /api/auth/forgot-password` - Request password reset
- `POST /api/auth/reset-password` - Reset password

### Videos & Notes
- `POST /api/videos/generate-notes` - Generate notes from YouTube URL
- `GET /api/videos/{video_id}/notes` - Get notes for a video
- `GET /api/videos/recent` - Get recent videos

### Chat
- `POST /api/chat/message` - Send chat message
- `GET /api/chat/history` - Get chat history

### User
- `GET /api/users/me` - Get current user
- `PUT /api/users/me` - Update user profile

## Development

Run with auto-reload:

```bash
uvicorn app.main:app --reload
```

## Testing

```bash
pytest
```

