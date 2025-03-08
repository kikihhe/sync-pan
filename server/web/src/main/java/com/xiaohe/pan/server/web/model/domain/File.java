package com.xiaohe.pan.server.web.model.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("file")
public class File {

    private Long id;

    private String fileName;

    private Long fileSize;

    private String fileType;

    private Integer storageType;

    private Long menuId;

    private Long owner;

    /**
     * 文件真实路径
     * storageType=local : 文件在服务器的路径
     * storageType=ali_oss : 文件在阿里云 oss 的url
     * storageType=minio : 文件在 minio 的下载url
     */
    private String realPath;



    @TableField(value = "create_time",fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    private Boolean deleted;
}
