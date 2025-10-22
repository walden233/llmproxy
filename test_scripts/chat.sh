curl -X POST \
  http://localhost:8060/v1/chat \
  -H "ACCESS-KEY: ak-6b390271901d43069d32ab59a9983e1a" \
  -H "Content-Type: application/json" \
  -d '{
    "userMessage": "Hello!",
   "options": {
        "temperature": 1,
        "max_tokens": 512,
        "top_p": 0.9,
        "frequency_penalty": 0.0
    }
  }'