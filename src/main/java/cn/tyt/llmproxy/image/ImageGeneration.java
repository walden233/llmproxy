package cn.tyt.llmproxy.image;


import cn.tyt.llmproxy.dto.request.ImageGenerationRequest;
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

public class ImageGeneration {
    public static ImageGenerationResponse imageEdit(ImageGenerationRequest request, String apiKey, LlmModel selectedModel, Map<String, Object> options, String baseImageUrl){
        ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
        Dispatcher dispatcher = new Dispatcher();
        ArkService service = ArkService.builder().dispatcher(dispatcher).connectionPool(connectionPool).apiKey(apiKey).build();
        Double guidanceScale=5.5;
        if(options==null)
            options = new HashMap<>();
        if(options.containsKey("strength")){
            guidanceScale = ((Double) options.get("strength"))*9+1;
        }
        GenerateImagesRequest generateRequest = GenerateImagesRequest.builder()
                .model(selectedModel.getModelIdentifier())
                .prompt(request.getPrompt())
                .image(baseImageUrl)
                .watermark((Boolean) options.getOrDefault("watermark",false))
                .seed((Integer) options.getOrDefault("seed",-1))
                .guidanceScale(guidanceScale)
                .build();
        ImagesResponse imagesResponse;
        try {
            System.out.println("---sync call, please wait a moment----");
            imagesResponse = service.generateImages(generateRequest);
        } catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }

        if(imagesResponse.getError()!=null){
            throw new RuntimeException("远端图片生成失败:"+imagesResponse.getError().getMessage());
        }
        List<String> outUrls = new ArrayList<>();
        for(ImagesResponse.Image img :imagesResponse.getData()){
            outUrls.add(img.getUrl());
        }
        service.shutdownExecutor();
        return new ImageGenerationResponse(
                outUrls,
                request.getPrompt(),
                selectedModel.getModelIdentifier()
        );
    }
}
