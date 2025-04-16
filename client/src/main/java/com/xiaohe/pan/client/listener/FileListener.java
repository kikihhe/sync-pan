package com.xiaohe.pan.client.listener;

import com.xiaohe.pan.client.storage.MD5Storage;
import com.xiaohe.pan.client.storage.MD5StorageFactory;
import com.xiaohe.pan.client.storage.Node;
import com.xiaohe.pan.common.enums.EventType;
import com.xiaohe.pan.client.event.EventContainer;
import com.xiaohe.pan.client.model.Event;
import com.xiaohe.pan.common.util.MD5Util;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileListener extends FileAlterationListenerAdaptor {
    private final String remoteDirectory;
    private final EventContainer eventContainer;

    public FileListener(String remoteDirectory) {
        this.remoteDirectory = remoteDirectory;
        this.eventContainer = EventContainer.getInstance();
    }

    @Override
    public void onDirectoryCreate(File directory) {
        handleDirectoryCreate(directory.toPath());
    }

    @Override
    public void onDirectoryChange(File directory) {
        handleDirectoryChange(directory.toPath());
    }

    @Override
    public void onDirectoryDelete(File directory) {
        handleDirectoryDeleteEvent(directory.toPath());
    }



    @Override
    public void onFileCreate(File file) {
        handleFileCreate(file.toPath());
    }

    @Override
    public void onFileChange(File file) {
        handlePotentialFileModification(file.toPath());
    }

    @Override
    public void onFileDelete(File file) {
        handleFileDeleteEvent(file.toPath());
    }


    private void handleDirectoryChange(Path dirPath) {
        Optional<MD5Storage> storageOpt = MD5StorageFactory.getInstance().findStorageForPath(dirPath);
        if (!storageOpt.isPresent() || !Files.isDirectory(dirPath)) {
            return;
        }

        MD5Storage storage = storageOpt.get();
        String dirRelativePath = storage.getRelativePath(dirPath);
        storage.lock.writeLock().lock();
        try {
            Optional<Node> dirNodeOpt = storage.getNode(dirRelativePath);
            if (!dirNodeOpt.isPresent()) {
                System.err.println("目录节点不存在: " + dirRelativePath + ". Attempting recovery.");
                try {
                    storage.addPathToTree(dirPath, true);
                } catch (IOException addEx) {
                    System.err.println("目录节点添加失败 " + dirRelativePath + ": " + addEx.getMessage());
                }
                return;
            }
            Node dirNode = dirNodeOpt.get();
            if (!dirNode.isDirectory()) {
                System.err.println("Error: Node is not a directory in storage: " + dirRelativePath);
                return;
            }

            // 2. 获取当前目录下的实际文件和已存储的文件
            Map<String, Path> liveFileChildren = new HashMap<>();
            try (Stream<Path> stream = Files.list(dirPath)) {
                stream.filter(Files::isRegularFile) // 只处理文件，不处理目录
                        .forEach(filePath -> liveFileChildren.put(filePath.getFileName().toString(), filePath));
            } catch (IOException e) {
                System.err.println("遍历目录失败 " + dirPath + ": " + e.getMessage());
                return;
            }

            Map<String, Node> storedFileChildren = dirNode.getChildren().values().stream()
                    .filter(node -> !node.isDirectory()) // Files only
                    .collect(Collectors.toMap(Node::getName, node -> node));

            // 3. 找出可能被删除的文件（临时存储）
            Map<String, Node> potentiallyDeletedFiles = new HashMap<>();
            for (Map.Entry<String, Node> entry : storedFileChildren.entrySet()) {
                if (!liveFileChildren.containsKey(entry.getKey())) {
                    potentiallyDeletedFiles.put(entry.getValue().getRelativePath(), entry.getValue());
                    // System.out.println("    标记为可能被删除的文件: " + entry.getValue().getRelativePath());
                }
            }

            // 4. 遍历当前文件，识别新增/修改/重命名的文件
            for (Map.Entry<String, Path> liveEntry : liveFileChildren.entrySet()) {
                String liveFileName = liveEntry.getKey();
                Path liveFilePath = liveEntry.getValue();
                Node storedNode = storedFileChildren.get(liveFileName);

                if (storedNode == null) {
                    // 情况A：文件不在存储中 -> 可能是新增或重命名
                    String liveFileMd5 = null;
                    try {
                        liveFileMd5 = MD5Util.getMD5(liveFilePath);
                    } catch (IOException e) {
                        System.err.println("计算文件MD5失败 " + liveFilePath + ": " + e.getMessage());
                        // 没有MD5无法判断是否是重命名，就当作新文件处理吧
                        handleFileCreate(liveFilePath);
                        continue; // 继续处理下一个文件
                    }

                    if (liveFileMd5 == null) {
                        // 还是拿不到MD5
                        System.err.println("警告：无法获取文件MD5 " + liveFilePath + "，就当作新文件处理了");
                        handleFileCreate(liveFilePath);
                        continue;
                    }

                    // 在可能被删除的文件中找找有没有MD5相同的（说明是重命名）
                    String renamedFromRelativePath = null;
                    Node renamedFromNode = null;
                    for (Map.Entry<String, Node> deletedEntry : potentiallyDeletedFiles.entrySet()) {
                        Node potentialMatch = deletedEntry.getValue();
                        // 比对MD5（虽然文件的MD5不太可能是null，但还是要处理一下）
                        if (Objects.equals(liveFileMd5, potentialMatch.getMd5()) && !potentialMatch.isDirectory()) {
                            renamedFromRelativePath = potentialMatch.getRelativePath();
                            renamedFromNode = potentialMatch;
                            break; // 找到匹配的了
                        }
                    }

                    if (renamedFromNode != null) {
                        // 找到匹配的了 -> 说明是重命名
                        // System.out.println("    发现重命名：" + renamedFromRelativePath + " -> " + storage.getRelativePath(liveFilePath));

                        // 先从存储中删掉旧节点（先不更新父节点）
                        storage.removeNodeFromTree(renamedFromRelativePath, false);

                        // 把新节点加到存储中（这步会更新父节点）
                        // 要尽量保证addPathToTree用对MD5
                        // 让addPathToTree重新计算一下吧
                        Node newNode = storage.addPathToTree(liveFilePath, true);
                        if (newNode != null && !Objects.equals(newNode.getMd5(), liveFileMd5)) {
                            // 如果addPathToTree算出来的MD5不一样（可能是并发导致的？），记个日志
                            System.err.println("警告：重命名处理时MD5不匹配，文件：" + newNode.getRelativePath());
                        }


                        // 生成删除旧文件和创建新文件的事件
                        eventContainer.addEvent(createEvent(storage, storage.getAbsolutePath(renamedFromRelativePath), EventType.FILE_DELETE));
                        eventContainer.addEvent(createEvent(storage, liveFilePath, EventType.FILE_CREATE));

                        // 从待删除列表中移除，这样后面就不会再处理了
                        potentiallyDeletedFiles.remove(renamedFromRelativePath);

                    } else {
                        // 没找到匹配的 -> 就是新增文件
                        // System.out.println("    发现新增文件：" + storage.getRelativePath(liveFilePath));
                        handleFileCreate(liveFilePath);
                    }

                } else {
                    // 情况B：文件在存储中 -> 检查是否被修改了
                    //System.out.println("    检查文件是否被修改：" + storedNode.getRelativePath());
                    handlePotentialFileModification(liveFilePath, storedNode.getRelativePath(), storage);
                }
            }

            // 5. 处理真正被删除的文件
            // potentiallyDeletedFiles中剩下的就是真的被删除了
            for (Map.Entry<String, Node> entry : potentiallyDeletedFiles.entrySet()) {
                String deletedRelativePath = entry.getKey();
                Node deletedNode = entry.getValue();
                //System.out.println("    处理删除的文件：" + deletedRelativePath);
                handleFileDelete(storage.getAbsolutePath(deletedRelativePath), deletedRelativePath, storage);
            }

            // 6. 目录处理已跳过

            // 7. 最后更新目录本身的MD5
            String currentStoredMd5 = dirNode.getMd5();
            String newCalculatedMd5 = storage.calculateSingleDirectoryMD5(dirNode);
            if (!Objects.equals(currentStoredMd5, newCalculatedMd5)) {
                // System.out.println("  需要重新计算目录的MD5 " + dirRelativePath + "。旧的：" + currentStoredMd5 + "，新的：" + newCalculatedMd5);
                dirNode.setMd5(newCalculatedMd5);
                storage.updateParentDirectoryMD5s(dirNode); // 向上更新父目录
            }

        } catch (IOException e) {
            System.err.println("处理目录变更时发生IO异常，目录：" + dirRelativePath + "，错误：" + e.getMessage());
        } catch (Exception e) {
            System.err.println("处理目录变更时发生意外错误，目录：" + dirRelativePath + "，错误：" + e.getMessage());
            e.printStackTrace();
        } finally {
            storage.lock.writeLock().unlock(); // 释放锁
        }
    }
    private void handleDirectoryCreate(Path dirPath) {
        Optional<MD5Storage> storageOpt = MD5StorageFactory.getInstance().findStorageForPath(dirPath);
        if (!storageOpt.isPresent() || !Files.isDirectory(dirPath)) return; // Check if it's actually a directory now

        MD5Storage storage = storageOpt.get();
        String relativePath = storage.getRelativePath(dirPath);

        storage.lock.writeLock().lock();
        try {
            // Use addPathToTree which handles adding the node and linking parents
            // It initially sets directory MD5 to null.
            Node newNode = storage.addPathToTree(dirPath, false); // Add node first

            if (newNode != null) {
                // Calculate empty directory MD5 and set it
                String emptyDirMD5 = MD5Util.calculateMD5FromString("");
                newNode.setMd5(emptyDirMD5);


                storage.updateParentDirectoryMD5s(newNode);

                // Add the event
                eventContainer.addEvent(createEvent(storage, dirPath, EventType.DIRECTORY_CREATE));
                // System.out.println("  -> Processed DIRECTORY_CREATE: " + relativePath);
            } else {
                // Node might already exist if event is duplicated, or addPathToTree failed
                // System.out.println("  Directory node already exists or creation failed for: " + relativePath);
                // Check if it exists and update MD5 just in case
                Optional<Node> existingNodeOpt = storage.getNode(relativePath);
                if(existingNodeOpt.isPresent() && existingNodeOpt.get().isDirectory() && existingNodeOpt.get().getMd5() == null){
                    existingNodeOpt.get().setMd5(MD5Util.calculateMD5FromString(""));
                    storage.updateParentDirectoryMD5s(existingNodeOpt.get());
                    eventContainer.addEvent(createEvent(storage, dirPath, EventType.DIRECTORY_CREATE)); // Still add event
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling directory create for " + relativePath + ": " + e.getMessage());
        } finally {
            storage.lock.writeLock().unlock();
        }
    }
    private void handleDirectoryDeleteEvent(Path dirPath) {
        Optional<MD5Storage> storageOpt = MD5StorageFactory.getInstance().findStorageForPath(dirPath);
        // It's possible the storage root itself was deleted, handle gracefully.
        if (!storageOpt.isPresent()) {
            // Check if the path *was* a storage root
            // md5Factory.removeStorage(dirPath); // Clean up factory map if root deleted
            return;
        }

        MD5Storage storage = storageOpt.get();
        String relativePath = storage.getRelativePath(dirPath);
        handleDirectoryDelete(dirPath, relativePath, storage); // Call the common logic
    }
    private void handleDirectoryDelete(Path dirPath, String relativePath, MD5Storage storage) {
        storage.lock.writeLock().lock();
        try {
            Optional<Node> nodeOpt = storage.getNode(relativePath);
            if (nodeOpt.isPresent()) {

                storage.removeNodeFromTree(relativePath, true);
                eventContainer.addEvent(createEvent(storage, dirPath, EventType.DIRECTORY_DELETE));
            } else {

            }
        } catch (IOException e) {
            System.err.println("Error handling directory delete for " + relativePath + ": " + e.getMessage());
        } finally {
            storage.lock.writeLock().unlock();
        }
    }
    private void handleFileCreate(Path filePath) {
        Optional<MD5Storage> storageOpt = MD5StorageFactory.getInstance().findStorageForPath(filePath);

        if (!storageOpt.isPresent() || !Files.isRegularFile(filePath)) return;

        MD5Storage storage = storageOpt.get();
        String relativePath = storage.getRelativePath(filePath);

        storage.lock.writeLock().lock();
        try {

            Node newNode = storage.addPathToTree(filePath, true);

            if (newNode != null) {
                eventContainer.addEvent(createEvent(storage, filePath, EventType.FILE_CREATE));

            } else {


            }
        } catch (IOException e) {
            System.err.println("Error handling file create for " + relativePath + ": " + e.getMessage());
        } finally {
            storage.lock.writeLock().unlock();
        }
    }
    private void handlePotentialFileModification(Path filePath) {
        Optional<MD5Storage> storageOpt = MD5StorageFactory.getInstance().findStorageForPath(filePath);
        if (!storageOpt.isPresent()) return; // 不在监控路径中

        MD5Storage storage = storageOpt.get();
        String relativePath = storage.getRelativePath(filePath);

        // 先检查文件是否还存在
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            // 文件不见了或者变成目录了？就当作删除处理
            // System.out.println("    文件不存在或者不是普通文件了，当作删除处理：" + relativePath);
            handleFileDeleteEvent(filePath); // 用事件处理器来检查存储
            return;
        }

        // 调用核心逻辑来处理文件修改
        handlePotentialFileModification(filePath, relativePath, storage);
    }

    /**
     * Core logic for checking and handling file modification. Assumes file exists.
     */
    private void handlePotentialFileModification(Path filePath, String relativePath, MD5Storage storage) {
        storage.lock.writeLock().lock(); // Need write lock to potentially update MD5
        try {
            Optional<Node> nodeOpt = storage.getNode(relativePath);
            if (!nodeOpt.isPresent()) {
                // File exists but not in tree? Should be treated as create.
                // System.out.println("    File found (" + relativePath + ") but not in storage, treating as create.");
                // Release write lock before calling another method that acquires it
                storage.lock.writeLock().unlock();
                handleFileCreate(filePath);
                storage.lock.writeLock().lock(); // Re-acquire lock if needed (though handleFileCreate finishes)
                return; // Exit after handling as create
            }

            Node node = nodeOpt.get();
            if (node.isDirectory()) {
                System.err.println("Warning: Checking modification for a directory node: " + relativePath);
                return; // Should not happen
            }

            String oldMD5 = node.getMd5();
            String newMD5 = MD5Util.getMD5(filePath); // Calculate current MD5

            if (newMD5 != null && !newMD5.equals(oldMD5)) {
                // System.out.println("  Detected FILE_MODIFY: " + relativePath + " OldMD5: " + oldMD5 + " NewMD5: " + newMD5);
                // Update MD5 in the node and trigger parent updates
                storage.updateFileNodeMD5(relativePath, newMD5);
                eventContainer.addEvent(createEvent(storage, filePath, EventType.FILE_MODIFY));
            } else if (newMD5 == null) {
                // Failed to calculate new MD5 for existing file - permission error?
                System.err.println("Warning: Cannot calculate MD5 for existing file, skipping modification check: " + relativePath);
            }
            // else MD5 unchanged or newMD5 is null -> do nothing

        } catch (IOException e) {
            System.err.println("Error during file modification check for " + relativePath + ": " + e.getMessage());
        } finally {
            // Only unlock if the current thread still holds the lock
            if (storage.lock.isWriteLockedByCurrentThread()) {
                storage.lock.writeLock().unlock();
            }
        }
    }
    private void handleFileDeleteEvent(Path filePath) {
        Optional<MD5Storage> storageOpt = MD5StorageFactory.getInstance().findStorageForPath(filePath);
        if (!storageOpt.isPresent()) return;

        MD5Storage storage = storageOpt.get();
        String relativePath = storage.getRelativePath(filePath);
        handleFileDelete(filePath, relativePath, storage); // 调用通用的删除处理逻辑
    }
    private void handleFileDelete(Path filePath, String relativePath, MD5Storage storage) {
        storage.lock.writeLock().lock();
        try {
            Optional<Node> nodeOpt = storage.getNode(relativePath);
            if (nodeOpt.isPresent() && !nodeOpt.get().isDirectory()) { // 确保是文件节点
                // System.out.println("  正在处理文件删除：" + relativePath);
                storage.removeNodeFromTree(relativePath, true); // 删除节点，更新父节点
                eventContainer.addEvent(createEvent(storage, filePath, EventType.FILE_DELETE));
            } else {
                // 节点不存在或者是目录
                // System.out.println("  文件节点已经被删除了或者是个目录：" + relativePath);
            }
        } catch (IOException e) {
            System.err.println("处理文件删除时出错，文件：" + relativePath + "，错误：" + e.getMessage());
        } finally {
            storage.lock.writeLock().unlock();
        }
    }

    private Event createEvent(MD5Storage storage, Path absolutePath, EventType type) {
        // Assumes Event constructor takes: Path, EventType, remotePathString, storageRootPath
        File file = new File(absolutePath.toString());
        return new Event(file, type, this.remoteDirectory);
    }

    private MD5Storage getMD5Storage() {
        Optional<MD5Storage> storageOpt = MD5StorageFactory.getInstance().getMd5Storage(remoteDirectory);

        if (!storageOpt.isPresent())
            return null;

        return storageOpt.get();
    }
}
