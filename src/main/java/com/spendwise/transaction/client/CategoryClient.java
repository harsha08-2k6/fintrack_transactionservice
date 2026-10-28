package com.spendwise.transaction.client;

import com.spendwise.transaction.dto.CategoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "category-service")
public interface CategoryClient {

    @GetMapping("/api/categories/{id}")
    CategoryDto getCategoryById(@PathVariable("id") Long id);
}
