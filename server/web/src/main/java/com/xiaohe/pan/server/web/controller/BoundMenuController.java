package com.xiaohe.pan.server.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.model.dto.EventDTO;
import com.xiaohe.pan.common.model.dto.EventsDTO;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import com.xiaohe.pan.server.web.model.domain.Device;
import com.xiaohe.pan.server.web.model.dto.BoundMenuDTO;
import com.xiaohe.pan.server.web.model.vo.BoundMenuVO;
import com.xiaohe.pan.server.web.service.BoundMenuService;
import com.xiaohe.pan.server.web.service.DeviceService;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/bound")
public class BoundMenuController {

    private static final Logger logger = LoggerFactory.getLogger(BoundMenuController.class);

    @Resource
    private BoundMenuService boundMenuService;

    @Resource
    private DeviceService deviceService;


    @PostMapping("/createBinding")
    public Result<BoundMenu> createBinding(@RequestBody BoundMenu request) throws JsonProcessingException {
        Long userId = SecurityContextUtil.getCurrentUserId();
        return Result.success(boundMenuService.createBinding(userId, request));
    }

    @DeleteMapping("/{bindingId}")
    public Result<Void> removeBinding(@PathVariable Long bindingId) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        boundMenuService.removeBinding(userId, bindingId);
        return Result.success("删除成功");
    }

    @GetMapping("/{deviceId}")
    public Result<List<BoundMenuVO>> getDeviceBindings(@PathVariable Long deviceId) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        return Result.success(boundMenuService.getDeviceBindings(userId, deviceId));
    }

    @PostMapping("/sync")
    public Result<String> sync(@RequestParam(required = false) Map<String, MultipartFile> files, @RequestBody EventsDTO eventsDTO) {
        // 验证设备密钥
        if (eventsDTO == null || eventsDTO.getDeviceId() == null || !StringUtils.hasText(eventsDTO.getSecret())) {
            return Result.error("设备ID或密钥不能为空");
        }
        
        try {
            // 验证设备密钥
            Device device = deviceService.verifySecret(String.valueOf(eventsDTO.getDeviceId()), eventsDTO.getSecret());
            
            logger.info("接收到来自设备 {} 的同步请求，共 {} 个事件，{} 个文件", 
                    eventsDTO.getDeviceId(), 
                    eventsDTO.getEvents() != null ? eventsDTO.getEvents().size() : 0,
                    files != null ? files.size() : 0);
            
            // 按时间戳排序处理事件
            if (eventsDTO.getEvents() != null && !eventsDTO.getEvents().isEmpty()) {
                // 按时间戳排序
                eventsDTO.getEvents().sort((e1, e2) -> (int) (e1.getTimestamp() - e2.getTimestamp()));
                
                // 处理每个事件
                for (EventDTO eventDTO : eventsDTO.getEvents()) {
                    logger.info("处理事件: {}, 路径: {}", eventDTO.getType(), eventDTO.getLocalPath());
                    
                    // 查找与此事件关联的文件
                    MultipartFile eventFile = null;
                    if (files != null && !files.isEmpty()) {
                        // 尝试查找与事件路径匹配的文件
                        for (Map.Entry<String, MultipartFile> entry : files.entrySet()) {
                            if (entry.getKey().contains(eventDTO.getLocalPath()) || 
                                (entry.getValue().getOriginalFilename() != null && 
                                 entry.getValue().getOriginalFilename().contains(eventDTO.getLocalPath()))) {
                                eventFile = entry.getValue();
                                break;
                            }
                        }
                    }
                    
                    if (eventFile != null) {
                        logger.info("事件 {} 关联的文件: {}, 大小: {}", 
                                eventDTO.getLocalPath(), 
                                eventFile.getOriginalFilename(), 
                                eventFile.getSize());
                        // TODO: 处理与事件关联的文件
                    }
                    
                    // TODO: 实现事件处理逻辑
                }
            }
            
            return Result.success("同步成功");
        } catch (BusinessException e) {
            logger.error("同步请求处理失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("同步请求处理异常", e);
            return Result.error("服务器内部错误");
        }
    }
}
