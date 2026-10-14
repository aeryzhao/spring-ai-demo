package org.aeryzhao.rag.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面入口控制器：仅做首页跳转，不属于 API，通过 {@link Hidden} 排除在 OpenAPI 文档之外。
 */
@Hidden
@Controller
public class PageController {

    @GetMapping({"/", "/api-test", "/app"})
    public String entry() {
        return "redirect:/api-test.html";
    }
}
