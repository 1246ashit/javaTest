package com.example.demo.Config;

import com.example.demo.DTO.RoomDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, RoomDTO> roomRedisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, RoomDTO> tmpl = new RedisTemplate<>();
        tmpl.setConnectionFactory(cf);
        tmpl.setKeySerializer(new StringRedisSerializer());
        tmpl.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        tmpl.afterPropertiesSet();
        return tmpl;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }
}
