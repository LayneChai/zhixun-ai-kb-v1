package com.zhixun.kb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhixun.kb.entity.SysRole;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {
    void updateRoleMenus(Long roleId, List<Long> menuIds);
}
