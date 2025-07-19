curl -X POST \
  http://10.170.6.64:8060/v1/chat \
  -H "ACCESS-KEY: ak-" \
  -H "Content-Type: application/json" \
  -d '{
    "userMessage": "Hello!"
  }'