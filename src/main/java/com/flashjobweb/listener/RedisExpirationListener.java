package com.flashjobweb.listener;

import com.flashjobweb.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class RedisExpirationListener extends KeyExpirationEventMessageListener {

    private final JobService jobService;


    public RedisExpirationListener(RedisMessageListenerContainer redisMessageListenerContainer, JobService jobService) {
        super(redisMessageListenerContainer);
        this.jobService = jobService;
    }


    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();


        if (expiredKey.startsWith("job_start:")) {
            String jobIdStr = expiredKey.split(":")[1];
            log.info("REDIS EVENT: Tới giờ BẮT ĐẦU Job {}! Tiến hành xử lý ", jobIdStr);

            try {
                jobService.handleJobStart(UUID.fromString(jobIdStr));
            } catch (Exception e) {
                log.error("Lỗi khi xử lý Job Start từ Redis: ", e);
            }
        } else if (expiredKey.startsWith("job_end:")) {
            String jobIdStr = expiredKey.split(":")[1];
            log.info("REDIS EVENT: Tới giờ KẾT THÚC dự kiến Job {}! Xử lý nhắc nhở & kiểm tra hoàn thành", jobIdStr);

            try {
                jobService.handleJobEndTime(UUID.fromString(jobIdStr));
            } catch (Exception e) {
                log.error("Lỗi khi xử lý Job End từ Redis: ", e);
            }
        }
    }
}