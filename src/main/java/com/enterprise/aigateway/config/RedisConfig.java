package com.enterprise.aigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  @Bean
  public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
      ReactiveRedisConnectionFactory factory) {
    StringRedisSerializer keySerializer = new StringRedisSerializer();
    Jackson2JsonRedisSerializer<Object> valueSerializer =
        new Jackson2JsonRedisSerializer<>(Object.class);

    RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
        RedisSerializationContext.newSerializationContext(keySerializer);

    RedisSerializationContext<String, Object> context = builder.value(valueSerializer).build();

    return new ReactiveRedisTemplate<>(factory, context);
  }

  // Dùng reactive redis để hỗ trợ trong môi trường webflux
  @Bean
  public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
      ReactiveRedisConnectionFactory factory) {
    return new ReactiveStringRedisTemplate(factory);
  }
}
