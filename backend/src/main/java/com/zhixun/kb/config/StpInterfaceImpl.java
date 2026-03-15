package com.zhixun.kb.config;

import cn.dev33.satoken.stp.StpInterface;
import com.zhixun.kb.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private static final List<String> ADMIN_EXTRA_PERMS = List.of("system:log:list");

    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        List<String> roleKeys = sysMenuMapper.selectRoleKeysByUserId(userId);
        if (roleKeys.contains("admin") || roleKeys.contains("super-admin")) {
            Set<String> perms = new LinkedHashSet<>(sysMenuMapper.selectAllPerms());
            perms.addAll(ADMIN_EXTRA_PERMS);
            return new ArrayList<>(perms);
        }
        return sysMenuMapper.selectPermsByUserId(userId);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return sysMenuMapper.selectRoleKeysByUserId(Long.parseLong(loginId.toString()));
    }
}
