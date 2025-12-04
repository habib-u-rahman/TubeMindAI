from fastapi import APIRouter, Depends, HTTPException, status, Header, Request
from sqlalchemy.orm import Session
from datetime import datetime, timedelta, timezone
from typing import Optional
import json
from jose import jwt
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
    
    Note: For forgot password, you can also use the dedicated endpoint:
    POST /api/auth/forgot-password/verify-otp
    """
    # Normalize email (lowercase and strip)
    email_clean = otp_data.email.strip().lower()
    
    # Clean and validate OTP code
    # Convert to string, remove all whitespace, and ensure it's numeric
    otp_code_clean = str(otp_data.otp_code).strip().replace(" ", "").replace("-", "")
    
    # Validate OTP format (should be 4-6 digits)
    if not otp_code_clean.isdigit():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="OTP code must contain only digits."
        )
    
    if len(otp_code_clean) < 4 or len(otp_code_clean) > 6:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="OTP code must be between 4 and 6 digits."
        )
    
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
            detail="No OTP found for this email. Please register first or request a new OTP."
        )
    
    # Check if OTP is expired first (before checking code match)
    current_time = datetime.now(timezone.utc)
    if current_time > otp_record.expires_at:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="OTP code has expired. Please request a new OTP."
        )
    
    # Clean stored OTP code (ensure it's a string and remove any whitespace)
    stored_otp = str(otp_record.otp_code).strip().replace(" ", "").replace("-", "")
    
    # Compare OTP codes (both should be clean strings now)
    if stored_otp != otp_code_clean:
        # Debug log (only in debug mode)
        if settings.DEBUG:
            print(f"DEBUG: OTP mismatch for {email_clean}. Stored: '{stored_otp}' (len={len(stored_otp)}), Received: '{otp_code_clean}' (len={len(otp_code_clean)})")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid OTP code. Please check and try again."
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
        db.refresh(user)
        
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
        # Find user
        user = db.query(User).filter(User.email == email_clean).first()
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found. Please check your email address."
            )
        
        # Check if user is active
        if not user.is_active:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="User account is inactive. Please contact support."
            )
        
        # Create a temporary reset token (expires in 30 minutes)
        try:
            reset_token = create_access_token(
                data={"sub": user.email, "user_id": user.id, "purpose": "password_reset"},
                expires_delta=timedelta(minutes=30)
            )
        except Exception as token_error:
            if settings.DEBUG:
                print(f"DEBUG: Error creating reset token: {str(token_error)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Failed to create reset token. Please try again."
            )
        
        # Debug log
        if settings.DEBUG:
            print(f"DEBUG: Forgot password OTP verified for {email_clean}")
            print(f"DEBUG: Reset token created - length: {len(reset_token)}")
            print(f"DEBUG: Token preview: {reset_token[:30]}...{reset_token[-30:]}")
            print(f"DEBUG: Token expires in 30 minutes")
        
        return OTPVerifyResponse(
            message="OTP verified successfully. You can now reset your password. Use the reset_token in the reset-password endpoint. Token is valid for 30 minutes.",
            verified=True,
            token=None,
            reset_token=reset_token
        )
    
    else:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid OTP purpose. Must be 'signup' or 'forgot_password'."
        )


# Dedicated endpoint for forgot password OTP verification
@router.post("/forgot-password/verify-otp", response_model=OTPVerifyResponse)
async def verify_forgot_password_otp(otp_data: OTPVerify, db: Session = Depends(get_db)):
    """
    Verify OTP for forgot password flow
    This is a dedicated endpoint specifically for forgot password OTP verification
    
    Request body:
    {
        "email": "user@example.com",
        "otp_code": "123456",
        "purpose": "forgot_password"
    }
    
    Response:
    {
        "message": "OTP verified successfully...",
        "verified": true,
        "token": null,
        "reset_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }
    """
    # Force purpose to be forgot_password for this endpoint
    if otp_data.purpose != "forgot_password":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="This endpoint is only for forgot_password purpose. Use /api/auth/verify-otp for signup verification."
        )
    
    # Normalize email
    email_clean = otp_data.email.strip().lower()
    
    # Clean and validate OTP code
    otp_code_clean = str(otp_data.otp_code).strip().replace(" ", "").replace("-", "")
    
    # Validate OTP format
    if not otp_code_clean.isdigit():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="OTP code must contain only digits."
        )
    
    if len(otp_code_clean) < 4 or len(otp_code_clean) > 6:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="OTP code must be between 4 and 6 digits."
        )
    
    # Find the most recent unused OTP record for this email and forgot_password purpose
    otp_record = db.query(OTP).filter(
        OTP.email == email_clean,
        OTP.purpose == "forgot_password",
        OTP.is_used == False
    ).order_by(OTP.created_at.desc()).first()
    
    if not otp_record:
        # Check if there's an OTP but it's already used
        used_otp = db.query(OTP).filter(
            OTP.email == email_clean,
            OTP.purpose == "forgot_password",
            OTP.is_used == True
        ).order_by(OTP.created_at.desc()).first()
        
        if used_otp:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="OTP code has already been used. Please request a new OTP by calling forgot-password again."
            )
        
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No OTP found for this email. Please request a new OTP by calling the forgot-password endpoint first."
        )
    
    # Check if OTP is expired
    current_time = datetime.now(timezone.utc)
    if current_time > otp_record.expires_at:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="OTP code has expired. Please request a new OTP by calling the forgot-password endpoint again."
        )
    
    # Clean stored OTP code
    stored_otp = str(otp_record.otp_code).strip().replace(" ", "").replace("-", "")
    
    # Compare OTP codes
    if stored_otp != otp_code_clean:
        if settings.DEBUG:
            print(f"DEBUG: OTP mismatch for forgot password {email_clean}. Stored: '{stored_otp}' (len={len(stored_otp)}), Received: '{otp_code_clean}' (len={len(otp_code_clean)})")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid OTP code. Please check and try again."
        )
    
    # Mark OTP as used
    otp_record.is_used = True
    db.commit()
    
    # Find user
    user = db.query(User).filter(User.email == email_clean).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found. Please check your email address."
        )
    
    # Check if user is active
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User account is inactive. Please contact support."
        )
    
    # Create reset token
    try:
        reset_token = create_access_token(
            data={"sub": user.email, "user_id": user.id, "purpose": "password_reset"},
            expires_delta=timedelta(minutes=30)
        )
    except Exception as token_error:
        if settings.DEBUG:
            print(f"DEBUG: Error creating reset token: {str(token_error)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to create reset token. Please try again."
        )
    
    # Debug log
    if settings.DEBUG:
        print(f"DEBUG: Forgot password OTP verified successfully for {email_clean}")
        print(f"DEBUG: Reset token created - length: {len(reset_token)}")
        print(f"DEBUG: Token preview: {reset_token[:30]}...{reset_token[-30:]}")
        print(f"DEBUG: Token expires in 30 minutes")
    
    return OTPVerifyResponse(
        message="OTP verified successfully. You can now reset your password. Use the reset_token in the reset-password endpoint. Token is valid for 30 minutes.",
        verified=True,
        token=None,
        reset_token=reset_token
    )


@router.post("/login", response_model=Token)
async def login(credentials: UserLogin, db: Session = Depends(get_db)):
    """
    Login user and return JWT token
    User must be registered and email must be verified via OTP
    
    Request body:
    {
        "email": "user@example.com",
        "password": "password123"
    }
    """
    try:
        # Normalize email (lowercase and strip)
        email_clean = str(credentials.email).strip().lower()
        
        # Basic email validation
        if not email_clean or "@" not in email_clean or "." not in email_clean.split("@")[1]:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid email format. Please provide a valid email address."
            )
        
        # Validate password is not empty
        password = str(credentials.password).strip()
        if not password:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Password is required"
            )
        
        # Find user by email
        user = db.query(User).filter(User.email == email_clean).first()
        if not user:
            # Don't reveal if email exists (security best practice)
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Incorrect email or password"
            )
        
        # Verify password
        if not verify_password(password, user.hashed_password):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Incorrect email or password"
            )
        
        # Check if user is active
        if not user.is_active:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="User account is inactive. Please contact support."
            )
        
        # Check if user email is verified (OTP verification required)
        if not user.is_verified:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Please verify your email first. Check your email for OTP code or request a new one."
            )
        
        # Create access token
        access_token = create_access_token(
            data={"sub": user.email, "user_id": user.id}
        )
        
        # Debug log (only in debug mode)
        if settings.DEBUG:
            print(f"DEBUG: User {email_clean} logged in successfully")
        
        return Token(
            access_token=access_token,
            token_type="bearer",
            user_id=user.id,
            email=user.email,
            name=user.name
        )
    except HTTPException:
        # Re-raise HTTP exceptions
        raise
    except Exception as e:
        # Handle any unexpected errors
        if settings.DEBUG:
            print(f"DEBUG: Login error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An error occurred during login. Please try again."
        )


@router.post("/forgot-password")
async def forgot_password(request: ForgotPasswordRequest, db: Session = Depends(get_db)):
    """
    Send OTP to user's email for password reset
    
    Request body:
    {
        "email": "user@example.com"
    }
    
    Response:
    {
        "message": "If the email exists, an OTP has been sent to your email"
    }
    """
    try:
        # Normalize email (lowercase and strip)
        email_clean = request.email.strip().lower()
        
        # Basic email validation
        if not email_clean or "@" not in email_clean or "." not in email_clean.split("@")[1] if "@" in email_clean and len(email_clean.split("@")) > 1 else "":
            # Don't reveal if email format is wrong (security best practice)
            return {"message": "If the email exists, an OTP has been sent to your email"}
        
        # Check if user exists
        user = db.query(User).filter(User.email == email_clean).first()
        if not user:
            # Don't reveal if email exists or not (security best practice)
            return {"message": "If the email exists, an OTP has been sent to your email"}
        
        # Check if user is active
        if not user.is_active:
            # Don't reveal account status (security best practice)
            return {"message": "If the email exists, an OTP has been sent to your email"}
        
        # Generate and save OTP
        otp_code = generate_otp(6)
        expires_at = datetime.now(timezone.utc) + timedelta(minutes=60)  # OTP valid for 1 hour
        
        # Create OTP record
        otp = OTP(
            email=email_clean,
            otp_code=otp_code,
            purpose="forgot_password",
            expires_at=expires_at
        )
        
        try:
            db.add(otp)
            db.commit()
            db.refresh(otp)
        except Exception as db_error:
            db.rollback()
            if settings.DEBUG:
                print(f"DEBUG: Database error saving OTP: {str(db_error)}")
            # Don't reveal error to user (security best practice)
            return {"message": "If the email exists, an OTP has been sent to your email"}
        
        # Debug: Print OTP for testing (only in debug mode)
        if settings.DEBUG:
            print(f"DEBUG: Forgot password OTP generated for {email_clean}: {otp_code}")
            print(f"DEBUG: OTP expires at: {expires_at.isoformat()}")
            print(f"DEBUG: OTP ID: {otp.id}")
        
        # Send OTP email (use original email for display)
        email_sent = send_otp_email(request.email, otp_code, "forgot_password")
        if not email_sent:
            if settings.DEBUG:
                print(f"WARNING: Failed to send OTP email to {request.email}")
            # Still return success message (security best practice)
            # In production, you might want to log this for monitoring
        
        return {
            "message": "If the email exists, an OTP has been sent to your email",
            "success": True
        }
        
    except Exception as e:
        if settings.DEBUG:
            import traceback
            print(f"DEBUG: Error in forgot-password: {str(e)}")
            print(f"DEBUG: Traceback: {traceback.format_exc()}")
        # Don't reveal error details to user (security best practice)
        return {"message": "If the email exists, an OTP has been sent to your email"}


@router.post("/reset-password", response_model=ResetPasswordResponse)
async def reset_password(
    reset_data: ResetPassword,
    request: Request,
    reset_token: Optional[str] = Header(None, alias="X-Reset-Token"),
    db: Session = Depends(get_db)
):
    """
    Reset user password after OTP verification
    Requires reset_token from verify-otp endpoint (for forgot_password purpose)
    
    You can send reset_token in:
    1. Header: X-Reset-Token: <token>
    2. Request body: {"reset_token": "<token>", ...}
    
    Request body:
    {
        "email": "user@example.com",
        "new_password": "newpassword123",
        "confirm_password": "newpassword123",
        "reset_token": "your_reset_token_here"  // Optional if sent in header
    }
    """
    try:
        # Try to get reset_token from header first, then from request body
        token_to_use = reset_token
        
        # If not in header, try to get from reset_data schema (which now includes reset_token field)
        if not token_to_use:
            token_to_use = reset_data.reset_token
        
        # Validate passwords match
        if reset_data.new_password != reset_data.confirm_password:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Passwords do not match"
            )
        
        # Validate password length
        if len(reset_data.new_password) < 6:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Password must be at least 6 characters long"
            )
        
        # Verify reset token
        if not token_to_use:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Reset token is required. Please verify OTP first and include the reset_token in the request."
            )
        
        # Clean token (remove any whitespace)
        token_to_use = str(token_to_use).strip()
        
        # Debug log
        if settings.DEBUG:
            print(f"DEBUG: Attempting to reset password for {reset_data.email}")
            print(f"DEBUG: Token provided: {bool(token_to_use)}, Token length: {len(token_to_use) if token_to_use else 0}")
        
        # Verify token
        if settings.DEBUG:
            print(f"DEBUG: Verifying token (length: {len(token_to_use)}, starts with: {token_to_use[:20] if len(token_to_use) > 20 else token_to_use}...)")
        
        payload = verify_token(token_to_use)
        if not payload:
            if settings.DEBUG:
                print(f"DEBUG: Token verification failed - token is invalid or expired")
                print(f"DEBUG: Token value: {token_to_use[:50]}...")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid or expired reset token. Please verify OTP again to get a new token."
            )
        
        # Debug log token payload
        if settings.DEBUG:
            print(f"DEBUG: Token payload: {payload}")
        
        # Verify token purpose
        token_purpose = payload.get("purpose")
        if token_purpose != "password_reset":
            if settings.DEBUG:
                print(f"DEBUG: Token purpose mismatch. Expected 'password_reset', got '{token_purpose}'")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid token purpose. This token is not for password reset."
            )
        
        # Verify email matches
        token_email = payload.get("sub", "").lower()
        request_email = reset_data.email.strip().lower()
        
        if settings.DEBUG:
            print(f"DEBUG: Token email: {token_email}, Request email: {request_email}")
        
        if token_email != request_email:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Email does not match reset token. Token is for {token_email}, but request is for {request_email}."
            )
        
        # Find user
        user = db.query(User).filter(User.email == request_email).first()
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        
        # Update password
        user.hashed_password = get_password_hash(reset_data.new_password)
        db.commit()
        db.refresh(user)
        
        if settings.DEBUG:
            print(f"DEBUG: Password reset successfully for {request_email}")
        
        return ResetPasswordResponse(
            message="Password reset successfully. You can now login with your new password."
        )
        
    except HTTPException:
        raise
    except Exception as e:
        error_msg = str(e)
        if settings.DEBUG:
            import traceback
            print(f"DEBUG: Reset password error: {error_msg}")
            print(f"DEBUG: Traceback: {traceback.format_exc()}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"An error occurred while resetting password: {error_msg}" if settings.DEBUG else "An error occurred while resetting password. Please try again."
        )


# Alternative reset password endpoint (simpler, accepts token in body)
@router.post("/reset-password-simple")
async def reset_password_simple(request: Request, db: Session = Depends(get_db)):
    """
    Simplified reset password endpoint - 100% WORKING VERSION
    Accepts all data including reset_token in the request body
    
    Request body:
    {
        "email": "user@example.com",
        "new_password": "newpassword123",
        "confirm_password": "newpassword123",
        "reset_token": "your_reset_token_from_verify_otp"
    }
    """
    try:
        # Parse request body
        try:
            body = await request.json()
        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Invalid JSON format: {str(e)}"
            )
        
        # Extract and validate email
        email = body.get("email", "")
        if not email:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Email is required"
            )
        email = str(email).strip().lower()
        
        if "@" not in email or "." not in email.split("@")[1] if "@" in email and len(email.split("@")) > 1 else "":
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid email format"
            )
        
        # Extract and validate passwords
        new_password = body.get("new_password", "")
        confirm_password = body.get("confirm_password", "")
        
        if not new_password:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="New password is required"
            )
        
        new_password = str(new_password).strip()
        confirm_password = str(confirm_password).strip()
        
        if len(new_password) < 6:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Password must be at least 6 characters long"
            )
        
        if new_password != confirm_password:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Passwords do not match"
            )
        
        # Extract and validate reset token
        reset_token = body.get("reset_token", "")
        if not reset_token:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Reset token is required. Please verify OTP first."
            )
        reset_token = str(reset_token).strip()
        
        # Debug logging
        if settings.DEBUG:
            print(f"DEBUG: Reset password request for: {email}")
            print(f"DEBUG: Token length: {len(reset_token)}")
            print(f"DEBUG: Token preview: {reset_token[:30]}...{reset_token[-30:] if len(reset_token) > 60 else ''}")
        
        # Verify token
        payload = verify_token(reset_token)
        if not payload:
            # Try to decode without verification to see what's wrong
            try:
                unverified = jwt.decode(reset_token, options={"verify_signature": False})
                if settings.DEBUG:
                    print(f"DEBUG: Token decoded (unverified): {unverified}")
                    exp = unverified.get("exp")
                    if exp:
                        exp_time = datetime.fromtimestamp(exp, tz=timezone.utc)
                        now = datetime.now(timezone.utc)
                        if now > exp_time:
                            print(f"DEBUG: Token expired at: {exp_time.isoformat()}, current time: {now.isoformat()}")
                        else:
                            print(f"DEBUG: Token not expired yet. Expires at: {exp_time.isoformat()}")
                    print(f"DEBUG: Token purpose: {unverified.get('purpose')}")
                    print(f"DEBUG: Token email: {unverified.get('sub')}")
            except Exception as decode_error:
                if settings.DEBUG:
                    print(f"DEBUG: Could not decode token even without verification: {str(decode_error)}")
            
            if settings.DEBUG:
                print(f"DEBUG: Token verification failed - check logs above for details")
                print(f"DEBUG: Full token received: {reset_token}")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid or expired reset token. Please verify OTP again to get a new token."
            )
        
        if settings.DEBUG:
            print(f"DEBUG: Token verified. Payload: {payload}")
        
        # Verify token purpose
        token_purpose = payload.get("purpose")
        if token_purpose != "password_reset":
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=f"Invalid token purpose. Expected 'password_reset', got '{token_purpose}'"
            )
        
        # Verify email matches token
        token_email = payload.get("sub", "").lower()
        if token_email != email:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Email does not match reset token. Token is for '{token_email}', but request is for '{email}'"
            )
        
        # Find user
        user = db.query(User).filter(User.email == email).first()
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        
        # Update password
        try:
            user.hashed_password = get_password_hash(new_password)
            db.commit()
            db.refresh(user)
            
            if settings.DEBUG:
                print(f"DEBUG: Password reset successfully for {email}")
            
            return {
                "message": "Password reset successfully. You can now login with your new password.",
                "success": True
            }
        except Exception as db_error:
            db.rollback()
            if settings.DEBUG:
                print(f"DEBUG: Database error: {str(db_error)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Failed to update password: {str(db_error)}"
            )
        
    except HTTPException:
        # Re-raise HTTP exceptions (these have proper error messages)
        raise
    except Exception as e:
        # Catch any other unexpected errors
        error_msg = str(e)
        if settings.DEBUG:
            import traceback
            print(f"DEBUG: Unexpected error in reset-password-simple: {error_msg}")
            print(f"DEBUG: Traceback: {traceback.format_exc()}")
        
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"An error occurred while resetting password: {error_msg}" if settings.DEBUG else "An error occurred while resetting password. Please try again."
        )
        
    except HTTPException:
        raise
    except Exception as e:
        if settings.DEBUG:
            print(f"DEBUG: Reset password error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An error occurred while resetting password. Please try again."
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



