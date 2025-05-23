package com.xiaohe.pan.server.web.model.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String username;
    private String password;
    private boolean rememberMe;
}
