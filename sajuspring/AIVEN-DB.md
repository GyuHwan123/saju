# Aiven MySQL 연결

`sajuspring` 디렉터리에서 실행합니다. `.env.example`을 `.env`로 복사하고
실제 `DB_PASSWORD`를 입력합니다. 기존 `.env`가 있으면 덮어쓰지 않습니다.
spring-dotenv가 파일을 읽으며, 시스템 환경변수가 있으면 우선합니다.
비밀번호를 JDBC URL이나 명령행 인수에 넣지 않습니다.

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

## TLS 및 스키마 정책

- `sslMode=REQUIRED`로 암호화 연결을 강제하며 TLS 1.2/1.3만 허용합니다.
- 이 모드는 서버 인증서/호스트 이름을 검증하지 않습니다. 인증까지 적용하려면
  Aiven 프로젝트 CA를 별도 Java truststore에 등록하고 Connector/J의
  `trustCertificateKeyStoreUrl`, `trustCertificateKeyStorePassword` 등을
  비밀 환경변수로 구성한 뒤 `sslMode=VERIFY_IDENTITY`로 변경합니다.
- `ddl-auto=validate`, `spring.sql.init.mode=never`로 테이블 생성/변경과
  SQL 초기화를 차단합니다. 테이블이 없거나 엔티티와 불일치하면 기동이 실패합니다.
- 기존 프로젝트는 Hibernate `update`를 사용했으며 Flyway/Liquibase 의존성이나
  별도 스키마 SQL은 없습니다. 최초 스키마는 기존 엔티티와 Hibernate의 명명 규칙을
  기준으로 DDL을 별도 생성하여 검토한 뒤, 대상 DB와 기존 데이터 확인 후 적용해야 합니다.
  `create`, `create-drop`, `update`로 전환하거나 로컬 데이터를 자동 이관하지 않습니다.

## 2026-10-11 확인 결과

- Maven Wrapper 3.9.16 실행 성공. 일반 디렉터리에서 `.Target[0]` 접근 시 발생하던
  PowerShell 오류만 수정했습니다.
- Java 21 환경에서 Java 17 대상 컴파일 성공(`release 17`). Java 17 런타임은
  설치되어 있지 않아 해당 런타임에서의 실행은 확인하지 못했습니다.
- 기존 MySQL Connector/J(실제 해석 버전 9.7.0), spring-dotenv 4.0.0 유지.
- Spring Boot에서 Aiven MySQL 8.4.11 연결 성공.
- 별도 읽기 전용 JDBC 조회: TLSv1.3, TLS_AES_256_GCM_SHA384 확인.
- `defaultdb` 기본 테이블 0개, `users` 테이블 없음.
- 기존 컨텍스트 테스트 1개 오류 및 서버 기동 중단 원인:
  `Schema-validation: missing table [users]`.
  `spring-boot:run`은 DevTools 영향으로 Maven 자체 종료 코드가 0이어도
  애플리케이션 기동에 실패할 수 있으므로 서버 로그를 기준으로 판단했습니다.
- DB 테이블/데이터 생성·변경·삭제 및 로컬 데이터 마이그레이션은 수행하지 않았습니다.

참고: https://dev.mysql.com/doc/connector-j/en/connector-j-reference-using-ssl.html
및 https://aiven.io/docs/platform/concepts/tls-ssl-certificates
