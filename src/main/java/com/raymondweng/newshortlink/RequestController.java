package com.raymondweng.newshortlink;

import com.raymondweng.newshortlink.exception.LinkNotFoundException;
import com.raymondweng.newshortlink.request.Link;
import com.raymondweng.newshortlink.response.CreateResponse;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping("/create/{type}")
    public CreateResponse create(@PathVariable String type, @RequestBody Link link) {
        CreateResponse response = new CreateResponse();

        //TODO below is only for testing
        response.setLink(link.getLink());
        response.setError(null);
        response.setShort_link("https://rwlink.us.kg/test");
        response.setExpiration("2024/5/14 00:00");

        return response;
    }
}
