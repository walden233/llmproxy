package cn.tyt.llmproxy.dto.request;

import lombok.Data;

@Data
public class ImageInput {
    /**
     * 图片内容的 Base64 编码，或者是图片的 URL（二选一）
     */
    private String base64; // 若前端以 base64 上传
    private String url;    // 若前端已上传至文件服务，可用 URL 引用

}