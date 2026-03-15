package com.zhixun.kb.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SysUserVO {
    private Long id;
    private String username;
    private Integer status;
    private LocalDateTime createdAt;
    private List<Long> roleIds;
    private List<String> roleNames;
}

