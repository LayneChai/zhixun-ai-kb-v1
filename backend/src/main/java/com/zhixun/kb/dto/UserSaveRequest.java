package com.zhixun.kb.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserSaveRequest {
    private String username;
    private String password;
    private Integer status;
    private List<Long> roleIds;
}

