package com.xiaohe.pan.common.util;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    public static String createToken(Map<String, Object> claims) throws JsonProcessingException {
        Date currentDate = new Date();
        String json = objectMapper.writeValueAsString(claims);
        return JWT.create()
                .withHeader(createHeader())
                .withClaim("payload", json)
                .withIssuedAt(currentDate)
                .withExpiresAt(new Date(currentDate.getTime() + EXPIRE_TIME))
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 通用Token生成方法
     * @param t 自定义类型
     */
    public static <T> String createToken(T t) throws JsonProcessingException {
        Map<String, Object> map = objectMapper.convertValue(t, new TypeReference<Map<String, Object>>() {
            @Override
            public int compareTo(TypeReference<Map<String, Object>> o) {
                return super.compareTo(o);
            }
        });
        return createToken(map);
    }


    /**
     * 将声明转换为指定类型对象
     */
    public static <T> T parseAndConvert(String token, Class<T> clazz) {
        try {
            DecodedJWT decodedJWT = getVerifier().verify(token);
            Map<String, Claim> claims = decodedJWT.getClaims();
            Claim payload = claims.get("payload");
            String json = payload.asString();
            return objectMapper.readValue(json, clazz);
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

