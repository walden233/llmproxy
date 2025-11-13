curl -X POST \
  http://localhost:8060/v1/orders \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0eXQiLCJpYXQiOjE3NjI5MzY3MzQsImV4cCI6MjA3ODI5NjczNH0.GJZBIP761H3LX-euVSzMTL9cdFI_xbnlkdd0hp5frD0" \
  -H "Content-Type: application/json" \
  -d '{
        "userId": 2,
        "amount": 100.1
    }'