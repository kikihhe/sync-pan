package com.xiaohe.pan.server.web.model.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("file")
@EqualsAndHashCode(callSuper = true)
public class File extends BaseDomain{

    private Long id;

    private String fileName;

    private Long fileSize;

    private String fileType;

    private Integer storageType;

    private Long menuId;

    private Long owner;

    /**
     * 文件唯一标识
     */
    private String identifier;

    /**
     * 文件真实路径
     * storageType=local : 文件在服务器的路径
     * storageType=ali_oss : 文件在阿里云 oss 的url
     * storageType=minio : 文件在 minio 的下载url
     */
    private String realPath;

    /**
     * 文件在网盘中展示的路径
     */
    private String displayPath;
}
