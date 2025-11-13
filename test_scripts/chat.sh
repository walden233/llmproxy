curl -X POST \
  http://localhost:8060/v1/chat \
  -H "ACCESS-KEY: sk-f27d3372eeaf4f12909d0dbca0050640" \
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