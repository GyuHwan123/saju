# V1 마이그레이션 적용 결과

2026-10-11 (Asia/Seoul). 사용자 승인에 따라 Aiven `defaultdb`에 적용 완료.

## 적용 직전 확인

- 접속 대상 호스트/포트/DB allowlist 확인. 기존 로컬 MySQL은 사용하지 않음.
- 읽기 전용 JDBC 조회: 테이블/뷰 0, 루틴 0, 이벤트 0, 트리거 0.
- Flyway core/mysql 모두 13.10.0 확인.
- V1 SHA-256이 기존 검증값과 일치:
  `B711A92CA2D1016BEE2E293BA61E986389253549668D5A511F0CA92041638A87`

## 마이그레이션 및 생성 테이블

- Flyway Java API `migrate()` 1회 실행. success=true, migrationsExecuted=1, targetSchemaVersion=1.
- clean-disabled=true, baseline-on-migrate=false, validate-on-migrate=true, target=1.
- 생성 확인: `users`, `client_services`, `external_user_links`, `saju_profiles`, `saju_results`, `flyway_schema_history`.
- history는 1행: installed_rank=1, version=1,
  script=`V1__initial_saju_schema.sql`, checksum=967920277, success=true.
- V1 SQL은 변경하지 않았으며 적용 후에도 SHA-256 일치 확인.

## 기동 및 테스트

- `application.properties`에서 Flyway 활성화. 기존 Hibernate `ddl-auto=validate`, SQL init `never` 유지.
- Spring Boot 실제 웹 서버 기동 성공: active=true, EntityManagerFactory open=true.
  검증 시 임시 HTTP 포트를 사용했고 기동 확인 후 정상 종료함.
- Maven compile 성공.
- 전체 `./mvnw.cmd -B test`: BUILD SUCCESS, 테스트 4개, 실패 0, 오류 0, 건너뜀 0.
  FlywayCompatibilityTests 2개, SchemaMigrationContractTests 1개, SajuspringApplicationTests 1개.
- 기동/컨텍스트 테스트에서는 이미 적용된 V1을 검증하며 추가 V1 적용은 없음.

## 이번 작업의 수정 범위

- `src/main/resources/application.properties`: Flyway enabled=false → true 및 주석 갱신.
- `FLYWAY-APPLIED.md`: 적용 결과 기록.
- `FLYWAY-UPGRADE.md`: 적용 완료 보고서 링크 추가.
- `target/` 아래에는 일회성 실행 도구 및 빌드/테스트 산출물이 생성됨(버전 관리 대상 아님).
- 앞선 작업에서 이미 존재하던 변경은 유지함.
- 로컬 MySQL, 로그인, 프런트엔드, Python 계산 코드는 변경하지 않음.
- DROP, TRUNCATE, Flyway clean, repair는 실행하지 않음.
- DB 비밀번호 및 접속 URI는 보고서에 기록하지 않음.
