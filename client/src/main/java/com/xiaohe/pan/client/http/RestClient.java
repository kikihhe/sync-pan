package com.xiaohe.pan.client.http;
 
import com.alibaba.fastjson.JSON;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
 
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
 
/**
 * Http通用:RestClient
 */

public final class RestClient {
    private static final Logger log = LoggerFactory.getLogger(RestClient.class);

    private static final RestClient INSTANCE = new RestClient();
    /**
     * 建立连接时间
     */
    private static final int CONNECTION_TIME = 3000;
    /**
     * 发送请求时间
     */
    private static final int CONNECTION_REQUEST_TIME = 30000;
    /**
     * 读取数据时间
     */
    private static final int SOCKET_TIME = 180000;
    /**
     * 重试次数
     */
    private static final int RETRY = 3;
    /**
     * 请求类型:FORM表单
     */
    private static final String REQUEST_TYPE_FORM = "application/x-www-form-urlencoded; charset=UTF-8";
    /**
     * 请求类型:JSON体
     */
    private static final String REQUEST_TYPE_JSON = "application/json; charset=utf-8";

    private RestClient() {
    }

    /**
     * Http Get:不带请求参数
     *
     * @param url       url
     * @param headerMap headerMap
     * @return String
     */
    public String get(String url, Map<String, Object> headerMap) {
        return get(url, headerMap, null);
    }

    /**
     * Http Get:带请求参数
     *
     * @param url       url
     * @param headerMap headerMap
     * @param params    params
     * @return String
     */
    public String get(String url, Map<String, Object> headerMap, Map<String, Object> params) {
        checkUrl(url);
        checkHeader(headerMap);
        return doGet(url, headerMap, params, StandardCharsets.UTF_8);
    }

    /**
     * Http Post:Form表单不带请求参数
     *
     * @param url       URL
     * @param headerMap headerMap
     * @return String
     */
    public String post(String url, Map<String, Object> headerMap) {
        return doPostForForm(url, headerMap, null, StandardCharsets.UTF_8);
    }

    /**
     * Http Post:根据请头识别FORM表单还是JSON体
     *
     * @param url url
     * @param map map
     * @return String
     */
    public String post(String url, Map<String, Object> headerMap, Map<String, Object> map) {
        checkUrl(url);
        checkHeader(headerMap);
        checkBody(map);
        String contentType = (String) headerMap.get("Content-type");
        if (REQUEST_TYPE_FORM.equalsIgnoreCase(contentType)) {
            return doPostForForm(url, headerMap, map, StandardCharsets.UTF_8);
        } else if (REQUEST_TYPE_JSON.equalsIgnoreCase(contentType)) {
            return doPostForJson(url, headerMap, map, StandardCharsets.UTF_8);
        } else {
            throw new RuntimeException("not support this post type");
        }
    }

    /**
     * HTTP Put:Form表单方式不带参数
     * Content-type:application/x-www-form-urlencoded; charset=UTF-8
     *
     * @param url       url
     * @param headerMap headerMap
     * @return String
     */
    public static String put(String url, Map<String, Object> headerMap) {
        checkUrl(url);
        checkHeader(headerMap);
        return doPut(url, headerMap, null);
    }

    /**
     * Http Delete:不带参数的表单形式
     * Content-type:application/x-www-form-urlencoded; charset=UTF-8
     *
     * @param url       url
     * @param headerMap header
     * @return HttpResult
     */
    public static String delete(String url, Map<String, Object> headerMap) {
        checkUrl(url);
        checkHeader(headerMap);
        return doDelete(url, headerMap, null);
    }

    /**
     * HTTP Get:带请求参数
     *
     * @param url       请求的url地址 ?之前的地址
     * @param headerMap 请求头
     * @param params    请求的参数
     * @param charset   编码格式
     * @return String
     */
    public static String doGet(String url, Map<String, Object> headerMap, Map<String, Object> params, Charset charset) {
        StringBuffer resultBuffer = null;
        BufferedReader bufferedReader = null;
        CloseableHttpResponse response = null;
        URIBuilder uriBuilder = null;
        String result = null;
        try {
            uriBuilder = new URIBuilder(url);
            if (MapUtils.isNotEmpty(params)) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    uriBuilder.setParameter(entry.getKey(), entry.getValue().toString());
                }
            }
            HttpGet httpGet = new HttpGet(uriBuilder.build());
            headerMap.forEach((key, value) -> {
                httpGet.addHeader(key, value.toString());
            });
            response = httpClient().execute(httpGet);
            if (Objects.nonNull(response.getEntity())) {
                result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } catch (URISyntaxException e) {
            log.error("url synctax error", e);
        } catch (Exception e) {
            log.error("execute get request error", e);
        } finally {
            close(response);
        }
        return result;
    }

    /**
     * HTTP Post:Form表单方式
     * Content-type:application/x-www-form-urlencoded; charset=UTF-8
     *
     * @param url       请求的url地址?之前的地址
     * @param headerMap 请求头的参数
     * @param paramMap  请求的参数
     * @param charset   编码格式
     * @return String
     */
    public static String doPostForForm(String url, Map<String, Object> headerMap, Map<String, Object> paramMap, Charset charset) {
        List<NameValuePair> pairs = null;
        CloseableHttpResponse response = null;
        String result = null;
        pairs = new ArrayList<>(paramMap.size());
        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            if (entry.getValue() != null) {
                pairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
            }
        }
        HttpPost httpPost = new HttpPost(url);
        headerMap.forEach((key, value) -> {
            httpPost.setHeader(key, value.toString());
        });
        if (CollectionUtils.isNotEmpty(pairs)) {
            httpPost.setEntity(new UrlEncodedFormEntity(pairs, charset));
        }
        try {
            response = httpClient().execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity, charset);
            }
        } catch (Exception e) {
            log.error("execute post request error", e);
        } finally {
            close(response);
        }
        return result;
    }

    /**
     * HTTP Post:JSON形式
     * Content-type:application/json; charset=utf-8
     *
     * @param url       请求的url地址?之前的地址
     * @param headerMap 请求头的参数
     * @param bodyMap   请求的参数
     * @param charset   编码格式
     * @return String
     */
    public static String doPostForJson(String url, Map<String, Object> headerMap, Map<String, Object> bodyMap, Charset charset) {
        CloseableHttpResponse response = null;
        String result = null;
        HttpPost httpPost = new HttpPost(url);
        // Body参数设置到请求体中
        httpPost.setEntity(new StringEntity(JSON.toJSONString(bodyMap), charset));
        headerMap.forEach((key, value) -> {
            httpPost.setHeader(key, value.toString());
        });
        try {
            response = httpClient().execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity, charset);
            }
        } catch (Exception e) {
            log.error("execute post request error", e);
        } finally {
            close(response);
        }
        return result;
    }

    /**
     * HTTP Put:Form表单方式带参数
     * Content-type:application/x-www-form-urlencoded; charset=UTF-8
     *
     * @param url       url
     * @param headerMap headerMap
     * @param paramMap  paramMap
     * @return String
     */
    public static String doPut(String url, Map<String, Object> headerMap, Map<String, Object> paramMap) {
        HttpPut httpPut = new HttpPut(url);
        String result = null;
        CloseableHttpResponse response = null;
        List<NameValuePair> parameters = new ArrayList<>();
        if (MapUtils.isNotEmpty(paramMap)) {
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
            }
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        httpPut.setEntity(entity);
        headerMap.forEach((key, value) -> {
            httpPut.setHeader(key, value.toString());
        });
        try {
            response = httpClient().execute(httpPut);
            int code = response.getStatusLine().getStatusCode();
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("io error", e);
        } catch (Exception e) {
            log.error("execute put request error", e);
        } finally {
            close(response);
        }
        return result;
    }

    /**
     * Http Delete:不带参数的表单形式
     * Content-type:application/x-www-form-urlencoded; charset=UTF-8
     *
     * @param url       url
     * @param headerMap headerMap
     * @param paramMap  paramMap
     * @return String
     */
    public static String doDelete(String url, Map<String, Object> headerMap, Map<String, Object> paramMap) {
        URIBuilder uriBuilder = null;
        String result = null;
        CloseableHttpResponse response = null;
        try {
            uriBuilder = new URIBuilder(url);
            if (MapUtils.isNotEmpty(paramMap)) {
                for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                    uriBuilder.setParameter(entry.getKey(), entry.getValue().toString());
                }
            }
            HttpDelete httpDelete = new HttpDelete(uriBuilder.build());
            headerMap.forEach((key, value) -> {
                httpDelete.setHeader(key, value.toString());
            });
            response = httpClient().execute(httpDelete);
            int code = response.getStatusLine().getStatusCode();
            if (Objects.nonNull(response.getEntity())) {
                result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } catch (URISyntaxException e) {
            log.error("url synctax error", e);
        } catch (Exception e) {
            log.error("execute post request error", e);
        } finally {
            close(response);
        }
        return result;
    }

    public static RestClient getInstance() {
        return INSTANCE;
    }

    /**
     * 忽略证书验证的CloseableHttpClient对象,适配Http/Https
     *
     * @return CloseableHttpClient
     */
    public static CloseableHttpClient httpClient() {
        CloseableHttpClient closeableHttpClient = null;
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, getTrustingManager(), new java.security.SecureRandom());
            // 使用上面的策略初始化上下文
            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sc,
                    new String[]{"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"}, null, NoopHostnameVerifier.INSTANCE);
            closeableHttpClient = HttpClients.custom()
                    // 设置超时时间
                    .setDefaultRequestConfig(getRequestConfig())
                    // 设置连接池
                    .setConnectionManager(getPoolingManager())
                    // 设置重试策略,框架层面重试,可以在业务层面重试
                    //.setRetryHandler(getRetryHandler())
                    .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                    // 连接池清理过期连接
                    .evictExpiredConnections()
                    // 连接池释放空闲连接
                    .evictIdleConnections(60, TimeUnit.SECONDS)
                    .build();
        } catch (KeyManagementException e) {
            log.error("KeyManagement error", e);
        } catch (NoSuchAlgorithmException e) {
            log.error("No Such Algorithm error", e);
        }
        return closeableHttpClient;
    }

    /**
     * 信任所有证书
     *
     * @return TrustManager[]
     */
    private static TrustManager[] getTrustingManager() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }};
        return trustAllCerts;
    }

    /**
     * Http连接池配置
     *
     * @return HttpClientConnectionManager
     */
    public static HttpClientConnectionManager getPoolingManager() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, null, null);
        SSLContext.setDefault(sslContext);
        Registry<ConnectionSocketFactory> socketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.INSTANCE)
                        .register("https", new SSLConnectionSocketFactory(sslContext)).build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        // 设置整个连接池最大连接数
        connectionManager.setMaxTotal(200);
        // 最大路由
        connectionManager.setDefaultMaxPerRoute(10);
        return connectionManager;
    }

    /**
     * 封装RequestConfig
     *
     * @return RequestConfig
     */
    public static RequestConfig getRequestConfig() {
        RequestConfig requestConfig = RequestConfig.custom().
                setConnectTimeout(getConnectionTime()).
                setConnectionRequestTimeout(getConnectionRequestTime())
                .setSocketTimeout(getSocketTime()).build();
        return requestConfig;
    }

    /**
     * HttpClient的重试策略
     *
     * @return HttpRequestRetryHandler
     */
    public static HttpRequestRetryHandler getRetryHandler() {
        HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
            public boolean retryRequest(
                    IOException exception,
                    int executionCount,
                    HttpContext context) {
                // 重试三次
                if (executionCount >= RETRY) {
                    return false;
                }
                if (exception instanceof InterruptedIOException) {
                    // 超时
                    return false;
                }
                if (exception instanceof UnknownHostException) {
                    // 目标服务器不可达
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {
                    // 连接被拒绝
                    return false;
                }
                if (exception instanceof SSLException) {
                    // SSL握手异常
                    return false;
                }
                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
                if (idempotent) {
                    // 如果请求是幂等的,就再次尝试
                    return true;
                }
                return false;
            }
        };
        return retryHandler;
    }

    public static Integer getConnectionTime() {
        return CONNECTION_TIME;
    }

    public static Integer getConnectionRequestTime() {
        return CONNECTION_REQUEST_TIME;
    }

    public static Integer getSocketTime() {
        return SOCKET_TIME;
    }

    /**
     * 校验url,支持扩展
     *
     * @param url
     */
    private static void checkUrl(String url) {
        if (url == null || url.isEmpty()) {
            log.error("rest client request url not null");
            throw new RuntimeException("rest client request url not null");
        }
    }

    /**
     * 校验Header,支持扩展
     *
     * @param headerMap
     */
    private static void checkHeader(Map<String, Object> headerMap) {
        if (MapUtils.isEmpty(headerMap)) {
            log.error("rest client request header not null");
            throw new RuntimeException("rest client request header not null");
        }
    }

    /**
     * 校验Body,支持扩展
     *
     * @param bodyMap
     */
    private static void checkBody(Map<String, Object> bodyMap) {
        if (MapUtils.isEmpty(bodyMap)) {
            log.error("rest client request param map  not null");
            throw new RuntimeException("rest client request map  not null");
        }
    }

    /**
     * 关闭流资源
     *
     * @param closeable
     */
    private static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            log.error("close error", e);
        }
    }
}