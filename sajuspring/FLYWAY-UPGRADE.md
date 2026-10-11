# Flyway 업그레이드 검증 및 최종 적용 계획

2026-10-11 확인. 아래 내용은 승인 전 검증과 계획 기록입니다.
이후 사용자 승인에 따라 V1 적용을 완료했습니다. 최종 결과는 [FLYWAY-APPLIED.md](FLYWAY-APPLIED.md)를 참조하세요.

## 버전 선정

Maven Central의 flyway-core/flyway-mysql 메타데이터가 모두 release 13.10.0을 표시하며,
공식 릴리스 노트에도 2026-10-08 안정 릴리스로 기록되어 있습니다.
프로젝트 Boot 3.5.15의 기본 관리 버전은 11.7.2입니다.
최신 버전 전체에 대한 Boot 공급자의 인증을 의미하지는 않지만 아래 프로젝트 검사를
통과했으므로 이 프로젝트에서는 13.10.0으로 업그레이드하는 것이 적절하다고 판단했습니다.

| 선택 | 장점 | 단점 |
|---|---|---|
| 11.7.2 유지 | Boot 기본 의존성 조합 유지, 메이저 API 변경 위험 최소 | MySQL 8.4가 검증 상한 8.1을 넘는다는 경고 지속, 이후 수정 사항 미반영 |
| 13.10.0 업그레이드 | 최신 안정 릴리스, MySQL 검증 상한 9.4 및 실제 8.4 연결에서 기존 경고 해소 | Boot 관리 버전 override 필요, API/선택 의존성/결과 포맷 변경 확인 필요 |

pom.xml에 flyway.version=13.10.0만 추가하여 core/mysql을 함께 맞췄습니다.
Boot, Java 목표, Connector/J, spring-dotenv 및 Jackson 버전은 변경하지 않았습니다.

## 호환성 확인

- Flyway Java API의 문서상 요구사항은 JDK 17 이상입니다.
- 실제 13.10.0의 Flyway/MySQLParser class major version=61(Java 17).
  빌드 역시 release 17로 성공했습니다. 설치된 런타임은 Java 21이므로 실제 Java 17
  런타임에서의 실행 검증까지 한 것은 아닙니다.
- Boot 3.5.15의 FlywayAutoConfiguration을 실제 생성하는 오프라인 테스트 통과.
  테스트 전용 FlywayMigrationStrategy가 migrate 호출을 대체하며 DataSource 접근 0회를 검증합니다.
  enabled=true 경로의 bean 생성/설정/초기화 연결을 확인했지만 실제 migration SQL은 실행하지 않습니다.
- 기존 users의 Hibernate 메타데이터 일치 테스트와 13.10.0 MySQLParser 검사 통과.
- Flyway core/mysql은 모두 13.10.0, Jackson databind/core는 Boot 관리 2.21.4,
  annotations는 Boot 관리 2.21입니다. tools.jackson(Jackson 3)은 의존성 트리에 없습니다.
- 13.x는 Jackson/TOML/JSON의 선택적 의존성 및 별도 utilities 모듈 변화가 있습니다.
  현재 application.properties 및 기본 JDBC info/migrate 구성에서는 해당 기능을 사용하지 않습니다.
  향후 TOML 프로젝트 설정, JSON 직렬화, 유료 기능을 도입하면 필요한 모듈을 다시 검토해야 합니다.
- 릴리스 사이의 Java 결과 timestamp 포맷, 라이선스/CLI/코드 분석 설정 변경은
  이 프로젝트가 해당 기능을 사용하지 않으므로 현재 코드에 직접 변경이 필요하지 않습니다.
- Flyway 내부 API를 사용하는 MySQLParser 테스트는 버전 변경 시 재컴파일이 필요한 검사 도구입니다.
  운영 애플리케이션은 내부 파서 API에 의존하지 않습니다.

## 검증 결과

실행 명령(테스트는 DB/네트워크를 사용하지 않으며 최초 Maven 의존성 준비는 다운로드 필요):

```powershell
.\mvnw.cmd -B '-Dtest=SchemaMigrationContractTests,FlywayCompatibilityTests' test
```

- 오프라인 테스트 3개 통과, 실패/오류 0, BUILD SUCCESS.
- Aiven 대상으로 allowlist를 확인한 후 JDBC 세션 transaction_read_only=1에서만 조회했습니다.
- MySQL 8.4.11, TLSv1.3, TLS_AES_256_GCM_SHA384.
- defaultdb 테이블/뷰 0, 루틴 0, 이벤트 0, 트리거 0.
- Flyway 13.10.0 info 성공, 현재 적용 버전 없음, V1 PENDING.
- 이전 MySQL 검증 범위 업그레이드 경고는 나타나지 않았습니다.
  설치된 MySQLDatabase 바이트코드에서도 권고 기준 9.4를 확인했습니다.
- info 이후 테이블/뷰 0, flyway_schema_history 미생성.
- V1 SQL 변경 없음. SHA-256:
  B711A92CA2D1016BEE2E293BA61E986389253549668D5A511F0CA92041638A87
- application.properties의 enabled=false, ddl-auto=validate, SQL init never 유지.
- 서버/전체 Spring 컨텍스트 테스트/마이그레이션은 실행하지 않았습니다.
  실제 MySQL DDL 수용 여부와 Hibernate validate 성공은 스키마 적용 후 확인할 수 있습니다.

## 최종 적용 계획 — 아직 실행하지 않음

1. 사용자로부터 Flyway 13.10.0으로 V1을 적용한다는 별도 승인을 받습니다.
2. 적용 직전 Aiven defaultdb 대상, 빈 스키마, V1 해시, core/mysql 버전 일치를 다시 확인합니다.
   새 테이블이나 예상하지 못한 history가 있으면 적용을 중단하고 보고합니다.
3. 승인된 실행에서 Flyway 활성화. clean disabled와 baseline-on-migrate false,
   Hibernate validate를 유지하여 V1을 적용합니다.
4. users, client_services, external_user_links, saju_profiles, saju_results 생성 및
   flyway_schema_history의 V1 success=true/체크섬을 확인합니다.
5. Spring Boot 정상 기동과 JPA validate를 로그로 확인하고 전체 Maven 테스트를 실행합니다.
6. 설정 변경과 검증 결과를 보고합니다. 로컬 MySQL, 로그인, 프런트엔드, 사주 계산 코드는 변경하지 않습니다.

MySQL의 CREATE TABLE들은 마이그레이션 전체 단위로 롤백되지 않습니다.
중간 실패 시 일부 테이블/history가 남을 수 있으며 즉시 중단하여 원인과 현 상태를 보고하고
추가 승인을 받습니다. DROP/TRUNCATE/clean/repair/기존 데이터 삭제를 수행하지 않습니다.
DB 제약과 인증/행 접근 제한은 별개이며 기존 permitAll 문제는 이전 설계 문서의 별도 항목입니다.

## 근거

- https://repo.maven.apache.org/maven2/org/flywaydb/flyway-core/maven-metadata.xml
- https://repo.maven.apache.org/maven2/org/flywaydb/flyway-mysql/maven-metadata.xml
- https://documentation.red-gate.com/flyway/release-notes-and-older-versions/release-notes-for-flyway-engine
- https://documentation.red-gate.com/flyway/reference/usage/api-java
- https://raw.githubusercontent.com/spring-projects/spring-boot/v3.5.15/spring-boot-project/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/flyway/FlywayAutoConfiguration.java
