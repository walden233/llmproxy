import os
import json
import base64

def extract_images_from_json(json_file):
    with open(json_file, 'r', encoding='utf-8') as f:
        try:
            data = json.load(f)
            
        except json.JSONDecodeError:
            print(f"跳过无效的 JSON 文件: {json_file}")
            return

    images = data.get("images")
    if not isinstance(images, list):
        print(f"文件 {json_file} 中未找到有效的 images 字段")
        return

    for idx, img in enumerate(images):
        b64_data = img.get("base64")
        if b64_data:
            try:
                img_data = base64.b64decode(b64_data)
                output_filename = f"{os.path.splitext(json_file)[0]}_image_{idx + 1}.png"
                with open(output_filename, 'wb') as out_file:
                    out_file.write(img_data)
                print(f"已保存图片: {output_filename}")
            except base64.binascii.Error:
                print(f"文件 {json_file} 中第 {idx + 1} 张图片 base64 解码失败")
        else:
            print(f"文件 {json_file} 中第 {idx + 1} 张图片没有 base64 字段")


if __name__ == "__main__":
    extract_images_from_json("llmproxy\doc\llmproxy_pytest\llmproxy-info.json")
