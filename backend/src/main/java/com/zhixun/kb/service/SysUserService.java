package com.zhixun.kb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhixun.kb.entity.SysUser;

import java.util.List;

public interface SysUserService extends IService<SysUser> {
    void updateUserRoles(Long userId, List<Long> roleIds);
}
