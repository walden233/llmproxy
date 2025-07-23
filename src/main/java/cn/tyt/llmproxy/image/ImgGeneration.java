package cn.tyt.llmproxy.image;

// Copyright (c) Alibaba, Inc. and its affiliates.

import cn.tyt.llmproxy.dto.request.ImageGenerationRequest;
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

public class ImgGeneration {
    public static ImageGenerationResponse imageGenerate(ImageGenerationRequest request, String apiKey, LlmModel selectedModel, Map<String, Object> options) throws ApiException, NoApiKeyException {
//        String prompt = "一间有着精致窗户的花店，漂亮的木质门，摆放着花朵";
        if(options==null)
            options = new HashMap<>();
        if(!options.containsKey("n"))
            options.put("n",1);
        ImageSynthesisParam param =
                ImageSynthesisParam.builder()
                        .apiKey(apiKey)
                        .model(selectedModel.getModelIdentifier())
                        .prompt(request.getPrompt())
                        .parameters(options)
                        .build();
        ImageSynthesis imageSynthesis = new ImageSynthesis();
        ImageSynthesisResult result = null;
        try {
            System.out.println("---sync call, please wait a moment----");
            result = imageSynthesis.call(param);
        } catch (ApiException | NoApiKeyException e){
            throw new RuntimeException(e.getMessage());
        }

        if(result.getOutput()==null || result.getOutput().getResults()==null){
            throw new RuntimeException("远端图片生成失败");
        }
        List<String> outUrls = new ArrayList<>();
        for(Map<String, String> r :result.getOutput().getResults()){
            outUrls.add(r.get("url"));
        }
        return new ImageGenerationResponse(
                outUrls,
                result.getOutput().getResults().get(0).getOrDefault("actual_prompt",request.getPrompt()),
                selectedModel.getModelIdentifier()
        );
    }
    public static ImageGenerationResponse imageEdit(ImageGenerationRequest request, String apiKey, LlmModel selectedModel, Map<String, Object> options, String baseImageUrl) {
        // 设置parameters参数
//        String baseImage = "C:/java/model_service/llmproxy/doc/llmproxy_pytest/test_image3.jpg";
        if(options==null)
            options = new HashMap<>();
        if(!options.containsKey("n"))
            options.put("n",1);
        ImageSynthesisParam param =
                ImageSynthesisParam.builder()
                        .apiKey(apiKey)
                        .model(selectedModel.getModelIdentifier())//"wanx2.1-imageedit"
                        .function(ImageSynthesis.ImageEditFunction.DESCRIPTION_EDIT)
                        .prompt(request.getPrompt())
//                        .maskImageUrl(maskImageUrl)
                        .baseImageUrl(baseImageUrl)
                        .parameters(options)
                        .build();

        ImageSynthesis imageSynthesis = new ImageSynthesis();
        ImageSynthesisResult result = null;
        try {
            System.out.println("---sync call, please wait a moment----");
            result = imageSynthesis.call(param);
        } catch (ApiException | NoApiKeyException e){
            throw new RuntimeException(e.getMessage());
        }

        if(result.getOutput()==null || result.getOutput().getResults()==null){
            throw new RuntimeException("远端图片生成失败");
        }
        List<String> outUrls = new ArrayList<>();
        for(Map<String, String> r :result.getOutput().getResults()){
            outUrls.add(r.get("url"));
        }
        return new ImageGenerationResponse(
                outUrls,
                result.getOutput().getResults().get(0).getOrDefault("actual_prompt",request.getPrompt()),
                selectedModel.getModelIdentifier()
        );
    }
}