package com.zhixun.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AskRequest {
    @NotNull
    private Long sessionId;
    @NotBlank
    private String question;
    private Boolean stream = false;
}
