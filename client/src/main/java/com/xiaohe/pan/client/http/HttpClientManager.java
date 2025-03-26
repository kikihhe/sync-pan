package com.xiaohe.pan.client.http;


import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class HttpClientManager {
    private final String SERVER_BASE_URL;

    private final CloseableHttpClient httpClient;

    public HttpClientManager(String url) {
        this.httpClient = HttpClients.createDefault();
        SERVER_BASE_URL = url;
    }

    public String get(String url) throws IOException {
        return get(url, null, null);
    }

    public String get(String url, Map<String, String> params) throws IOException {
        return get(url, null, params);
    }

    public String get(String url, Map<String, String> headers, Map<String, String> params) throws IOException {
        String fullUrl = buildUrlWithParams(url, params);
        HttpGet httpGet = new HttpGet(fullUrl);
        addHeaders(httpGet, headers);

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            return EntityUtils.toString(response.getEntity());
        }
    }

    public String post(String url) throws IOException {
        Map<String, String> headers = generateDefaultHeaders();
        return post(url, headers, null, null);
    }

    public String post(String url, Map<String, String> params) throws IOException {
        Map<String, String> headers = generateDefaultHeaders();
        return post(url, headers, params, null);
    }
    public String post(String url, String body) throws IOException {
        Map<String, String> headers = generateDefaultHeaders();
        return post(url, headers, null, body);
    }


    public String post(String url, Map<String, String> headers, Map<String, String> params) throws IOException {
        return post(url, headers, params, null);
    }

    public String post(String url, Map<String, String> headers, String body) throws IOException {
        return post(url, headers, null, body);
    }

    public String post(String url, Map<String, String> headers, Map<String, String> params, String body) throws IOException {
        String fullUrl = SERVER_BASE_URL + buildUrlWithParams(url, params);
        HttpPost httpPost = new HttpPost(fullUrl);
        addHeaders(httpPost, headers);

        if (body != null) {
            httpPost.setEntity(new StringEntity(body));
        }

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            return EntityUtils.toString(response.getEntity());
        }
    }
    private String buildUrlWithParams(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) return url;

        StringBuilder sb = new StringBuilder(url);
        if (!url.contains("?")) sb.append("?");
        else if (!url.endsWith("&")) sb.append("&");

        params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
        return sb.deleteCharAt(sb.length() - 1).toString(); // Remove last &
    }

    private void addHeaders(HttpRequestBase request, Map<String, String> headers) {
        if (headers != null) {
            headers.forEach(request::setHeader);
        }
    }

    private Map<String, String> generateDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json;charset=utf8");
        return headers;
    }
}
