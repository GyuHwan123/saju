package com.saju.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private Long id;

    private String email;

    private String nickname;

    private String profileImage;

    private String role;

    private String loginType;
}