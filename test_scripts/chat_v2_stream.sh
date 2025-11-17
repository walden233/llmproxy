#!/usr/bin/env bash

# Sample for consuming the /v2/chat/stream endpoint (Server-Sent Events).

curl -N -X POST \
  http://localhost:8060/v1/v2/chat/stream \
  -H "ACCESS-KEY: sk-f27d3372eeaf4f12909d0dbca0050640" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "glm-4-plus",
    "stream": true,
    "messages": [
      {
        "role": "system",
        "content": [
          {
            "type": "text",
            "text": "You are a streaming assistant."
          }
        ]
      },
      {
        "role": "user",
        "content": [
          {
            "type": "text",
            "text": "请通过 SSE 流式回答这条消息。"
          }
        ]
      }
    ],
    "temperature": 0.5,
    "top_p": 0.9,
    "max_tokens": 512
  }'
