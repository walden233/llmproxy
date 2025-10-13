package cn.tyt.llmproxy.image;

import cn.tyt.llmproxy.dto.LlmModelConfigDto;
import cn.tyt.llmproxy.entity.LlmModel;

public class ImageGeneratorFactory {

    /**
     * Creates an instance of an ImageGeneratorService based on the model identifier.
     *
     * @param model    The LlmModel entity containing the model identifier.
     * @return An instance of a class that implements ImageGeneratorService.
     * @throws IllegalArgumentException if the model identifier is not supported.
     */
    public static ImageGeneratorService createGenerator(LlmModelConfigDto model) {
        String modelIdentifier = model.getModelIdentifier();

        if (modelIdentifier == null || modelIdentifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Model identifier cannot be null or empty.");
        }

        if (modelIdentifier.startsWith("wanx")) {
            return new AliImageGenerator(modelIdentifier, model.getApiKey());
        } else if (modelIdentifier.startsWith("seed")||modelIdentifier.startsWith("doubao")) {
            return new ArkImageGenerator(modelIdentifier, model.getApiKey());
        } else {
            // You can add more providers here with 'else if' blocks.
            throw new IllegalArgumentException("Unsupported model identifier: " + modelIdentifier);
        }
    }
}