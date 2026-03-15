package com.zhixun.kb.config;

import cn.dev33.satoken.stp.StpInterface;
import com.zhixun.kb.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        List<String> roleKeys = sysMenuMapper.selectRoleKeysByUserId(userId);
        if (roleKeys.contains("admin") || roleKeys.contains("super-admin")) {
            return sysMenuMapper.selectAllPerms();
        }
        return sysMenuMapper.selectPermsByUserId(userId);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return sysMenuMapper.selectRoleKeysByUserId(Long.parseLong(loginId.toString()));
    }
}
