#deepseek apikey获取: https://platform.deepseek.com/
curl -X POST \
  http://10.170.6.64:8060/v1/models \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzUyMjE3OTQ4LCJleHAiOjIwNjc1Nzc5NDh9.q6_LdvuAJ5CaJlQwYpFAUdij7ERMAZRYNq9RoUMXF0w" \
  -H "Content-Type: application/json" \
  -d '{
        "displayName": "deepseek-v3",
        "apiKey": "sk-eb8bf23e464147f19a8a16fbafa85ef7",
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
        "apiKey": "bc5b4115ed4804053abcd0f2d6f77c3e.jWT0d5kr7Ns6rodl",
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
        "apiKey": "bc5b4115ed4804053abcd0f2d6f77c3e.jWT0d5kr7Ns6rodl",
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
        "apiKey": "sk-3e093b89cfa34d11af175568251f9ffd",
        "modelIdentifier": "wanx2.1-imageedit",
        "urlBase": null,
        "capabilities": ["image-to-image"],
        "priority": 3
    }'