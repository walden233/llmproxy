curl -X POST \
  http://localhost:8060/v1/models \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzYwNDI3ODg5LCJleHAiOjIwNzU3ODc4ODl9.lkZvrv838pmIBYxYeusG8jCQXDuA-kG8Vb0xFTfZKp4" \
  -H "Content-Type: application/json" \
  -d '{
        "displayName": "deepseek-v3",
        "modelIdentifier": "deepseek-chat",
        "capabilities": ["text-to-text"],
        "pricing":{"input":0.0001,"output":0.0001},
        "providerName": "deepseek",
        "urlBase": "https://api.deepseek.com/v1",
        "apiKey": "",
        "priority": 5
    }'