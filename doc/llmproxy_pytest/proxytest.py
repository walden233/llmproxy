import requests
import json
import base64
import os
from PIL import Image, ImageDraw, ImageFont

# --- Configuration ---
# Adjust these values to match your environment
BASE_URL = "http://localhost:8080"  # Your Spring Boot application's address
LOGIN_ENDPOINT = "/v1/auth/login"
CHAT_ENDPOINT = "/v1/chat"

# Credentials from your Java test setup
USERNAME = "test"
PASSWORD = "password"

# --- Test Data ---
# The script will create this image if it doesn't exist.
# You can also replace this with the path to your own image.
IMAGE_PATH = "test_image2.jpg" 
USER_MESSAGE = "这张图片里有什么内容？" # Your custom message to the model
# The model identifier you want to test with (from your Java test)
MODEL_IDENTIFIER = " " # Example: "glm-4v-plus-0111"

def get_auth_token(base_url: str, username: str, password: str) -> str | None:
    """Logs in to the application and returns the JWT token."""
    login_url = f"{base_url}{LOGIN_ENDPOINT}"
    login_payload = {
        "username": username,
        "password": password
    }
    headers = {
        "Content-Type": "application/json"
    }
    
    print(f"Attempting to log in as '{username}' at {login_url}...")
    
    try:
        response = requests.post(login_url, headers=headers, json=login_payload)
        response.raise_for_status()  # Raises an HTTPError for bad responses (4xx or 5xx)
        
        response_data = response.json()
        
        # Based on your Result<T> structure
        if response_data.get("code") == 200:
            token = response_data.get("data", {}).get("token")
            if token:
                print("Login successful. Token received.")
                return token
            else:
                print("Login failed: 'token' not found in response data.")
                return None
        else:
            print(f"Login failed: API returned code {response_data.get('code')} with message '{response_data.get('message')}'")
            return None
            
    except requests.exceptions.RequestException as e:
        print(f"An error occurred during login: {e}")
        return None

def call_chat_api_with_image(base_url: str, token: str):
    """Constructs the request and calls the /chat endpoint with an image."""
    chat_url = f"{base_url}{CHAT_ENDPOINT}"
    
    # 1. Encode the local image to Base64
    with open(IMAGE_PATH, "rb") as image_file:
        base64_image = base64.b64encode(image_file.read()).decode('utf-8')

    # 2. Construct the payload based on ChatRequest_dto
    chat_payload = {
        "userMessage": USER_MESSAGE,
        "modelIdentifier": MODEL_IDENTIFIER,
        "history": [], # You can add history here if needed
        "options": {},   # You can add options like temperature here
        "images": [
            {
                "base64": base64_image,
                #"base64": f"data:image/jpeg;base64,{base64_image}", #有这串前缀也行
                #"url": "https://i0.hdslb.com/bfs/archive/fbcca754eadc47994aaaa0964a7ddf366cd8033a.png", 
                "url": None,
                "role": "user_upload"
            }
        ]
    }

    # 3. Prepare headers with the auth token
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}"
    }

    print("\nSending request to /chat endpoint with the following payload (image data is truncated):")
    #print_payload["images"][0]["base64"] = print_payload["images"][0]["base64"][:30] + "..."
    print(json.dumps(chat_payload, indent=2, ensure_ascii=False))

    # 4. Make the API call
    try:
        response = requests.post(chat_url, headers=headers, json=chat_payload)
        response.raise_for_status()
        
        print("\n--- Full Response from /chat ---")
        # Use .json() to automatically parse the JSON response
        print(json.dumps(response.json(), indent=2, ensure_ascii=False))

    except requests.exceptions.RequestException as e:
        print(f"\nAn error occurred while calling the chat API: {e}")
        if e.response is not None:
            print(f"Response Body: {e.response.text}")

if __name__ == "__main__":

    # Step 1: Get the authentication token
    auth_token = get_auth_token(BASE_URL, USERNAME, PASSWORD)
    
    # Step 2: If login was successful, call the chat API
    if auth_token:
        call_chat_api_with_image(BASE_URL, auth_token)
    else:
        print("\nCould not proceed to chat API call due to login failure.")