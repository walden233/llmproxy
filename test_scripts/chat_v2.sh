#!/usr/bin/env bash

# Basic sample for hitting the /v2/chat endpoint with an OpenAI-style payload.

curl -X POST \
  http://localhost:8060/v1/v2/chat \
  -H "ACCESS-KEY: sk-f27d3372eeaf4f12909d0dbca0050640" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "glm-4-plus",
    "messages": [
      {
        "role": "system",
        "content": [
          {
            "type": "text",
            "text": "You are a concise assistant."
          }
        ]
      },
      {
        "role": "user",
        "content": [
          {
            "type": "text",
            "text": "Hello from the /v2/chat test script!"
          }
        ]
      }
    ],
    "temperature": 0.7,
    "top_p": 0.9,
    "max_tokens": 256
  }'
