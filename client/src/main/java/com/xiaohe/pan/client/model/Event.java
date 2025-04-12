package com.xiaohe.pan.client.model;

import com.xiaohe.pan.common.enums.EventType;
import java.io.File;

/**
 * 文件发生的时间
 */
public class Event implements Comparable<Event> {
    /**
     * 发生修改的文件
     */
    private final File file;

    /**
     * 文件路径
     */
    private final String relativePath;

    /**
     * 该事件的类型
     */
    private final EventType type;

    /**
     * 事件发生的时间，毫秒
     */
    private final long timestamp;

    /**
     * 该文件绑定的远端文件
     */
    private final String remoteMenuPath;

    /**
     * 该文件绑定的远端的目录
     */
    private Long remoteMenuId;

    public Event(File file, EventType type, String remoteMenuPath) {
        this.file = file;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.remoteMenuPath = remoteMenuPath;
        // 获取相对路径，用于在远程目录中定位文件
        this.relativePath = file.getAbsolutePath();
    }

    public File getFile() {
        return file;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getRemoteMenuPath() {
        return remoteMenuPath;
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
                file, type, timestamp, relativePath, remoteMenuPath);
    }

    public Long getRemoteMenuId() {
        return remoteMenuId;
    }

    public void setRemoteMenuId(Long remoteMenuId) {
        this.remoteMenuId = remoteMenuId;
    }

    @Override
    public int compareTo(Event other) {
        return (int) (this.timestamp - other.timestamp);
    }
}
