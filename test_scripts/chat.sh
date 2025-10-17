curl -X POST \
  http://localhost:8060/v1/chat \
  -H "ACCESS-KEY: ak-53364d1ae3d243409f36a52681ee89f4" \
  -H "Content-Type: application/json" \
  -d '{
    "userMessage": "Hello!"
  }'