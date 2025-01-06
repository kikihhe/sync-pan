package com.xiaohe.pan.client.model;

import com.xiaohe.pan.client.enums.EventType;
import com.xiaohe.pan.common.util.DateUtil;

import java.io.File;
import java.util.concurrent.TimeUnit;


public class Event implements Comparable<Event> {
    // 发生修改的文件
    private final File file;
    // 文件路径
    private final String relativePath;
    // 该事件的类型
    private final EventType type;
    // 事件发生的时间，毫秒
    private final long timestamp;
    // 该文件绑定的远端文件
    private final String remoteDirectory;

    public Event(File file, EventType type, String remoteDirectory) {
        this.file = file;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.remoteDirectory = remoteDirectory;
        // 获取相对路径，用于在远程目录中定位文件
        this.relativePath = file.getAbsolutePath().substring(file.getParent().length() + 1);
    }

    public File getFile() {
        return file;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getRemoteDirectory() {
        return remoteDirectory;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public EventType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return 31 * file.hashCode() + type.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Event{file=%s, type=%s, timestamp=%s, relativePath=%s, remoteDirectory=%s}",
                file, type, timestamp, relativePath, remoteDirectory);
    }

    @Override
    public int compareTo(Event other) {
        return (int) (this.timestamp - other.timestamp);
    }
}
