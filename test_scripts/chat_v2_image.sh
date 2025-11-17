#!/usr/bin/env bash

# /v2/chat sample where the user message mixes text and an inline image.

curl -X POST \
  http://localhost:8060/v1/v2/chat \
  -H "ACCESS-KEY: sk-f27d3372eeaf4f12909d0dbca0050640" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "glm-4v-plus",
    "messages": [
      {
        "role": "system",
        "content": [
          {
            "type": "text",
            "text": "You are a vision assistant that describes images in Chinese."
          }
        ]
      },
      {
        "role": "user",
        "content": [
          {
            "type": "text",
            "text": "这张图片里有什么？给我一段简短描述。"
          },
          {
            "type": "image_url",
            "image_url": {
              "url": "https://pics2.baidu.com/feed/5bafa40f4bfbfbed03490449fad23538afc31fa0.jpeg"
            }
          }
        ]
      }
    ],
    "temperature": 0.2,
    "top_p": 0.95,
    "max_tokens": 512
  }'
