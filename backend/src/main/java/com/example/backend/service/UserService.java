package com.example.backend.service;

import java.beans.Transient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return this.userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return this.userRepository.findById(id);
    }

    public Optional<User> getUserByToken(String token) {
        return this.userRepository.findByToken(token);
    }

    @Transactional
    public User createOrLoginNewUser(String username) {
        // ユーザーが登録済みならユーザー返す
        if (isUserPresent(username)) {
            Optional<User> userOpt = this.userRepository.findByUsername(username);
            User alreadyUser = userOpt.get();
            return alreadyUser;
        }
        // 未登録なら新たに登録
        else {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setToken(UUID.randomUUID().toString());
            return this.userRepository.save(newUser);
        }
    }

    public Boolean isUserPresent(String username) {
        Optional<User> userOpt = this.userRepository.findByUsername(username);
        return userOpt.isPresent();
    }

    @Transactional
    public ResponseEntity<String> deleteUser(Long id) {
        this.userRepository.deleteById(id);
        return ResponseEntity.ok("id=" + id + "のユーザーを削除");
    }
}
