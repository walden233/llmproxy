curl -X POST \
  http://10.170.6.64:8060/v1/generate-image \
  -H "ACCESS-KEY: ak-ab4de06be56b463db9c9b07031029bf7" \
  -H "Content-Type: application/json" \
  -d '{
        "prompt": "把花的颜色改成黄色",
        "originImage": {
          "url": "https://pics2.baidu.com/feed/5bafa40f4bfbfbed03490449fad23538afc31fa0.jpeg"
        },
        "options": {
            "n": 2,
            "seed": 42,
            "prompt_extend": true,
            "watermark":false,
            "strength":1
        }
    }'