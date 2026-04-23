package com.ovs.backend.controller;

import com.ovs.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final UserRepository userRepository;

    public DebugController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream()
                .map(user -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", user.getId());
                    map.put("email", user.getEmail());
                    map.put("fullName", user.getFullName());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
