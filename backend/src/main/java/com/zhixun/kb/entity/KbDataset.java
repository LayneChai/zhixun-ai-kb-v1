package com.zhixun.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_dataset")
public class KbDataset {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Long createdBy;
    private Integer status;
    private Integer isDefault;
    private LocalDateTime createdAt;
}
