//package com.xiaohe.pan.server.web.config;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.core.*;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//@Configuration
//public class RedisConfig {
//    @Autowired
//    private RedisConnectionFactory factory;
//
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate() {
//        // 将template 泛型设置为 <String, Object>
//        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
//        // 使用 String 序列化方式，序列化 KEY。
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        // 使用 String 序列化方式，序列化 VALUE。
//        redisTemplate.setValueSerializer(new StringRedisSerializer());
//        // 使用 String 序列化方式，序列化 HashKEY。
//        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
//        // 使用 String 序列化方式，序列化 ValueKEY。
//        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
//        // 配置连接工厂
//        redisTemplate.setConnectionFactory(factory);
//        return redisTemplate;
//    }
//
//    /**
//     *  HashOperations
//     *  操作 Hash 类型数据
//     **/
//    @Bean
//    public HashOperations<String, String, Object> hashOperations(RedisTemplate<String, Object> redisTemplate) {
//        return redisTemplate.opsForHash();
//    }
//
//    /**
//     *  HashOperations
//     * 操作 String 类型数据
//     **/
//    @Bean
//    public ValueOperations<String, String> valueOperations(RedisTemplate<String, String> redisTemplate) {
//        return redisTemplate.opsForValue();
//    }
//
//    /**
//     *  HashOperations
//     * 操作 List 类型数据
//     **/
//    @Bean
//    public ListOperations<String, Object> listOperations(RedisTemplate<String, Object> redisTemplate) {
//        return redisTemplate.opsForList();
//    }
//
//    /**
//     *  HashOperations
//     * 操作 Set 类型数据
//     **/
//    @Bean
//    public SetOperations<String, Object> setOperations(RedisTemplate<String, Object> redisTemplate) {
//        return redisTemplate.opsForSet();
//    }
//
//    /**
//     *  HashOperations
//     * 操作 SortedSet 类型数据
//     **/
//    @Bean
//    public ZSetOperations<String, Object> zSetOperations(RedisTemplate<String, Object> redisTemplate) {
//        return redisTemplate.opsForZSet();
//    }
//}