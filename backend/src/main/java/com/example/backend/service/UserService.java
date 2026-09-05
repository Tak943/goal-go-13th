package com.example.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return this.userRepository.findAll();
    }

    public User createOrLoginNewUser(String username) {
        //ユーザーが登録済みならユーザー返す
        if(isUserPresent(username)){
            Optional<User> userOpt = this.userRepository.findByUsername(username);
            User alreadyUser = userOpt.get();
            return alreadyUser;
        }
        //未登録なら新たに登録
        else {
            User newUser = new User();
            newUser.setUsername(username);
            return this.userRepository.save(newUser);
        }
    }

    public Boolean isUserPresent(String username){
        Optional<User> userOpt = this.userRepository.findByUsername(username);
        return userOpt.isPresent();
    }

    public ResponseEntity<String> deleteUser(Long id){
        this.userRepository.deleteById(id);
        return ResponseEntity.ok("id=" + id + "のユーザーを削除");
    }
}
