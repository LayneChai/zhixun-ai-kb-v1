package com.zhixun.kb.dto;

import lombok.Data;

@Data
public class MenuSaveRequest {
    private Long parentId;
    private String menuName;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private String menuType;
    private Integer sort;
    private String status;
}

