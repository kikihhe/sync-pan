package com.xiaohe.pan.client.listener;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.xiaohe.pan.client.model.BoundDirectory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang3.StringUtils;

public class FileListenerMonitor {

    private final List<BoundDirectory> boundDirectories = new ArrayList<>();
    private final long interval = TimeUnit.SECONDS.toMillis(1);
    private FileAlterationMonitor fileAlterationMonitor;

    private FileListenerMonitor() {}

    public static FileListenerMonitor getInstance() {
        // 使用单例模式保持唯一实例
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final FileListenerMonitor INSTANCE = new FileListenerMonitor();
    }

    public synchronized boolean bindDirectory(String local, String remoteMenuPath, Long remoteMenuId) {
        File dir = new File(local);
        // 检查目录是否存在并且是一个目录
        if (!dir.exists()) {
            // 文件或目录不存在，创建一个
            dir.mkdirs();
        }
        // 验证目录/文件是否已绑定或是否为其他已绑定目录的子目录
        for (BoundDirectory boundDirectory : boundDirectories) {
            if (boundDirectory.getDirectory().equals(dir) || isSubdirectory(boundDirectory.getDirectory(), dir)) {
                System.out.println("目录已绑定或为子目录，拒绝绑定: " + dir.getPath());
                return false;
            }
        }
        // 如果目录有效，创建监听器并绑定
        FileListener listener = new FileListener(remoteMenuPath);
        FileAlterationObserver observer = new FileAlterationObserver(dir, createFilter());
        observer.addListener(listener);
        BoundDirectory boundDirectory = new BoundDirectory(dir, remoteMenuPath, listener, remoteMenuId);
        FileAlterationObserver fileAlterationObserver = new FileAlterationObserver(dir, createFilter());
        fileAlterationObserver.addListener(listener);
        fileAlterationMonitor.addObserver(fileAlterationObserver);
        boundDirectories.add(boundDirectory);
        return true;
    }

    private boolean isSubdirectory(File potentialParent, File child) {
        try {
            return FileUtils.directoryContains(potentialParent, child);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        List<FileAlterationObserver> observers = new ArrayList<>();
        for (int i = 0; i < boundDirectories.size(); i++) {
            BoundDirectory boundDirectory = boundDirectories.get(i);
            FileAlterationObserver fileAlterationObserver = new FileAlterationObserver(boundDirectory.getDirectory(), createFilter());
            fileAlterationObserver.addListener(boundDirectory.getListener());
            observers.add(i, fileAlterationObserver);
        }
        fileAlterationMonitor = new FileAlterationMonitor(interval, observers.toArray(new FileAlterationObserver[0]));
        try {
            fileAlterationMonitor.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void stop() throws Exception {
        fileAlterationMonitor.stop();
    }
    public void restart() throws Exception {
        stop();
        start();
    }

    private IOFileFilter createFilter() {
        IOFileFilter directories = FileFilterUtils.and(FileFilterUtils.directoryFileFilter(), HiddenFileFilter.VISIBLE);
        IOFileFilter txtFiles = FileFilterUtils.and(FileFilterUtils.fileFileFilter(), FileFilterUtils.suffixFileFilter(".txt"));
        return FileFilterUtils.or(directories, txtFiles);
    }

    public BoundDirectory getBoundDirectoryByRemotePath(String remoteDirectory) {
        if (StringUtils.isEmpty(remoteDirectory)) {
            return null;
        }
        for (BoundDirectory bd : boundDirectories) {
            if (bd.getRemote().equals(remoteDirectory)) {
                return bd;
            }
        }
        return null;
    }
}