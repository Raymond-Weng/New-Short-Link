package com.raymondweng.newshortlink;

import com.raymondweng.newshortlink.exception.LinkNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.sql.SQLException;

@RestController
public class RequestController {
    @GetMapping("/{id}")
    public RedirectView request(@PathVariable String id) throws SQLException {
        try {
            return new RedirectView(LinkManager.getURL(id));
        } catch (LinkNotFoundException e) {
            return new RedirectView("/404");
        }
    }

    @GetMapping("/discord")
    public RedirectView discord() throws SQLException {
        return new RedirectView("https://discord.com/invite/yvdDjgPwxf");
    }
}
