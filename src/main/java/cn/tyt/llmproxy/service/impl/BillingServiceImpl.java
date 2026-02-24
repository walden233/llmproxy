package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.common.enums.ResultCode;
import cn.tyt.llmproxy.common.exception.BusinessException;
import cn.tyt.llmproxy.entity.BillingLedger;
import cn.tyt.llmproxy.mapper.BillingLedgerMapper;
import cn.tyt.llmproxy.mapper.UserMapper;
import cn.tyt.llmproxy.service.IBillingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements IBillingService {

    private final BillingLedgerMapper billingLedgerMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public BillingLedger charge(String requestId,
                                Integer userId,
                                Integer accessKeyId,
                                Integer modelId,
                                Integer promptTokens,
                                Integer completionTokens,
                                Integer imageCount,
                                BigDecimal amount,
                                String bizType) {
        if (!StringUtils.hasText(requestId)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "requestId is required");
        }
        BillingLedger existing = findByRequestId(requestId);
        if (existing != null) {
            return existing;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "amount must be non-zero");
        }

        BillingLedger ledger = new BillingLedger();
        ledger.setRequestId(requestId);
        ledger.setUserId(userId);
        ledger.setAccessKeyId(accessKeyId);
        ledger.setModelId(modelId);
        ledger.setPromptTokens(promptTokens);
        ledger.setCompletionTokens(completionTokens);
        ledger.setImageCount(imageCount);
        ledger.setAmount(amount);
        ledger.setStatus(BillingLedger.STATUS_INIT);
        ledger.setBizType(bizType);
        ledger.setCreatedAt(LocalDateTime.now());
        ledger.setUpdatedAt(LocalDateTime.now());
        try {
            billingLedgerMapper.insert(ledger);
        } catch (DuplicateKeyException e) {
            return findByRequestId(requestId);
        }

        int updated = userMapper.updateBalance(userId, amount);
        if (updated == 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "insufficient balance or concurrent update");
        }
        ledger.setStatus(BillingLedger.STATUS_SETTLED);
        ledger.setUpdatedAt(LocalDateTime.now());
        int updatedRows = billingLedgerMapper.updateById(ledger);
        if (updatedRows == 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "failed to finalize billing ledger");
        }
        return ledger;
    }

    @Override
    @Transactional
    public BillingLedger creditTopup(String requestId, Integer userId, BigDecimal amount) {
        return charge(requestId, userId, null, null, null, null, null, amount, BillingLedger.BIZ_TOPUP);
    }

    private BillingLedger findByRequestId(String requestId) {
        return billingLedgerMapper.selectOne(
                new LambdaQueryWrapper<BillingLedger>().eq(BillingLedger::getRequestId, requestId)
        );
    }
}
