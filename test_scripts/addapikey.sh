curl -X POST \
  http://localhost:8060/v1/providers/keys \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzYwNDI3ODg5LCJleHAiOjIwNzU3ODc4ODl9.lkZvrv838pmIBYxYeusG8jCQXDuA-kG8Vb0xFTfZKp4" \
  -H "Content-Type: application/json" \
  -d '{
        "providerName": "deepseek",
        "apiKey": "sk-aa8560306b8a41d5824a4cc2fe82f49f"
    }'