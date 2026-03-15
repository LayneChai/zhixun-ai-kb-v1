package com.zhixun.kb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhixun.kb.entity.SysMenu;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {
    List<SysMenu> selectMenuTreeByUserId(Long userId);

    List<SysMenu> buildMenuTree(List<SysMenu> menus);
}
