package cn.tyt.llmproxy.dto.request;

import lombok.Data;

@Data
public class ProviderUpdateRequest {
    private String name;
    private String urlBase;
}