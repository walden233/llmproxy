curl -X POST \
  http://localhost:8060/v1/orders/pay-success \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzYwNjA0ODExLCJleHAiOjIwNzU5NjQ4MTF9.qeqKAfT5WT60-kWiafv0_Qygtz9OQAp3Fw3PdhuE5JI" \
  -H "Content-Type: application/json" \
  -d '{
        "orderNo": "a830e6fe60974f18bc71145ccc5ea4bd"
    }'