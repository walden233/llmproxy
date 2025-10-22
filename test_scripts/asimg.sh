curl -X POST \
  http://localhost:8060/v1/async/generate-image \
  -H "ACCESS-KEY: ak-6b390271901d43069d32ab59a9983e1a" \
  -H "Content-Type: application/json" \
  -d '{
      "prompt": "画蓝天",
      "options": {
          "size": "1664*928",
          "n": 1,
          "seed": 42,
          "prompt_extend": true,
          "watermark":false
        }
    }'