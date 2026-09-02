package com.hhuly.ai.robot.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hhuly.ai.robot.domain.dos.UserDO;
import com.hhuly.ai.robot.domain.mapper.UserMapper;
import com.hhuly.ai.robot.enums.ResponseCodeEnum;
import com.hhuly.ai.robot.exception.BizException;
import com.hhuly.ai.robot.model.vo.auth.LoginReqVO;
import com.hhuly.ai.robot.model.vo.auth.LoginRspVO;
import com.hhuly.ai.robot.model.vo.auth.RegisterUserReqVO;
import com.hhuly.ai.robot.model.vo.auth.UserInfoVO;
import com.hhuly.ai.robot.service.AuthService;
import com.hhuly.ai.robot.utils.Response;
import com.hhuly.ai.robot.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 认证服务实现
 *
 * @author: li
 * @date: 2026/9/2
 **/
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    /** 登录后写入 Sa-Token 会话（由 sa-token-redis 持久化到 Redis）的用户信息键 */
    private static final String SESSION_USER_ID = "login_user_id";
    private static final String SESSION_USERNAME = "login_username";
    private static final String SESSION_NICKNAME = "login_nickname";
    private static final String SESSION_AVATAR = "login_avatar";
    private static final String SESSION_ROLE = "login_role";

    @Override
    public Response<?> register(RegisterUserReqVO registerUserReqVO) {
        String username = registerUserReqVO.getUsername();

        // 用户名唯一性校验
        Long count = userMapper.selectCount(Wrappers.<UserDO>lambdaQuery()
                .eq(UserDO::getUsername, username));
        if (count != null && count > 0) {
            throw new BizException(ResponseCodeEnum.USERNAME_EXISTED);
        }

        // 昵称缺省使用用户名
        String nickname = StringUtils.hasText(registerUserReqVO.getNickname())
                ? registerUserReqVO.getNickname()
                : username;

        LocalDateTime now = LocalDateTime.now();
        userMapper.insert(UserDO.builder()
                .username(username)
                .password(passwordEncoder.encode(registerUserReqVO.getPassword()))
                .nickname(nickname)
                .role("USER")
                .status(1)
                .createTime(now)
                .updateTime(now)
                .build());

        return Response.success();
    }

    @Override
    public Response<LoginRspVO> login(LoginReqVO loginReqVO) {
        UserDO user = userMapper.selectOne(Wrappers.<UserDO>lambdaQuery()
                .eq(UserDO::getUsername, loginReqVO.getUsername()));

        // 用户不存在或密码不匹配，统一提示"用户名或密码错误"，避免暴露用户是否存在
        if (user == null || !matchPassword(loginReqVO.getPassword(), user.getPassword())) {
            throw new BizException(ResponseCodeEnum.USERNAME_OR_PASSWORD_ERROR);
        }

        // Sa-Token 登录（loginId = 用户主键）
        StpUtil.login(user.getId());
        // 登录成功即把用户信息写入会话（Redis），/auth/info 可直接快速命中
        cacheUserInfo(StpUtil.getSession(), user);
        log.info("## 用户登录成功，userId = {}, username = {}", user.getId(), user.getUsername());

        return Response.success(LoginRspVO.builder()
                .token(StpUtil.getTokenInfo().getTokenValue())
                .build());
    }

    @Override
    public Response<?> logout() {
        StpUtil.logout();
        return Response.success();
    }

    @Override
    public Response<UserInfoVO> getLoginUserInfo() {
        if (!UserContext.isLogin()) {
            throw new BizException(ResponseCodeEnum.NOT_LOGIN);
        }

        SaSession session = StpUtil.getSession();
        // 优先读 Redis 会话缓存，未命中则查库并回填缓存
        String cachedUserId = session.getString(SESSION_USER_ID);
        if (!StringUtils.hasText(cachedUserId)) {
            UserDO user = userMapper.selectById(UserContext.getUserId());
            if (user == null) {
                throw new BizException(ResponseCodeEnum.USER_NOT_EXISTED);
            }
            cacheUserInfo(session, user);
            return Response.success(toUserInfoVO(user));
        }

        return Response.success(UserInfoVO.builder()
                .id(Long.valueOf(cachedUserId))
                .username(session.getString(SESSION_USERNAME))
                .nickname(session.getString(SESSION_NICKNAME))
                .avatar(normalize(session.getString(SESSION_AVATAR)))
                .role(session.getString(SESSION_ROLE))
                .build());
    }

    /**
     * 把用户信息写入 Sa-Token Session 并回存 Redis（跟随登录态生命周期自动过期）
     */
    private void cacheUserInfo(SaSession session, UserDO user) {
        session.set(SESSION_USER_ID, String.valueOf(user.getId()));
        session.set(SESSION_USERNAME, user.getUsername());
        session.set(SESSION_NICKNAME,
                StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        session.set(SESSION_AVATAR, user.getAvatar() == null ? "" : user.getAvatar());
        session.set(SESSION_ROLE, StringUtils.hasText(user.getRole()) ? user.getRole() : "USER");
        session.update();
    }

    private UserInfoVO toUserInfoVO(UserDO user) {
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    /**
     * BCrypt 密文校验（密文非法时统一按密码错误处理）
     */
    private boolean matchPassword(String rawPassword, String encodedPassword) {
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
