package com.neobankengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
public class ApiError
{
    private Instant timestamp;
    private int status;
    private String error;         // e.g. "Not Found"
    private String message;       // human message
    private String path;          // request path
    private List<String> details; // optional for field errors
}
