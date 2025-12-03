from datetime import datetime, timedelta, timezone
from typing import Optional
from jose import JWTError, jwt
from passlib.context import CryptContext
from app.config import settings
import secrets
import string

# Password hashing
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a password against its hash"""
    return pwd_context.verify(plain_password, hashed_password)


def get_password_hash(password: str) -> str:
    """Hash a password"""
    return pwd_context.hash(password)


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    """Create JWT access token"""
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.now(timezone.utc) + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    
    # Convert datetime to timestamp for JWT
    expire_timestamp = int(expire.timestamp())
    to_encode.update({"exp": expire_timestamp})
    
    if settings.DEBUG:
        print(f"DEBUG: Creating token with expiration: {expire.isoformat()} (timestamp: {expire_timestamp})")
    
    encoded_jwt = jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
    return encoded_jwt


def verify_token(token: str) -> Optional[dict]:
    """Verify and decode JWT token"""
    try:
        if not token or not isinstance(token, str):
            if settings.DEBUG:
                print(f"DEBUG: Token is empty or not a string")
            return None
        
        # Remove any whitespace and quotes
        token = token.strip().strip('"').strip("'")
        
        if not token:
            if settings.DEBUG:
                print(f"DEBUG: Token is empty after stripping")
            return None
        
        if settings.DEBUG:
            print(f"DEBUG: Verifying token (length: {len(token)})")
            print(f"DEBUG: Token starts with: {token[:20]}...")
            print(f"DEBUG: Using SECRET_KEY length: {len(settings.SECRET_KEY)}")
            print(f"DEBUG: Using algorithm: {settings.ALGORITHM}")
        
        # Decode and verify token
        payload = jwt.decode(
            token, 
            settings.SECRET_KEY, 
            algorithms=[settings.ALGORITHM],
            options={"verify_signature": True, "verify_exp": True}
        )
        
        if settings.DEBUG:
            print(f"DEBUG: Token verified successfully!")
            print(f"DEBUG: Payload: {payload}")
        
        return payload
        
    except JWTError as e:
        # Catch all JWT errors (expired, invalid signature, etc.)
        if settings.DEBUG:
            print(f"DEBUG: JWT Error: {str(e)}")
            print(f"DEBUG: Error type: {type(e).__name__}")
            import traceback
            print(f"DEBUG: Traceback: {traceback.format_exc()}")
        return None
    except Exception as e:
        # Any other error
        if settings.DEBUG:
            print(f"DEBUG: Token verification error: {str(e)}")
            print(f"DEBUG: Error type: {type(e).__name__}")
            import traceback
            print(f"DEBUG: Traceback: {traceback.format_exc()}")
        return None


def generate_otp(length: int = 6) -> str:
    """Generate random OTP code"""
    return ''.join(secrets.choice(string.digits) for _ in range(length))

