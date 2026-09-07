package com.example.backend.controller;

import com.example.backend.service.GoalService;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        logger.info("ユーザー一覧取得リクエストを受信しました。");
        return this.userService.getAllUsers();
    }

    @PostMapping("/login")
    public User createOrLoginNewUser(@RequestBody User user) {
        logger.info("ログイン/新規登録リクエストを受信。対象ユーザー名: {}", user.getUsername());
        return this.userService.createOrLoginNewUser(user.getUsername());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable("id") Long id) {
        logger.info("ユーザー削除リクエストを受信。対象ID: {}", id);
        return this.userService.deleteUser(id);
    }

}
