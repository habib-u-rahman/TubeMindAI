from pydantic import BaseModel, EmailStr, Field
from typing import Optional
from datetime import datetime


# Registration Schemas
class UserRegister(BaseModel):
    name: str = Field(..., min_length=2, max_length=100)
    email: EmailStr
    password: str = Field(..., min_length=6)


class UserRegisterResponse(BaseModel):
    message: str
    email: str

    class Config:
        from_attributes = True


# Login Schemas
class UserLogin(BaseModel):
    email: EmailStr
    password: str


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: int
    email: str
    name: str


# OTP Schemas
class OTPRequest(BaseModel):
    email: EmailStr
    purpose: str = Field(..., pattern="^(signup|forgot_password)$")


class OTPVerify(BaseModel):
    email: EmailStr
    otp_code: str = Field(..., min_length=4, max_length=6)
    purpose: str = Field(..., pattern="^(signup|forgot_password)$")


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

