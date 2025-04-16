// src/main/java/com/xiaohe/pan/client/storage/MD5StorageManager.java
package com.xiaohe.pan.client.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MD5StorageFactory {

    private static final MD5StorageFactory INSTANCE = new MD5StorageFactory();

    // Key: 绑定目录的绝对路径 (Path), Value: 对应的 MD5Storage
    private final Map<Path, MD5Storage> storageMap = new ConcurrentHashMap<>();

    private MD5StorageFactory() {}

    public static MD5StorageFactory getInstance() {
        return INSTANCE;
    }

    public MD5Storage createOrGetStorage(String rootDir) throws IOException {
        Path rootPath = Paths.get(rootDir).toAbsolutePath();

        MD5Storage storage = storageMap.computeIfAbsent(rootPath, key -> {
            try {
                System.out.println("Creating new MD5Storage instance for: " + key);
                return new MD5Storage(key); // Create instance
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid root path provided for MD5Storage: " + key + ": " + e.getMessage());
                throw new RuntimeException("Invalid root path for MD5Storage", e);
            }
        });

        return storageMap.computeIfAbsent(rootPath, key -> {
            try {
                System.out.println("Creating and Initializing new MD5Storage for: " + key);
                MD5Storage newStorage = new MD5Storage(key);
                newStorage.buildInitialTree(); // <-- Build tree upon creation
                return newStorage;
            } catch (IOException e) {
                System.err.println("Failed to initialize MD5Storage for " + key + ": " + e.getMessage());
                throw new RuntimeException("Failed to initialize MD5Storage", e);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid root path provided for MD5Storage: " + key + ": " + e.getMessage());
                throw new RuntimeException("Invalid root path for MD5Storage", e);
            }
        });

        // The previous logic was flawed, corrected above using computeIfAbsent correctly.
    }

    /**
     * 根据根目录路径获取 MD5Storage (实例方法)
     * @param rootPath 根目录绝对路径
     * @return Optional 包含 MD5Storage，如果不存在则为空
     */
    public Optional<MD5Storage> getStorage(Path rootPath) {
        return Optional.ofNullable(storageMap.get(rootPath.toAbsolutePath()));
    }

    /**
     * 根据根目录路径字符串获取 MD5Storage
     * @param path 根目录的路径字符串
     * @return Optional 包含 MD5Storage，如果不存在或路径无效则为空
     */
    public Optional<MD5Storage> getMd5Storage(String path) {
        if (path == null || path.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            Path rootPath = Paths.get(path).toAbsolutePath();
            // 调用实例方法来获取
            return INSTANCE.getStorage(rootPath);
        } catch (Exception e) {
            // 处理无效路径等异常
            System.err.println("Error getting MD5Storage for path '" + path + "': " + e.getMessage());
            return Optional.empty();
        }
    }


    /**
     * 根据任意文件/目录路径查找其所属的 MD5Storage (实例方法)
     * @param anyPath 文件系统中的任意绝对路径
     * @return Optional 包含所属的 MD5Storage，如果该路径不属于任何受管理的存储，则为空
     */
    public Optional<MD5Storage> findStorageForPath(Path anyPath) {
        Path absolutePath = anyPath.toAbsolutePath();
        // 遍历所有管理的根路径，看哪个是给定路径的前缀
        for (Path rootPath : storageMap.keySet()) {
            if (absolutePath.startsWith(rootPath)) {
                // 确保获取的是最新的 storage 实例
                return Optional.ofNullable(storageMap.get(rootPath));
                // return Optional.of(storageMap.get(rootPath)); // 旧方式可能获取到 null 如果在迭代中被移除
            }
        }
        return Optional.empty();
    }

    /**
     * 移除指定根目录的 MD5Storage (实例方法)
     * @param rootPath 要移除的存储的根目录绝对路径
     */
    public void removeStorage(Path rootPath) {
        Path absoluteRoot = rootPath.toAbsolutePath();
        if (storageMap.containsKey(absoluteRoot)) {
            System.out.println("Removing MD5Storage for: " + absoluteRoot);
            storageMap.remove(absoluteRoot);
        }
    }
}