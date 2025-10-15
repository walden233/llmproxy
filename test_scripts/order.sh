curl -X POST \
  http://localhost:8060/v1/orders \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzYwNDI3ODg5LCJleHAiOjIwNzU3ODc4ODl9.lkZvrv838pmIBYxYeusG8jCQXDuA-kG8Vb0xFTfZKp4" \
  -H "Content-Type: application/json" \
  -d '{
        "userId": 2,
        "amount": 5.5
    }'