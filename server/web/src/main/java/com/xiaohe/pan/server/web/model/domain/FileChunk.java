package com.xiaohe.pan.server.web.model.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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
import java.util.Date;

/**
 * 文件分片
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class FileChunk extends BaseDomain {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分片名称
     * ${文件名}_chunk_${chunkNumber}
     */
    private String filename;

    /**
     * 分片所属文件的唯一标识
     * 合并时通过 identifier 判断哪些分片属于同一个文件
     */
    private String identifier;

    /**
     * 分片的唯一标识
     * 断点重连后通过 chunkIdentifier 判断分片是否需要再次传输
     */
    private String chunkIdentifier;

    /**
     * 分片所属文件所在的目录
     */
    private Long menuId;

    /**
     * 分片真实的存储路径
     */
    private String realPath;

    /**
     * 分片编号
     */
    private Integer chunkNumber;

    /**
     * 分片的总个数
     */
    private Integer totalChunks;

    /**
     * 分片文件的大小
     */
    private Long chunkSize;

    /**
     * 分片所属文件的类型
     */
    private String fileType;

    private Long owner;

    /**
     * 分片所属文件的总大小
     */
    private Long totalSize;

    /**
     * 过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime expirationTime;

    /**
     * 存储类型
     */
    private Integer storageType;
}
