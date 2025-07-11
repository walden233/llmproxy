curl -X POST \
  http://localhost:8060/v1/models \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzUyMjE3OTQ4LCJleHAiOjIwNjc1Nzc5NDh9.q6_LdvuAJ5CaJlQwYpFAUdij7ERMAZRYNq9RoUMXF0w" \
  -H "Content-Type: application/json" \
  -d '{
    "displayName": "deepseek-v3",
    "apiKey": "sk-6bf312e5ab8f4c099439ba1a1080da8e",
    "modelIdentifier": "deepseek-chat",
    "urlBase": "https://api.deepseek.com/v1",
    "capabilities": ["text-to-text"],
    "priority": 5
  }'