# Signup and Login Flow

## Complete Flow

### 1. User Registration (Signup)
**Endpoint:** `POST /api/auth/register`

**Request:**
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

**What happens:**
- User account is created with `is_verified = false`
- OTP code is generated and saved
- OTP is sent to user's email
- User cannot login yet (email not verified)

---

### 2. OTP Verification (Signup)
**Endpoint:** `POST /api/auth/verify-otp`

**Request:**
```json
{
  "email": "john@example.com",
  "otp_code": "123456",
  "purpose": "signup"
}
```

**Response:**
```json
{
  "message": "Email verified successfully. You are now logged in.",
  "verified": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "reset_token": null
}
```

**What happens:**
- OTP code is verified
- User's email is marked as verified (`is_verified = true`)
- JWT token is returned for immediate login
- User can now login with email and password

---

### 3. Login
**Endpoint:** `POST /api/auth/login`

**Request:**
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

**What happens:**
- Email and password are verified
- User must be verified (OTP verified)
- JWT token is returned for API access

---

## Important Notes

1. **User must register first** - Creates account with `is_verified = false`
2. **OTP verification is required** - User must verify email via OTP before login
3. **Login only works for verified users** - `is_verified = true` required
4. **OTP is for registered users only** - OTP verification checks if user exists

## Error Cases

- **User not found during OTP verification:** "User not found. Please register first."
- **Email already verified:** "Email is already verified. You can login directly."
- **Login without verification:** "Please verify your email first. Check your email for OTP code."

