# Network Setup for Physical Device Testing

## Problem
When using a physical Android device connected via USB, you need to use your computer's IP address instead of `10.0.2.2` (which only works for emulator).

## Solution

### Step 1: Find Your Computer's IP Address

**Windows:**
```bash
ipconfig
```
Look for "IPv4 Address" under your active network adapter (Wi-Fi or Ethernet).

**Mac/Linux:**
```bash
ifconfig
# or
ip addr
```

### Step 2: Update API Configuration

The IP address has been automatically detected and updated in:
`app/src/main/java/com/example/tubemindai/api/ApiConfig.java`

Current IP: **10.94.179.99**

If this doesn't work, manually update the `BASE_URL` in `ApiConfig.java`:
```java
public static final String BASE_URL = "http://YOUR_IP_ADDRESS:8000/";
```

### Step 3: Ensure Backend is Running on Network Interface

Make sure your backend is running with `--host 0.0.0.0` to accept connections from other devices:

```bash
cd backend
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### Step 4: Check Windows Firewall

Windows Firewall might be blocking port 8000. To allow it:

1. Open Windows Defender Firewall
2. Click "Advanced settings"
3. Click "Inbound Rules" → "New Rule"
4. Select "Port" → Next
5. Select "TCP" and enter port "8000"
6. Allow the connection
7. Apply to all profiles

Or run this command in PowerShell (as Administrator):
```powershell
New-NetFirewallRule -DisplayName "Allow FastAPI Port 8000" -Direction Inbound -LocalPort 8000 -Protocol TCP -Action Allow
```

### Step 5: Ensure Same Network

Make sure your phone and computer are on the **same Wi-Fi network**.

### Step 6: Test Connection

1. On your phone's browser, try accessing: `http://10.94.179.99:8000/health`
2. You should see: `{"status":"healthy"}`
3. If this works, the Android app should also work

## Troubleshooting

### Still getting network errors?

1. **Check if backend is running:**
   ```bash
   # In browser on computer, try:
   http://localhost:8000/health
   ```

2. **Check if phone can reach computer:**
   - Use phone's browser to access: `http://10.94.179.99:8000/health`
   - If this fails, check firewall settings

3. **Verify IP address hasn't changed:**
   - IP addresses can change when reconnecting to Wi-Fi
   - Run `ipconfig` again to get current IP

4. **Try using USB tethering:**
   - Enable USB tethering on your phone
   - This creates a direct connection between phone and computer
   - Use the IP address shown in network adapter settings

5. **Check backend logs:**
   - Look for connection attempts in the backend console
   - If you see connection attempts but errors, it's a CORS or authentication issue
   - If no connection attempts, it's a network/firewall issue

## Quick Test

Run this on your computer to verify backend is accessible:
```bash
curl http://10.94.179.99:8000/health
```

If this works, your Android app should also work!

