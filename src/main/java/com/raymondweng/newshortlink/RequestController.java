package com.raymondweng.newshortlink;

import com.raymondweng.newshortlink.request.Link;
import com.raymondweng.newshortlink.response.CreateResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.sql.SQLException;
import java.util.Objects;

@RestController
public class RequestController {
    @GetMapping("/{id}")
    public RedirectView request(@PathVariable String id) throws SQLException {
        return new RedirectView(Objects.requireNonNullElse(LinkManager.getURL(id), "/404"));
    }

    @GetMapping("/discord")
    public RedirectView discord() {
        return new RedirectView("https://discord.com/invite/yvdDjgPwxf");
    }

    @PostMapping("/create/{name}")
    public CreateResponse create(@PathVariable String name, @RequestBody Link link) throws SQLException {
        return create(name, link.getLink());
    }

    public CreateResponse create(String name, String link) throws SQLException {
        CreateResponse response = new CreateResponse();
        response.setLink(link);
        System.out.println(name);
        if (name.equals("free")) {
            String id = LinkManager.getLink();
            while (!LinkManager.useName(link, id)) {
                id = LinkManager.getLink();
            }
            response.setShort_link("https://rwlink.us.kg/" + id);
            response.setError(null);
        } else if (LinkManager.useName(link, name)) {
            response.setShort_link("https://rwlink.us.kg/" + name);
            response.setError(null);
        } else {
            response.setError("The name may be used or invalid.");
        }

        return response;
    }
}
