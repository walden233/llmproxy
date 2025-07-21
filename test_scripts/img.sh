curl -X POST \
  http://10.170.6.64:8060/v1/generate-image \
  -H "ACCESS-KEY: ak-ab4de06be56b463db9c9b07031029bf7" \
  -H "Content-Type: application/json" \
  -d '{
      "prompt": "画蓝天",
      "options": {
          "size": "1400*720",
          "n": 2,
          "seed": 42,
          "prompt_extend": true,
          "watermark":false
        }
    }'