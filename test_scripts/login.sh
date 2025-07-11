curl -X POST \
  http://localhost:8060/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "test",
    "password": "password"
  }'
