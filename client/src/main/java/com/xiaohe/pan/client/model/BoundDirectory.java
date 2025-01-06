package com.xiaohe.pan.client.model;

import com.xiaohe.pan.client.listener.FileListener;

import java.io.File;

public class BoundDirectory {

    private final File directory;
    private final String remote;
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