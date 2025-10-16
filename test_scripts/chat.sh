curl -X POST \
  http://localhost:8060/v1/chat \
  -H "ACCESS-KEY: ak-f6ffaca2459c4430a5e5d8b7e94274b9" \
  -H "Content-Type: application/json" \
  -d '{
    "userMessage": "Hello!"
  }'