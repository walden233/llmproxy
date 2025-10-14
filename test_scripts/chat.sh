curl -X POST \
  http://localhost:8060/v1/chat \
  -H "ACCESS-KEY: ak-ab4de06be56b463db9c9b07031029bf7" \
  -H "Content-Type: application/json" \
  -d '{
    "userMessage": "Hello!"
  }'