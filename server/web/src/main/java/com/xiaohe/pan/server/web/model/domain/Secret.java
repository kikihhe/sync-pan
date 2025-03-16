package com.xiaohe.pan.server.web.model.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class Secret extends BaseDomain {
    private Long id;
    private Long userId;
    private String key;
    private String value;
}
