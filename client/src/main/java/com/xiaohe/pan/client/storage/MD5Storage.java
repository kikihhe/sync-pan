// src/main/java/com/xiaohe/pan/client/storage/MD5Storage.java
package com.xiaohe.pan.client.storage;


import com.xiaohe.pan.common.util.MD5Util;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MD5Storage {

    private final Path rootPath;
    private Node rootNode; // The root of our directory tree

    // Stores all nodes, keyed by their relative path for quick lookup
    private final Map<String, Node> nodeMap = new ConcurrentHashMap<>();

    // Lock for protecting tree structure modifications (add/remove nodes)
    // Read lock for accessing nodes, write lock for modifying structure/MD5s
    public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


    public MD5Storage(Path rootPath) {
        if (rootPath == null || !Files.isDirectory(rootPath)) {
            throw new IllegalArgumentException("Root path must be an existing directory: " + rootPath);
        }
        this.rootPath = rootPath.toAbsolutePath();
        // Initialize root node (MD5 will be calculated later)
        this.rootNode = new Node(rootPath.getFileName().toString(), "", true); // Relative path of root is ""
        this.nodeMap.put("", this.rootNode); // Add root to the map
    }

    /**
     * Builds the initial tree structure and calculates MD5s.
     * MUST be called after constructor.
     */
    public void buildInitialTree() throws IOException {
        lock.writeLock().lock(); // Exclusive lock for initial build
        try {
            System.out.println("Building initial MD5 tree for: " + rootPath);
            // Clear any previous state except the root node itself
            nodeMap.clear();
            rootNode.getChildren().clear();
            nodeMap.put("", rootNode); // Put root back

            try (Stream<Path> paths = Files.walk(this.rootPath)) {
                // Sort paths to ensure parents are likely processed before children, though not strictly necessary here
                paths.sorted()
                        .filter(path -> !path.equals(this.rootPath)) // Skip root path itself
                        .forEach(path -> {
                            try {
                                addPathToTree(path, false); // Add path without updating MD5s yet
                            } catch (IOException e) {
                                System.err.println("Error adding path during initial build " + path + ": " + e.getMessage());
                                // Decide how to handle: skip? throw?
                            }
                        });
            }

            // Now calculate all MD5s bottom-up
            calculateAllDirectoryMD5sBottomUp(this.rootNode);
            System.out.println("Finished building initial MD5 tree for: " + rootPath);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Adds a single path (file or directory) to the tree structure.
     * Calculates file MD5 immediately. Directory MD5 is initially null.
     * Links the new node to its parent.
     *
     * @param fullPath The absolute path of the file/directory to add.
     * @param updateMD5s If true, triggers parent MD5 updates after adding.
     * @return The created or existing Node.
     * @throws IOException If file access fails.
     */
    public Node addPathToTree(Path fullPath, boolean updateMD5s) throws IOException {
        // This method should ideally be called with the write lock held if updateMD5s is true or if structure changes

        Path relativePath = this.rootPath.relativize(fullPath);
        String relativePathStr = relativePath.toString();

        // Check if node already exists (idempotency)
        Node existingNode = nodeMap.get(relativePathStr);
        if (existingNode != null) {
            return existingNode;
        }

        // Find or create parent node
        Node parentNode;
        Path parentPath = relativePath.getParent();
        if (parentPath == null) { // Direct child of root
            parentNode = this.rootNode;
        } else {
            parentNode = nodeMap.get(parentPath.toString());
            if (parentNode == null) {
                // Parent doesn't exist, create it recursively (should have been created by walk order usually)
                // Ensure the parent *directory* actually exists on disk before creating node
                Path fullParentPath = this.rootPath.resolve(parentPath);
                if (Files.isDirectory(fullParentPath)) {
                    System.out.println("  Recursively adding missing parent: " + parentPath);
                    parentNode = addPathToTree(fullParentPath, false); // Don't trigger updates yet
                } else {
                    System.err.println("Error: Parent directory " + fullParentPath + " does not exist for path " + fullPath);
                    return null; // Cannot add node without existing parent directory
                }
            }
        }

        // Create the new node
        String name = fullPath.getFileName().toString();
        boolean isDirectory = Files.isDirectory(fullPath);
        String md5 = null;
        if (!isDirectory) {
            md5 = MD5Util.getMD5(fullPath); // Calculate file MD5 now
        }

        Node newNode = new Node(name, relativePathStr, isDirectory, parentNode, md5);

        // Add to parent's children and the global map
        if (parentNode != null) { // Should not be null unless it's the root being added (which isn't done here)
            parentNode.addChild(newNode);
        } else if (!relativePathStr.isEmpty()){ // Should not happen if parent logic is correct
            System.err.println("Critical Error: Could not find parent for non-root node: " + relativePathStr);
            return null;
        }

        nodeMap.put(relativePathStr, newNode);
        //System.out.println("    Added node: " + newNode);

        // Optionally trigger parent MD5 updates immediately (used by event listeners)
        if (updateMD5s && parentNode != null) {
            //System.out.println("  Triggering parent MD5 update from addPathToTree for " + relativePathStr);
            updateParentDirectoryMD5s(newNode);
        }

        return newNode;
    }

    /**
     * Recursively calculates MD5 for a directory and its parents, bottom-up.
     * Assumes file MD5s and child nodes are already present.
     * @param directoryNode The node of the directory to start calculations from.
     * @return The calculated MD5 of the directoryNode.
     */
    private String calculateAllDirectoryMD5sBottomUp(Node directoryNode) throws IOException {
        lock.writeLock().lock(); // Need write lock to modify MD5s in nodes
        try {
            if (!directoryNode.isDirectory()) {
                return directoryNode.getMd5(); // Should already have MD5 if it's a file
            }

            StringBuilder childMd5sConcatenated = new StringBuilder();
            // Get children, sort by name for consistent MD5 calculation
            List<Node> sortedChildren = directoryNode.getChildren().values().stream()
                    .sorted(Comparator.comparing(Node::getName))
                    .collect(Collectors.toList());

            for (Node child : sortedChildren) {
                String childMd5;
                if (child.isDirectory()) {
                    // Recursively calculate child directory MD5 if not already done (or re-calculate)
                    // Check if child MD5 is already calculated in this run maybe? Avoid redundant calls?
                    // For initial build, it forces calculation. For updates, it ensures consistency.
                    childMd5 = calculateAllDirectoryMD5sBottomUp(child);
                } else {
                    childMd5 = child.getMd5();
                }

                if (childMd5 != null) {
                    childMd5sConcatenated.append(childMd5);
                } else {
                    // Handle case where child MD5 is unexpectedly null (e.g., file read error)
                    System.err.println("Warning: Child MD5 is null for " + child.getRelativePath() + " while calculating parent " + directoryNode.getRelativePath());
                    childMd5sConcatenated.append("NULL_MD5"); // Append placeholder
                }
            }

            String newMd5 = MD5Util.calculateMD5FromString(childMd5sConcatenated.toString());
            directoryNode.setMd5(newMd5);
            //System.out.println("    Calculated MD5 for Dir " + directoryNode.getRelativePath() + ": " + newMd5);
            return newMd5;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a node (and its children if it's a directory) from the tree.
     * @param relativePath The relative path of the node to remove.
     * @param updateMD5s If true, triggers parent MD5 updates after removal.
     */
    public void removeNodeFromTree(String relativePath, boolean updateMD5s) throws IOException {
        lock.writeLock().lock(); // Exclusive lock for structure modification
        try {
            Node nodeToRemove = nodeMap.get(relativePath);
            if (nodeToRemove == null) {
                //System.out.println("Node already removed or never existed: " + relativePath);
                return; // Node doesn't exist
            }

            Node parent = nodeToRemove.getParent();

            // Recursively remove children from the map if it's a directory
            if (nodeToRemove.isDirectory()) {
                // Create a copy of children keys to avoid ConcurrentModificationException
                List<String> childNames = new ArrayList<>(nodeToRemove.getChildren().keySet());
                for (String childName : childNames) {
                    Node childNode = nodeToRemove.getChild(childName);
                    if (childNode != null) {
                        removeNodeFromTree(childNode.getRelativePath(), false); // Recursive call, don't update MD5s yet
                    }
                }
            }

            // Remove node from parent's children list
            if (parent != null) {
                parent.removeChild(nodeToRemove.getName());
            }

            // Remove node from the main map
            nodeMap.remove(relativePath);
            //System.out.println("    Removed node: " + relativePath);


            // Trigger parent MD5 updates if requested and parent exists
            if (updateMD5s && parent != null) {
                //System.out.println("  Triggering parent MD5 update from removeNodeFromTree for parent of " + relativePath);
                updateParentDirectoryMD5s(parent); // Update the parent itself
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Updates the MD5 for a file node and triggers parent updates.
     * @param relativePath Relative path of the file.
     * @param newMd5 The new MD5 hash.
     */
    public void updateFileNodeMD5(String relativePath, String newMd5) throws IOException {
        lock.writeLock().lock(); // Write lock to modify MD5
        try {
            Node node = nodeMap.get(relativePath);
            if (node != null && !node.isDirectory()) {
                node.setMd5(newMd5);
                //System.out.println("    Updated File MD5: " + relativePath + " -> " + newMd5);
                updateParentDirectoryMD5s(node); // Trigger parent updates
            } else if (node == null) {
                System.err.println("Warning: Trying to update MD5 for non-existent node: " + relativePath);
            } else {
                System.err.println("Warning: Trying to update MD5 for a directory node using updateFileNodeMD5: " + relativePath);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }


    /**
     * Recalculates and updates the MD5s of parent directories upwards from the given node.
     * @param startingNode The node whose parents need updating.
     */
    public void updateParentDirectoryMD5s(Node startingNode) throws IOException {
        lock.writeLock().lock(); // Write lock as we are modifying MD5s
        try {
            Node parent = startingNode.getParent();
            while (parent != null) {
                String oldMd5 = parent.getMd5();
                String newMd5 = calculateSingleDirectoryMD5(parent); // Calculate just this one directory
                if (!newMd5.equals(oldMd5)) {
                    parent.setMd5(newMd5);
                    //System.out.println("      Updated Parent Dir MD5: " + parent.getRelativePath() + " -> " + newMd5);
                    parent = parent.getParent(); // Continue upwards
                } else {
                    //System.out.println("      Parent Dir MD5 unchanged: " + parent.getRelativePath());
                    break; // If parent MD5 didn't change, no need to go further up
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Calculates the MD5 for a *single* directory based on its current children's MD5s stored in the tree.
     * Assumes child MD5s are up-to-date. Called by updateParentDirectoryMD5s.
     * Requires read lock if called externally, assumes write lock if called internally by update logic.
     */
    public String calculateSingleDirectoryMD5(Node directoryNode) {
        // Assumes caller holds appropriate lock (read or write)

        if (!directoryNode.isDirectory()) {
            throw new IllegalArgumentException("Cannot calculate directory MD5 for a file node: " + directoryNode.getRelativePath());
        }

        StringBuilder childMd5sConcatenated = new StringBuilder();
        List<Node> sortedChildren = directoryNode.getChildren().values().stream()
                .sorted(Comparator.comparing(Node::getName))
                .collect(Collectors.toList());

        if (sortedChildren.isEmpty()) {
            return MD5Util.calculateMD5FromString(""); // Empty directory
        }

        for (Node child : sortedChildren) {
            String childMd5 = child.getMd5();
            if (childMd5 != null) {
                childMd5sConcatenated.append(childMd5);
            } else {
                // This indicates an inconsistency in the tree's MD5 state
                System.err.println("Critical Warning: Child node " + child.getRelativePath() + " has null MD5 during parent calculation for " + directoryNode.getRelativePath());
                childMd5sConcatenated.append("NULL_CHILD_MD5"); // Use a placeholder
            }
        }
        return MD5Util.calculateMD5FromString(childMd5sConcatenated.toString());
    }

    // --- Public Accessor Methods ---

    public Optional<Node> getNode(String relativePath) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(nodeMap.get(relativePath));
        } finally {
            lock.readLock().unlock();
        }
    }

    // Optional: Provide a way to get MD5 directly if needed, using the node map
    public Optional<String> getMd5(String relativePath) {
        lock.readLock().lock();
        try {
            Node node = nodeMap.get(relativePath);
            return Optional.ofNullable(node).map(Node::getMd5);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Path getRootPath() {
        return rootPath;
    }

    public String getRelativePath(Path fullPath) {
        return rootPath.relativize(fullPath.toAbsolutePath()).toString();
    }

    public Path getAbsolutePath(String relativePath) {
        return rootPath.resolve(relativePath).normalize();
    }

    // Optional: Method to get all nodes (returns a copy for safety)
    public Map<String, Node> getAllNodes() {
        lock.readLock().lock();
        try {
            return new ConcurrentHashMap<>(nodeMap); // Return a copy
        } finally {
            lock.readLock().unlock();
        }
    }
}