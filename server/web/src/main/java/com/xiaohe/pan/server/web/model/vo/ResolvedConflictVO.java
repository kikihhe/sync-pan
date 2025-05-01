package com.xiaohe.pan.server.web.model.vo;

import com.xiaohe.pan.server.web.model.domain.File;
import com.xiaohe.pan.server.web.model.domain.Menu;
import lombok.Data;

import java.util.List;

@Data
public class ResolvedConflictVO {
    private List<File> resolvedFileList;
    private List<Menu> resolvedMenuList;
}
