package com.xiaohe.pan.common.model.dto;

import lombok.Data;

@Data
public class MergeEvent {
    /**
     * 该事件发生所在的绑定目录
     * eg: /kikihhe/mac
     */
    private String remoteBoundMenuPath;

    /**
     * 该事件发生所在的远端目录
     * eg: /kikihhe/mac/文档
     */
    private String remoteMenuPath;

    /**
     * 该事件发生所在的绑定目录的本地路径
     * eg: D:/kikihhe/绑定测试
     */
    private String localBoundMenuPath;

    /**
     * 解决冲突所在的远端目录路径
     */
    private String resolveRemotePath;
    private String resolveLocalPath;

    /**
     * 事件在本地的全路径
     */
    private String localPath;

    /**
     * 文件/目录 的名称
     * 云端全路径: remoteMenuPath + filename
     * 本地全路径: localBoundMenuPath + (remoteMenuPath - boundMenuPath) + filename
     */
    private String filename;

    /**
     * 1: menu
     * 2: file
     */
    private Integer fileType;

    /**
     * 如果是文件，则填充内容
     */
    private byte[] data;

    /**
     * 获取云端的绝对路径
     * @return
     */
    public String getRemoteAbsolutePath() {
        return remoteMenuPath + "/" + filename;
    }
}
