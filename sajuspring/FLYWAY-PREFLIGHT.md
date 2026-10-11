# V1 적용 전 검증 — 2026-10-11, 승인 대기

이 문서는 11.7.2 기준의 이전 검증 기록입니다. 이후 13.10.0으로 업그레이드하고
재검증한 최신 결과 및 적용 계획은 [FLYWAY-UPGRADE.md](FLYWAY-UPGRADE.md)를 참조합니다.

## 결과

- 대상 allowlist(지정 Aiven 호스트/포트 및 defaultdb)를 확인하고 읽기 전용 JDBC 세션으로 조회했습니다.
- MySQL 8.4.11, TLSv1.3, TLS_AES_256_GCM_SHA384 확인.
- defaultdb의 테이블/뷰 0개, 루틴 0개, 이벤트 0개, 트리거 0개.
  테이블이 없으므로 기존 업무 테이블 데이터도 없습니다.
- Flyway 11.7.2의 `info()` 성공: 현재 적용 버전 없음, V1 PENDING.
  info 이후 테이블/뷰도 0개이며 flyway_schema_history는 생성되지 않았습니다.
- V1을 Flyway 11.7.2 MySQLParser로 파싱하여 CREATE TABLE 5개 확인.
  문장 분리/파싱 검증이며 MySQL 서버의 전체 DDL 문법/제약 검증을 대신하지 않습니다.
- 오프라인 SchemaMigrationContractTests 1개 통과, 실패/오류 0. Java 17 대상 컴파일 성공.
  users 11개 컬럼의 이름/타입/길이/nullable, IDENTITY, PK, 이메일 UNIQUE 일치.
- Spring Boot 서버 및 전체 컨텍스트 테스트는 승인 전 실행하지 않았습니다.
- spring.flyway.enabled=false, ddl-auto=validate, SQL init never 유지.

## SQL 검토

적용 파일: src/main/resources/db/migration/V1__initial_saju_schema.sql

SHA-256: B711A92CA2D1016BEE2E293BA61E986389253549668D5A511F0CA92041638A87

생성 순서: users → client_services → external_user_links → saju_profiles → saju_results.
모든 FK 대상이 먼저 생성되며 signed BIGINT 타입이 일치합니다.
외부 프로필 복합 FK의 대상 `(client_service_id, id)`는 명시적 UNIQUE 키입니다.
참조 자식 키는 기존 인덱스 또는 InnoDB 자동 생성 인덱스로 지원됩니다.
모든 FK는 RESTRICT이고 CASCADE/SET NULL/SET DEFAULT가 없습니다.
프로필 소유자 CHECK는 자체/외부 소유를 구분하며 복합 FK의 NULL 우회를 막습니다.
CHECK 함수는 결정적인 CHAR_LENGTH, TRIM, JSON_TYPE이고 AUTO_INCREMENT 컬럼을 참조하지 않습니다.
MySQL의 CHECK/FK 병용 제한은 이 설계의 RESTRICT에는 해당하지 않음을
MySQL 8.4.0 sql/sql_table.cc의 FK 검증 코드로 확인했습니다.
ENUM, JSON, BIT, DATETIME(6), TIME(0), utf8mb4_0900 collations는 MySQL 8.4 형식으로 검토했습니다.
업무 SQL은 CREATE TABLE만 포함하며 seed 데이터나 파괴적 SQL이 없습니다.

## 호환성과 승인 시 고려할 위험

1. Flyway core/mysql은 11.7.2로 해석됩니다. 실제 `info()`는 성공하지만
   MySQL 8.4가 해당 버전의 검증 범위(8.1)를 넘어선다는 업그레이드 권고를 출력합니다.
   이는 연결을 차단하는 오류는 아니지만, V1 실행 성공을 보장하지 않습니다.
   현재 버전을 유지하고 이 제한을 수용하여 적용할지, Flyway 버전을 먼저 재검토할지 결정이 필요합니다.
2. MySQL DDL은 마이그레이션 전체를 트랜잭션으로 롤백할 수 없습니다.
   중간 실패 시 앞서 생성한 일부 테이블과 실패 history가 남을 수 있습니다.
   실패하면 중단하여 원인을 보고하고 별도 승인을 받습니다. DROP/TRUNCATE/clean/repair는 수행하지 않습니다.
3. 승인 후 업무 테이블 5개와 Flyway history 테이블이 생성되고 history에 V1 성공 기록이 추가됩니다.
   적용 직전 대상/빈 DB 상태와 위 SQL 해시를 다시 확인해야 합니다.
4. users 호환 검사는 메타데이터 기반입니다. 실제 MySQL DDL 수용 및 Hibernate validate,
   서버 기동, 전체 Maven 테스트는 승인 후 확인합니다.
5. DB 제약은 서비스 간 조회 권한을 구현하지 않습니다. 기존 permitAll 및 엔티티 직접 반환은
   외부 공개 전 별도 수정 사항이며 이번 적용 범위에서 변경하지 않습니다.

## 다음 단계 — 현재 실행하지 않음

사용자 승인 후 Flyway 활성화, V1 실행, 테이블 5개와 history 성공 확인,
Hibernate validate/서버 정상 기동 및 Maven 전체 테스트를 진행합니다.
기존 로컬 MySQL과 비밀번호/접속 정보는 변경하거나 출력하지 않습니다.

근거:
- https://raw.githubusercontent.com/flyway/flyway/flyway-11.7.2/flyway-database/flyway-mysql/src/main/java/org/flywaydb/database/mysql/MySQLDatabase.java
- https://dev.mysql.com/doc/refman/8.4/en/create-table.html
- https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html
- https://raw.githubusercontent.com/mysql/mysql-server/mysql-8.4.0/sql/sql_table.cc
