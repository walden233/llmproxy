curl -X POST \
  http://10.170.6.64:8060/v1/chat \
  -H "ACCESS-KEY: ak-ab4de06be56b463db9c9b07031029bf7" \
  -H "Content-Type: application/json" \
  -d '{
    "userMessage": "你拍二",
    "history": [
        {
        "role": "user",
        "content": "你拍一"
        },
        {
        "role": "assistant",
        "content": "我不拍一"
        }
    ],
    "options": {
        "temperature": 0.7,
        "max_tokens": 512,
        "top_p": 0.9,
        "frequency_penalty": 0.0
    },
    "modelIdentifier": "glm-4-plus"
    }'