# SAJU 프로젝트 현황 보고서

- 작성일: 2026-10-11 (한국 시간)
- 조사 대상: `C:\Users\송규환\Desktop\saju`
- 기준 브랜치 / 커밋: `main` / `901e601`
- 조사 방법: 소스·설정·Git 이력·기존 DOCX 문서 검토, 프런트엔드 빌드 및 린트, Python 실행, 백엔드 테스트 실행 시도

## 1. 현재 상태 요약

프로젝트는 **React + Spring Boot + Python + MySQL 구조를 준비하고, 화면 및 회원 모델의 골격을 만든 초기 개발 단계**다. 홈·로그인 경로, 공통 레이아웃, 인증 상태 저장용 Context, 사용자 엔티티와 DTO, Spring에서 Python을 호출하는 테스트 코드가 있다.

하지만 실제 사주 계산, 생년월일 입력 및 결과 화면, 회원가입·로그인 API, 프런트엔드와 백엔드의 API 연결은 아직 구현되지 않았다. 현재 코드를 완성된 사주 서비스로 보기는 어렵다. 마지막 커밋 내용으로 볼 때 **화면 구조와 인증 기반을 준비하던 시점**에서 이어서 개발할 수 있는 상태다.

프런트엔드 프로덕션 빌드는 성공했다. 린트는 인증 Context 파일의 오류 2건으로 실패했다. Python은 `hello python` 출력까지 확인했다. 백엔드는 Maven Wrapper 시작 오류로 테스트가 실행되지 않았으므로 서버 기동·DB 연결·HTTP API 동작은 미검증이다.

## 2. 프로젝트 구성

| 위치 | 역할 | 현재 상태 |
| --- | --- | --- |
| `sajuview/` | React 프런트엔드 | 두 페이지와 공통 레이아웃, 인증 Context 구성 |
| `sajuspring/` | Spring Boot 백엔드 | 사용자 조회 및 Python 호출용 테스트 API 구성 |
| `sajupython/` | 사주 계산 담당으로 사용할 Python 영역 | 출력 테스트용 `test.py`만 존재 |
| `README.md` | 프로젝트 안내 | `# saju` 제목만 작성 |
| `saju 프로젝트 중요 이론 정리 템플릿.docx` | 학습·설계 참고 문서 | 기술 설명과 개발 메모 포함, Git 미추적 |
| `docs/` | 프로젝트 문서 | 이번 현황 보고서 저장 위치 |

### 기술 구성

| 구분 | 설정 파일 기준 |
| --- | --- |
| 프런트엔드 | React `^19.2.6`, React Router DOM `^7.18.1` |
| 프런트엔드 도구 | Vite `^8.0.12`, ESLint `^10.3.0` |
| 백엔드 | Spring Boot `3.5.15`, Java 목표 버전 `17` |
| 백엔드 의존성 | Spring Web, Data JPA, Security, MySQL Connector, Lombok, spring-dotenv `4.0.0` |
| Maven Wrapper | Maven `3.9.16` 배포 URL 설정 |
| Python | 별도 의존성 명세나 패키지 구성 없음 |
| 데이터베이스 | 로컬 MySQL, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` 설정 필요 |

위 버전은 소스 설정값이다. 이번 빌드 로그에서 실제 실행된 Vite는 `8.0.16`이었다. 실행 환경에서 확인한 버전은 Node.js `22.17.1`, Java `21.0.8`, Python `3.12.0`이다. Java 실행 환경과 프로젝트 목표 버전은 다르지만, 이것만으로 오류라고 판단할 수는 없다.

## 3. 구현된 기능과 미완성 부분

### 프런트엔드

- `src/App.jsx`: BrowserRouter와 AuthProvider를 구성하고 `/`, `/login` 경로를 등록했다.
- `src/layouts/MainLayout.jsx`: Header, Outlet, Footer로 공통 화면 구조를 만들었다.
- `src/components/Header.jsx`: 홈·로그인 이동 링크가 있다.
- `src/pages/Home.jsx`, `Login.jsx`: 각각 화면 제목만 표시한다. 입력 폼이나 실제 처리 기능은 없다.
- `src/contexts/AuthProvider.jsx`: 사용자 정보를 React 상태로 보관하고 `login`, `logout`, `isLogin`을 제공한다. 서버 인증이나 저장소 복원 처리는 없으므로 새로고침하면 상태가 초기화된다.
- `src/hooks/useAuth.js`: Context 접근과 Provider 내부 사용 여부 검사를 제공한다. Context 파일에도 별도의 `useAuth`가 있어 중복되어 있다.

API 호출 코드, 회원가입 페이지, 보호된 경로, 사주 입력·결과 페이지는 없다. Vite의 백엔드 프록시 설정과 Spring의 별도 CORS 설정도 확인되지 않았다. 개발 환경의 서로 다른 포트에서 API를 연결할 때 통신 방식을 정해야 한다.

스타일 파일은 존재하지만 `App.css`, `Header.css`, `Footer.css`, `MainLyout.css`를 불러오는 코드가 없다. 현재 진입점에서 불러오는 스타일은 `index.css`다. 따라서 작성된 공통 화면 스타일이 실제로 적용되지 않는 상태다. 레이아웃 CSS 파일명 `MainLyout.css`에는 오타도 있다.

### 백엔드

| 기능 | 코드상 구현 | 미완성 / 확인 필요 |
| --- | --- | --- |
| `GET /users` | Repository의 `findAll()`로 사용자 전체 조회 | 접근 제한, DTO 변환, 페이지 처리 없음 |
| `GET /test` | SajuService를 통해 Python 스크립트 실행 | 실제 사주 입력·계산·결과 계약 없음 |
| 사용자 모델 | 이메일, 비밀번호, 닉네임, 프로필, 권한, 로그인 방식, 가입·수정·로그인 시각, 탈퇴 여부 | 사용자 생성·인증·탈퇴 처리 없음 |
| DTO | LoginDto, SignupDto, UserDto 정의 | 현재 Controller와 Service에서 사용하지 않음 |
| 로그인 방식 | LOCAL, GOOGLE, NAVER, KAKAO 열거형 | OAuth 연동 구현 없음 |
| 권한 | USER, ADMIN 열거형 | 실제 권한 검사 없음 |
| 보안 설정 | SecurityFilterChain 구성 | 모든 요청 허용, CSRF 비활성화 |

회원가입·로그인 엔드포인트, 비밀번호 인코더 및 저장 처리, 세션 또는 JWT 인증은 없다. 엔티티의 비밀번호 주석에 BCrypt가 언급되지만 암호화가 구현되었다는 뜻은 아니다.

`UserEntity`에는 가입·수정 시각을 기록하는 JPA 생명주기 메서드가 있다. 신규 사용자에게 `role`, `loginType`을 부여하는 로직은 아직 없다. Repository에도 이메일 조회 등 인증에 필요한 사용자 정의 메서드가 없다.

### Spring ↔ Python 연동

현재 코드의 호출 흐름은 다음과 같다. HTTP 요청을 직접 실행해 확인한 결과가 아니라 소스에서 확인한 구조다.

```text
GET /test
  → SajuController
  → SajuService
  → PythonClient의 구현체 PythonRunnerService
  → python ../sajupython/test.py
  → 표준 출력 첫 줄 반환
```

인터페이스로 Python 호출을 분리해 두어 추후 다른 실행 방식으로 교체할 기반은 있다. 현재 구현체는 로컬 프로세스 호출 방식 하나뿐이며 FastAPI 서버는 없다.

`test.py`는 `print("hello python")` 한 줄이다. 천간·지지, 음양오행, 양력·음력 변환, 절기, 생년월일시 기반 사주 계산은 구현되지 않았다.

호출 코드에는 다음 보완점이 있다.

- `../sajupython/` 상대 경로 때문에 Spring을 실행하는 작업 디렉터리에 영향을 받는다.
- `python` 명령이 PATH에서 해석되어야 한다.
- 출력 첫 줄만 읽으며 JSON 등 구조화된 결과 형식이 없다.
- 프로세스 종료 코드, 오류 출력, 제한 시간, 스트림 정리 처리가 없다.
- 입력 인자와 입력 검증, 실패 시 API 오류 응답 규칙이 없다.

### 데이터베이스 설정

`application.properties`는 로컬 MySQL에 연결하도록 구성되어 있으며 연결 정보는 환경변수 자리표시자를 사용한다. 로컬 `sajuspring/.env` 파일이 존재하고 Git 추적 대상에는 포함되지 않았다. 비밀값은 보고서에 수록하지 않았다. 설정값의 유효성과 실제 DB 연결은 확인하지 않았다.

JPA 설정은 `ddl-auto=update`이며 SQL 로그가 활성화되어 있다. 버전 관리되는 DB 마이그레이션 파일이나 별도 초기 데이터 스크립트는 확인되지 않았다.

## 4. 검증 결과

| 항목 | 실행 내용 | 결과 |
| --- | --- | --- |
| 프런트엔드 빌드 | `sajuview`에서 `npm.cmd run build` | 성공, 30개 모듈 변환 및 `dist` 생성 |
| 프런트엔드 린트 | `sajuview`에서 `npm.cmd run lint` | 실패, 오류 2건 |
| Python 실행 | `sajuspring`에서 `python ../sajupython/test.py` | 성공, `hello python` 출력 |
| 백엔드 테스트 | `sajuspring`에서 `.\mvnw.cmd -o test` | Wrapper 시작 실패, 테스트 미실행 |
| 브라우저 화면·사용자 흐름 | 실행하지 않음 | 미검증 |
| 서버 기동·DB·API | 실행하지 않음 | 미검증 |

린트 오류는 `src/contexts/AuthProvider.jsx`의 3행과 30행에서 발생했다. 규칙은 `react-refresh/only-export-components`이며, 컴포넌트와 Context·훅을 같은 파일에서 export하는 구성이 원인이다. Context와 훅을 별도 파일로 정리하면 기존 중복 훅도 함께 해소할 수 있다.

백엔드 테스트 시 출력된 오류는 `Cannot index into a null array.` 및 `Cannot start maven from wrapper`다. Maven 테스트 본체가 시작되지 않았으므로 Java 컴파일 실패나 테스트 실패로 해석하면 안 된다. Wrapper 실패의 정확한 원인은 이번 조사에서 확정하지 않았다. 기존 `target` 산출물은 있지만 최신 소스의 정상 빌드 증거로 사용하지 않았다.

백엔드 테스트 소스는 기본 `contextLoads()` 한 건뿐이다. 프런트엔드와 Python에는 별도 자동화 테스트가 확인되지 않았다.

## 5. 우선 해결할 문제

| 우선순위 | 문제 | 영향 및 권장 조치 |
| --- | --- | --- |
| 높음 | `/users`에서 UserEntity를 그대로 반환 | 일반적인 JSON 직렬화 시 비밀번호 필드가 응답에 포함될 수 있다. UserDto로 응답하고 접근 권한을 제한한다. 실제 HTTP 응답은 미검증이다. |
| 높음 | 모든 요청 허용 상태 | 사용자 조회를 포함한 API에 인증·권한 정책이 없다. 인증 방식 결정 후 엔드포인트별 정책을 적용한다. |
| 높음 | 실제 사주 계산 부재 | 서비스의 핵심 기능을 사용할 수 없다. 입력·결과 명세와 계산 기준을 먼저 정한다. |
| 중간 | Maven Wrapper 실행 실패 | 백엔드 검증이 막혀 있다. 실행 환경과 Wrapper를 점검하고 빌드·테스트를 다시 확인한다. |
| 중간 | ESLint 오류 2건 및 useAuth 중복 | 인증 파일의 export 구조를 정리한다. |
| 중간 | CSS 파일 미연결 | 스타일 import를 연결하고 레이아웃 파일명을 정리한다. |
| 중간 | Python 실행 오류 처리 부재 | 경로, 종료 코드, 오류 출력, 제한 시간과 결과 형식을 관리한다. |
| 중간 | 실행·설정 안내 부족 | 환경변수 예시, 설치·기동 순서, API 명세를 문서화한다. |

CSRF 설정은 최종 인증 방식에 맞춰 검토해야 한다. 현재 비활성화되어 있다는 사실만으로 모든 인증 방식에 동일한 조치를 적용할 수는 없다.

## 6. Git 및 문서 상태

- 조사 시작 시 브랜치는 `main`, 최신 커밋은 `901e601`이었다.
- 최신 커밋 날짜는 Git 기록상 `2026-07-12`다. 메시지는 `feat: install router dom, add design structure, and make useAuth hooks`다.
- 직전 관련 커밋 `77168b8`은 사용자 엔티티와 인증 DTO 추가 작업이다.
- 조사 시작 시 추적 파일의 수정은 없고, 이론 정리 DOCX 파일만 미추적 상태였다.
- 원격 저장소와의 동기화 여부는 확인하지 않았다.
- 루트 README는 제목만 있고 프런트엔드 README는 기본 템플릿이다. 팀원이나 미래의 개발자가 바로 실행하기에는 안내가 부족하다.
- 기존 DOCX에는 React·Spring·Python·MySQL, 인증 방식, 의존성 주입, AI 활용 등에 관한 학습 메모가 있다. 해당 메모는 구현 완료의 증거가 아니다. AI 호출·문서 요약·LangChain 연동 코드는 현재 프로젝트에서 확인되지 않았다.
- DOCX의 `sessionStorage` 설명에는 서버 저장공간이라는 부정확한 표현이 있어 추후 정리가 필요하다. `sessionStorage`는 브라우저 측 저장소다.

이번 작업은 현황 보고서 추가이며 애플리케이션 소스는 수정하지 않았다. 빌드로 생성된 프런트엔드 `dist`는 Git ignore 대상이다.

## 7. 개발 재개 순서 제안

1. **개발 환경과 기본 검증 복구**: Maven Wrapper 오류를 해결하고 MySQL 연결·Spring 기동을 확인한다. 프런트엔드 린트 및 CSS 연결을 정리하고 실행 안내·비밀값 없는 환경변수 예시를 작성한다.
2. **사용자 기능 완성**: 세션 또는 JWT 등 인증 방식을 정한 뒤 회원가입·로그인·로그아웃·현재 사용자 조회를 구현한다. 비밀번호 암호화, 입력 검증, 중복 이메일 검사, DTO 응답과 접근 제어를 적용하고 프런트엔드 폼을 연결한다.
3. **사주 계산 명세 및 Python 구현**: 생년월일시, 양력·음력 구분, 시간대, 출생시각 미상 처리 등 입력 범위와 계산 기준을 정한다. 계산 결과의 JSON 형식과 기준 사례를 만들고 Python 계산 함수를 구현한다.
4. **전체 사용자 흐름 연결**: 사주 계산 API를 구성하고 입력 화면 → 계산 요청 → 결과 화면을 연결한다. 로딩·입력 오류·계산 실패 화면과 필요한 결과 저장 정책을 추가한다.
5. **배포 준비**: 사용자·계산 기능의 핵심 테스트, DB 마이그레이션, 운영 환경 설정과 로그 정책을 정비한다.

첫 완료 목표로는 **사용자가 생년월일 정보를 입력하면 Python 계산 결과를 Spring API를 통해 받아 화면에 표시하는 흐름**을 권장한다. 회원 기능의 선행 여부는 비회원 계산 허용 정책에 따라 조정할 수 있다. 현재 자료만으로 구체적인 완료율이나 개발 소요 기간을 산정하기는 어렵다.

## 8. 주요 근거 파일

- 화면 경로 및 인증 상태: [App.jsx](../sajuview/src/App.jsx), [AuthProvider.jsx](../sajuview/src/contexts/AuthProvider.jsx), [useAuth.js](../sajuview/src/hooks/useAuth.js)
- 사용자 처리: [UserController.java](../sajuspring/src/main/java/com/saju/controller/UserController.java), [UserService.java](../sajuspring/src/main/java/com/saju/service/UserService.java), [UserEntity.java](../sajuspring/src/main/java/com/saju/entity/UserEntity.java)
- 보안 설정: [SecurityConfig.java](../sajuspring/src/main/java/com/saju/config/SecurityConfig.java)
- Python 연동: [SajuController.java](../sajuspring/src/main/java/com/saju/controller/SajuController.java), [PythonRunnerService.java](../sajuspring/src/main/java/com/saju/service/PythonRunnerService.java), [test.py](../sajupython/test.py)
- 의존성·DB 설정: [package.json](../sajuview/package.json), [pom.xml](../sajuspring/pom.xml), [application.properties](../sajuspring/src/main/resources/application.properties)

본 보고서는 조사 시점의 로컬 파일과 제한된 실행 검증을 기준으로 작성했다. 운영 서비스 존재 여부, 실제 데이터베이스 내용, 원격 브랜치의 추가 작업은 판단 범위에 포함하지 않았다.
