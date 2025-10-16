package cn.tyt.llmproxy.dto;

import lombok.Data;

@Data
public class AccessKeyInfo {
    String keyValue;
    boolean isValid;
    boolean isBalanceSufficient;
    private Integer userId;
}
