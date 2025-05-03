package com.xiaohe.pan.storage.oss;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.xiaohe.pan.common.constants.SyncPanConstants;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.FileUtils;
import com.xiaohe.pan.common.util.UUIDUtil;
import com.xiaohe.pan.storage.api.AbstractStorageService;
import com.xiaohe.pan.storage.api.context.DeleteFileContext;
import com.xiaohe.pan.storage.api.context.MergeFileContext;
import com.xiaohe.pan.storage.api.context.ReadFileContext;
import com.xiaohe.pan.storage.api.context.StoreFileChunkContext;
import com.xiaohe.pan.storage.api.context.StoreFileContext;
import com.xiaohe.pan.storage.oss.property.OssStorageEngineProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ConditionalOnProperty(prefix = "storage", value = "ali_oss")
public class OSSStorageService extends AbstractStorageService {
    private static final Integer TEN_THOUSAND_INT = 10000;

    private static final String CACHE_KEY_TEMPLATE = "oss_cache_upload_id_%s_%s";

    private static final String IDENTIFIER_KEY = "identifier";

    private static final String UPLOAD_ID_KEY = "uploadId";

    private static final String USER_ID_KEY = "userId";

    private static final String PART_NUMBER_KEY = "partNumber";

    private static final String E_TAG_KEY = "eTag";

    private static final String PART_SIZE_KEY = "partSize";

    private static final String PART_CRC_KEY = "partCRC";

    @Autowired
    private OssStorageEngineProperty config;

    @Autowired
    private OSSClient client;

    @Override
    public void doDelete(DeleteFileContext context) throws IOException {
        List<String> realFilePathList = context.getRealFilePathList();
        realFilePathList.stream().forEach(realPath -> {
            // 如果是普通文件直接删了
            if (!checkHaveParams(realPath)) {
                client.deleteObject(config.getBucketName(), realPath);
            } else {
                // 如果是文件的分片
                JSONObject params = analysisUrlParams(realPath);
                if (Objects.nonNull(params) && !params.isEmpty()) {
                    String uploadId = params.getString(UPLOAD_ID_KEY);
                    String identifier = params.getString(IDENTIFIER_KEY);
                    Long userId = params.getLong(USER_ID_KEY);
                    String cacheKey = getCacheKey(identifier, userId);
                    // 从缓存种移除
                    getCache().evict(cacheKey);
                    try {
                        AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(config.getBucketName(), getBaseUrl(realPath), uploadId);
                        client.abortMultipartUpload(request);
                    } catch (Exception e) {

                    }
                }
            }
        });
    }

    @Override
    public void doMergeFile(MergeFileContext context) throws IOException {
        String cacheKey = getCacheKey(context.getIdentifier(), context.getUserId());

        ChunkUploadEntity entity = getCache().get(cacheKey, ChunkUploadEntity.class);

        if (Objects.isNull(entity)) {
            throw new BusinessException("文件分片合并失败，文件的唯一标识为：" + context.getIdentifier());
        }

        List<String> chunkPaths = context.getRealPathList();
        List<PartETag> partETags = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(chunkPaths)) {
            partETags = chunkPaths.stream()
                    .filter(StringUtils::isNotBlank)
                    .map(this::analysisUrlParams)
                    .filter(Objects::nonNull)
                    .filter(jsonObject -> !jsonObject.isEmpty())
                    .map(jsonObject -> new PartETag(jsonObject.getIntValue(PART_NUMBER_KEY),
                            jsonObject.getString(E_TAG_KEY),
                            jsonObject.getLongValue(PART_SIZE_KEY),
                            jsonObject.getLong(PART_CRC_KEY)
                    )).collect(Collectors.toList());
        }
        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(config.getBucketName(), entity.getObjectKey(), entity.uploadId, partETags);
        CompleteMultipartUploadResult result = client.completeMultipartUpload(request);
        if (Objects.isNull(result)) {
            throw new BusinessException("文件分片合并失败，文件的唯一标识为：" + context.getIdentifier());
        }

        getCache().evict(cacheKey);

        context.setRealPath(entity.getObjectKey());
    }

    @Override
    public void doRealFile(ReadFileContext context) throws IOException {
        OSSObject ossObject = client.getObject(config.getBucketName(), context.getRealPath());
        if (Objects.isNull(ossObject)) {
            throw new BusinessException("文件读取失败，文件的名称为：" + context.getRealPath());
        }
        FileUtils.writeStream2StreamNormal(ossObject.getObjectContent(), context.getOutputStream());
    }

    @Override
    public void doStore(StoreFileContext context) throws IOException {
        String realPath = getFilePath(FileUtils.getFileSuffix(context.getFilename()));
        client.putObject(config.getBucketName(), realPath, context.getInputStream());
        context.setRealPath(realPath);
    }

    /**
     * OSS文件分片上传的步骤:
     * 1、初始化文件分片上传，获取一个全局唯一的uploadId
     * 2、并发上传文件分片，每一个文件分片都需要带有初始化返回的uploadId
     * 3、所有分片上传完成，触发文件分片合并的操作
     */
    @Override
    public void doStoreChunk(StoreFileChunkContext context) throws IOException {
        if (context.getTotalChunks() > TEN_THOUSAND_INT) {
            throw new BusinessException("文件分片个数超过了限制，分片数量不得大于: " + TEN_THOUSAND_INT);
        }
        String cacheKey = getCacheKey(context.getIdentifier(), context.getUserId());
        ChunkUploadEntity entity = getCache().get(cacheKey, ChunkUploadEntity.class);
        if (Objects.isNull(entity)) {
            entity = initChunkUpload(context.getFilename(), cacheKey);
        }

        UploadPartRequest request = new UploadPartRequest();
        request.setBucketName(config.getBucketName());
        request.setKey(entity.getObjectKey());
        request.setUploadId(entity.getUploadId());
        request.setInputStream(context.getInputStream());
        request.setPartSize(context.getCurrentChunkSize());
        request.setPartNumber(context.getChunkNumber());

        UploadPartResult result = client.uploadPart(request);
        if (Objects.isNull(result)) {
            throw new BusinessException("文件分片上传失败");
        }

        PartETag partETag = result.getPartETag();

        // 拼装文件分片的url
        JSONObject params = new JSONObject();
        params.put(IDENTIFIER_KEY, context.getIdentifier());
        params.put(UPLOAD_ID_KEY, entity.getUploadId());
        params.put(USER_ID_KEY, context.getUserId());
        params.put(PART_NUMBER_KEY, partETag.getPartNumber());
        params.put(E_TAG_KEY, partETag.getETag());
        params.put(PART_SIZE_KEY, partETag.getPartSize());
        params.put(PART_CRC_KEY, partETag.getPartCRC());

        String realPath = assembleUrl(entity.getObjectKey(), params);

        context.setRealPath(realPath);
    }
    /**
     * 初始化文件分片上传
     * <p>
     * 1、执行初始化请求
     * 2、保存初始化结果到缓存中
     *
     * @param filename
     * @param cacheKey
     * @return
     */
    private ChunkUploadEntity initChunkUpload(String filename, String cacheKey) {
        String filePath = getFilePath(filename);

        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(config.getBucketName(), filePath);
        InitiateMultipartUploadResult result = client.initiateMultipartUpload(request);

        if (Objects.isNull(result)) {
            throw new BusinessException("文件分片上传初始化失败");
        }

        ChunkUploadEntity entity = new ChunkUploadEntity();
        entity.setObjectKey(filePath);
        entity.setUploadId(result.getUploadId());

        getCache().put(cacheKey, entity);

        return entity;
    }
    /**
     * 该实体为文件分片上传树池化之后的全局信息载体
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @EqualsAndHashCode
    @ToString
    public static class ChunkUploadEntity implements Serializable {

        private static final long serialVersionUID = -1646644290507942507L;

        /**
         * 分片上传全局唯一的uploadId
         */
        private String uploadId;

        /**
         * 文件分片上传的实体名称
         */
        private String objectKey;

    }
    /**
     * 获取对象的完整名称
     * 年/月/日/UUID.fileSuffix
     *
     * @param fileSuffix
     * @return
     */
    private String getFilePath(String fileSuffix) {
        return new StringBuffer()
                .append(DateUtil.thisYear())
                .append(SyncPanConstants.SLASH_STR)
                .append(DateUtil.thisMonth() + 1)
                .append(SyncPanConstants.SLASH_STR)
                .append(DateUtil.thisDayOfMonth())
                .append(SyncPanConstants.SLASH_STR)
                .append(UUIDUtil.getUUID())
                .append(fileSuffix)
                .toString();
    }
    /**
     * 拼装URL
     *
     * @param baseUrl
     * @param params
     * @return baseUrl?paramKey1=paramValue1&paramKey2=paramValue2
     */
    private String assembleUrl(String baseUrl, JSONObject params) {
        if (Objects.isNull(params) || params.isEmpty()) {
            return baseUrl;
        }
        StringBuffer urlStringBuffer = new StringBuffer(baseUrl);
        urlStringBuffer.append(SyncPanConstants.QUESTION_MARK_STR);
        List<String> paramsList = Lists.newArrayList();
        StringBuffer urlParamsStringBuffer = new StringBuffer();
        params.entrySet().forEach(entry -> {
            urlParamsStringBuffer.setLength(SyncPanConstants.ZERO_INT);
            urlParamsStringBuffer.append(entry.getKey());
            urlParamsStringBuffer.append(SyncPanConstants.EQUALS_MARK_STR);
            urlParamsStringBuffer.append(entry.getValue());
            paramsList.add(urlParamsStringBuffer.toString());
        });
        return urlStringBuffer.append(Joiner.on(SyncPanConstants.AND_MARK_STR).join(paramsList)).toString();
    }
    /**
     * 获取基础URL
     *
     * @param url
     * @return
     */
    private String getBaseUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return SyncPanConstants.EMPTY_STR;
        }
        if (checkHaveParams(url)) {
            return url.split(getSplitMark(SyncPanConstants.QUESTION_MARK_STR))[0];
        }
        return url;
    }
    /**
     * 检查是否是含有参数的URL
     *
     * @param url
     * @return
     */
    private boolean checkHaveParams(String url) {
        return StringUtils.isNotBlank(url) && url.indexOf(SyncPanConstants.QUESTION_MARK_STR) != SyncPanConstants.MINUS_ONE_INT;
    }
    /**
     * 获取截取字符串的关键标识
     * 由于java的字符串分割会按照正则去截取
     * 我们的URL会影响标识的识别，故添加左右中括号去分组
     *
     * @param mark
     * @return
     */
    private String getSplitMark(String mark) {
        return new StringBuffer(SyncPanConstants.LEFT_BRACKET_STR)
                .append(mark)
                .append(SyncPanConstants.RIGHT_BRACKET_STR)
                .toString();
    }
    /**
     * 分析URL参数
     *
     * @param url
     * @return
     */
    private JSONObject analysisUrlParams(String url) {
        JSONObject result = new JSONObject();
        if (!checkHaveParams(url)) {
            return result;
        }
        String paramsPart = url.split(getSplitMark(SyncPanConstants.QUESTION_MARK_STR))[1];
        if (StringUtils.isNotBlank(paramsPart)) {
            List<String> paramPairList = Splitter.on(SyncPanConstants.AND_MARK_STR).splitToList(paramsPart);
            paramPairList.stream().forEach(paramPair -> {
                String[] paramArr = paramPair.split(getSplitMark(SyncPanConstants.EQUALS_MARK_STR));
                if (paramArr != null && paramArr.length == SyncPanConstants.TWO_INT) {
                    result.put(paramArr[0], paramArr[1]);
                }
            });
        }
        return result;
    }
    /**
     * 获取分片上传的缓存Key
     *
     * @param identifier
     * @param userId
     * @return
     */
    private String getCacheKey(String identifier, Long userId) {
        return String.format(CACHE_KEY_TEMPLATE, identifier, userId);
    }
}
