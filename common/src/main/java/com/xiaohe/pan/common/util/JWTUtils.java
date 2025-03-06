package com.xiaohe.pan.common.util;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohe.pan.common.exceptions.BusinessException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于生成和解析JWT
 */
public class JWTUtils {

    /**
     * 声明一个秘钥
     */
    private static final String SECRET = "xiaohe";

    /**
     * 过期时间，30天
     */
    private static final Integer EXPIRE_TIME = 1000 * 60 * 60 * 30;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 通用Token生成方法
     * @param claims 自定义声明集合
     */
    public static String createToken(Map<String, Object> claims) {
        Date currentDate = new Date();
        return JWT.create()
                .withHeader(createHeader())
                .withPayload(claims)
                .withIssuedAt(currentDate)
                .withExpiresAt(new Date(currentDate.getTime() + EXPIRE_TIME))
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 通用Token生成方法
     * @param t 自定义类型
     */
    public static <T> String createToken(T t) {
        Map<String, Object> map = objectMapper.convertValue(t, new TypeReference<Map<String, Object>>() {
            @Override
            public int compareTo(TypeReference<Map<String, Object>> o) {
                return super.compareTo(o);
            }
        });
        return createToken(map);
    }

    /**
     * 解析Token获取所有声明（自动类型转换）
     */
    public static Map<String, Object> parseClaims(String token) {
        try {
            DecodedJWT decodedJWT = getVerifier().verify(token);

            return objectMapper.convertValue(decodedJWT.getClaims(), new TypeReference<Map<String, Object>>() {
                @Override
                public int compareTo(TypeReference<Map<String, Object>> o) {
                    return super.compareTo(o);
                }
            });
        } catch (JWTVerificationException | IllegalArgumentException e) {
            throw new BusinessException("token 解析 claims 失败");
        }
    }

    /**
     * 将声明转换为指定类型对象
     */
    public static <T> T parseAndConvert(String token, Class<T> clazz) {
        try {
            Map<String, Object> claims = parseClaims(token);
            return objectMapper.convertValue(claims, clazz);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }

    // 私有工具方法
    private static Map<String, Object> createHeader() {
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        return header;
    }
    private static JWTVerifier getVerifier() {
        return JWT.require(Algorithm.HMAC256(SECRET)).build();
    }

}

