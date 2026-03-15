package com.zhixun.kb.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleSaveRequest {
    private String roleName;
    private String roleKey;
    private String status;
    private List<Long> menuIds;
}

