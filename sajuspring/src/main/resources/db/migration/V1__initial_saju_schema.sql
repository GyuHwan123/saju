-- Review before applying. No seed data, destructive DDL, or automatic baseline.
-- Existing UserEntity mappings: signed BIGINT, BIT boolean, native ENUM, DATETIME(6).
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NULL,
    nickname VARCHAR(50) NOT NULL,
    profile_image VARCHAR(500) NULL,
    role ENUM('ADMIN', 'USER') NOT NULL,
    login_type ENUM('GOOGLE', 'KAKAO', 'LOCAL', 'NAVER') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    last_login_at DATETIME(6) NULL,
    deleted BIT(1) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE client_services (
    id BIGINT NOT NULL AUTO_INCREMENT,
    service_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_client_services_key UNIQUE (service_key),
    CONSTRAINT ck_client_services_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE external_user_links (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_service_id BIGINT NOT NULL,
    -- Opaque, case-sensitive subject supplied by an authenticated client.
    external_subject VARCHAR(191) COLLATE utf8mb4_0900_bin NOT NULL,
    user_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_external_subject UNIQUE (client_service_id, external_subject),
    CONSTRAINT uq_external_link_scope UNIQUE (client_service_id, id),
    INDEX ix_external_links_user (user_id),
    CONSTRAINT fk_external_links_client FOREIGN KEY (client_service_id)
        REFERENCES client_services (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_external_links_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_external_subject_nonempty CHECK (CHAR_LENGTH(TRIM(external_subject)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE saju_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    -- Exactly one owner: native SAJU user OR a tenant-scoped external identity.
    user_id BIGINT NULL,
    client_service_id BIGINT NULL,
    external_user_link_id BIGINT NULL,
    -- Separate components preserve lunar dates such as lunar February 30.
    birth_year SMALLINT NOT NULL,
    birth_month TINYINT NOT NULL,
    birth_day TINYINT NOT NULL,
    birth_time TIME(0) NULL,
    calendar_type VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    is_leap_month BIT(1) NOT NULL DEFAULT b'0',
    time_zone VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'Asia/Seoul',
    -- Optional calculation input; never infer from name or external identity.
    calculation_sex VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX ix_profiles_user_created (user_id, created_at, id),
    INDEX ix_profiles_external_created (client_service_id, external_user_link_id, created_at, id),
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_profiles_external_scope FOREIGN KEY (client_service_id, external_user_link_id)
        REFERENCES external_user_links (client_service_id, id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_profiles_owner CHECK (
        (user_id IS NOT NULL AND client_service_id IS NULL AND external_user_link_id IS NULL)
        OR (user_id IS NULL AND client_service_id IS NOT NULL AND external_user_link_id IS NOT NULL)
    ),
    CONSTRAINT ck_profiles_calendar CHECK (calendar_type IN ('SOLAR', 'LUNAR')),
    CONSTRAINT ck_profiles_birth CHECK (birth_year BETWEEN 1 AND 9999 AND birth_month BETWEEN 1 AND 12
        AND birth_day BETWEEN 1 AND 31 AND (calendar_type <> 'LUNAR' OR birth_day <= 30)),
    CONSTRAINT ck_profiles_leap CHECK (calendar_type = 'LUNAR' OR is_leap_month = b'0'),
    CONSTRAINT ck_profiles_time CHECK (birth_time IS NULL OR (birth_time >= '00:00:00' AND birth_time < '24:00:00')),
    CONSTRAINT ck_profiles_timezone CHECK (CHAR_LENGTH(TRIM(time_zone)) > 0),
    CONSTRAINT ck_profiles_sex CHECK (calculation_sex IS NULL OR calculation_sex IN ('MALE', 'FEMALE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE saju_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    saju_profile_id BIGINT NOT NULL,
    profile_updated_at DATETIME(6) NOT NULL,
    engine_version VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    result_schema_version SMALLINT NOT NULL,
    -- Only computed output: do not duplicate birth inputs, names, or external subjects.
    result_payload JSON NOT NULL,
    calculated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX ix_results_profile_calculated (saju_profile_id, calculated_at, id),
    INDEX ix_results_expiry (expires_at),
    CONSTRAINT fk_results_profile FOREIGN KEY (saju_profile_id)
        REFERENCES saju_profiles (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_results_engine CHECK (CHAR_LENGTH(TRIM(engine_version)) > 0),
    CONSTRAINT ck_results_schema CHECK (result_schema_version > 0),
    CONSTRAINT ck_results_payload CHECK (JSON_TYPE(result_payload) = 'OBJECT'),
    CONSTRAINT ck_results_expiry CHECK (expires_at IS NULL OR expires_at >= calculated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
