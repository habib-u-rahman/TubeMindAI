"""
Script to add is_admin column to users table
Run this once to update the database schema
"""
import sys
from sqlalchemy import text
from app.database import engine

def add_admin_column():
    """Add is_admin column to users table"""
    try:
        with engine.connect() as conn:
            # Check if column already exists
            check_query = text("""
                SELECT column_name 
                FROM information_schema.columns 
                WHERE table_name='users' AND column_name='is_admin'
            """)
            result = conn.execute(check_query)
            if result.fetchone():
                print("Column is_admin already exists!")
                return
            
            # Add the column
            alter_query = text("""
                ALTER TABLE users 
                ADD COLUMN is_admin BOOLEAN DEFAULT FALSE NOT NULL
            """)
            conn.execute(alter_query)
            conn.commit()
            print("Successfully added is_admin column to users table!")
            
    except Exception as e:
        print(f"Error adding column: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    print("Adding is_admin column to users table...")
    add_admin_column()
    print("Done!")

