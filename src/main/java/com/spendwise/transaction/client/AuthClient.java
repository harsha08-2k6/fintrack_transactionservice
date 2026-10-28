package com.spendwise.transaction.client;

import com.spendwise.transaction.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/api/auth/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);
}
