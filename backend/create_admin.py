"""
Script to create an admin user in the database
Run this script once to create the admin user
"""
import sys
from sqlalchemy.orm import Session
from app.database import SessionLocal, engine, Base
from app.models import User
from app.core.security import get_password_hash

# Create tables if they don't exist
Base.metadata.create_all(bind=engine)

def create_admin_user():
    """Create admin user if it doesn't exist"""
    db: Session = SessionLocal()
    try:
        admin_email = "habibbuneri343@gmail.com"
        admin_password = "habib321"
        admin_name = "Admin User"
        
        # Check if admin already exists
        existing_admin = db.query(User).filter(User.email == admin_email).first()
        if existing_admin:
            # Update existing user to be admin
            existing_admin.is_admin = True
            existing_admin.is_verified = True
            existing_admin.is_active = True
            existing_admin.hashed_password = get_password_hash(admin_password)
            existing_admin.name = admin_name
            db.commit()
            print(f"Admin user updated successfully!")
            print(f"  Email: {admin_email}")
            print(f"  Password: {admin_password}")
            print(f"  Admin status: Active")
            return
        
        # Create new admin user
        hashed_password = get_password_hash(admin_password)
        admin_user = User(
            name=admin_name,
            email=admin_email,
            hashed_password=hashed_password,
            is_active=True,
            is_verified=True,
            is_admin=True
        )
        
        db.add(admin_user)
        db.commit()
        db.refresh(admin_user)
        
        print(f"Admin user created successfully!")
        print(f"  Email: {admin_email}")
        print(f"  Password: {admin_password}")
        print(f"  User ID: {admin_user.id}")
        print(f"  Admin status: Active")
        
    except Exception as e:
        db.rollback()
        print(f"Error creating admin user: {str(e)}")
        sys.exit(1)
    finally:
        db.close()

if __name__ == "__main__":
    print("Creating admin user...")
    create_admin_user()
    print("\nAdmin user is ready to use!")

