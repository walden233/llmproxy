package cn.tyt.llmproxy.service.impl;

import cn.tyt.llmproxy.entity.AsyncJob;
import cn.tyt.llmproxy.mapper.AsyncJobMapper;
import cn.tyt.llmproxy.service.IAsyncJobService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AsyncJobServiceImpl implements IAsyncJobService {

    @Autowired
    private AsyncJobMapper asyncJobMapper;

    @Override
    @Transactional
    public AsyncJob createAsyncJob(Integer userId, Integer accessKeyId, Map<String, Object> requestPayload) {
        AsyncJob job = new AsyncJob();
        job.setJobId(UUID.randomUUID().toString());
        job.setUserId(userId);
        job.setAccessKeyId(accessKeyId);
        job.setStatus(AsyncJob.STATUS_PENDING);
        job.setRequestPayload(requestPayload);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        
        asyncJobMapper.insert(job);
        return job;
    }

    @Override
    public AsyncJob getJobStatus(String jobId) {
        LambdaQueryWrapper<AsyncJob> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AsyncJob::getJobId, jobId);
        return asyncJobMapper.selectOne(queryWrapper);
    }

    @Override
    @Transactional
    public boolean updateJobStatus(String jobId, String status, String modelName, Map<String, Object> resultPayload, String errorMessage) {
        LambdaQueryWrapper<AsyncJob> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AsyncJob::getJobId, jobId);
        
        AsyncJob job = asyncJobMapper.selectOne(queryWrapper);
        if (job == null) {
            return false;
        }
        job.setModelName(modelName);
        job.setStatus(status);
        job.setResultPayload(resultPayload);
        job.setErrorMessage(errorMessage);
        job.setUpdatedAt(LocalDateTime.now());
        
        return asyncJobMapper.updateById(job) > 0;
    }
}
