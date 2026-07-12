package com.saju.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.saju.entity.UserEntity;
import com.saju.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class UserService {
	
	private final UserRepository userRepository;

    public List<UserEntity> getUsers() {
        return userRepository.findAll();
    }

}
