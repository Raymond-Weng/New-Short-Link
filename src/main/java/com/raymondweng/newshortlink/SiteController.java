package com.raymondweng.newshortlink;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SiteController {
    @RequestMapping("/")
    public String index() {
        return "index.html";
    }

    @RequestMapping("/api")
    public String api() {
        return "api.html";
    }
}
