# SAJU 초기 스키마 검토안 — 아직 적용하지 않음

## 기존 코드 분석

- `UserEntity`: Long IDENTITY PK, 이메일 UNIQUE/필수, 닉네임 필수,
  nullable 비밀번호/프로필 이미지, Role/ LoginType STRING enum,
  LocalDateTime 생성·수정·마지막 로그인 시각, 필수 Boolean deleted.
- `SignupDto`는 이메일/비밀번호/닉네임, `LoginDto`는 이메일/비밀번호,
  `UserDto`는 공개용 회원 정보를 표현합니다. 외부 서비스 subject/tenant 필드는 없습니다.
- `UserRepository`는 `JpaRepository<UserEntity, Long>`만 확장합니다.
- `UserService.getUsers()`는 전체 엔티티를 반환하고 `/users`도 이를 직접 직렬화합니다.
  비밀번호 해시 노출 가능성이 있습니다. `SecurityConfig`는 모든 요청 permitAll이며
  CSRF가 비활성화되어 있습니다. 현재 외부 서비스 인증 및 데이터 격리는 구현되지 않았습니다.
- 사주 API는 입력 DTO 없는 `/test` 경로에서 Python 결과 문자열을 반환합니다.
  새 프로필/결과 테이블과 연동하는 저장 코드는 이번 범위에 포함하지 않습니다.

## 식별과 소유권

`users`는 기존 SAJU 자체 회원만 저장합니다. 이메일/닉네임 필수 조건을 유지하므로
외부 회원을 가짜 이메일로 등록하지 않습니다. `external_user_links.user_id`는 nullable이며
외부 서비스의 불변 opaque subject만으로 회원을 식별할 수 있습니다.
동일 subject도 서비스가 다르면 별개 회원입니다. subject는 대소문자를 구분하며
최대 191자로 제한합니다. 공백뿐인 값은 금지하고 앞뒤 공백 정규화는 API에서 처리합니다.
외부 서비스는 이메일·실명·비밀번호·원본 OAuth 토큰을 SAJU에 제공할 필요가 없습니다.

내부 계정 연결은 양쪽 인증/사용자 동의를 확인한 별도 절차에서만 수행합니다.
이메일 일치로 자동 연결하지 않습니다. 한 SAJU 회원은 여러 서비스/subject와 연결될 수 있으며,
`(client_service_id, user_id)`에 UNIQUE를 두지 않습니다. 연결했다고 다른 서비스의 프로필이나
SAJU 자체 프로필이 자동 공유되지는 않습니다.

프로필은 자체 회원 소유 또는 외부 회원 소유 중 정확히 한 가지입니다.
CHECK로 소유자 조합을 검증하고, 외부 소유는 `(client_service_id, external_user_link_id)`
복합 FK로 다른 서비스의 링크를 참조하지 못하도록 합니다.
결과는 하나의 프로필에만 귀속됩니다. 서비스 ID/회원 ID를 결과에 중복 저장하지 않고
프로필에서 소유권을 상속하므로 서로 모순되는 서비스 값이 생기지 않습니다.

## 테이블과 제약

모든 테이블은 InnoDB, utf8mb4, signed BIGINT AUTO_INCREMENT PK를 사용합니다.
식별 코드는 이진 collation, 표시 문자열과 기존 회원 이메일은 `utf8mb4_0900_ai_ci`를 사용합니다.
이메일 UNIQUE는 대소문자/악센트를 구분하지 않습니다. 국제화 이메일 정규화 정책은
회원 기능 구현 시 별도 확정해야 합니다.

| 테이블 | 필수 정보 / 선택 정보 | FK / UNIQUE | 조회용 인덱스 |
|---|---|---|---|
| users | 이메일, 닉네임, role, login_type, created_at, deleted / password 등 | 이메일 UNIQUE | PK, 이메일 UNIQUE |
| client_services | service_key, display_name, ACTIVE/DISABLED 상태, 시각 | service_key UNIQUE | PK, service_key UNIQUE |
| external_user_links | client_service_id, external_subject, 시각 / 내부 user_id | 서비스·사용자 FK, (서비스, subject) UNIQUE, (서비스, id) UNIQUE | user_id 및 UNIQUE 인덱스 |
| saju_profiles | 소유자, 출생 연·월·일, calendar_type, leap 여부, time_zone, 시각 / 출생 시각, 계산용 성별 | 사용자 FK 또는 서비스·외부 링크 복합 FK, 소유자 CHECK | 사용자·생성일·ID, 서비스·링크·생성일·ID |
| saju_results | 프로필, 계산 당시 profile_updated_at, engine_version, 양수 result_schema_version, 결과 JSON, 계산 시각 / 만료 시각 | 프로필 FK, JSON object CHECK | 프로필·계산일·ID, expires_at |

별도 client FK는 프로필 복합 FK 경로로 보장되므로 중복하지 않습니다.
모든 FK는 ON DELETE/UPDATE RESTRICT입니다. 자동 CASCADE 삭제를 하지 않습니다.
개인정보 삭제 시 결과 → 프로필 → 외부 링크 → 회원 순서로 명시적으로 처리하는
서비스 기능이 필요하며, client 삭제 전에도 소유 데이터를 별도로 정리해야 합니다.

## 출생 정보·결과 정책

- 음력 2월 30일 같은 원본 입력을 보존하려고 MySQL DATE 대신 연·월·일을 분리합니다.
  CHECK는 연도 1~9999, 월 1~12, 일 1~31(음력은 최대 30)을 검사합니다.
  양력 윤년/월별 날짜, 음력 윤달·유효일, 엔진의 지원 연도는 API에서 검증해야 합니다.
- 시간 미상은 NULL이며 자정과 구분합니다. 시각은 하루 범위로 제한합니다.
  IANA time_zone의 유효성은 애플리케이션이 검증합니다. 기본값은 기존 환경의 Asia/Seoul입니다.
- 이름, 이메일, 주소, 좌표, 외부 토큰을 프로필에 복제하지 않습니다.
  진태양시/위치 보정이 필요한 엔진은 추후 최소 입력 구조를 별도 설계해야 합니다.
- calculation_sex는 선택적 MALE/FEMALE 계산 입력입니다. 엔진에 필요한 경우에만 수집하며
  NULL을 자동 추론하지 않습니다. 추가 분류/정책은 계산 엔진 계약에 맞춰 재검토합니다.
- JSON은 계산 결과만 저장하며 출생 입력·외부 식별자·토큰을 포함하지 않습니다.
  서버에서 JSON 구조를 검증하고 엔진 및 결과 포맷 버전을 함께 기록해야 합니다.
- 프로필 수정 이후 과거 결과가 최신 입력의 결과로 사용되지 않도록
  `profile_updated_at` 스냅샷을 비교합니다. 마이크로초 시각만으로 동시성 제어를 보장하지 않으므로
  향후 저장 서비스는 잠금/원자적 읽기 정책을 구현해야 합니다. 현재는 캐시 유일성을 강제하지 않습니다.
- 새 테이블 시각은 UTC DATETIME(6) 운영 계약을 권장합니다. CURRENT_TIMESTAMP는 세션 시간대에
  의존하므로 새 저장 기능 구현 시 JDBC/세션 UTC 설정을 함께 확정해야 합니다.
  기존 users의 LocalDateTime/Asia-Seoul 동작은 이번에 변경하지 않습니다.
- expires_at은 삭제 대상 탐색용이며 자동 삭제 기능은 아닙니다. 보존 기간, 동의, 철회,
  백업 삭제 정책을 정하고 삭제 작업을 구현해야 합니다. users.deleted만으로 익명화되지는 않습니다.

## 외부 서비스 간 접근 제한

DB FK/CHECK는 잘못된 소유자 연결을 막지만 SELECT 권한을 회원/서비스별로 제한하지 않습니다.
MySQL에 이 설계만으로 행 단위 접근 통제가 생기지는 않습니다.
외부 API 구현 시 인증된 서비스의 ID를 서버에서 결정하고 요청의 service ID를 신뢰하지 않아야 합니다.
자격 증명은 OIDC/JWT 또는 별도 API 키 관리 시스템에서 관리합니다.
`client_services`는 서비스 등록부이며 평문 API 키/토큰 저장소가 아닙니다.
서비스 DISABLED 상태 확인과 외부 subject의 위조 방지 검증도 필요합니다.

프로필 조회는 서비스와 subject를 함께 제한해야 합니다. 결과 조회도 프로필/외부 링크를
JOIN하고 동일 범위 필터를 적용합니다. 예시(설명용, 실행하지 않음):

```sql
SELECT r.* FROM saju_results r
JOIN saju_profiles p ON p.id = r.saju_profile_id
JOIN external_user_links l
  ON l.id = p.external_user_link_id AND l.client_service_id = p.client_service_id
WHERE p.client_service_id = :authenticated_client_id
  AND l.external_subject = :verified_subject
  AND r.id = :result_id;
```

`findById`/`findAll` 단독 조회나 내부 user_id를 이용한 서비스 간 공유를 금지하는
Repository/API 계약이 필요합니다. 현재 permitAll 및 엔티티 직접 반환을 해결하고
인증·권한·격리 테스트를 완료하기 전에는 외부 공개용 API로 사용하면 안 됩니다.

## JPA 일치 및 검증 범위

users의 camelCase 필드는 Boot의 물리 명명 규칙에 맞춰 snake_case로 작성했습니다.
Long은 BIGINT, Boolean은 BIT(1), LocalDateTime은 DATETIME(6),
문자열 enum은 Hibernate 6의 MySQL native ENUM을 사용합니다.
기존 `@PrePersist`/`@PreUpdate` 동작을 유지하므로 users에 자동 시간 기본값을 추가하지 않습니다.
SQL에서 nullable 여부, 문자열 길이, enum 값, IDENTITY, PK, 이메일 UNIQUE를 유지합니다.
추가 4개 테이블의 JPA 엔티티/DTO/Repository는 아직 작성하지 않았으며 SQL 설계 단계입니다.

`SchemaMigrationContractTests`는 JDBC 메타데이터 접근을 끄고 Hibernate 메타데이터와
V1 users 정의의 컬럼 이름/타입/길이/nullable, PK, UNIQUE, AUTO_INCREMENT를 비교합니다.
Spring 컨텍스트를 실행하거나 어떤 DB에도 접속하지 않습니다.

```powershell
.\mvnw.cmd -B '-Dtest=SchemaMigrationContractTests' test
```

이 검사는 MySQL SQL 실행·FK/CHECK 동작·실제 Hibernate validate를 대체하지 않습니다.
DB에 SQL 실행을 하지 말라는 지시에 따라 해당 검증은 승인 후 진행합니다.

2026-10-11 검증 결과: Java 21에서 Java 17 대상 컴파일 성공,
Hibernate 6.6.53.Final의 MySQL 8.4 메타데이터 검사 1개 통과(실패/오류 0).
Flyway는 Boot 관리 버전 11.7.2로 해석되었습니다. BIT와 BIT(1)의 동등 표기 및
ENUM 선언의 공백은 비교 시 정규화했습니다. DB 접속이나 SQL 실행은 없었습니다.

## Flyway 적용 전 확인사항과 예상 변경

- Boot 관리 버전의 `flyway-core`, `flyway-mysql`을 추가했습니다. Maven Flyway 실행 플러그인은
  추가하지 않았으며 Connector/J와 dotenv 의존성은 유지했습니다.
- `spring.flyway.enabled=false`로 자동 실행을 차단했습니다. Hibernate validate와 SQL init never 유지,
  clean disabled, baseline-on-migrate false, validate-on-migrate true를 명시했습니다.
- SQL에는 CREATE TABLE 5개만 있으며 seed INSERT, ALTER, DROP, CREATE DATABASE, USE는 없습니다.
  연결된 DB에 적용됩니다. IF NOT EXISTS로 충돌을 숨기지 않습니다.
- 승인 후 대상 defaultdb가 여전히 비어 있는지, Flyway history/충돌 테이블이 없는지 재확인합니다.
  그 다음 승인된 배포 실행에서만 Flyway를 활성화하고 `users` Hibernate validate를 확인합니다.
  예상 생성물은 업무 테이블 5개와 Flyway의 `flyway_schema_history`입니다.
- MySQL DDL은 마이그레이션 전체가 한 번에 롤백되지 않습니다. 중간 실패 시 자동 DROP/clean/repair를
  실행하지 않고 생성 상태와 history를 조사합니다. 이미 데이터가 있으면 적용 계획을 다시 검토합니다.
- 적용 전 V1은 검토 수정할 수 있지만 적용 후에는 변경하지 않고 V2 등 새 버전으로 수정합니다.
- 이번 작업에서는 서버/기존 Spring 컨텍스트 테스트/마이그레이션을 실행하지 않고
  Aiven 및 기존 로컬 DB에 SQL을 보내지 않습니다. 실제 적용은 사용자 승인 이후의 별도 작업입니다.

참고 문서:
- https://documentation.red-gate.com/flyway/reference/database-driver-reference/mysql
- https://docs.jboss.org/hibernate/orm/6.6/introduction/pdf/Hibernate_Introduction.pdf
