package com.eventpro.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Builder @AllArgsConstructor
@Getter @Setter
public class ErrorResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> fieldErrors;
}
