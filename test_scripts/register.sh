curl -X POST \
http://localhost:8060/v1/auth/register \
-H 'Content-Type: application/json' \
-d '{ 
"username": "tyt", 
"password": "password", 
"email": "tyt@example.com" 
}'
