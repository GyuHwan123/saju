package com.saju.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupDto {

    private String email;

    private String password;

    private String nickname;
}