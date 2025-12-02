# API Testing Guide

## Common Issues and Solutions

### JSON Decode Error

If you get a JSON decode error, check:

1. **Content-Type Header**: Make sure you're sending `Content-Type: application/json`
2. **Valid JSON**: Ensure your JSON is properly formatted
3. **No Special Characters**: Remove any control characters or invalid characters

### Example Valid Request (Register)

**Using curl:**
```bash
curl -X POST "http://localhost:8000/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123"
  }'
```

**Using Python requests:**
```python
import requests

url = "http://localhost:8000/api/auth/register"
data = {
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123"
}
response = requests.post(url, json=data)
print(response.json())
```

**Using Postman:**
1. Set method to POST
2. URL: `http://localhost:8000/api/auth/register`
3. Headers: `Content-Type: application/json`
4. Body: Select "raw" and "JSON", then paste:
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

### Common JSON Errors

1. **Trailing commas**: `{"name": "John",}` ❌
2. **Missing quotes**: `{name: "John"}` ❌
3. **Control characters**: Remove any `\n`, `\t`, etc. in string values
4. **Invalid escape sequences**: Use `\\` for backslash

### Testing OTP Verification

After registration, check the console for the OTP code (if DEBUG=True), then:

```bash
curl -X POST "http://localhost:8000/api/auth/verify-otp" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "otp_code": "123456",
    "purpose": "signup"
  }'
```

