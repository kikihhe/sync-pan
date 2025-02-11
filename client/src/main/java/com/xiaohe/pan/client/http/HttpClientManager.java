package com.xiaohe.pan.client.http;


import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;


public class HttpClientManager {

    private final CloseableHttpClient httpClient;

    public HttpClientManager() {
        this.httpClient = HttpClients.createDefault();
    }

    // 发送 GET 请求
    public String get(String url, String headerKey, String headerValue) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader(headerKey, headerValue);

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            return EntityUtils.toString(response.getEntity());
        }
    }

    // 发送 POST 请求
    public String post(String url, String headerKey, String headerValue, String body) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(headerKey, headerValue);
        httpPost.setEntity(new StringEntity(body));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            return EntityUtils.toString(response.getEntity());
        }
    }
}
