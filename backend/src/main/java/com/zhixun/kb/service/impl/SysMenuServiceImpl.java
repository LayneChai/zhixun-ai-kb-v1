package com.zhixun.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhixun.kb.entity.SysMenu;
import com.zhixun.kb.mapper.SysMenuMapper;
import com.zhixun.kb.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> selectMenuTreeByUserId(Long userId) {
        List<SysMenu> menus;
        List<String> roleKeys = baseMapper.selectRoleKeysByUserId(userId);
        if (roleKeys.contains("admin") || roleKeys.contains("super-admin")) { // 假设 ID=1 是超级管理员
            menus = list(new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getMenuType, "M", "C")
                    .eq(SysMenu::getStatus, "0")
                    .orderByAsc(SysMenu::getSort));
        } else {
            // 这里可以通过 Mapper 关联查询用户拥有的菜单
            // 简化版：先查出用户所有权限关联的菜单 ID，再查菜单实体
            // 实际项目中建议在 Mapper 写 Join SQL
            menus = baseMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getMenuType, "M", "C")
                    .eq(SysMenu::getStatus, "0")
                    .last("AND id IN (SELECT menu_id FROM sys_role_menu rm JOIN sys_user_role ur ON rm.role_id = ur.role_id WHERE ur.user_id = "
                            + userId + ")")
                    .orderByAsc(SysMenu::getSort));
        }
        return buildMenuTree(menus);
    }

    @Override
    public List<SysMenu> buildMenuTree(List<SysMenu> menus) {
        List<SysMenu> returnList = new ArrayList<>();
        List<Long> tempList = menus.stream().map(SysMenu::getId).collect(Collectors.toList());
        for (SysMenu menu : menus) {
            // 如果是顶级节点，且没有父节点在当前列表中（或者父节点为0）
            if (!tempList.contains(menu.getParentId())) {
                recursionFn(menus, menu);
                returnList.add(menu);
            }
        }
        if (returnList.isEmpty()) {
            returnList = menus;
        }
        return returnList;
    }

    private void recursionFn(List<SysMenu> list, SysMenu t) {
        List<SysMenu> childList = getChildList(list, t);
        t.setChildren(childList);
        for (SysMenu tChild : childList) {
            if (hasChild(list, tChild)) {
                recursionFn(list, tChild);
            }
        }
    }

    private List<SysMenu> getChildList(List<SysMenu> list, SysMenu t) {
        return list.stream()
                .filter(n -> Objects.equals(n.getParentId(), t.getId()))
                .collect(Collectors.toList());
    }

    private boolean hasChild(List<SysMenu> list, SysMenu t) {
        return !getChildList(list, t).isEmpty();
    }
}
