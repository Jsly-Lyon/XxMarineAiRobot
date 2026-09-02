package com.hhuly.ai.robot.domain.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户 DO 实体类
 *
 * @author: li
 * @date: 2026/9/2
 * @description: 对应 t_user 表；登录鉴权与用户画像载体
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user")
public class UserDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名（唯一） */
    private String username;

    /** BCrypt 密文 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 角色：USER / ADMIN 等 */
    private String role;

    /** 状态：1 正常 / 0 禁用 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
