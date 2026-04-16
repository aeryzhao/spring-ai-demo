package org.aeryzhao.rag.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/api-test", "/app"})
    public String entry() {
        return "redirect:/api-test.html";
    }
}
