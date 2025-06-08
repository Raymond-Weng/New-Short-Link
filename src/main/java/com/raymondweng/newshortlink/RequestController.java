package com.raymondweng.newshortlink;

import com.raymondweng.newshortlink.request.Link;
import com.raymondweng.newshortlink.response.CreateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;
import java.sql.SQLException;
import java.util.Objects;

@RestController
public class RequestController {
    @GetMapping("/{id}")
    public ResponseEntity<String> request(@PathVariable String id) throws SQLException {
        Pair<String, Boolean> pair = LinkManager.getURL(id);
        if (pair == null) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/404")).build();
        } else {
            if (pair.getValue()) {
                return ResponseEntity
                        .ok()
                        .header("Content-Type", "text/html")
                        .body("<html><head><meta charset='UTF-8'><title>正在重新導向</title></head>" +
                                "<body><p>你好！這裡是Raymond Weng製作的縮網址服務！</p>" +
                                "<script>window.location.href = '" + Objects.requireNonNullElse(pair.getKey(), "/404") + "';</script>" +
                                "</body></html>");
            } else {
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(pair.getKey())).build();
            }
        }

    }

    @GetMapping("/discord")
    public RedirectView discord() {
        return new RedirectView("https://discord.com/invite/yvdDjgPwxf");
    }

    @PostMapping("/create/{name}")
    public CreateResponse create(@PathVariable String name, @RequestBody Link link) throws SQLException {
        return create(name, link.getLink(), false);
    }

    public CreateResponse create(String name, String link, boolean preventPreView) throws SQLException {
        CreateResponse response = new CreateResponse();
        response.setLink(link);
        if (name.equals("free")) {
            String id = LinkManager.getLink();
            while (!LinkManager.useName(link, id, preventPreView)) {
                id = LinkManager.getLink();
            }
            response.setShort_link("https://rwlink.us.kg/" + id);
            response.setError(null);
        } else if (LinkManager.useName(link, name, preventPreView)) {
            response.setShort_link("https://rwlink.us.kg/" + name);
            response.setError(null);
        } else {
            response.setError("The name may be used or invalid.");
        }

        return response;
    }
}
