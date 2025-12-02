"""
Database initialization script
Run this to create all tables in the database
"""
from app.database import engine, Base
from app.models import User, OTP

if __name__ == "__main__":
    print("Creating database tables...")
    Base.metadata.create_all(bind=engine)
    print("Database tables created successfully!")

