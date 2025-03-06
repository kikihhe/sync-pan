package com.xiaohe.pan.common.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class IPage {

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}