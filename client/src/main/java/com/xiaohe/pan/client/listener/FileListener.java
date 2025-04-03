package com.xiaohe.pan.client.listener;

import com.xiaohe.pan.common.enums.EventType;
import com.xiaohe.pan.client.event.EventContainer;
import com.xiaohe.pan.client.model.Event;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;

import java.io.File;

public class FileListener extends FileAlterationListenerAdaptor {
    private final String remoteDirectory;
    private final EventContainer eventContainer;

    public FileListener(String remoteDirectory) {
        this.remoteDirectory = remoteDirectory;
        this.eventContainer = EventContainer.getInstance();
    }

    @Override
    public void onDirectoryCreate(File directory) {
        eventContainer.addEvent(new Event(directory, EventType.DIRECTORY_CREATE, remoteDirectory));
    }

    @Override
    public void onDirectoryChange(File directory) {
        eventContainer.addEvent(new Event(directory, EventType.DIRECTORY_MODIFY, remoteDirectory));
    }

    @Override
    public void onDirectoryDelete(File directory) {
        eventContainer.addEvent(new Event(directory, EventType.DIRECTORY_DELETE, remoteDirectory));
    }

    @Override
    public void onFileCreate(File file) {
        eventContainer.addEvent(new Event(file, EventType.FILE_CREATE, remoteDirectory));
    }

    @Override
    public void onFileChange(File file) {
        eventContainer.addEvent(new Event(file, EventType.FILE_MODIFY, remoteDirectory));
    }

    @Override
    public void onFileDelete(File file) {
        eventContainer.addEvent(new Event(file, EventType.FILE_DELETE, remoteDirectory));
    }

}
