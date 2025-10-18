package cn.tyt.llmproxy.dto;

import lombok.Data;

@Data
public class AccessKeyInfo {
    String keyValue;
    Integer keyId;
    boolean isValid;
    boolean isBalanceSufficient;
    private Integer userId;
}
