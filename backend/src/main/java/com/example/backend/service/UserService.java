package com.example.backend.service;

import java.beans.Transient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
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
        //機密事項であるトークンはログには載せない
        logger.info("トークンによるユーザー検索を実行します。");
        return this.userRepository.findByToken(token);
    }

    @Transactional
    public User createOrLoginNewUser(String username) {
        // ユーザーが登録済みならユーザー返す
        if (isUserPresent(username)) {
            logger.info("既存ユーザーのログイン処理を実行: {}", username);
            Optional<User> userOpt = this.userRepository.findByUsername(username);
            User alreadyUser = userOpt.get();
            logger.info("ログイン成功。ユーザーID: {}", alreadyUser.getId());
            return alreadyUser;
        }
        // 未登録なら新たに登録
        else {
            logger.info("新規ユーザーの登録処理を実行: {}", username);
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setToken(UUID.randomUUID().toString());
            User savedUser = this.userRepository.save(newUser);
            logger.info("新規登録成功。新しいユーザーID: {}", savedUser.getId());
            return savedUser;
        }
    }

    public Boolean isUserPresent(String username) {
        Optional<User> userOpt = this.userRepository.findByUsername(username);
        return userOpt.isPresent();
    }

    @Transactional
    public ResponseEntity<String> deleteUser(Long id) {
        // 削除処理はエラーが起きやすいから、try-catchで囲む
        try {
            logger.info("DBからユーザー(ID: {})の削除を試みます...", id);
            this.userRepository.deleteById(id);
            logger.info("ユーザーの削除に成功しました。");
            
            return ResponseEntity.ok("id=" + id + "のユーザーを削除");
            
        } catch (Exception e) {
            logger.error("ユーザー削除中にエラーが発生しました。ID: {}, 原因: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
