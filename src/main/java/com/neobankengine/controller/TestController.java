package com.neobankengine.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController
{

    @GetMapping("/protected")
    public String protectedEndpoint()
    {
        return "You reached a protected endpoint!";
    }

    @GetMapping("/whoami")
    public Map<String,Object> whoami() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return Map.of(
                "name", a == null ? null : a.getName(),
                "auth", a == null ? null : a.getAuthorities()
        );
    }

}
