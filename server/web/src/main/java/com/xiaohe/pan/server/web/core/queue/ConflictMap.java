package com.xiaohe.pan.server.web.core.queue;

import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.Menu;
import com.xiaohe.pan.server.web.model.vo.ConflictVO;
import com.xiaohe.pan.server.web.model.vo.FileConflictVO;
import com.xiaohe.pan.server.web.model.vo.MenuConflictVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ConflictMap {
    /**
     * key: 发生的事件全路径（云端）
     * value：事件
     */
    public static ConcurrentHashMap<String, FileConflictVO> fileConflictMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, MenuConflictVO> menuConflictMap = new ConcurrentHashMap<>();

    public void addFileConflict(File file, String oldName, Integer type) {
        FileConflictVO fileConflictVO = new FileConflictVO();
        fileConflictVO.setFile(file);
        fileConflictVO.setOldName(oldName);
        fileConflictVO.setType(type);
        fileConflictMap.put(file.getDisplayPath(), fileConflictVO);
    }
    public void addMenuConflict(Menu menu, String oldName, Integer type) {
        MenuConflictVO menuConflictVO = new MenuConflictVO();
        menuConflictVO.setMenu(menu);
        menuConflictVO.setOldName(oldName);
        menuConflictVO.setType(type);
        menuConflictMap.put(oldName, menuConflictVO);
    }
    public ConflictVO getAllConflicts(String displayPath) {
        ConflictVO result = new ConflictVO();
        List<FileConflictVO> fileResults = new ArrayList<>();
        List<MenuConflictVO> menuResults = new ArrayList<>();

        // 处理文件冲突
        fileConflictMap.forEach((path, conflict) -> {
            if (path.startsWith(displayPath)) {
                // 检查是否属于某个目录冲突
                boolean isSubFile = menuConflictMap.keySet().stream()
                        .anyMatch(menuPath -> path.startsWith(menuPath + "/"));

                if (!isSubFile) {
                    fileResults.add(conflict);
                }
            }
        });

        // 处理目录冲突
        menuConflictMap.forEach((path, conflict) -> {
            if (path.startsWith(displayPath)) {
                // 收集该目录下的子文件冲突
                List<FileConflictVO> subFiles = fileConflictMap.entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith(path + "/"))
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList());

                conflict.setSubFileList(subFiles);
                menuResults.add(conflict);
            }
        });

        result.setFileConflictVOList(fileResults);
        result.setMenuConflictVOList(menuResults);
        return result;
    }
    public void clear() {
        fileConflictMap.clear();
        menuConflictMap.clear();
    }
}
