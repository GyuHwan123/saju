package com.saju.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.saju.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long>{
	
}
