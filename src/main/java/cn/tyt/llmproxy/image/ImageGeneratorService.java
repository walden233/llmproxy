package cn.tyt.llmproxy.image;

import cn.tyt.llmproxy.dto.request.ImageGenerationRequest;
import cn.tyt.llmproxy.dto.response.ImageGenerationResponse;

import java.util.Map;

/**
 * An interface for image generation services.
 * It defines the contract for generating and editing images,
 * allowing for different underlying provider implementations.
 *
 * It implements AutoCloseable to ensure that any underlying resources
 * (like connection pools or executors) can be released properly.
 */
public interface ImageGeneratorService extends AutoCloseable {

    /**
     * Generates an image based on a text prompt.
     *
     * @param request The request DTO containing the prompt and other details.
     * @return A response DTO containing the URLs of the generated images.
     */
    ImageGenerationResponse generateImage(ImageGenerationRequest request);

    /**
     * Edits an existing image based on a text prompt.
     *
     * @param request The request DTO containing the prompt and other details.
     * @return A response DTO containing the URLs of the edited images.
     */
    ImageGenerationResponse editImage(ImageGenerationRequest request);

    /**
     * Closes the service and releases any resources.
     * Overridden from AutoCloseable to avoid a checked 'Exception'.
     */
    @Override
    void close();
}