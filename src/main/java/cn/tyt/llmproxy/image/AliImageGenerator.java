// src/main/java/cn/tyt/llmproxy/image/AlibabaImageGenerator.java
package cn.tyt.llmproxy.image;

import cn.tyt.llmproxy.dto.request.ImageGenerationRequest;
import cn.tyt.llmproxy.dto.request.ImageInput;
import cn.tyt.llmproxy.dto.response.ImageGenerationResponse;
import cn.tyt.llmproxy.entity.LlmModel;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AliImageGenerator implements ImageGeneratorService {

    private final String modelId;
    private final String apiKey;

    public AliImageGenerator(String modelId, String apiKey) {
        this.modelId = modelId;
        this.apiKey = apiKey;
    }

    @Override
    public ImageGenerationResponse generateImage(ImageGenerationRequest request) {
        Map<String, Object> options = request.getOptions();
        if (options == null) {
            options = new HashMap<>();
        }
        options.putIfAbsent("n", 1);

        ImageSynthesisParam param = ImageSynthesisParam.builder()
                .apiKey(this.apiKey)
                .model(this.modelId)
                .prompt(request.getPrompt())
                .parameters(options)
                .build();

        return callApi(param, request.getPrompt());
    }

    @Override
    public ImageGenerationResponse editImage(ImageGenerationRequest request) {
        Map<String, Object> options = request.getOptions();
        ImageInput img = request.getOriginImage();
        if(img==null)
            throw new IllegalArgumentException("原始图像为空");
        String baseImageUrl = img.getUrl();
        if (options == null) {
            options = new HashMap<>();
        }
        options.putIfAbsent("n", 1);

        ImageSynthesisParam param = ImageSynthesisParam.builder()
                .apiKey(this.apiKey)
                .model(this.modelId)
                .function(ImageSynthesis.ImageEditFunction.DESCRIPTION_EDIT)
                .prompt(request.getPrompt())
                .baseImageUrl(baseImageUrl)
                .parameters(options)
                .build();

        return callApi(param, request.getPrompt());
    }

    private ImageGenerationResponse callApi(ImageSynthesisParam param, String originalPrompt) {
        ImageSynthesis imageSynthesis = new ImageSynthesis();
        ImageSynthesisResult result;
        try {
            System.out.println("--- [Alibaba] Sync call, please wait a moment ----");
            result = imageSynthesis.call(param);
        } catch (ApiException | NoApiKeyException e) {
            throw new RuntimeException("Alibaba API call failed: " + e.getMessage(), e);
        }

        if (result.getOutput() == null || result.getOutput().getResults() == null) {
            throw new RuntimeException("[Alibaba] 远端图片生成失败");
        }

        List<String> outUrls = new ArrayList<>();
        for (Map<String, String> r : result.getOutput().getResults()) {
            outUrls.add(r.get("url"));
        }

        String actualPrompt = result.getOutput().getResults().get(0).getOrDefault("actual_prompt", originalPrompt);

        return new ImageGenerationResponse(
                outUrls,
                actualPrompt,
                this.modelId
        );
    }

    @Override
    public void close() {
    }
}