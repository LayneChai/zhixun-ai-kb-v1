package com.zhixun.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RenameSessionRequest {
    @NotNull
    private Long sessionId;
    @NotBlank
    private String title;
}
