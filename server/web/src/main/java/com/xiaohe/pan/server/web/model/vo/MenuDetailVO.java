package com.xiaohe.pan.server.web.model.vo;


import com.xiaohe.pan.server.web.model.domain.Menu;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MenuDetailVO extends Menu {

    /**
     * 该目录下所有文件的总和
     */
    private Long menuSize;
}
