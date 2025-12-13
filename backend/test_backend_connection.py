#!/usr/bin/env python3
"""
Simple script to test if the backend is running and accessible.
Run this to verify your backend setup before testing the Android app.
"""

import requests
import sys

def test_backend(base_url="http://localhost:8000"):
    """Test backend connection and endpoints"""
    print(f"\n{'='*60}")
    print(f"Testing Backend Connection")
    print(f"{'='*60}\n")
    print(f"Base URL: {base_url}\n")
    
    # Test root endpoint
    print("1. Testing root endpoint (/)...")
    try:
        response = requests.get(f"{base_url}/", timeout=5)
        if response.status_code == 200:
            print(f"   [OK] Root endpoint working: {response.json()}")
        else:
            print(f"   [FAIL] Root endpoint returned: {response.status_code}")
    except requests.exceptions.ConnectionError:
        print(f"   [FAIL] Cannot connect to {base_url}")
        print(f"   -> Make sure backend is running!")
        print(f"   -> Run: uvicorn app.main:app --reload --host 0.0.0.0 --port 8000")
        return False
    except Exception as e:
        print(f"   [FAIL] Error: {e}")
        return False
    
    # Test health endpoint
    print("\n2. Testing health endpoint (/health)...")
    try:
        response = requests.get(f"{base_url}/health", timeout=5)
        if response.status_code == 200:
            print(f"   [OK] Health check passed: {response.json()}")
        else:
            print(f"   [FAIL] Health check returned: {response.status_code}")
    except Exception as e:
        print(f"   [FAIL] Error: {e}")
        return False
    
    # Test API docs
    print("\n3. Testing API docs (/docs)...")
    try:
        response = requests.get(f"{base_url}/docs", timeout=5)
        if response.status_code == 200:
            print(f"   [OK] API docs accessible")
        else:
            print(f"   [FAIL] API docs returned: {response.status_code}")
    except Exception as e:
        print(f"   [FAIL] Error: {e}")
    
    print(f"\n{'='*60}")
    print("[SUCCESS] Backend is running and accessible!")
    print(f"{'='*60}\n")
    print("Next steps:")
    print("1. Note your computer's IP address:")
    print("   - Windows: Run 'ipconfig' in CMD")
    print("   - Mac/Linux: Run 'ifconfig' or 'ip addr'")
    print("2. Update ApiConfig.java in Android app:")
    print("   - For emulator: http://10.0.2.2:8000/")
    print("   - For physical device: http://YOUR_IP:8000/")
    print(f"{'='*60}\n")
    
    return True

if __name__ == "__main__":
    # Allow custom URL as argument
    url = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8000"
    success = test_backend(url)
    sys.exit(0 if success else 1)

