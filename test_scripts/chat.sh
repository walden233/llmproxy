curl -X POST \
  http://localhost:8060/v1/chat \
  -H "ACCESS-KEY: ak-e971464388c34f31828fcb0dbcb64cc1" \
  -H "Content-Type: application/json" \
  -d '{
    "userMessage": "Hello!",
   "options": {
        "temperature": 0,
        "max_tokens": 512,
        "top_p": 0.9,
        "frequency_penalty": 0.0
    }
  }'