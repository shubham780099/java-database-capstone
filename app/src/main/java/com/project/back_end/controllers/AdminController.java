package com.project.back_end.controllers;

import com.project.back_end.models.Admin;
import com.project.back_end.services.Service;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin login endpoint: {@code POST /admin}. */
@RestController
@RequestMapping("${api.path}admin")
public class AdminController {

    private final Service service;

    public AdminController(Service service) {
        this.service = service;
    }

    /** Body: {@code {"username": "...", "password": "..."}}. Returns {"token": "..."} or an error message. */
    @PostMapping
    public ResponseEntity<Map<String, String>> adminLogin(@RequestBody Admin admin) {
        return service.validateAdmin(admin);
    }
}
