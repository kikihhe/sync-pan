package com.xiaohe.pan.common.util;

import java.util.List;
import java.util.stream.Collectors;

public class PathUtils {
    /**
     * 查找路径列表的公共前缀
     * @param paths 需要包含至少1个路径
     */
    public static String findCommonPrefix(List<String> paths) {
        if (paths == null || paths.isEmpty()) return "";
        
        // 统一路径分隔符并排序
        List<String> normalized = paths.stream()
            .map(p -> p.replace('\\', '/'))
            .sorted()
            .collect(Collectors.toList());

        String first = normalized.get(0);
        String last = normalized.get(normalized.size()-1);
        
        int minLength = Math.min(first.length(), last.length());
        int diffIndex = 0;
        
        // 查找首个差异位置
        while (diffIndex < minLength 
                && first.charAt(diffIndex) == last.charAt(diffIndex)) {
            diffIndex++;
        }

        // 回退到最后一个目录分隔符
        String potentialPrefix = first.substring(0, diffIndex);
        int lastSlash = potentialPrefix.lastIndexOf('/');
        
        // 处理根目录情况
        if (lastSlash <= 0) {
            return potentialPrefix.startsWith("/") ? "/" : "";
        }
        
        return potentialPrefix.substring(0, lastSlash + 1);
    }
}