curl -X POST \
  http://localhost:8060/v1/orders/pay-success \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzYwNDI3ODg5LCJleHAiOjIwNzU3ODc4ODl9.lkZvrv838pmIBYxYeusG8jCQXDuA-kG8Vb0xFTfZKp4" \
  -H "Content-Type: application/json" \
  -d '{
        "orderNo": "1749fac9e9d4496faf4befcb7d019abf"
    }'