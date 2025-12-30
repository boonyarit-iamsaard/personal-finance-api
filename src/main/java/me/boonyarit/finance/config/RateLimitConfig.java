package me.boonyarit.finance.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Bean
    public ProxyManager<String> lettuceProxyManager() {
        RedisClient client = RedisClient.create(RedisURI.builder().withHost("localhost").withPort(6379).build());

        return Bucket4jLettuce.casBasedBuilder(client)
            .expirationAfterWrite(
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(60))
            )
            .build()
            .withMapper(key -> key.getBytes(StandardCharsets.UTF_8));
    }
}
