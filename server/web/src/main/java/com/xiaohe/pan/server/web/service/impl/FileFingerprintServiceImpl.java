package com.xiaohe.pan.server.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.pan.server.web.mapper.FileFingerprintMapper;
import com.xiaohe.pan.server.web.model.domain.FileFingerprint;
import com.xiaohe.pan.server.web.service.FileFingerprintService;
import org.springframework.stereotype.Service;

@Service
public class FileFingerprintServiceImpl extends ServiceImpl<FileFingerprintMapper, FileFingerprint> implements FileFingerprintService {
}