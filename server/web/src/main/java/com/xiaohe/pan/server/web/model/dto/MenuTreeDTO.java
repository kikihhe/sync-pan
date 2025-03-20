package com.xiaohe.pan.server.web.model.dto;


import lombok.Data;

import java.util.List;

@Data
public class MenuTreeDTO {
    private Long id;
    private String menuName;
    private Long parentId;
    private Integer menuLevel;
    private String displayPath;
    private List<MenuTreeDTO> children;
}
