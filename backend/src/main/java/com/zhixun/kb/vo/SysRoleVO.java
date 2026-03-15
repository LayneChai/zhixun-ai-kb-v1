package com.zhixun.kb.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SysRoleVO {
    private Long id;
    private String roleName;
    private String roleKey;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<Long> menuIds;
}

