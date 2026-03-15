package com.zhixun.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_config")
public class LlmConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String provider;
    private String displayName;
    private String baseUrl;
    private String apiKey;
    private String model;
    private Integer enabled;
    private Integer isDefault;
    private String extraJson;
    private LocalDateTime createdAt;
}
