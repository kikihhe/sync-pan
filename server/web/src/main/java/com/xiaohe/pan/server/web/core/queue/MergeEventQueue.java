package com.xiaohe.pan.server.web.core.queue;

import com.alibaba.fastjson.JSON;
import com.xiaohe.pan.server.web.model.dto.ResolveConflictDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MergeEventQueue {
    @Autowired
    private StringRedisTemplate redisTemplate;

    public void addResolveConflict(String deviceKey, ResolveConflictDTO dto) {
        // 序列化 DTO 为 JSON
        String json = JSON.toJSONString(dto);
        // 存储到 Redis List 右侧
        redisTemplate.opsForList().rightPush(deviceKey, json);
    }

    public List<ResolveConflictDTO> pollResolveConflict(String deviceKey) throws InterruptedException {
        // 获取并删除整个 list
        Long size = redisTemplate.opsForList().size(deviceKey);
        if (size == null || size == 0) {
            return new ArrayList<>();
        }

        List<String> jsonList = redisTemplate.opsForList().range(deviceKey, 0, -1);
        redisTemplate.delete(deviceKey);  // 原子操作删除整个 list

        return jsonList.stream()
                .map(json -> JSON.parseObject(json, ResolveConflictDTO.class))
                .collect(Collectors.toList());
    }

    public void clearResolveConflict(String deviceKey, Long currentMenuId) {
        if (deviceKey == null) return;
        if (currentMenuId == null) {
            // 删除整个 list
            redisTemplate.delete(deviceKey);
        } else {
            // 使用 Lua 脚本删除指定 currentMenuId 的条目
            String script =
                    "local key = KEYS[1] " +
                            "local targetId = ARGV[1] " +
                            "local items = redis.call('LRANGE', key, 0, -1) " +
                            "local newList = {} " +
                            "for i, item in ipairs(items) do " +
                            "    local dto = cjson.decode(item) " +
                            "    if dto.currentMenuId ~= targetId then " +
                            "        table.insert(newList, item) " +
                            "    end " +
                            "end " +
                            "redis.call('DEL', key) " +
                            "if #newList > 0 then " +
                            "    redis.call('RPUSH', key, unpack(newList)) " +
                            "end";
            redisTemplate.execute(
                    new DefaultRedisScript<>(script, Long.class),
                    Collections.singletonList(deviceKey),
                    Collections.singletonList(currentMenuId.toString())
            );
        }
    }




//
//    private BlockingQueue<MergeEvent> mergeEventQueue = new LinkedBlockingQueue<>();
//    /**
//     * key: device_key
//     */
//    private ConcurrentHashMap<String, BlockingQueue<ResolveConflictDTO>> resolveConflictQueue = new ConcurrentHashMap<>();
//    public void addResolveConflict(String deviceKey, ResolveConflictDTO resolveConflictDTO) {
//            resolveConflictQueue.computeIfAbsent(deviceKey, k -> new LinkedBlockingQueue<>(10))
//                    .offer(resolveConflictDTO);
//    }
//    public List<ResolveConflictDTO> pollResolveConflict(String deviceKey) throws InterruptedException {
//        BlockingQueue<ResolveConflictDTO> queue = resolveConflictQueue.get(deviceKey);
//        if (queue == null) {
//            return new ArrayList<>();
//        }
//        ResolveConflictDTO poll = queue.poll(3, TimeUnit.SECONDS);
//        if (poll == null) {
//            return null;
//        }
//        List<ResolveConflictDTO> resolveConflictVOS = new ArrayList<>();
//        resolveConflictVOS.add(poll);
//        resolveConflictQueue.clear();
//        return resolveConflictVOS;
//    }
//
//    public void clearResolveConflict(String deviceKey) throws InterruptedException {
//        resolveConflictQueue.remove(deviceKey);
//    }
//    public boolean isEmpty(String deviceKey) {
//        BlockingQueue<ResolveConflictDTO> resolveConflictDTOS = resolveConflictQueue.get(deviceKey);
//        return CollectionUtils.isEmpty(resolveConflictDTOS);
//    }

}
