package com.xiaohe.pan.server.web.model.vo;

import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.Menu;
import lombok.Data;

import java.util.List;

@Data
public class FileAndMenuListVO {
    private List<File> fileList;
    private List<Menu> menuList;
}
