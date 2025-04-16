package com.xiaohe.pan.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MD5Util {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    // getMD5(File), getMD5(Path), calculateFileMD5, bytesToHex 方法保持不变

    public static String getMD5(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            return null; // 或者抛出异常
        }
        return calculateFileMD5(file.toPath());
    }

    public static String getMD5(Path path) throws IOException {
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            return null;
        }
        return calculateFileMD5(path);
    }

    private static String calculateFileMD5(Path path) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    md.update(buffer, 0, read);
                }
            }
            return bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            // MD5 应该总是可用的
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * 计算目录的 MD5 值
     * 目录的 MD5 值基于其直接子项的 MD5
     * 1. 列出所有直接子项（文件和目录）
     * 2. 如果没有子项，目录 MD5 为空字符串 "" 的 MD5
     * 3. 如果有子项：
     *    a. 按子项名称排序
     *    b. 从传入的 childrenMD5Map (来自 MD5Storage) 获取每个子项的 MD5
     *    c. 将这些子项的 MD5 值按排序后的顺序拼接起来
     *    d. 计算拼接后字符串的 MD5
     *
     * @param directoryPath 要计算 MD5 的目录路径
     * @param childrenMD5Map 包含当前所有已知 MD5 的 Map (key: 相对路径, value: MD5)
     * @param storageRootPath MD5Storage 的根路径，用于计算子项的相对路径
     * @return 目录的 MD5 字符串
     * @throws IOException 如果读取目录时发生 IO 错误
     */
    public static String calculateDirectoryMD5(Path directoryPath, Map<String, String> childrenMD5Map, Path storageRootPath) throws IOException {
        // 确保是目录
        if (!Files.isDirectory(directoryPath)) {
            // 理论上不应发生，因为调用者应确保
            System.err.println("Warning: calculateDirectoryMD5 called on non-directory: " + directoryPath);
            // 返回空字符串的 MD5 或 null，取决于错误处理策略
            return calculateMD5FromString("");
        }

        List<Path> children;
        try (Stream<Path> stream = Files.list(directoryPath)) {
            // 按文件名排序
            children = stream.sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
        }

        // 1. 如果目录为空
        if (children.isEmpty()) {
            return calculateMD5FromString("");
        }

        // 2. 目录不为空，拼接子项 MD5
        StringBuilder md5StringToHash = new StringBuilder();
        boolean missingChildMD5 = false;

        for (Path child : children) {
            // 计算子项相对于 storage 根目录的路径，作为 Map 的 Key
            String childRelativePath = storageRootPath.relativize(child).toString();
            String childMD5 = childrenMD5Map.get(childRelativePath);

            if (childMD5 != null) {
                // System.out.println("    Appending child MD5: " + child.getFileName() + " -> " + childMD5);
                md5StringToHash.append(childMD5); // 只拼接 MD5 值
            } else {
                // 子项的 MD5 不在 Map 中！这在 bottom-up 计算中理论上不应发生
                // 除非文件刚被添加但 MD5 尚未存入 map，或者 map 数据不一致
                System.err.println("Error: MD5 not found in map for child: " + childRelativePath + " while calculating MD5 for directory: " + directoryPath);
                md5StringToHash.append(child.getFileName().toString());
                // 3. 尝试立即计算子项 MD5（如果它是文件，如果是目录则问题更复杂）
                missingChildMD5 = true;
            }
        }

        // 如果有子项 MD5 缺失，可以根据策略决定是否返回计算结果
        if (missingChildMD5) {
            System.err.println("Warning: Directory MD5 for " + storageRootPath.relativize(directoryPath) + " might be inaccurate due to missing child MD5s.");
            // 可能返回 null 或继续计算一个（可能不准确的）MD5
        }

        // 计算拼接后字符串的 MD5
        return calculateMD5FromString(md5StringToHash.toString());
    }


    /**
     * 计算给定字符串的 MD5
     * @param content 输入字符串
     * @return MD5 哈希值
     */
    public static String calculateMD5FromString(String content) {
        if (content == null) {
            content = "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes());
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }


    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_DIGITS[v >>> 4];
            hexChars[j * 2 + 1] = HEX_DIGITS[v & 0x0F];
        }
        return new String(hexChars);
    }
}