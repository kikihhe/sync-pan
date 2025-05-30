package com.xiaohe.pan.server.web.model.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("user")
public class User extends BaseDomain implements Serializable {
    private Long id;

    private String username;

    private String password;

    private String email;
    /**
     * 盐
     */
    private String salt;

    /**
     * 密保问题
     */
    private String question;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 密保答案
     */
    private String answer;
}
