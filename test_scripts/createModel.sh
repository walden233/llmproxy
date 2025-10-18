# curl -X POST \
#   http://localhost:8060/v1/models \
#   -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzYwNDI3ODg5LCJleHAiOjIwNzU3ODc4ODl9.lkZvrv838pmIBYxYeusG8jCQXDuA-kG8Vb0xFTfZKp4" \
#   -H "Content-Type: application/json" \
#   -d '{
#         "displayName": "deepseek-v3",
#         "modelIdentifier": "deepseek-chat",
#         "capabilities": ["text-to-text"],
#         "pricing":{"input":0.0001,"output":0.0001},
#         "providerName": "deepseek",
#         "urlBase": "https://api.deepseek.com/v1",
#         "apiKey": "",
#         "priority": 5
#     }'

curl -X POST \
  http://localhost:8060/v1/models \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJyb290IiwiaWF0IjoxNzYwNzgyMTYwLCJleHAiOjIwNzYxNDIxNjB9.AQiZenkbNjhww74BKmYoFdPL4WM6NWVSBnifWAVrY10" \
  -H "Content-Type: application/json" \
  -d '{
        "displayName": "qwen-image-plus",
        "modelIdentifier": "qwen-image-plus",
        "capabilities": ["text-to-image"],
        "pricing":{"output":0.2},
        "providerName": "Ali",
        "urlBase": "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation",
        "apiKey": "sk-2a8a028420b946b4b4bbbce178e554cf",
        "priority": 5
    }'