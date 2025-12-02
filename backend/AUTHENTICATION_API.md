# Authentication API Documentation

## Overview
Complete authentication system with JWT tokens, OTP verification via email, and password reset functionality.

## Database Models

### User Model
- `id`: Primary key
- `name`: User's full name
- `email`: Unique email address
- `hashed_password`: Bcrypt hashed password
- `is_active`: Account status
- `is_verified`: Email verification status
- `created_at`: Registration timestamp
- `updated_at`: Last update timestamp

### OTP Model
- `id`: Primary key
- `email`: User's email
- `otp_code`: 6-digit OTP code
- `purpose`: 'signup' or 'forgot_password'
- `is_used`: Whether OTP has been used
- `expires_at`: OTP expiration time (30 minutes)
- `created_at`: OTP creation timestamp

## API Endpoints

### 1. Register User
**POST** `/api/auth/register`

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "message": "Registration successful. Please check your email for OTP verification code.",
  "email": "john@example.com"
}
```

**Notes:**
- Creates user account (not verified)
- Generates 6-digit OTP
- Sends OTP to user's email
- OTP expires in 1 hour

---

### 2. Verify OTP
**POST** `/api/auth/verify-otp`

**Request Body:**
```json
{
  "email": "john@example.com",
  "otp_code": "123456",
  "purpose": "signup"
}
```

**Response (for signup):**
```json
{
  "message": "Email verified successfully",
  "verified": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (for forgot_password):**
```json
{
  "message": "OTP verified successfully. You can now reset your password.",
  "verified": true,
  "token": null,
  "reset_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Notes:**
- For signup: Verifies email and returns JWT token for immediate login
- For forgot_password: Returns reset_token (valid for 10 minutes) to use in reset-password endpoint
- OTP can only be used once
- OTP must not be expired
- Each purpose (signup/forgot_password) has different OTP handling

---

### 3. Login
**POST** `/api/auth/login`

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer",
  "user_id": 1,
  "email": "john@example.com",
  "name": "John Doe"
}
```

**Notes:**
- User must be verified (email confirmed)
- User must be active
- Returns JWT token valid for 30 minutes

---

### 4. Forgot Password
**POST** `/api/auth/forgot-password`

**Request Body:**
```json
{
  "email": "john@example.com"
}
```

**Response:**
```json
{
  "message": "If the email exists, an OTP has been sent"
}
```

**Notes:**
- Generates OTP for password reset
- Sends OTP to email
- Doesn't reveal if email exists (security)

---

### 5. Reset Password
**POST** `/api/auth/reset-password`

**Headers:**
```
X-Reset-Token: <reset_token_from_verify_otp>
```

**Request Body:**
```json
{
  "email": "john@example.com",
  "new_password": "newpassword123",
  "confirm_password": "newpassword123"
}
```

**Response:**
```json
{
  "message": "Password reset successfully. You can now login with your new password."
}
```

**Notes:**
- Requires reset_token from verify-otp endpoint (forgot_password purpose)
- Reset token expires in 30 minutes
- Passwords must match
- Email must match the token
- Updates user's password

---

### 6. Get Current User
**GET** `/api/auth/me`

**Headers:**
```
Authorization: Bearer <token>
```

**Response:**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "is_active": true,
  "is_verified": true,
  "created_at": "2024-01-01T00:00:00"
}
```

**Notes:**
- Requires valid JWT token
- Returns current authenticated user info

## Security Features

1. **Password Hashing**: Bcrypt with automatic salt
2. **JWT Tokens**: Secure token-based authentication
3. **OTP Expiration**: 30-minute expiration for OTPs
4. **Email Verification**: Required before login
5. **Token Expiration**: 30-minute JWT token validity

## Email Configuration

- **SMTP Host**: smtp.gmail.com
- **SMTP Port**: 587
- **Email**: tubemindai343@gmail.com
- **App Password**: Configured in settings

## Error Responses

All endpoints return appropriate HTTP status codes:
- `200`: Success
- `201`: Created
- `400`: Bad Request (validation errors)
- `401`: Unauthorized (invalid credentials)
- `403`: Forbidden (account inactive/unverified)
- `404`: Not Found
- `500`: Internal Server Error

## Testing

Use the interactive API docs at: `http://localhost:8000/docs`

Or use curl/Postman to test endpoints.

