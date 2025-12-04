from pydantic import BaseModel, EmailStr, Field, field_validator
from typing import Optional
from datetime import datetime


# Registration Schemas
class UserRegister(BaseModel):
    name: str = Field(..., min_length=2, max_length=100, description="User's full name", examples=["John Doe"])
    email: EmailStr = Field(..., description="User email address", examples=["john@example.com"])
    password: str = Field(..., min_length=6, description="User password (minimum 6 characters)", examples=["password123"])
    
    class Config:
        json_schema_extra = {
            "example": {
                "name": "John Doe",
                "email": "john@example.com",
                "password": "password123"
            }
        }


class UserRegisterResponse(BaseModel):
    message: str
    email: str

    class Config:
        from_attributes = True


# Login Schemas
class UserLogin(BaseModel):
    email: str = Field(..., description="User email address", examples=["user@example.com"])
    password: str = Field(..., min_length=1, description="User password", examples=["password123"])
    
    class Config:
        json_schema_extra = {
            "example": {
                "email": "user@example.com",
                "password": "password123"
            }
        }
        
        # Make validation more lenient
        str_strip_whitespace = True


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: int
    email: str
    name: str


# OTP Schemas
class OTPRequest(BaseModel):
    email: EmailStr
    purpose: str = Field(..., description="OTP purpose: 'signup' or 'forgot_password'")
    
    @field_validator('purpose', mode='before')
    @classmethod
    def normalize_purpose(cls, v: str) -> str:
        """Normalize purpose value to handle common typos"""
        v = str(v).strip().lower()
        # Handle common typo: "forget_password" -> "forgot_password"
        if v == "forget_password":
            return "forgot_password"
        # Accept valid values
        if v in ["signup", "forgot_password"]:
            return v
        # Raise error for invalid values
        raise ValueError("Purpose must be either 'signup' or 'forgot_password' (you can also use 'forget_password')")


class OTPVerify(BaseModel):
    email: EmailStr = Field(..., description="User email address", examples=["user@example.com"])
    otp_code: str = Field(..., min_length=4, max_length=6, description="OTP code (4-6 digits)", examples=["123456"])
    purpose: str = Field(..., description="OTP purpose: 'signup' or 'forgot_password' (also accepts 'forget_password')", examples=["signup"])
    
    @field_validator('purpose', mode='before')
    @classmethod
    def normalize_purpose(cls, v: str) -> str:
        """Normalize purpose value to handle common typos"""
        v = str(v).strip().lower()
        # Handle common typo: "forget_password" -> "forgot_password"
        if v == "forget_password":
            return "forgot_password"
        # Accept valid values
        if v in ["signup", "forgot_password"]:
            return v
        # Raise error for invalid values
        raise ValueError("Purpose must be either 'signup' or 'forgot_password' (you can also use 'forget_password')")
    
    class Config:
        json_schema_extra = {
            "example": {
                "email": "user@example.com",
                "otp_code": "123456",
                "purpose": "signup"
            }
        }


class OTPVerifyResponse(BaseModel):
    message: str
    verified: bool
    token: Optional[str] = None  # JWT token for signup
    reset_token: Optional[str] = None  # Reset token for forgot_password


# Password Reset Schemas
class ForgotPasswordRequest(BaseModel):
    email: EmailStr


class ResetPassword(BaseModel):
    email: EmailStr
    new_password: str = Field(..., min_length=6)
    confirm_password: str = Field(..., min_length=6)
    reset_token: Optional[str] = Field(None, description="Reset token from verify-otp endpoint (can also be sent in X-Reset-Token header)")
    
    class Config:
        json_schema_extra = {
            "example": {
                "email": "user@example.com",
                "new_password": "newpassword123",
                "confirm_password": "newpassword123",
                "reset_token": "your_reset_token_here"
            }
        }


class ResetPasswordResponse(BaseModel):
    message: str


# User Response
class UserResponse(BaseModel):
    id: int
    name: str
    email: str
    is_active: bool
    is_verified: bool
    created_at: datetime

    class Config:
        from_attributes = True

