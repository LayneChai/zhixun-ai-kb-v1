package com.zhixun.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("qa_message")
public class QaMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String question;
    private String answer;
    private String referencesJson;
    private Long latencyMs;
    private Integer tokenCount;
    private LocalDateTime createdAt;
}
