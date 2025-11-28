"""
Health check script for the embedding service
"""

import requests
import sys

def check_health():
    """Check if the embedding service is healthy"""
    try:
        response = requests.get("http://localhost:8000/health", timeout=5)
        if response.status_code == 200:
            return 0
        else:
            return 1
    except Exception:
        return 1

if __name__ == "__main__":
    sys.exit(check_health())

