package cn.tyt.llmproxy.common.utils;

// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisListResult;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.task.AsyncTaskListParam;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;

import java.util.HashMap;
import java.util.Map;

public class ImgGeneration {
    public static ImageSynthesisResult imageGenerate(String prompt, String apiKey, String modelId, Map<String, Object> options) throws ApiException, NoApiKeyException {
//        String prompt = "一间有着精致窗户的花店，漂亮的木质门，摆放着花朵";
        ImageSynthesisParam param =
                ImageSynthesisParam.builder()
//                        .apiKey("sk-2a8a028420b946b4b4bbbce178e554cf")
//                        .model("wanx2.1-t2i-turbo")
//                        .prompt(prompt)
//                        .n(1)
//                        .size("1024*1024")
//                        .build();
                        .apiKey(apiKey)
                        .model(modelId)
                        .prompt(prompt)
//                        .seed((Integer) options.getOrDefault('n',42))
//                        .n((Integer) options.getOrDefault('n',1))
//                        .size(size)
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
        return result;
//        System.out.println(JsonUtils.toJson(result));
    }
    public static ImageSynthesisResult imageEdit(String prompt, String apiKey, String modelId,Map<String, Object> options, String baseImageUrl) throws ApiException, NoApiKeyException {
        // 设置parameters参数
//        String baseImage = "C:/java/model_service/llmproxy/doc/llmproxy_pytest/test_image3.jpg";
        ImageSynthesisParam param =
                ImageSynthesisParam.builder()
                        .apiKey(apiKey)
                        .model(modelId)//"wanx2.1-imageedit"
                        .function(ImageSynthesis.ImageEditFunction.DESCRIPTION_EDIT)
                        .prompt(prompt)
//                        .maskImageUrl(maskImageUrl)
                        .baseImageUrl(baseImageUrl)
//                        .n(1)
//                        .size(size)
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
//        System.out.println(JsonUtils.toJson(result));
        return result;
    }

    public static void listTask() throws ApiException, NoApiKeyException {
        ImageSynthesis is = new ImageSynthesis();
        AsyncTaskListParam param = AsyncTaskListParam.builder().build();
        ImageSynthesisListResult result = is.list(param);
        System.out.println(result);
    }

    public void fetchTask() throws ApiException, NoApiKeyException {
        String taskId = "your task id";
        ImageSynthesis is = new ImageSynthesis();
        // If set DASHSCOPE_API_KEY environment variable, apiKey can null.
        ImageSynthesisResult result = is.fetch(taskId, null);
        System.out.println(result.getOutput());
        System.out.println(result.getUsage());
    }

//    public static void main(String[] args){
//        try{
//            basicCall();
//            //listTask();
//        }catch(ApiException|NoApiKeyException e){
//            System.out.println(e.getMessage());
//        }
//    }
}