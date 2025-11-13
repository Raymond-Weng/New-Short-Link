package com.raymondweng.newshortlink;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SiteController {
    @RequestMapping("/")
    public String index() {
        if (NewShortLinkApplication.dotenv.get("MAINTAIN").equals("true")) {
            return "maintain.html";
        }
        return "index.html";
    }

    @RequestMapping("/api")
    public String api() {
        return "api.html";
    }

    @RequestMapping("/404")
    public String notFound() {
        return "404.html";
    }

    @RequestMapping("/contacts")
    public String contacts() {
        return "contact.html";
    }


}
