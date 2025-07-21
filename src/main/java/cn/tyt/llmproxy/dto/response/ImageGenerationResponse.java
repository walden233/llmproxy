package cn.tyt.llmproxy.dto.response; 

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationResponse {
    private List<String> imageUrls; // 生成图像的URL
    private String actualPrompt; // 模型可能修改过的 prompt
    private String usedModelIdentifier;
    // 可以是 Base64 编码的图像数据，如果模型直接返回图像字节
    // private String imageBase64Data;
}