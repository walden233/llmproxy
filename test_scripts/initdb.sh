#deepseek apikey获取: https://platform.deepseek.com/
curl -X POST \
  http://10.170.6.64:8060/v1/models \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzUyMjE3OTQ4LCJleHAiOjIwNjc1Nzc5NDh9.q6_LdvuAJ5CaJlQwYpFAUdij7ERMAZRYNq9RoUMXF0w" \
  -H "Content-Type: application/json" \
  -d '{
        "displayName": "deepseek-v3",
        "apiKey": "",
        "modelIdentifier": "deepseek-chat",
        "urlBase": "https://api.deepseek.com/v1",
        "capabilities": ["text-to-text"],
        "priority": 5
    }'

#glm apikey获取: https://open.bigmodel.cn/
curl -X POST \
  http://10.170.6.64:8060/v1/models \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzUyMjE3OTQ4LCJleHAiOjIwNjc1Nzc5NDh9.q6_LdvuAJ5CaJlQwYpFAUdij7ERMAZRYNq9RoUMXF0w" \
  -H "Content-Type: application/json" \
  -d '{
        "displayName": "GLM-4V",
        "apiKey": "",
        "modelIdentifier": "glm-4v-plus-0111",
        "urlBase": "https://open.bigmodel.cn/api/paas/v4",
        "capabilities": ["text-to-text","image-to-text"],
        "priority": 6
    }'

curl -X POST \
  http://10.170.6.64:8060/v1/models \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzUyMjE3OTQ4LCJleHAiOjIwNjc1Nzc5NDh9.q6_LdvuAJ5CaJlQwYpFAUdij7ERMAZRYNq9RoUMXF0w" \
  -H "Content-Type: application/json" \
  -d '{
        "displayName": "GLM-4",
        "apiKey": "",
        "modelIdentifier": "glm-4-plus",
        "urlBase": "https://open.bigmodel.cn/api/paas/v4",
        "capabilities": ["text-to-text"],
        "priority": 6
    }'

#阿里百炼 apikey获取: https://bailian.console.aliyun.com/
curl -X POST \
  http://10.170.6.64:8060/v1/models \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzUyMjE3OTQ4LCJleHAiOjIwNjc1Nzc5NDh9.q6_LdvuAJ5CaJlQwYpFAUdij7ERMAZRYNq9RoUMXF0w" \
  -H "Content-Type: application/json" \
  -d '{
        "displayName": "ALI-WanX",
        "apiKey": "  ",
        "modelIdentifier": "wanx2.1-t2i-turbo",
        "urlBase": null,
        "capabilities": ["text-to-image"],
        "priority": 3
    }'

curl -X POST \
  http://10.170.6.64:8060/v1/models \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzUyMjE3OTQ4LCJleHAiOjIwNjc1Nzc5NDh9.q6_LdvuAJ5CaJlQwYpFAUdij7ERMAZRYNq9RoUMXF0w" \
  -H "Content-Type: application/json" \
  -d '{
        "displayName": "ALI-WanX",
        "apiKey": "",
        "modelIdentifier": "wanx2.1-imageedit",
        "urlBase": null,
        "capabilities": ["image-to-image"],
        "priority": 3
    }'