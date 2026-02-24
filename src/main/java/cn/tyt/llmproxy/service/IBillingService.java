package cn.tyt.llmproxy.service;

import cn.tyt.llmproxy.entity.BillingLedger;

import java.math.BigDecimal;

public interface IBillingService {

    BillingLedger charge(String requestId,
                         Integer userId,
                         Integer accessKeyId,
                         Integer modelId,
                         Integer promptTokens,
                         Integer completionTokens,
                         Integer imageCount,
                         BigDecimal amount,
                         String bizType);

    BillingLedger creditTopup(String requestId, Integer userId, BigDecimal amount);
}
