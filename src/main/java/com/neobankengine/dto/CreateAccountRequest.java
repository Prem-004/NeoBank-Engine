package com.neobankengine.dto;

import lombok.Data;

@Data
public class CreateAccountRequest
{
    // optional initial deposit
    private Double initialDeposit = 0.0;
}
