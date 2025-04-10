package com.xiaohe.pan.server.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.model.dto.EventDTO;
import com.xiaohe.pan.common.model.dto.EventsDTO;
import com.xiaohe.pan.common.util.Result;
import com.xiaohe.pan.server.web.model.domain.BoundMenu;
import com.xiaohe.pan.server.web.model.domain.Device;
import com.xiaohe.pan.server.web.model.dto.BoundMenuDTO;
import com.xiaohe.pan.common.enums.EventType;
import com.xiaohe.pan.server.web.model.vo.BoundMenuVO;
import com.xiaohe.pan.server.web.service.BoundMenuService;
import com.xiaohe.pan.server.web.service.DeviceService;
import com.xiaohe.pan.server.web.util.SecurityContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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
    public Result<String> sync(@RequestPart("json") String jsonData,
                               HttpServletRequest request) throws IOException {
        // 解析JSON数据
        ObjectMapper objectMapper = new ObjectMapper();
        EventsDTO eventsDTO = objectMapper.readValue(jsonData, EventsDTO.class);

        // 验证设备和密钥
        Device device = deviceService.verifySecret(
                String.valueOf(eventsDTO.getDeviceId()),
                eventsDTO.getSecret()
        );

        // 获取所有上传的文件
        Map<String, MultipartFile> fileMap = new HashMap<>();
        if (request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            fileMap = multipartRequest.getFileMap();
        }

        // 处理每个事件
        for (EventDTO eventDTO : eventsDTO.getEvents()) {
            // 获取关联的文件（如果有）
            MultipartFile file = null;
            if (eventDTO.getFileIndex() != null) {
                String fileKey = "file_" + eventDTO.getFileIndex();
                file = fileMap.get(fileKey);
            }

            // 根据事件类型处理
//            switch (eventDTO.getType()) {
//                case EventType.FILE_CREATE:
//                    // 处理文件创建
//                    if (file != null) {
//                        // 保存文件到适当的位置
////                        fileService.saveFile(eventDTO.getRemoteMenuId(), eventDTO.getLocalPath(), file);
//                    } else {
//                        // 创建目录
////                        fileService.createDirectory(eventDTO.getRemoteMenuId(), eventDTO.getLocalPath());
//                    }
//                    break;
//                case EventType.FILE_MODIFY:
//                    // 处理文件修改
//                    if (file != null) {
////                        fileService.updateFile(eventDTO.getRemoteMenuId(), eventDTO.getLocalPath(), file);
//                    }
//                    break;
//                case EventType.FILE_DELETE:
//                    // 处理文件/目录删除
////                    fileService.deleteFile(eventDTO.getRemoteMenuId(), eventDTO.getLocalPath());
//                    break;
//                // 根据需要处理其他事件类型
//            }
        }

        return Result.success("同步成功");
    }
}
