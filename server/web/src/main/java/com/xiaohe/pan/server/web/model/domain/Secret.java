package com.xiaohe.pan.server.web.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class Secret extends BaseDomain {
    private Long id;
    private Long userId;
    @TableField("`key`") // 处理SQL关键字冲突
    private String key;
    private String value;
}
