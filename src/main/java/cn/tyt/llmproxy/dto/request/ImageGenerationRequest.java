package cn.tyt.llmproxy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ImageGenerationRequest {
    @NotBlank(message = "图像描述不能为空")
    private String prompt;

    // 可选：指定使用哪个文生图模型
    private Integer modelInternalId;
    private String modelIdentifier;

    // 可选：支持图片输入
    private ImageInput originImage;

    // 可选：其他模型特定参数
//    {
//        "size": "1024x1024",
//        "n": 2,
//        "seed": 42,
//        "prompt_extend": true,
//        "watermark":false,
//        "strength":0.5
//    }
    private Map<String, Object> options;
}