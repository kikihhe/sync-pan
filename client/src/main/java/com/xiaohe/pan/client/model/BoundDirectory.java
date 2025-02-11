package com.xiaohe.pan.client.model;

import com.xiaohe.pan.client.listener.FileListener;

import java.io.File;

/**
 * 已绑定文件夹
 */
public class BoundDirectory {
    /**
     * 本地目录
     */
    private final File directory;

    /**
     * 远程目录
     */
    private final String remote;

    /**
     * 此绑定文件夹对应的监听器
     */
    private final FileListener listener;

    public BoundDirectory(File directory, String remote, FileListener listener) {
        this.directory = directory;
        this.listener = listener;
        this.remote = remote;
    }

    public File getDirectory() {
        return directory;
    }

    public FileListener getListener() {
        return listener;
    }

    public String getRemote() {
        return remote;
    }

}