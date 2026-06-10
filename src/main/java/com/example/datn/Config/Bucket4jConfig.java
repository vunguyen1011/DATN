package com.example.datn.Config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class Bucket4jConfig {

    private final LettuceConnectionFactory lettuceConnectionFactory;

    @Bean
    public ProxyManager<String> proxyManager() {
        RedisClient redisClient = (RedisClient) lettuceConnectionFactory.getNativeClient();
        var redisConnection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        var expirationStrategy = ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(1));
        var clientConfig = ClientSideConfig.getDefault().withExpirationAfterWriteStrategy(expirationStrategy);
        return LettuceBasedProxyManager.builderFor(redisConnection).withClientSideConfig(clientConfig).build();
    }
}