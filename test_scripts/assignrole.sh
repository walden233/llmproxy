curl -X POST \
  http://localhost:8060/v1/auth/assign-role \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJyb290IiwiaWF0IjoxNzYyOTM0MzE4LCJleHAiOjIwNzgyOTQzMTh9.cpFdk3CumGJXOjDrmWcOVMLfhMdnHtzu-moe5e5oTrk" \
  -H "Content-Type: application/json" \
  -d '{
        "userId": 2,
        "role": "ROLE_ROOT_ADMIN"
    }'