package com.legalaid.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.legalaid.backend.websocket.PresenceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/users/online")
    public boolean isUserOnline(@RequestParam String email) {
        return presenceService.isOnline(email);
    }
}

