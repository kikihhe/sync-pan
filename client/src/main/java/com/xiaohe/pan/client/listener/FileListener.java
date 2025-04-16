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

            // 2. Get Live and Stored *File* Children
            Map<String, Path> liveFileChildren = new HashMap<>();
            try (Stream<Path> stream = Files.list(dirPath)) {
                stream.filter(Files::isRegularFile) // Files only
                        .forEach(filePath -> liveFileChildren.put(filePath.getFileName().toString(), filePath));
            } catch (IOException e) {
                System.err.println("Error listing directory " + dirPath + ": " + e.getMessage());
                return;
            }

            Map<String, Node> storedFileChildren = dirNode.getChildren().values().stream()
                    .filter(node -> !node.isDirectory()) // Files only
                    .collect(Collectors.toMap(Node::getName, node -> node));

            // 3. Identify Potential Deletes (Store temporarily)
            Map<String, Node> potentiallyDeletedFiles = new HashMap<>();
            for (Map.Entry<String, Node> entry : storedFileChildren.entrySet()) {
                if (!liveFileChildren.containsKey(entry.getKey())) {
                    potentiallyDeletedFiles.put(entry.getValue().getRelativePath(), entry.getValue());
                    // System.out.println("    Marked as potentially deleted: " + entry.getValue().getRelativePath());
                }
            }

            // 4. Identify Potential Adds / Modifies / Renames by iterating live files
            for (Map.Entry<String, Path> liveEntry : liveFileChildren.entrySet()) {
                String liveFileName = liveEntry.getKey();
                Path liveFilePath = liveEntry.getValue();
                Node storedNode = storedFileChildren.get(liveFileName);

                if (storedNode == null) {
                    // Case A: Live file NOT in storage -> Potential Add or Rename
                    String liveFileMd5 = null;
                    try {
                        liveFileMd5 = MD5Util.getMD5(liveFilePath);
                    } catch (IOException e) {
                        System.err.println("Error calculating MD5 for live file " + liveFilePath + ": " + e.getMessage());
                        // Cannot determine rename without MD5, treat as Add? Or skip?
                        // Let's try adding it, assuming it's new.
                        handleFileCreate(liveFilePath);
                        continue; // Move to next live file
                    }

                    if (liveFileMd5 == null) {
                        // Still couldn't get MD5
                        System.err.println("Warning: Could not get MD5 for live file " + liveFilePath + ". Treating as Add.");
                        handleFileCreate(liveFilePath);
                        continue;
                    }

                    // Search for a potentially deleted file with the same MD5
                    String renamedFromRelativePath = null;
                    Node renamedFromNode = null;
                    for (Map.Entry<String, Node> deletedEntry : potentiallyDeletedFiles.entrySet()) {
                        Node potentialMatch = deletedEntry.getValue();
                        // Check MD5 equality (handle potential null MD5 in stored node although unlikely for files)
                        if (Objects.equals(liveFileMd5, potentialMatch.getMd5()) && !potentialMatch.isDirectory()) {
                            renamedFromRelativePath = potentialMatch.getRelativePath();
                            renamedFromNode = potentialMatch;
                            break; // Found a match
                        }
                    }

                    if (renamedFromNode != null) {
                        // Found a match -> RENAME
                        // System.out.println("    Detected RENAME: " + renamedFromRelativePath + " -> " + storage.getRelativePath(liveFilePath));

                        // Remove the old node representation from storage (don't update parents yet)
                        storage.removeNodeFromTree(renamedFromRelativePath, false);

                        // Add the new node representation to storage (this updates parents)
                        // Need to ensure addPathToTree uses the correct MD5 if possible
                        // Let's modify addPathToTree slightly, or just let it recalculate
                        Node newNode = storage.addPathToTree(liveFilePath, true);
                        if (newNode != null && !Objects.equals(newNode.getMd5(), liveFileMd5)) {
                            // If addPathToTree recalculated a different MD5 (e.g., race condition?), log warning
                            System.err.println("Warning: MD5 mismatch during rename handling for " + newNode.getRelativePath());
                        }


                        // Generate DELETE event for old path and CREATE event for new path
                        eventContainer.addEvent(createEvent(storage, storage.getAbsolutePath(renamedFromRelativePath), EventType.FILE_DELETE));
                        eventContainer.addEvent(createEvent(storage, liveFilePath, EventType.FILE_CREATE));

                        // Remove from potentiallyDeleted map so it's not processed later
                        potentiallyDeletedFiles.remove(renamedFromRelativePath);

                    } else {
                        // No match found -> Genuine ADD
                        // System.out.println("    Detected ADD: " + storage.getRelativePath(liveFilePath));
                        handleFileCreate(liveFilePath);
                    }

                } else {
                    // Case B: Live file IS in storage -> Check for Modification
                    //System.out.println("    Checking existing file for modification: " + storedNode.getRelativePath());
                    handlePotentialFileModification(liveFilePath, storedNode.getRelativePath(), storage);
                }
            }

            // 5. Process Genuine Deletes
            // Any remaining entries in potentiallyDeletedFiles are actual deletes
            for (Map.Entry<String, Node> entry : potentiallyDeletedFiles.entrySet()) {
                String deletedRelativePath = entry.getKey();
                Node deletedNode = entry.getValue();
                //System.out.println("    Processing genuine DELETE: " + deletedRelativePath);
                handleFileDelete(storage.getAbsolutePath(deletedRelativePath), deletedRelativePath, storage);
            }

            // 6. Directory Handling Skipped

            // 7. Final MD5 Update for the directory itself
            String currentStoredMd5 = dirNode.getMd5();
            String newCalculatedMd5 = storage.calculateSingleDirectoryMD5(dirNode);
            if (!Objects.equals(currentStoredMd5, newCalculatedMd5)) {
                // System.out.println("  Final MD5 recalc needed for " + dirRelativePath + ". Old: " + currentStoredMd5 + ", New: " + newCalculatedMd5);
                dirNode.setMd5(newCalculatedMd5);
                storage.updateParentDirectoryMD5s(dirNode); // Propagate upwards
            }

        } catch (IOException e) {
            System.err.println("IOException during directory change handling for " + dirRelativePath + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during directory change handling for " + dirRelativePath + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            storage.lock.writeLock().unlock(); // Release Lock
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
        if (!storageOpt.isPresent()) return; // Not in a monitored path

        MD5Storage storage = storageOpt.get();
        String relativePath = storage.getRelativePath(filePath);

        // Check if file still exists before proceeding
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            // File disappeared or became a directory? Treat as delete.
            // System.out.println("    File no longer exists or not a regular file, treating change as delete: " + relativePath);
            handleFileDeleteEvent(filePath); // Use the event handler which checks storage
            return;
        }

        // Call the core logic that requires the storage object
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
        handleFileDelete(filePath, relativePath, storage); // Call the common logic
    }
    private void handleFileDelete(Path filePath, String relativePath, MD5Storage storage) {
        storage.lock.writeLock().lock();
        try {
            Optional<Node> nodeOpt = storage.getNode(relativePath);
            if (nodeOpt.isPresent() && !nodeOpt.get().isDirectory()) { // Ensure it's a file node
                // System.out.println("  Handling FILE_DELETE: " + relativePath);
                storage.removeNodeFromTree(relativePath, true); // Removes node, updates parents
                eventContainer.addEvent(createEvent(storage, filePath, EventType.FILE_DELETE));
            } else {
                // Node doesn't exist or is a directory
                // System.out.println("  File node already removed or is directory: " + relativePath);
            }
        } catch (IOException e) {
            System.err.println("Error handling file delete for " + relativePath + ": " + e.getMessage());
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
