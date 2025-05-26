package com.xiaohe.pan.server.web.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("file_fingerprint")
@EqualsAndHashCode(callSuper = true)
public class FileFingerprint extends BaseDomain {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String identifier;
    private String realPath;
    private Integer referenceCount;
    /**
     * 废弃
     */
//    private Long referenceId;
    /**
     * 引用类型,1-file,2-chunk
     */
    private int referenceType;
}