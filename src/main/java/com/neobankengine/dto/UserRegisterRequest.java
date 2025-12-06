package com.neobankengine.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email address")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}
