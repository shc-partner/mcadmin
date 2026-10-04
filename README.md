# RPGCore 0.4.9 개발 구조

> 기준 버전: **RPGCore 0.4.9**  
> 기준 서버: **Paper 26.2 계열**  
> 기준 환경: **Ubuntu 24.04 / Temurin JDK 25 / MariaDB**  
> 프로젝트 루트: **`/srv/minecraft`**  

---

## 1. 프로젝트 개요

RPGCore는 Paper 기반 Minecraft RPG 서버의 핵심 기능을 담당하는 커스텀 플러그인이다.

설계의 중심은 개별 콘텐츠 설명이 아니라 다음 개발 원칙에 있다.

- RPGCore가 핵심 게임 규칙과 플레이어 데이터의 최종 소유자가 된다.
- MariaDB를 영구 데이터 저장소로 사용한다.
- Bukkit / Paper API 접근과 DB 작업의 실행 스레드를 분리한다.
- 외부 플러그인은 표시, 모델, HUD, NPC, 경제, 리소스 생성 등의 보조 계층으로 사용한다.
- 운영 서버와 개발 소스를 분리한다.
- 리소스팩은 여러 외부 소스를 직접 배포하지 않고 RPGCore 최종 팩으로 조합한다.
- 운영 변경은 백업 → 수정 → 빌드 → 배포 → 로그 확인 → 인게임 검증 순서로 수행한다.

현재 프로젝트에는 성장, 직업, 스킬, 전투, 아이템, 던전, NPC, 경제, 칭호, 업적, 가구, 게임형 콘텐츠 등의 모듈이 존재하지만, 이 문서에서는 각 게임 규칙의 세부 수치나 플레이 방식은 다루지 않는다.

---

## 2. 개발 환경

```text
OS              Ubuntu 24.04
Java            Temurin JDK 25
Paper           26.2-111-main 계열
RPGCore         0.4.9
Database        MariaDB
Build           Gradle 9.6.1
```

Java:

```text
/usr/lib/jvm/temurin-25-jdk-amd64/bin/java
```

운영 JVM 기준:

```text
-Xms8G -Xmx12G
```

Paper API 기준:

```text
api-version: 26.2
```

---

## 3. 전체 디렉토리 구조

프로젝트 루트:

```text
/srv/minecraft
```

주요 구조:

```text
/srv/minecraft/
├── server/                     # 운영 Paper 서버
├── plugins-source/
│   └── RPGCore/                # RPGCore 개발 소스
├── resourcepacks/
│   ├── rpgcore/                # 최종 리소스팩 소스
│   ├── source-models/          # 외부/원본 모델 보관
│   └── rpgcore-0.4.9.zip       # 배포 리소스팩
├── backups/                    # 모든 백업
├── scripts/                    # 관리 스크립트
└── redeploy-resourcepack.py    # 리소스팩 배포 스크립트
```

핵심 경로:

```text
Source      /srv/minecraft/plugins-source/RPGCore
Server      /srv/minecraft/server
Plugin      /srv/minecraft/server/plugins/RPGCore-0.4.9.jar
Backup      /srv/minecraft/backups
Pack Source /srv/minecraft/resourcepacks/rpgcore
Pack ZIP    /srv/minecraft/resourcepacks/rpgcore-0.4.9.zip
```

---

## 4. RPGCore 프로젝트 구조

기본 Gradle 프로젝트:

```text
RPGCore/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/hcs/rpgcore/
│       └── resources/
├── gradle/
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
└── gradlew.bat
```

리소스:

```text
src/main/resources/
├── plugin.yml
├── config.yml
└── database.yml        # 운영 비밀정보, Git 제외
```

Git 배포본에서는 실제 DB 설정 대신 다음과 같은 예제 파일을 사용한다.

```text
database.example.yml
```

---

## 5. Java 패키지 구조

현재 RPGCore는 기능 단위 패키지 구조를 사용한다.

대표 구조:

```text
com.hcs.rpgcore
├── boss/
├── check/
├── classjob/
├── combat/
├── command/
├── craft/
├── database/
├── dismantle/
├── dungeon/
├── elixir/
├── enchant/
├── furniture/
├── horse/
├── hud/
├── item/
├── level/
├── listener/
├── locator/
├── mana/
├── market/
├── mob/
├── placeholder/
├── player/
├── runtime/
├── shop/
├── skill/
├── slot/
├── starter/
├── stat/
├── storage/
└── title/
```

각 패키지는 가능한 한 다음 책임 중 하나를 담당한다.

```text
Listener       Paper 이벤트 수신
Service        비즈니스 로직
Repository     DB 접근
Factory        Bukkit ItemStack 등 객체 생성
Task           반복 작업 / 스케줄 작업
Command        명령 처리
State / DTO    상태 및 데이터 전달
Registry       정의 등록 / 조회
```

---

## 6. 메인 플러그인 초기화 구조

진입점:

```text
RPGCorePlugin
```

주요 책임:

1. 설정 로드
2. DB 연결 초기화
3. Repository 생성
4. Service 생성
5. Listener 생성 및 등록
6. Command 등록
7. Placeholder / 외부 플러그인 hook
8. 반복 Task 시작
9. 종료 시 리소스 정리

권장 의존성 방향:

```text
RPGCorePlugin
    ↓
Repository 생성
    ↓
Service 생성
    ↓
Listener / Command 생성
```

하위 Service가 `RPGCorePlugin` 전체를 직접 참조하는 방식은 가능한 한 피하고, 필요한 의존성만 생성자 주입한다.

---

## 7. 계층 구조

RPGCore의 일반적인 기능 흐름:

```text
Paper Event / Command
        ↓
Listener / Command
        ↓
Service
        ↓
Repository
        ↓
MariaDB
```

표시가 필요한 경우:

```text
Repository / Service
        ↓
Main Thread
        ↓
Player / Inventory / BossBar / HUD / Message
```

외부 플러그인 사용 시:

```text
Service
   ├── Vault
   ├── Citizens
   ├── PlaceholderAPI
   ├── BetterHud
   ├── BetterModel
   ├── ItemsAdder
   └── Mythic / ModelEngine 계열
```

---

## 8. 데이터베이스 계층

RPGCore의 영구 데이터는 MariaDB에 저장한다.

DatabaseManager는 공통 DB 연결 계층을 담당하고, 기능별 Repository가 SQL을 소유한다.

대표 패턴:

```text
Service
    ↓
SomeRepository
    ↓
PreparedStatement
    ↓
MariaDB
```

Repository 설계 원칙:

- SQL은 Listener에 직접 작성하지 않는다.
- 플레이어 식별은 UUID를 기본으로 한다.
- INSERT / UPDATE는 중복 실행에 안전하도록 설계한다.
- 필요한 경우 DB unique key 또는 primary key로 중복을 방지한다.
- 운영 데이터 변경은 명시적인 트랜잭션 상태를 남긴다.
- Bukkit API 객체를 DB 스레드에서 직접 조작하지 않는다.

---

## 9. 플레이어 식별 구조

내부 식별:

```text
player_uuid
```

계정 이름:

```text
player_name
```

표시용 이름:

```text
display_name
```

원칙:

```text
UUID          내부 영구 식별
player_name   Mojang 계정명 기록
display_name  UI / 이름표 / 표시 목적
```

DB 키와 운영 로직을 `display_name`에 의존시키지 않는다.

---

## 10. 비동기 처리 구조

DB 작업은 서버 메인 틱을 막지 않도록 비동기 실행을 기본으로 한다.

대표 흐름:

```text
Main Thread
    ↓
Bukkit 상태 확인
    ↓
runTaskAsynchronously
    ↓
Repository / MariaDB
    ↓
runTask
    ↓
Bukkit 객체 변경 / 메시지 출력
```

### 메인 스레드에서 처리할 것

- Bukkit Entity 접근
- Inventory 변경
- Player 메시지
- World 변경
- 대부분의 Vault 호출
- NPC / GUI 조작

### 비동기 처리할 것

- SELECT
- INSERT
- UPDATE
- DELETE
- 단순 DB 계산

비동기 스레드에서 Bukkit 객체를 직접 변경하지 않는다.

---

## 11. Repository / Service 분리 원칙

### Repository

담당:

```text
SQL
DB row 읽기
DB row 삽입
DB row 갱신
DB 상태 전이
```

담당하지 않음:

```text
플레이어 메시지
GUI
월드 조작
이펙트
게임 진행
```

### Service

담당:

```text
조건 판단
게임 규칙
여러 Repository 조합
외부 API 호출 조정
결과 처리
```

### Listener

담당:

```text
Paper 이벤트를 Service 호출로 변환
```

Listener가 복잡한 비즈니스 로직을 직접 소유하지 않도록 유지한다.

---

## 12. 안전한 거래 상태 전이

경제성 기능은 단순히 골드 차감 후 결과를 저장하지 않는다.

대표 상태:

```text
PREPARED
DEBIT_IN_FLIGHT
DEBIT_CONFIRMED
CANCELLED
REFUND_PENDING
```

기본 흐름:

```text
DB 거래 생성
    ↓
PREPARED
    ↓
DEBIT_IN_FLIGHT
    ↓
Vault withdraw
    ↓
성공 → DEBIT_CONFIRMED
실패 → CANCELLED
```

목적:

- 서버 비정상 종료 대응
- 중복 차감 방지
- 차감 성공 여부 추적
- 환불 판단 가능
- 운영자가 DB에서 거래 상태를 확인 가능

---

## 13. 데이터 진행도 공통 구조

누적 조건이 필요한 기능은 공용 진행도 테이블 구조를 사용할 수 있다.

현재 구현 예:

```text
rpg_player_achievement_progress
```

기본 구조:

```text
player_uuid
progress_type
progress_target
progress_value
updated_at
```

키:

```text
(player_uuid, progress_type, progress_target)
```

이 구조는 특정 콘텐츠에 종속시키지 않고 다음과 같이 일반화할 수 있다.

```text
이벤트 발생
    ↓
Progress Service
    ↓
Progress Repository
    ↓
progress_value 증가
    ↓
조건 충족 여부 확인
    ↓
후속 기능 호출
```

---

## 14. 명령 처리 구조

명령은 `command/` 패키지에 분리한다.

기본 패턴:

```text
Command
    ↓
권한 확인
    ↓
인자 검증
    ↓
Service 호출
    ↓
결과 메시지
```

관리자 명령과 플레이어 명령은 역할을 분리한다.

운영 데이터를 직접 변경하는 관리자 명령은 입력값 검증과 대상 검증을 반드시 수행한다.

---

## 15. 이벤트 처리 구조

Paper 이벤트 Listener는 가능한 한 얇게 유지한다.

예:

```text
Event
  ↓
대상 / 조건 최소 확인
  ↓
Service 호출
```

금지에 가까운 구조:

```text
Listener
  ├── 긴 SQL
  ├── 대규모 계산
  ├── 여러 외부 API 직접 호출
  └── 수백 줄 비즈니스 로직
```

---

## 16. GUI 구조

인벤토리 GUI는 다음 구조를 권장한다.

```text
Gui / Menu
    ↓
InventoryHolder
    ↓
Listener
    ↓
Service
```

GUI에 DB 쿼리나 경제 처리 로직을 직접 넣지 않는다.

필요 시 Repository에서 데이터를 가져온 뒤 UI 모델로 변환한다.

---

## 17. 외부 플러그인 의존성

현재 주요 외부 의존성:

```text
BetterHud
BetterModel
Citizens
ItemsAdder
ModelEngine
MythicArmors
MythicMobs
PlaceholderAPI
ProtocolLib
Vault
WorldEdit
WorldGuard
CraftEngine 계열
```

설계 원칙:

```text
RPGCore         게임 규칙 / 영구 데이터
Citizens        NPC 표현
Vault           경제 API
BetterHud       HUD 표현
PlaceholderAPI  값 노출
BetterModel     모델 표현
ItemsAdder      외부 리소스 생성 보조
Mythic*         몹 / 방어구 외형 및 동작 보조
```

외부 플러그인이 RPGCore의 DB나 핵심 게임 규칙의 원본이 되지 않도록 한다.

---

## 18. Placeholder 구조

RPGCore는 PlaceholderAPI를 통해 외부 HUD / UI가 사용할 값을 제공한다.

권장 구조:

```text
RPGCore internal data
        ↓
Placeholder Expansion
        ↓
PlaceholderAPI
        ↓
BetterHud / 기타 표시 플러그인
```

Placeholder 클래스에 새로운 게임 규칙을 구현하지 않는다.

---

## 19. HUD 구조

BetterHud는 표현 계층이다.

RPGCore:

```text
상태 계산
    ↓
Placeholder / HUD Service
    ↓
BetterHud
```

BetterHud 설정과 RPGCore 비즈니스 로직을 강하게 결합하지 않는다.

HUD asset 구조는 이미 정상 운영 중인 리소스 구조를 유지한다.

---

## 20. 리소스팩 소유 구조

최종 리소스팩의 소유자는 RPGCore 프로젝트이다.

소스:

```text
/srv/minecraft/resourcepacks/rpgcore
```

최종 배포 파일:

```text
/srv/minecraft/resourcepacks/rpgcore-0.4.9.zip
```

접속 클라이언트는 이 최종 ZIP을 다운로드한다.

외부 플러그인이 생성한 팩을 그대로 사용자에게 배포하는 구조가 아니다.

---

## 21. 리소스팩 빌드 파이프라인

관리 스크립트:

```text
/srv/minecraft/redeploy-resourcepack.py
```

개념 구조:

```text
RPGCore Source Pack
        +
BetterHud Resources
        +
BetterModel / ModelEngine Resources
        +
Mythic / Armor Resources
        +
검증된 ItemsAdder Output
        ↓
Merge
        ↓
Atlas 검증
        ↓
ZIP 생성
        ↓
SHA1 계산
        ↓
server.properties 갱신
```

배포 파일:

```text
/srv/minecraft/resourcepacks/rpgcore-0.4.9.zip
```

---

## 22. ItemsAdder의 역할

ItemsAdder는 최종 팩 소유자가 아니다.

사용 흐름:

```text
ItemsAdder source
    ↓
/iazip --uncompressed --apply-to none
    ↓
output_uncompressed
    ↓
필요 파일 검증
    ↓
RPGCore source pack으로 병합
```

금지:

```text
일반 /iazip 자동 배포
ItemsAdder auto apply
ItemsAdder auto hosting
최종 팩 자동 덮어쓰기
```

---

## 23. Atlas / Overlay 관리

Paper 26.2 resource pack format:

```text
88
```

Paper 26.2용 overlay 예:

```text
ia_overlay_26_2_plus
```

Atlas 관리 원칙:

- 기존 source 보존
- sprite ID 충돌 확인
- merge 전후 JSON 비교
- 외부 생성 atlas 전체 덮어쓰기 금지
- 대량 JSON 삭제 금지
- 리소스팩 변경 후 최종 ZIP에서 재검증

---

## 24. 빌드 구조

빌드:

```bash
cd /srv/minecraft/plugins-source/RPGCore
./gradlew clean build
```

결과:

```text
build/libs/RPGCore-0.4.9.jar
```

운영 배포:

```text
/srv/minecraft/server/plugins/RPGCore-0.4.9.jar
```

빌드 결과물은 Git에 포함하지 않는다.

---

## 25. 배포 절차

운영 플러그인 변경 시:

```text
1. 소스 상태 확인
2. /srv/minecraft/backups 에 백업
3. 소스 수정
4. ./gradlew clean build
5. build success 확인
6. 서버 중지
7. 운영 JAR 교체
8. 필요 시 SHA256 비교
9. 서버 시작
10. journal 로그 확인
11. 인게임 최소 기능 테스트
```

운영 중 JAR을 직접 덮어쓴 뒤 reload 하는 방식은 기본 배포 절차로 사용하지 않는다.

---

## 26. 로그 검증

배포 후 최소 확인 대상:

```text
RPGCore enable 성공
DB 연결 성공
Listener 등록 오류 없음
Command 등록 오류 없음
Placeholder hook 성공
외부 plugin hook 실패 여부
SQLException 없음
ClassNotFoundException 없음
NoSuchMethodError 없음
```

예:

```bash
sudo journalctl -u minecraft --no-pager | tail -200
```

필요한 모듈만 필터링:

```bash
sudo journalctl -u minecraft --no-pager \
| grep -Ei 'RPGCore|ERROR|WARN|Exception'
```

---

## 27. 백업 정책

모든 백업은 다음 위치만 사용한다.

```text
/srv/minecraft/backups
```

금지:

```text
소스 옆 *.bak
*.java.bak
임시 수정본을 src/main/java 안에 장기 보관
```

백업 디렉토리 예:

```text
/srv/minecraft/backups/<작업명>-YYYYMMDD-HHMMSS/
```

백업 대상 예:

```text
운영 JAR
수정 대상 Java
resource pack ZIP
resource pack source 일부
설정 파일
```

---

## 28. 소스 수정 원칙

운영 중인 0.4.9 기준에서는 구조 전체를 불필요하게 정리하지 않는다.

수정 순서:

```text
현재 코드 확인
    ↓
실제 호출 구조 확인
    ↓
최소 변경 범위 결정
    ↓
백업
    ↓
수정
    ↓
컴파일
    ↓
테스트
```

금지:

- 근거 없는 클래스 이동
- 대규모 package rename
- 사용 여부 확인 없이 파일 삭제
- 정상 리소스팩 JSON 일괄 삭제
- 기존 서비스 인터페이스 임의 변경
- 외부 플러그인 구조 추측 후 수정

---

## 29. 파일 정리 기준

Git 저장소에는 실제 개발 파일만 유지한다.

제외 대상 예:

```text
*.before-*
*.bak
*.old
Compilation
Task
build.gradle.kts.pre-*
build/
.gradle/
```

이전 수정본은 Git history 또는 `/srv/minecraft/backups`에서 관리한다.

---

## 30. GitHub 저장소 구조

권장 구조:

```text
mcadmin/
├── RPGCore/
│   ├── src/
│   ├── gradle/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   ├── gradlew
│   └── gradlew.bat
├── scripts/
├── docs/
├── .gitignore
└── README.md
```

운영 서버 전체를 Git 저장소에 넣지 않는다.

---

## 31. Git 제외 대상

반드시 제외:

```text
server/
backups/
logs/
crash-reports/

.gradle/
build/

database.yml
secret.yml
.env

*.class
*.log

resourcepacks/source-models/
resourcepacks/*.zip

구매 자산
외부 배포 제한 자산
운영 DB dump
```

Gradle wrapper JAR은 예외적으로 포함 가능하다.

```text
RPGCore/gradle/wrapper/gradle-wrapper.jar
```

---

## 32. 비밀정보 관리

실제 DB 설정은 Git에 포함하지 않는다.

운영:

```text
database.yml
```

Git:

```text
database.example.yml
```

예:

```yaml
database:
  host: localhost
  port: 3306
  name: rpg
  username: your_username
  password: your_password
```

커밋 전 확인:

```bash
grep -RIn \
  --exclude-dir=.git \
  -Ei 'password|api[_-]?key|access[_-]?token|secret|private[_-]?key|rcon' \
  .
```

---

## 33. Git 개발 흐름

기본 흐름:

```text
작업 전 pull
    ↓
코드 수정
    ↓
로컬 검토
    ↓
git diff
    ↓
commit
    ↓
push
```

권장 명령:

```bash
git status
git diff
git add <필요 파일>
git commit -m "<변경 내용>"
git push origin main
```

운영 소스에서는 `git add .`보다 변경 파일을 확인한 뒤 선택적으로 추가하는 방식을 권장한다.

---

## 34. 개발 모듈 목록

0.4.9의 주요 코드 모듈은 다음 정도로만 분류한다.

```text
Core / Bootstrap
Database
Player
Runtime
Level
Class
Stat
Combat
Mana
Skill
Item
Craft
Dismantle
Enchant
Dungeon
Mob / Boss
Shop / Economy
Furniture
Storage
HUD
Placeholder
Title / Achievement
Command
NPC Integration
Resource Pack Integration
```

세부 콘텐츠 규칙은 각 기능 소스와 별도 설계 문서에서 관리한다.

---

## 35. 신규 기능 추가 표준

신규 기능은 다음 구조를 우선 검토한다.

```text
<feature>/
├── <Feature>Listener.java
├── <Feature>Service.java
├── <Feature>Repository.java
├── <Feature>State.java
└── <Feature>Definition.java
```

모든 기능에 이 파일이 전부 필요한 것은 아니다.

기준:

- 이벤트만 필요 → Listener
- 규칙 필요 → Service
- DB 필요 → Repository
- 상태 필요 → State / DTO
- 등록형 데이터 → Definition / Registry

---

## 36. DB 기능 추가 표준

DB 기능 추가 시:

```text
1. 실제 요구 필드 정의
2. PK / UNIQUE 결정
3. Repository 작성
4. Service 작성
5. 비동기 호출 위치 결정
6. 실패 처리 정의
7. 운영 SQL 적용
8. 실제 row 확인
```

테이블 변경 전에 기존 schema를 반드시 확인한다.

이미 존재하는 컬럼을 추측으로 다시 추가하지 않는다.

---

## 37. 외부 API 연결 표준

새 외부 플러그인 연동 시:

```text
플러그인 존재 확인
    ↓
API 객체 획득
    ↓
RPGCore Adapter / Service 계층에서 사용
    ↓
실패 시 기능 비활성화 또는 안전 fallback
```

가능하면 외부 API 호출을 여러 Listener에 흩뿌리지 않는다.

---

## 38. 오류 처리 원칙

오류는 다음 세 부류로 나눈다.

### 개발 오류

```text
잘못된 상태
필수 정의 누락
코드 계약 위반
```

명확한 로그를 남긴다.

### 운영 입력 오류

```text
잘못된 명령 인자
대상 없음
잘못된 설정
```

서버 전체 예외로 확산시키지 않는다.

### 외부 의존성 오류

```text
Vault provider 없음
Citizens 없음
ItemsAdder 실패
DB 연결 실패
```

원인을 로그로 남기고 해당 기능만 제한하는 방향을 우선한다.

---

## 39. 테스트 기준

### 컴파일 테스트

```bash
./gradlew clean build
```

### 시작 테스트

```text
플러그인 enable
DB 연결
Listener 등록
외부 hook
```

### 인게임 최소 테스트

변경한 기능만 우선 검증한다.

### 회귀 테스트

공통 계층을 수정한 경우 연관 시스템을 확인한다.

예:

```text
DatabaseManager 수정
→ 접속 / 저장 / 상점 / 칭호 등 DB 사용 기능 확인

ShopEconomyService 수정
→ 모든 골드 차감 기능 확인

CustomItemFactory 수정
→ 제작 / 보상 / 상점 / 관리자 지급 확인
```

---

## 40. 운영과 개발의 분리

개발 소스:

```text
/srv/minecraft/plugins-source/RPGCore
```

운영 결과물:

```text
/srv/minecraft/server/plugins/RPGCore-0.4.9.jar
```

리소스팩 소스:

```text
/srv/minecraft/resourcepacks/rpgcore
```

배포 리소스팩:

```text
/srv/minecraft/resourcepacks/rpgcore-0.4.9.zip
```

소스, 빌드 결과물, 운영 결과물의 역할을 섞지 않는다.

---

## 41. 0.4.9 개발 완료 상태

현재 0.4.9는 다음 개발 기반이 구축된 상태이다.

```text
Paper 26.2 호환
Java 25 빌드 환경
Gradle 프로젝트
MariaDB 계층
Repository / Service 구조
Paper Listener 구조
Command 구조
외부 플러그인 연동
비동기 DB 처리
경제 거래 상태 전이
진행도 저장 구조
HUD / Placeholder 연결
NPC 연결
리소스팩 composer
Atlas / Overlay 대응
운영 배포 절차
백업 절차
```

게임 내부 기능은 이 개발 기반 위의 모듈로 취급한다.

---

## 42. 향후 리팩터링 방향

0.5.x 이후에는 필요에 따라 다음 개선을 검토할 수 있다.

### 42.1 공통 DB 추상화

반복되는 Repository 코드를 정리하되, 0.4.9 운영 안정성을 해치지 않는 범위에서 진행한다.

### 42.2 외부 플러그인 Adapter

```text
VaultAdapter
CitizensAdapter
ItemsAdderAdapter
BetterModelAdapter
```

형태로 외부 API 경계를 더 명확하게 만들 수 있다.

### 42.3 Feature 단위 등록

RPGCorePlugin의 초기화 코드가 지나치게 커질 경우:

```text
FeatureModule
```

형태로 등록 계층을 분리할 수 있다.

### 42.4 공통 진행도 엔진

현재 업적 진행도 구조를 범용 progression 구조로 일반화할 수 있다.

### 42.5 DB migration 관리

수동 SQL 적용이 늘어날 경우 버전별 migration 체계를 도입할 수 있다.

---

## 43. 핵심 유지 원칙

1. **RPGCore가 핵심 데이터와 규칙을 소유한다.**
2. **Listener는 얇게 유지한다.**
3. **비즈니스 로직은 Service에 둔다.**
4. **SQL은 Repository에 둔다.**
5. **DB 작업은 비동기로 처리한다.**
6. **Bukkit API는 적절한 메인 스레드에서 호출한다.**
7. **UUID를 영구 식별자로 사용한다.**
8. **외부 플러그인은 보조 계층으로 둔다.**
9. **경제 거래는 상태 전이를 기록한다.**
10. **리소스팩은 RPGCore 최종 composer가 소유한다.**
11. **운영 변경 전 `/srv/minecraft/backups`에 백업한다.**
12. **소스 안에 임시 백업 파일을 남기지 않는다.**
13. **대규모 구조 변경보다 실제 코드 확인 후 최소 변경을 우선한다.**
14. **빌드 성공만으로 완료하지 않고 startup log와 인게임 동작을 확인한다.**
15. **Git에는 개발 산출물만 포함하고 운영 데이터와 비밀정보는 제외한다.**

---

## 44. 문서 기준

이 문서는 **RPGCore 0.4.9 개발 완료 상태**를 기준으로 한다.

```text
Project Root   /srv/minecraft
RPGCore        0.4.9
Paper          26.2
Java           25
Database       MariaDB
```

게임 콘텐츠별 세부 규칙보다 **현재 코드를 안전하게 유지·확장하기 위한 개발 구조**를 우선 문서화한다.

향후 버전에서는 이 문서를 기반으로:

```text
docs/
├── ARCHITECTURE.md
├── DATABASE.md
├── RESOURCE_PACK.md
├── DEPLOYMENT.md
└── MODULES.md
```

형태로 분리할 수 있다.
