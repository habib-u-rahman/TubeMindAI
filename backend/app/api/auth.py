from fastapi import APIRouter, Depends, HTTPException, status, Header
from sqlalchemy.orm import Session
from datetime import datetime, timedelta, timezone
from typing import Optional
from app.database import get_db
from app.models import User, OTP
from app.schemas.auth import (
    UserRegister,
    UserRegisterResponse,
    UserLogin,
    Token,
    OTPRequest,
    OTPVerify,
    OTPVerifyResponse,
    ForgotPasswordRequest,
    ResetPassword,
    ResetPasswordResponse,
    UserResponse
)
from app.core.security import (
    get_password_hash,
    verify_password,
    create_access_token,
    generate_otp,
    verify_token
)
from app.core.email_service import send_otp_email
from app.config import settings

router = APIRouter()


@router.post("/register", response_model=UserRegisterResponse, status_code=status.HTTP_201_CREATED)
async def register(user_data: UserRegister, db: Session = Depends(get_db)):
    """
    Register a new user and send OTP to email
    
    Request body should be valid JSON:
    {
        "name": "John Doe",
        "email": "john@example.com",
        "password": "password123"
    }
    """
    """
    Register a new user and send OTP to email
    """
    # Normalize email (lowercase and strip)
    email_clean = user_data.email.strip().lower()
    
    # Check if user already exists
    existing_user = db.query(User).filter(User.email == email_clean).first()
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email already registered"
        )
    
    # Create new user (not verified yet)
    hashed_password = get_password_hash(user_data.password)
    
    new_user = User(
        name=user_data.name,
        email=email_clean,
        hashed_password=hashed_password,
        is_verified=False
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    
    # Generate and save OTP
    otp_code = generate_otp(6)
    expires_at = datetime.now(timezone.utc) + timedelta(minutes=60)  # OTP valid for 1 hour
    
    otp = OTP(
        email=email_clean,
        otp_code=otp_code,
        purpose="signup",
        expires_at=expires_at
    )
    db.add(otp)
    db.commit()
    db.refresh(otp)
    
    # Debug: Print OTP for testing (remove in production)
    if settings.DEBUG:
        print(f"DEBUG: OTP generated for {email_clean}: {otp_code}, expires at: {expires_at}")
    
    # Send OTP email (use original email for display)
    email_sent = send_otp_email(user_data.email, otp_code, "signup")
    if not email_sent:
        # If email fails, still return success but log the error
        print(f"Warning: Failed to send OTP email to {user_data.email}")
    
    return UserRegisterResponse(
        message="Registration successful. Please check your email for OTP verification code.",
        email=email_clean
    )


@router.post("/verify-otp", response_model=OTPVerifyResponse)
async def verify_otp(otp_data: OTPVerify, db: Session = Depends(get_db)):
    """
    Verify OTP code for signup or password reset
    Different handling for each purpose:
    - signup: Verifies email and returns JWT token for immediate login
    - forgot_password: Returns reset token for password reset
    """
    # Clean OTP code (remove whitespace, convert to string)
    otp_code_clean = str(otp_data.otp_code).strip()
    email_clean = otp_data.email.strip().lower()
    
    # Find the most recent unused OTP record for this email and purpose
    otp_record = db.query(OTP).filter(
        OTP.email == email_clean,
        OTP.purpose == otp_data.purpose,
        OTP.is_used == False
    ).order_by(OTP.created_at.desc()).first()
    
    if not otp_record:
        # Check if there's an OTP but it's already used
        used_otp = db.query(OTP).filter(
            OTP.email == email_clean,
            OTP.purpose == otp_data.purpose,
            OTP.is_used == True
        ).order_by(OTP.created_at.desc()).first()
        
        if used_otp:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="OTP code has already been used. Please request a new OTP."
            )
        
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No OTP found for this email. Please request a new OTP."
        )
    
    # Check if OTP code matches (strip and compare)
    stored_otp = str(otp_record.otp_code).strip()
    if stored_otp != otp_code_clean:
        # Debug log (only in debug mode)
        if settings.DEBUG:
            print(f"DEBUG: OTP mismatch for {email_clean}. Stored: '{stored_otp}', Received: '{otp_code_clean}'")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid OTP code. Please check and try again."
        )
    
    # Check if OTP is expired
    current_time = datetime.now(timezone.utc)
    if current_time > otp_record.expires_at:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="OTP code has expired. Please request a new OTP."
        )
    
    # Mark OTP as used
    otp_record.is_used = True
    db.commit()
    
    # Handle signup OTP verification
    if otp_data.purpose == "signup":
        # Find the user who registered with this email
        user = db.query(User).filter(User.email == email_clean).first()
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found. Please register first."
            )
        
        # Check if user is already verified
        if user.is_verified:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Email is already verified. You can login directly."
            )
        
        # Verify user email (mark as verified)
        user.is_verified = True
        db.commit()
        
        # Create JWT access token for immediate login
        access_token = create_access_token(
            data={"sub": user.email, "user_id": user.id}
        )
        
        return OTPVerifyResponse(
            message="Email verified successfully. You are now logged in.",
            verified=True,
            token=access_token,
            reset_token=None
        )
    
    # Handle forgot_password OTP verification
    elif otp_data.purpose == "forgot_password":
        user = db.query(User).filter(User.email == email_clean).first()
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        
        # Create a temporary reset token (expires in 10 minutes)
        reset_token = create_access_token(
            data={"sub": user.email, "user_id": user.id, "purpose": "password_reset"},
            expires_delta=timedelta(minutes=10)
        )
        
        return OTPVerifyResponse(
            message="OTP verified successfully. You can now reset your password.",
            verified=True,
            token=None,
            reset_token=reset_token
        )
    
    else:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid OTP purpose"
        )


@router.post("/login", response_model=Token)
async def login(credentials: UserLogin, db: Session = Depends(get_db)):
    """
    Login user and return JWT token
    User must be registered and email must be verified via OTP
    """
    # Normalize email and find user
    email_clean = credentials.email.strip().lower()
    user = db.query(User).filter(User.email == email_clean).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password"
        )
    
    # Verify password
    if not verify_password(credentials.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password"
        )
    
    # Check if user is active
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User account is inactive"
        )
    
    # Check if user email is verified (OTP verification required)
    if not user.is_verified:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Please verify your email first. Check your email for OTP code."
        )
    
    # Create access token
    access_token = create_access_token(
        data={"sub": user.email, "user_id": user.id}
    )
    
    return Token(
        access_token=access_token,
        token_type="bearer",
        user_id=user.id,
        email=user.email,
        name=user.name
    )


@router.post("/forgot-password")
async def forgot_password(request: ForgotPasswordRequest, db: Session = Depends(get_db)):
    """
    Send OTP to user's email for password reset
    """
    # Check if user exists
    user = db.query(User).filter(User.email == request.email).first()
    if not user:
        # Don't reveal if email exists or not (security best practice)
        return {"message": "If the email exists, an OTP has been sent"}
    
    # Generate and save OTP
    otp_code = generate_otp(6)
    expires_at = datetime.now(timezone.utc) + timedelta(minutes=60)  # OTP valid for 1 hour
    
    otp = OTP(
        email=request.email,
        otp_code=otp_code,
        purpose="forgot_password",
        expires_at=expires_at
    )
    db.add(otp)
    db.commit()
    
    # Send OTP email
    email_sent = send_otp_email(request.email, otp_code, "forgot_password")
    if not email_sent:
        print(f"Warning: Failed to send OTP email to {request.email}")
    
    return {"message": "If the email exists, an OTP has been sent"}


@router.post("/reset-password", response_model=ResetPasswordResponse)
async def reset_password(
    reset_data: ResetPassword,
    reset_token: Optional[str] = Header(None, alias="X-Reset-Token"),
    db: Session = Depends(get_db)
):
    """
    Reset user password after OTP verification
    Requires reset_token from verify-otp endpoint (for forgot_password purpose)
    """
    # Validate passwords match
    if reset_data.new_password != reset_data.confirm_password:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Passwords do not match"
        )
    
    # Verify reset token
    if not reset_token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Reset token is required. Please verify OTP first."
        )
    
    payload = verify_token(reset_token)
    if not payload:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired reset token"
        )
    
    # Verify token purpose
    if payload.get("purpose") != "password_reset":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token purpose"
        )
    
    # Verify email matches
    token_email = payload.get("sub")
    if token_email != reset_data.email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email does not match reset token"
        )
    
    # Find user
    user = db.query(User).filter(User.email == reset_data.email).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )
    
    # Update password
    user.hashed_password = get_password_hash(reset_data.new_password)
    db.commit()
    
    return ResetPasswordResponse(
        message="Password reset successfully. You can now login with your new password."
    )


# Dependency to extract token from Authorization header
def get_token_dependency(authorization: Optional[str] = Header(None)) -> str:
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authorization header missing"
        )
    
    try:
        scheme, token = authorization.split()
        if scheme.lower() != "bearer":
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid authentication scheme"
            )
        return token
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authorization header format"
        )


# Dependency to get current user from token
def get_current_user_dependency(
    token: str = Depends(get_token_dependency),
    db: Session = Depends(get_db)
) -> User:
    payload = verify_token(token)
    if not payload:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication credentials"
        )
    
    email = payload.get("sub")
    user = db.query(User).filter(User.email == email).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found"
        )
    
    return user


@router.get("/me", response_model=UserResponse)
async def get_current_user(
    current_user: User = Depends(get_current_user_dependency)
):
    """
    Get current authenticated user
    """
    return current_user

