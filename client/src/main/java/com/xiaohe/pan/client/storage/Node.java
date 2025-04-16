// Create a new file: src/main/java/com/xiaohe/pan/client/storage/Node.java
package com.xiaohe.pan.client.storage;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class Node {
    private final String name; // File or directory name
    private final String relativePath; // Path relative to the storage root
    private final boolean isDirectory;
    @Setter private String md5; // MD5 hash (can be null initially for directories)
    private final Node parent; // Reference to the parent node (null for root)
    // Use ConcurrentHashMap for thread safety when modifying children
    private final Map<String, Node> children = new ConcurrentHashMap<>(); // Key: child name

    // Constructor for the root node
    public Node(String name, String relativePath, boolean isDirectory) {
        this(name, relativePath, isDirectory, null, null); // Root has no parent, MD5 calculated later
    }

    // General constructor
    public Node(String name, String relativePath, boolean isDirectory, Node parent, String md5) {
        this.name = name;
        this.relativePath = relativePath;
        this.isDirectory = isDirectory;
        this.parent = parent;
        this.md5 = md5;
    }

    public void addChild(Node child) {
        if (child != null) {
            this.children.put(child.getName(), child);
        }
    }

    public Node removeChild(String childName) {
        return this.children.remove(childName);
    }

    public Node getChild(String childName) {
        return this.children.get(childName);
    }

    // Optional: Override equals and hashCode based on relativePath for uniqueness checks
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return java.util.Objects.equals(relativePath, node.relativePath);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(relativePath);
    }

    @Override
    public String toString() {
        return "Node{" +
               "name='" + name + '\'' +
               ", relativePath='" + relativePath + '\'' +
               ", isDirectory=" + isDirectory +
               ", md5='" + (md5 != null ? md5.substring(0, Math.min(md5.length(), 8)) + "..." : "null") + '\'' + // Truncate MD5 for brevity
               ", childrenCount=" + children.size() +
               '}';
    }
}