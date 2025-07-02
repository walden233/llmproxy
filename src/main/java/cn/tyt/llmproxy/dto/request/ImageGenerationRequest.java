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
    private String modelInternalId;
    private String modelIdentifier;

    // 可选：输出图像尺寸，例如 "1024x1024"
    private String size;

    // 支持图片输入
    private ImageInput originImage;

    // 可选：其他模型特定参数
    private Map<String, Object> options;
}