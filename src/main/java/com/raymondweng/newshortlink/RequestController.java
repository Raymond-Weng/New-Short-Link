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

    @PostMapping("/create/{name}")
    public CreateResponse create(@PathVariable String name, @RequestBody Link link) throws SQLException {
        CreateResponse response = new CreateResponse();
        response.setLink(link.getLink());
        String id;
        if(name.equals("free")) {
            id = LinkManager.getLink();
            response.setShort_link("https://rwlink.us.kg/" + id);
            response.setError(null);
        }else if(LinkManager.BAN_KEYS.contains(name)) {
            response.setShort_link(null);
            response.setError("Baned keys requested");
            return response;
        }else{

        }

        //TODO link the link (id) with requested link


        return response;
    }
}
