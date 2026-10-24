package com.flashjobweb.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
@Configuration
public class RedisConfig {
    private final RedisConnectionFactory connectionFactory;

    public RedisConfig(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    // Tự động bật tính năng Keyspace Notifications (Bắn Event khi Key hết hạn)
    @PostConstruct
    public void enableRedisKeyspaceNotifications() {
        try {
            connectionFactory.getConnection().serverCommands().setConfig("notify-keyspace-events", "Ex");
        } catch (Exception e) {
            // Nếu dùng Redis Cloud (AWS ElastiCache, v.v.) bị chặn lệnh CONFIG, bạn phải tự set tay trên giao diện web
            System.out.println("Lưu ý: Không thể tự động set CONFIG Redis. Hãy chắc chắn bạn đã bật notify-keyspace-events Ex");
        }
    }

    // Tạo Container để Spring Boot có thể nghe được tiếng la của Redis
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
