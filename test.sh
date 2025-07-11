curl -X POST \
http://localhost:8060/v1/auth/register \
-H 'Content-Type: application/json' \
-d '{ 
"username": "test", 
"password": "password", 
"email": "test@example.com" 
}'
