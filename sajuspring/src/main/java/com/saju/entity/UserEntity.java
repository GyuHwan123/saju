package com.saju.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.*;

@Entity
@Table(name="users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이메일
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    // BCrypt로 암호화된 비밀번호
    @Column(length = 255)
    private String password;
    
    // 닉네임
    @Column(nullable = false, length = 50)
    private String nickname;

    // 프로필 이미지
    @Column(length = 500)
    private String profileImage;

    // 권한(USER, ADMIN)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 로그인 방식(LOCAL, GOOGLE, NAVER, KAKAO)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginType loginType;

    // 가입일
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 수정일
    private LocalDateTime updatedAt;

    // 마지막 로그인
    private LocalDateTime lastLoginAt;

    // 탈퇴 여부
    @Column(nullable = false)
    private Boolean deleted;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        deleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

