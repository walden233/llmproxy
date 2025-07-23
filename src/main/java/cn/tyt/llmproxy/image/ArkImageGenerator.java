// src/main/java/cn/tyt/llmproxy/image/VolcengineImageGenerator.java
package cn.tyt.llmproxy.image;

import cn.tyt.llmproxy.dto.request.ImageGenerationRequest;
import cn.tyt.llmproxy.dto.request.ImageInput;
import cn.tyt.llmproxy.dto.response.ImageGenerationResponse;
import cn.tyt.llmproxy.entity.LlmModel;
import com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest;
import com.volcengine.ark.runtime.model.images.generation.ImagesResponse;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ArkImageGenerator implements ImageGeneratorService {

    // ConnectionPool and Dispatcher can be shared across all instances for efficiency.
    private static final ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.MINUTES);
    private static final Dispatcher dispatcher = new Dispatcher();

    private final String modelId;
    private final ArkService service;

    public ArkImageGenerator(String modelId, String apiKey) {
        this.modelId = modelId;
        this.service = ArkService.builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .apiKey(apiKey)
                .build();
    }

    @Override
    public ImageGenerationResponse generateImage(ImageGenerationRequest request) {
        Map<String, Object> options = request.getOptions();
        GenerateImagesRequest.Builder requestBuilder = createBaseRequestBuilder(request, options);
        return callApi(requestBuilder.build(), request.getPrompt());
    }

    @Override
    public ImageGenerationResponse editImage(ImageGenerationRequest request) {
        Map<String, Object> options = request.getOptions();
        ImageInput img = request.getOriginImage();
        if(img==null || (img.getBase64()==null && img.getUrl()==null))
            throw new IllegalArgumentException("原始图像为空");

        String baseImageUrl;
        if(img.getUrl()!=null)
            baseImageUrl = img.getUrl();
        else {
            baseImageUrl = img.getBase64();
            baseImageUrl = formatBase64(baseImageUrl);
        }

        GenerateImagesRequest.Builder requestBuilder = createBaseRequestBuilder(request, options);
        requestBuilder.image(baseImageUrl); // Add the base image for editing
        return callApi(requestBuilder.build(), request.getPrompt());
    }
    private String formatBase64(String base64Data) {
        StringBuilder sb=new StringBuilder("data:");
        sb.append(getMimeType(base64Data));
        sb.append(";base64,");
        sb.append(base64Data);
        return sb.toString();
    }
    private String getMimeType(String base64Data) {
        if (base64Data.startsWith("/9j/")) {
            return "image/jpeg";
        } else if (base64Data.startsWith("iVBORw0KGgo")) {
            return "image/png";
        }
        // 默认或未知
        throw new IllegalArgumentException("base64格式错误:"+base64Data.substring(0,30)+"...");
    }

    private GenerateImagesRequest.Builder createBaseRequestBuilder(ImageGenerationRequest request, Map<String, Object> options) {
        if (options == null) {
            options = new HashMap<>();
        }

        double guidanceScale = 5.5;
        if (options.containsKey("strength")) {
            guidanceScale = ((Number) options.get("strength")).doubleValue() * 9.0 + 1.0;
        }

        return GenerateImagesRequest.builder()
                .model(this.modelId)
                .prompt(request.getPrompt())
                .watermark((Boolean) options.getOrDefault("watermark", false))
                .seed((Integer) options.getOrDefault("seed", -1))
                .guidanceScale(guidanceScale);
    }

    private ImageGenerationResponse callApi(GenerateImagesRequest generateRequest, String originalPrompt) {
        ImagesResponse imagesResponse;
        try {
            System.out.println("--- [Volcengine] Sync call, please wait a moment ----");
            imagesResponse = service.generateImages(generateRequest);
        } catch (Exception e) {
            throw new RuntimeException("Volcengine API call failed: " + e.getMessage(), e);
        }

        if (imagesResponse.getError() != null) {
            throw new RuntimeException("[Volcengine] 远端图片生成失败: " + imagesResponse.getError().getMessage());
        }

        List<String> outUrls = new ArrayList<>();
        for (ImagesResponse.Image img : imagesResponse.getData()) {
            outUrls.add(img.getUrl());
        }

        return new ImageGenerationResponse(
                outUrls,
                originalPrompt, // Volcengine does not return an "actual_prompt"
                this.modelId
        );
    }

    @Override
    public void close() {
        // Properly shut down the executor when the service is no longer needed.
        if (this.service != null) {
            this.service.shutdownExecutor();
        }
    }
}