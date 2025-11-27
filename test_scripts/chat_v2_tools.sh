#!/usr/bin/env bash

# /v2/chat request that demonstrates tool invocation support.

curl -X POST \
  http://localhost:8060/v2/chat \
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
            "text": "You are an assistant that must consult tools when needed."
          }
        ]
      },
      {
        "role": "user",
        "content": [
          {
            "type": "text",
            "text": "查询一下今天上海的天气，然后告诉我是否适合户外跑步。"
          }
        ]
      }
    ],
    "tools": [
      {
        "type": "function",
        "function": {
          "name": "query_weather",
          "description": "Query the weather for a Chinese city and get temperature plus conditions.",
          "parameters": {
            "type": "object",
            "properties": {
              "city": {
                "type": "string",
                "description": "城市名，例如：上海"
              },
              "unit": {
                "type": "string",
                "enum": ["celsius", "fahrenheit"]
              }
            },
            "required": ["city"]
          }
        }
      }
    ],
    "temperature": 0.7,
    "top_p": 0.9,
    "max_tokens": 512
  }'
