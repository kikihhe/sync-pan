package com.xiaohe.pan.server.web.model.vo;


import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.xiaohe.pan.server.web.model.domain.Menu;
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
public class MenuDetailVO {
    /**
     * 目录id
     */
    private Long id;
    /**
     * 目录名称
     */
    private String menuName;
    /**
     * 目录层级
     * 方便查询，不再需要通过 parent 一级一级的组装
     */
    private Integer menuLevel;
    /**
     * 父目录id
     */
    private Long parentId;

    /**
     * 对外展示的路径
     */
    private String displayPath;

    /**
     * 所属用户
     */
    private Long owner;

    /**
     * 文件来源
     * 1: 云端
     * 2: 本地
     * 3: 已合并
     */
    private Integer source;

    /**
     * 是否已经绑定
     */
    private Boolean bound;

    /**
     * 该目录下所有文件的总和
     */
    private Long menuSize;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updateTime;

    private Boolean deleted;

}
