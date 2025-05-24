package com.xiaohe.pan.client.http;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.xiaohe.pan.common.model.dto.EventDTO;
import com.xiaohe.pan.common.model.dto.EventsDTO;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class HttpClientManager {
    private final String SERVER_BASE_URL;

    /**
     * 同步客户端
     */
    private final CloseableHttpClient httpClient;
    /**
     * 异步客户端
     */
    private final CloseableHttpAsyncClient asyncHttpClient;

    public HttpClientManager(String url) {
        this.httpClient = HttpClients.custom()
                .setMaxConnPerRoute(20)
                .setMaxConnTotal(100)
                .build();

        IOReactorConfig ioReactorConfig = IOReactorConfig.custom().setSoTimeout(5000).build();
        this.asyncHttpClient = HttpAsyncClients.custom()
                .setMaxConnPerRoute(20)
                .setMaxConnTotal(100)
                .setDefaultIOReactorConfig(ioReactorConfig)
                .build();
        // 启动
        this.asyncHttpClient.start();
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

    public String post(String url, Map<String, String> headers, Map<String, String> params, String body)
            throws IOException {
        String fullUrl = SERVER_BASE_URL + buildUrlWithParams(url, params);
        HttpPost httpPost = new HttpPost(fullUrl);
        Map<String, String> stringStringMap = generateDefaultHeaders();
        stringStringMap.putAll(headers);
        addHeaders(httpPost, headers);

        if (body != null) {
            httpPost.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));
        }

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            return EntityUtils.toString(response.getEntity());
        }
    }

    // 异步POST基础方法
    public Future<HttpResponse> asyncPost(String url, Map<String, String> headers, String body) {
        HttpPost httpPost = new HttpPost(SERVER_BASE_URL + url);
        if (CollUtil.isEmpty(headers)) {
            headers = generateDefaultHeaders();
        }
        addHeaders(httpPost, headers);

        if (body != null) {
            httpPost.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        }

        return asyncHttpClient.execute(httpPost, null);
    }

    // 带回调的异步POST
    public Future<HttpResponse> asyncPostWithCallback(String url, Map<String, String> headers, String body,
                                                      FutureCallback<HttpResponse> callback) {
        HttpPost httpPost = new HttpPost(SERVER_BASE_URL + url);
        if (CollUtil.isEmpty(headers)) {
            headers = generateDefaultHeaders();
        }
        addHeaders(httpPost, headers);

        if (body != null) {
            httpPost.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        }

        return asyncHttpClient.execute(httpPost, callback);
    }

    public void onewayPost(String url, Map<String, String> headers, String body) {
        // 忽略返回的Future对象
        asyncPost(url, headers, body);
    }

    private StringEntity buildMultipartEntity(File file, Map<String, String> formData) throws IOException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        // 添加文件部分
        builder.addBinaryBody("multipartFile", file, ContentType.APPLICATION_OCTET_STREAM, file.getName());

        // 添加其他表单参数
        if (formData != null) {
            formData.forEach((k, v) -> builder.addTextBody(k, v, ContentType.TEXT_PLAIN));
        }

        return new StringEntity(String.valueOf(builder.build().getContent()), ContentType.get(builder.build()));
    }

    private String buildUrlWithParams(String url, Map<String, String> params) {
        if (params == null || params.isEmpty())
            return url;

        StringBuilder sb = new StringBuilder(url);
        if (!url.contains("?"))
            sb.append("?");
        else if (!url.endsWith("&"))
            sb.append("&");

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
        headers.put("content-type", "application/json;charset=UTF-8");
        return headers;
    }

    // 添加关闭方法
    public void close() throws IOException {
        this.httpClient.close();
        this.asyncHttpClient.close();
    }

    public String upload(String url, File file, String body) throws IOException {
        HttpPost httpPost = new HttpPost(SERVER_BASE_URL + url);

        // 构建multipart请求体
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        // 添加文件
        builder.addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, file.getName());

        // 添加JSON请求体
        builder.addTextBody("body", body, ContentType.APPLICATION_JSON);

        // 设置请求体
        HttpEntity multipartEntity = builder.build();
        httpPost.setEntity(multipartEntity);

        // 执行请求
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            return EntityUtils.toString(response.getEntity());
        }
    }

    /**
     * 上传多个文件和事件数据
     *
     * @param url      请求URL
     * @param files    文件列表，每个文件对应一个事件
     * @param fileKeys 文件对应的键名列表，用于在服务端识别每个文件
     * @param body     JSON格式的事件数据
     * @return 服务器响应
     * @throws IOException 如果发生IO异常
     */
    public String uploadMultipleFiles(String url, List<File> files, List<String> fileKeys, String body) throws
            IOException {
        HttpPost httpPost = new HttpPost(SERVER_BASE_URL + url);

        // 构建multipart请求体
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        // 添加多个文件，每个文件使用唯一的键名
        if (files != null && !files.isEmpty() && fileKeys != null && files.size() == fileKeys.size()) {
            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                String key = fileKeys.get(i);
                if (file != null && file.exists() && !file.isDirectory()) {
                    builder.addBinaryBody(key, file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
                }
            }
        }

        // 添加JSON请求体
        builder.addTextBody("body", body, ContentType.APPLICATION_JSON);

        // 设置请求体
        HttpEntity multipartEntity = builder.build();
        httpPost.setEntity(multipartEntity);

        // 执行请求
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            return EntityUtils.toString(response.getEntity());
        }
    }

}