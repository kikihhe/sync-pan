package com.xiaohe.pan.server.web.util;


import com.xiaohe.pan.server.web.constants.RedisConstants;
import com.xiaohe.pan.server.web.mapper.MenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MenuUtil {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ThreadPoolTaskExecutor singleThreadExecutor;

    public void onAddFile(Long menuId, Long fileSize) {
        onUpdateFile(menuId, fileSize);
    }

    public void onDeleteFile(Long menuId, Long fileSize) {
        onUpdateFile(menuId, -fileSize);
    }

    public void onAddMenu(Long menuId, Long parentMenuId) {
        List<Long> parentMenuIdList = getParentMenuIdList(parentMenuId);
        if (!Objects.isNull(parentMenuId)) {
            parentMenuIdList.add(parentMenuId);
        }
        setParentIds(menuId, parentMenuIdList);
    }

    // 核心更新方法（异步+批量操作）
    public void onUpdateFile(Long menuId, Long deltaSize) {
        singleThreadExecutor.execute(() -> {
            List<Long> parentMenuIdList = getParentMenuIdList(menuId);
            parentMenuIdList.add(menuId);
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
                String sizeKey = RedisConstants.MenuRedisConstants.MENU_SIZE_KEY;
                parentMenuIdList.forEach(id -> hashOps.increment(sizeKey, id.toString(), deltaSize));
                return null;
            });
        });
    }

    // 获取目录的父级列表
    public List<Long> getParentMenuIdList(Long menuId) {
        if (Objects.isNull(menuId)) {
            return new ArrayList<>();
        }
        String redisKey = RedisConstants.MenuRedisConstants.MENU_PARENT_KEY + menuId;
        Set<String> parentIds = redisTemplate.opsForSet().members(redisKey);
        if (CollectionUtils.isEmpty(parentIds)) {
            return new ArrayList<>();
        }
        return parentIds.stream().map(Long::parseLong).collect(Collectors.toList());
    }

    // 设置目录的父级列表（异步操作）
    public void setParentIds(Long menuId, List<Long> parentIds) {
        if (CollectionUtils.isEmpty(parentIds)) {
            return;
        }
        singleThreadExecutor.execute(() -> {
            String redisKey = RedisConstants.MenuRedisConstants.MENU_PARENT_KEY + menuId;
            String[] array = parentIds.stream().map(String::valueOf).toArray(String[]::new);
            redisTemplate.opsForSet().add(redisKey, array);
        });
    }
    public Map<Long, Long> batchGetMenuSizes(List<Long> menuIds) {
        if (CollectionUtils.isEmpty(menuIds)) {
            return Collections.emptyMap();
        }

        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        String sizeKey = RedisConstants.MenuRedisConstants.MENU_SIZE_KEY;

        // 转换ID为String类型作为hash字段
        List<String> fields = menuIds.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());

        // 批量获取
        List<String> values = hashOps.multiGet(sizeKey, fields);

        // 构建结果映射
        Map<Long, Long> result = new HashMap<>();
        for (int i = 0; i < menuIds.size(); i++) {
            Long menuId = menuIds.get(i);
            String val = values.get(i);
            result.put(menuId, val != null ? Long.parseLong(val) : 0L);
        }

        return result;
    }

}
