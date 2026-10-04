# RPGCore 0.4.9 전체 설계 문서

> 기준 버전: **RPGCore 0.4.9**  
> 기준 서버: **Paper 26.2 계열**  
> 기준 환경: **Ubuntu 24.04 / Temurin JDK 25 / MariaDB**  
> 문서 목적: 현재까지 개발 완료된 RPGCore 0.4.9의 서버 구조, 핵심 시스템, 데이터 흐름, 콘텐츠 구성, 리소스팩 파이프라인, 배포 및 운영 기준을 한 문서에 정리한다.

---

## 1. 프로젝트 개요

RPGCore는 Paper 26.2 기반 Minecraft RPG 서버의 핵심 시스템을 담당하는 커스텀 플러그인이다.

주요 목표는 다음과 같다.

- 최대 레벨 99의 성장 시스템
- 전사 / 마법사 직업 시스템
- 커스텀 스킬
- 고정형 던전 및 웨이브 시스템
- 커스텀 무기 / 방어구 / 장비
- RPG 스탯 및 전투 계산
- HUD / 이름표 / 칭호 표시
- NPC 기반 상점 및 관리 기능
- 가구 구매 / 설치 / 착석
- 경마 / 슬롯머신
- 칭호 및 업적 기반 칭호 해금
- MariaDB 기반 영구 데이터 저장
- 서버 리소스팩 자동 조합 및 배포

RPGCore는 외부 플러그인을 보조적으로 활용하지만, 핵심 게임 규칙과 플레이어 데이터의 최종 소유자는 RPGCore로 유지한다.

---

## 2. 운영 환경

### 2.1 서버

```text
OS              Ubuntu 24.04
Java            Temurin JDK 25
Paper           26.2-111-main 계열
RPGCore         0.4.9
Database        MariaDB
Build           Gradle 9.6.1
```

Java 실행 경로 기준:

```text
/usr/lib/jvm/temurin-25-jdk-amd64/bin/java
```

운영 JVM 메모리 기준:

```text
-Xms8G -Xmx12G
```

---

## 3. 주요 디렉토리 구조

프로젝트 루트:

```text
/srv/minecraft
```

핵심 경로:

```text
/srv/minecraft/
├── server/
├── plugins-source/
│   └── RPGCore/
├── resourcepacks/
│   ├── rpgcore/
│   ├── source-models/
│   └── rpgcore-0.4.9.zip
├── backups/
├── scripts/
└── redeploy-resourcepack.py
```

RPGCore 소스:

```text
/srv/minecraft/plugins-source/RPGCore
```

운영 서버:

```text
/srv/minecraft/server
```

운영 플러그인:

```text
/srv/minecraft/server/plugins/RPGCore-0.4.9.jar
```

백업:

```text
/srv/minecraft/backups
```

### 운영 원칙

- 소스 디렉토리 안에 `.bak` 파일을 만들지 않는다.
- 모든 백업은 `/srv/minecraft/backups` 아래에 생성한다.
- 운영 서버 파일 수정 전 백업한다.
- 대규모 자동 정리 / JSON 일괄 삭제 / 구조 재배치는 금지한다.
- 정상 동작 중인 리소스팩 경로는 임의로 변경하지 않는다.

---

## 4. 빌드 및 배포

### 4.1 빌드

```bash
cd /srv/minecraft/plugins-source/RPGCore
./gradlew clean build
```

출력:

```text
build/libs/RPGCore-0.4.9.jar
```

### 4.2 배포

운영 대상:

```text
/srv/minecraft/server/plugins/RPGCore-0.4.9.jar
```

배포 원칙:

1. 기존 운영 JAR 백업
2. 서버 중지
3. 새 JAR 복사
4. SHA256 비교
5. 서버 시작
6. startup log 확인
7. 인게임 기능 테스트

---

## 5. RPGCore 플러그인 구조

RPGCore는 Paper 플러그인으로 동작하며 `api-version: 26.2`를 사용한다.

주요 외부 의존성:

- BetterModel
- PlaceholderAPI
- Vault
- Citizens
- ItemsAdder
- BetterHud
- MythicMobs
- MythicArmors
- ModelEngine
- WorldEdit
- WorldGuard
- CraftEngine 계열 리소스

운영 플러그인 구성에는 다음이 포함된다.

```text
BetterHud
Chairs
Chunky
Citizens
CustomCrops
EternalEconomy / Vault 계열 경제
ItemsAdder
ModelEngine
MythicArmors
MythicMobs
PlaceholderAPI
ProtocolLib
RPGCore
VanillaFurniture
Vault
BetterModel
CraftEngine
WorldEdit
WorldGuard
```

GriefPrevention은 0.4.9 운영 후 비활성화되었다.

---

## 6. 데이터베이스

RPGCore의 핵심 데이터는 MariaDB에 저장한다.

### 6.1 플레이어 기본 데이터

대표 테이블:

```text
rpg_players
```

주요 필드:

```text
player_uuid
player_name
display_name
level
experience
player_class
created_at
last_login_at
updated_at
```

원칙:

- 실제 계정 식별은 UUID와 원래 계정명으로 유지한다.
- 서버 표시 이름은 `display_name`을 별도로 사용할 수 있다.
- DB 내부 식별자를 한글 표시명으로 대체하지 않는다.

---

## 7. 레벨 / 경험치 시스템

최대 레벨:

```text
99
```

기본 구조:

```text
플레이어 행동
    ↓
경험치 획득
    ↓
LevelService
    ↓
현재 레벨 / EXP 계산
    ↓
DB 저장
    ↓
레벨업 후속 처리
```

특징:

- 몬스터 처치 EXP
- 던전 클리어 EXP
- 관리자 EXP / 레벨 조정
- 재접속 후 DB 복원
- 고레벨 칭호 자동 검사와 연계

전직 전 성장 제한 정책을 적용할 수 있으며, 초기 설계상 Lv.5 전직 구간이 핵심 분기점이다.

---

## 8. 직업 시스템

현재 핵심 직업:

```text
Warrior
Mage
```

직업 데이터는 플레이어 DB와 연계한다.

### 전사

주요 구현 스킬 예:

- Bash
- Dash
- Execution Slash
- Berserker Rage
- 기타 전사 공격 / 버프 스킬

### 마법사

주요 구현 스킬 예:

- Fire Bolt
- Teleport
- Gate of Babylon
- Meteor Strike
- Black Hole
- Mana Overload
- 기타 마법 공격 / 버프 스킬

### 스킬 공통 원칙

- 스킬 사용 조건 확인
- 재사용 대기시간
- 마나 / 자원 소모
- 대상 판정
- 데미지 처리
- 이펙트 / 사운드
- 공격 / 방어 버프 서비스와 연동

---

## 9. 전투 / 스탯 시스템

RPGCore는 바닐라 수치만 사용하는 것이 아니라 자체 RPG 스탯을 관리한다.

대표 스탯:

```text
공격력
방어력
표시 방어력
강인함
고정 피해 감소
HP
MP
EXP
직업
레벨
```

아이템 옵션 중 최대 HP / 최대 MP 증가 옵션은 사용하지 않는다.

최대 HP / MP는 레벨 및 직업 성장에 의해 결정되며 장비로 직접 증가시키지 않는다.

---

## 10. 아이템 등급

RPGCore의 아이템 등급은 다음 5단계로 고정한다.

```text
고급
희귀
영웅
전설
신화
```

칭호 등급도 같은 체계를 기준으로 사용할 수 있다.

---

## 11. 장비 / 커스텀 아이템

커스텀 장비는 RPGCore의 자체 아이템 정의와 외형 시스템을 결합한다.

주요 기능:

- 커스텀 무기
- 커스텀 방어구
- 공격력
- 방어력
- 강인함
- 고정 피해 감소
- 장비 슬롯
- 세트 외형
- 강화 / 제작 / 분해 연계

외형 제공 기술:

- BetterModel
- MythicArmors
- ModelEngine
- ItemsAdder
- CraftEngine 계열

핵심 게임 수치는 가능한 한 RPGCore가 소유한다.

---

## 12. 장비 제작 / 분해 / 강화

### 제작

- 제작 NPC / GUI
- 장비 분류
- 제작 재료 검사
- 제작 실행
- 미리보기
- 세트 미리보기

### 분해

- 장비 분해
- 분해 보상
- 반환 데이터 관리

### 강화

- 강화 시도
- 성공 / 실패
- 강화 수치 반영
- 향후 업적과 연결 가능

---

## 13. HUD / 표시 시스템

BetterHud와 RPGCore를 연계해 RPG UI를 제공한다.

표시 대상:

- HP
- MP
- 배고픔 / 상태
- 레벨
- 경험치
- 직업
- 공격력
- 방어력

플레이어는 별도 클라이언트 모드를 필수로 설치하지 않고 서버 리소스팩을 통해 UI를 제공받는 구조를 지향한다.

---

## 14. 플레이어 이름표 / 표시 이름

RPGCore에는 `PlayerNameTagService`가 존재한다.

DB의 `display_name`을 이용해 서버 내 표시 이름을 별도로 관리할 수 있다.

예:

```text
player_name  = ShinHongRyeon
display_name = 신홍련
```

원칙:

- UUID / 계정명은 내부 식별용
- display_name은 플레이어에게 보여주는 이름
- 명령어 대상 검색은 계정명 / 표시명 정책을 명확히 구분해야 한다.

---

## 15. 던전 시스템

RPGCore는 고정형 던전 중심으로 설계되어 있다.

공통 규칙:

- 플레이어가 지정 범위에 진입하면 던전 시작
- 웨이브 기반 진행
- 던전 소환 몹은 지정 범위를 이탈하지 못하도록 제어
- 클리어 시 EXP / 아이템 보상
- 보스 / 특수 던전은 개별 서비스로 관리

확인된 주요 던전 계열:

- Zombie Dungeon
- Ancient Depths
- Nether Fortress
- Enderman Dungeon
- Minotaur Dungeon
- Void Sanctum
- Shulker Dungeon
- Fallen Angel Boss Dungeon
- Red Dragon Dungeon

### 던전 UI 원칙

채팅 / Subtitle / BossBar 중심으로 진행 정보를 제공한다.

예:

- 입장 카운트다운
- Wave 시작
- Wave Clear
- 다음 Wave 안내
- 보스 상태
- 클리어 안내

---

## 16. 던전 보상

`DungeonRewardService`가 던전 보상 로직을 담당한다.

보상 예:

- 경험치
- 커스텀 아이템
- 재료
- 골드
- 던전별 보상

던전 고유 업적이 필요한 경우 공통 보상 메서드에 억지로 합치지 않고, 실제 해당 던전의 확정 클리어 지점에서 업적 서비스를 호출하는 구조를 권장한다.

---

## 17. 몬스터 처치 / EXP

`MobKillListener`가 플레이어의 몬스터 처치 이벤트를 처리한다.

처리 흐름:

```text
EntityDeathEvent
    ↓
killer 확인
    ↓
던전 / 몬스터 유형 판정
    ↓
획득 EXP 계산
    ↓
비동기 DB 저장
    ↓
레벨업 후속 처리
```

업적 처치 카운트는 EXP 지급 여부와 독립적으로 설계하는 것이 안전하다.

---

## 18. NPC 시스템

Citizens NPC를 RPG 콘텐츠 진입점으로 적극 활용한다.

NPC 역할 예:

- 직업 관련
- 장비 제작
- 장비 분해
- 상점
- 가구 상점
- 경마
- 슬롯머신
- 칭호 관리

NPC 기반 GUI를 통해 복잡한 명령어 사용을 최소화한다.

---

## 19. 가구 상점 - NPC 25

NPC 25는 가구 구매 UI를 담당한다.

DB:

```text
rpg_furniture_shop_items
```

주요 필드:

```text
id
shop_item_id
provider
provider_item_id
display_name
price_gold
display_order
enabled
```

지원 Provider:

```text
VANILLA
CRAFTENGINE
ITEMSADDER
RPGCORE
MYTHICMOBS
ENCHANT_BOOK
```

가구 상점 UI:

```text
Inventory Size = 45
Page Size      = 36
Previous       = 36
Buy            = 40
Next           = 44
```

기능:

- 아이템 미리보기
- 페이지 이동
- 구매
- 골드 차감
- 설치
- 착석
- 조명 기능
- Provider별 아이템 생성

---

## 20. ItemsAdder 연동

ItemsAdder는 핵심 시스템 소유자가 아니라 **외형 / 리소스 생성 보조 도구**로 사용한다.

원칙:

```text
ItemsAdder
    ↓
외부 리소스 로드
    ↓
IA behavior 등록
    ↓
uncompressed output 생성
    ↓
검증
    ↓
RPGCore source pack으로 명시적 병합
```

사용 명령:

```text
/iazip --uncompressed --apply-to none
```

금지:

- 일반 `/iazip`으로 운영 팩 자동 적용
- ItemsAdder가 최종 RPGCore 리소스팩을 직접 소유
- 자동 호스팅
- 자동 적용

ItemsAdder 설정은 최종 리소스팩을 덮어쓰지 않도록 유지한다.

---

## 21. Nieyels 가구

namespace:

```text
nieyels
```

구성:

```text
/srv/minecraft/server/plugins/ItemsAdder/contents/nieyels
```

기능:

- 가구 외형
- 설치
- 착석
- NPC 상점 연동
- Paper 26.2 resource item carrier 대응

---

## 22. FurniturePlus

namespace:

```text
furnituresplus
```

총 등록 가구:

```text
139
```

DB상 NPC 25 가구 상점에 ItemsAdder provider로 등록되었다.

특징:

- 9개 색상 계열 × 15종
- 추가 paint / radio 계열
- 설치
- 착석
- 조명
- NPC 판매

FurniturePlus 리소스는 ItemsAdder에서 생성한 뒤 RPGCore 최종 팩으로 병합한다.

---

## 23. 칭호 시스템 - NPC 26

NPC 26은 플레이어 칭호 관리 UI를 담당한다.

핵심 테이블:

```text
rpg_title_definitions
rpg_player_title_unlocks
rpg_player_title_equipped
```

### 칭호 정의

대표 필드:

```text
title_id
title_text
rarity
title_color
unlock_type
unlock_target
unlock_value
created_at
```

### 획득

`rpg_player_title_unlocks`에 획득 기록을 저장한다.

중복 획득은:

```text
PRIMARY KEY(player_uuid, title_id)
```

형태로 방지한다.

### 장착

플레이어는 획득한 칭호만 장착 가능하다.

자동 장착은 하지 않는다.

---

## 24. 기존 칭호

칭호 예시:

### 재벌

```text
title_id      wealthy_10m
조건          10,000,000 골드 이상 보유
등급          전설
unlock_type   balance_at_least

---

## 25. 업적 기반 칭호 시스템

0.4.9에서 업적 기반 칭호 진행도 시스템이 추가되었다.

신규 테이블:

```text
rpg_player_achievement_progress
```

구조:

```text
player_uuid
progress_type
progress_target
progress_value
updated_at
```

기본 키:

```text
(player_uuid, progress_type, progress_target)
```

Repository:

```text
AchievementProgressRepository
```

Service:

```text
AchievementTitleUnlockService
```

구조:

```text
콘텐츠에서 조건 충족
    ↓
AchievementTitleUnlockService
    ↓
AchievementProgressRepository
    ↓
진행도 증가
    ↓
목표값 도달
    ↓
PlayerTitleCollectionRepository.unlockTitle()
    ↓
rpg_player_title_unlocks
    ↓
NPC 26에서 장착 가능
```

---

## 26. 도박중독 업적 칭호

0.4.9에서 실제 구현 및 테스트 완료된 업적 칭호.

```text
title_id        gambling_addiction
title_text      도박중독
rarity          희귀
unlock_type     paid_gambling_count
unlock_target   total
unlock_value    100
```

조건:

```text
경마장 또는 슬롯머신 실제 유료 이용 합계 100회
```

카운트 인정:

- 실제 골드 차감 성공
- DB 상태가 `DEBIT_CONFIRMED`까지 정상 전환
- 정상 유료 이용으로 확정

제외:

- 골드 부족
- 차감 실패
- 취소
- 무료 이용
- 환불 / 실패 처리

검증 완료:

```text
progress_value = 100
```

도달 즉시 `도박중독` 칭호가 획득되는 것을 실제 테스트 완료했다.

---

## 27. 경마 시스템 - NPC 22

경마 NPC:

```text
NPC 22
```

주요 클래스:

```text
HorseRaceNpcListener
HorseRaceBetService
HorseRaceRepository
```

베팅 흐름:

```text
PREPARED
    ↓
DEBIT_IN_FLIGHT
    ↓
Vault 골드 차감
    ↓
DEBIT_CONFIRMED
```

골드 차감 실패 시:

```text
CANCELLED
```

서버 종료 / 불명확 거래는 안전한 상태 전이 및 환불 검토 경로를 사용한다.

업적 카운트는 `DEBIT_CONFIRMED` 성공 직후 호출한다.

---

## 28. 슬롯머신 시스템 - NPC 23

슬롯머신 NPC:

```text
NPC 23
```

주요 클래스:

```text
SlotMachineNpcListener
SlotMachineBetService
SlotMachineRepository
```

기본 베팅:

```text
MIN_BET  = 100
MAX_BET  = 100,000,000
BET_STEP = 100
```

릴 심볼 예:

```text
BAKED_POTATO
APPLE
GOLDEN_APPLE
```

결과는 골드 차감 전에 DB에 준비한 후 안전하게 차감 상태를 전환한다.

흐름:

```text
PREPARED
    ↓
DEBIT_IN_FLIGHT
    ↓
Vault withdraw
    ↓
DEBIT_CONFIRMED
    ↓
릴 애니메이션
    ↓
정산
```

업적 카운트는 `DEBIT_CONFIRMED` 성공 직후 호출한다.

---

## 29. 경제 시스템

Vault 기반 경제 서비스를 사용한다.

대표 서비스:

```text
ShopEconomyService
```

사용처:

- NPC 상점
- 가구 구매
- 경마
- 슬롯머신
- 보유 골드 칭호
- 기타 결제형 콘텐츠

Vault 접근은 메인 스레드 제약을 고려하고, DB 기록은 비동기 처리하는 패턴을 사용한다.

---

## 30. 리소스팩 전체 구조

RPGCore는 최종 리소스팩의 소유자다.

소스:

```text
/srv/minecraft/resourcepacks/rpgcore
```

최종 ZIP:

```text
/srv/minecraft/resourcepacks/rpgcore-0.4.9.zip
```

현재 정상 운영 SHA1 기준:

```text
e5ed0ba1d9b8f2fc4233ce7956908fe9996b313a
```

배포 URL:

```text
http://46.250.248.12:8080/rpgcore-0.4.9.zip
```

Paper 26.2 resource pack format:

```text
88
```

---

## 31. 리소스팩 Overlay

Paper 26.2 대응 overlay:

```text
ia_overlay_26_2_plus
```

기준:

```json
{
  "min_format": 88,
  "max_format": 9999,
  "formats": {
    "min_inclusive": 88,
    "max_inclusive": 9999
  },
  "directory": "ia_overlay_26_2_plus"
}
```

---

## 32. 리소스팩 재배포 파이프라인

관리 스크립트:

```text
/srv/minecraft/redeploy-resourcepack.py
```

역할:

1. RPGCore source pack 확인
2. 기존 배포 ZIP 백업
3. BetterHud 리소스 병합
4. BetterModel / ModelEngine 리소스 병합
5. MythicArmors / 외부 생성 리소스 병합
6. ItemsAdder에서 명시적으로 가져온 리소스 병합
7. atlas collision 검사
8. 최종 ZIP 생성
9. SHA1 계산
10. `server.properties`의 resource-pack / SHA1 갱신

백업 위치:

```text
/srv/minecraft/backups/resourcepack-redeploy/<timestamp>
```

---

## 33. Atlas 관리

Paper 26.2 / ItemsAdder 병합 시 atlas가 중요하다.

FurniturePlus 적용 시 `assets/minecraft/atlases/items.json`에 IA sprite source를 추가하여 문제를 해결했다.

원칙:

- 기존 atlas source 보존
- 신규 sprite ID 충돌 방지
- ModelEngine merge 전후 atlas 보존 확인
- 대규모 JSON 삭제 금지
- 생성된 IA atlas를 무조건 최종 팩으로 덮어쓰지 않음

---

## 34. BetterHud

BetterHud는 HUD 표시와 리소스 출력에 사용한다.

주의:

- BetterHud font JSON을 임의로 대량 삭제하지 않는다.
- 기존 HUD asset 구조 유지
- RPGCore resource pack composer가 필요한 리소스를 최종 팩에 병합한다.

---

## 35. BetterModel / ModelEngine

커스텀 모델은 BetterModel / ModelEngine을 활용한다.

용도:

- 몬스터 모델
- 플레이어 장비 미리보기
- 일부 커스텀 엔티티
- 외형 표현

BBModel 파일은 직접 생성 / 외부 구입 자산이 혼재할 수 있으므로 GitHub 공개 시 라이선스 확인이 필요하다.

---

## 36. MythicMobs / MythicArmors

### MythicMobs

- 일부 커스텀 몬스터
- 보스
- 던전 몹
- 외형 / 스킬 연계

### MythicArmors

- 커스텀 방어구 외형
- 세트 장비 표현

RPGCore는 실제 RPG 능력치와 획득 / 구매 / 제작 규칙을 소유하고 외부 플러그인은 외형 및 몹 동작을 보조한다.

---

## 37. 주요 명령어

기본 명령:

```text
/rpgcore
/rpg
/stats
/class <warrior|mage>
/rpgclaim
```

관리자:

```text
/rpgadmin
```

기존 테스트 예:

```text
/rpgadmin level set <player> <level>
/rpgadmin exp set <player> <exp>
/rpgadmin exp add <player> <exp>
```

칭호 명령도 별도 제공된다.

---

## 38. 비동기 처리 원칙

DB 작업은 메인 서버 틱을 막지 않도록 비동기 처리한다.

대표 패턴:

```text
Main Thread
    ↓
게임 상태 / Bukkit API 확인
    ↓
runTaskAsynchronously
    ↓
MariaDB 작업
    ↓
runTask
    ↓
플레이어 메시지 / Bukkit 상태 반영
```

Vault 등 메인 스레드 접근이 필요한 API는 메인 스레드에서 호출한다.

---

## 39. 안전한 상태 전이 원칙

경마 / 슬롯과 같이 실제 골드가 움직이는 기능은 단순 `withdraw()` 후 처리하지 않고 DB 상태를 먼저 기록한다.

예:

```text
PREPARED
DEBIT_IN_FLIGHT
DEBIT_CONFIRMED
CANCELLED
REFUND_PENDING
```

목적:

- 서버 강제 종료 대응
- 중복 차감 방지
- 불명확 거래 추적
- 수동 검토 가능
- 보상 / 환불 신뢰성 향상

---

## 40. 운영 백업 정책

모든 백업:

```text
/srv/minecraft/backups
```

원칙:

- 소스 변경 전 백업
- 운영 JAR 교체 전 백업
- resource pack 배포 전 백업
- DB 구조 변경 전 SQL 확인
- `.bak` 파일을 소스 디렉토리에 만들지 않음

전체 `/srv/minecraft` 백업 시 제외 가능:

```text
/srv/minecraft/server/world
/srv/minecraft/backups
```

---

## 41. GitHub 관리 원칙

GitHub에는 개발 소스와 직접 작성한 문서 / 스크립트만 올린다.

포함 권장:

```text
plugins-source/RPGCore/src/
build.gradle.kts
settings.gradle.kts
gradle/
gradlew
gradlew.bat
README.md
docs/
직접 작성한 scripts
```

제외:

```text
server/
backups/
world/
logs/
crash-reports/
database.yml
secret.yml
.env
resourcepacks/source-models/
구매한 BBModel / Texture / Armor / Furniture 자산
생성된 JAR
생성된 ZIP
```

---

## 42. 0.4.9 완료 상태 요약

RPGCore 0.4.9에서 현재 완료 및 실제 동작 검증된 주요 영역:

- Paper 26.2 / Java 25 대응
- MariaDB 연결
- 플레이어 데이터 저장
- 레벨 / EXP
- 직업
- 전사 / 마법사 스킬
- 전투 / 스탯
- 장비 / 제작 / 분해
- 커스텀 아이템
- HUD
- 플레이어 이름표
- 다수 던전
- 던전 보상
- NPC 상점
- 가구 상점 NPC 25
- ItemsAdder provider
- Nieyels 가구
- FurniturePlus 139개
- 설치 / 착석 / 조명
- 칭호 관리 NPC 26
- 레벨 칭호
- 재벌 칭호
- 용살자 칭호
- 경마 NPC 22
- 슬롯머신 NPC 23
- 업적 진행도 시스템
- `도박중독` 업적 칭호
- resource pack composer
- Paper 26.2 atlas / overlay 대응
- 운영 백업 / 배포 절차

---

## 43. 향후 확장 방향

0.4.9 구조를 그대로 활용해 다음 기능을 확장할 수 있다.

### 업적

- 몬스터 누적 처치
- 던전 누적 클리어
- 보스 처치
- 장비 제작
- 장비 분해
- 강화 성공 / 실패
- 상점 구매
- 가구 구매
- 누적 골드 소비
- 플레이타임
- 고난도 조건부 보스 클리어

### 칭호

업적 진행도와 `rpg_title_definitions`의 다음 필드를 연결한다.

```text
unlock_type
unlock_target
unlock_value
```

현재 `AchievementTitleUnlockService`를 일반화하면 다수 업적을 하나의 구조로 관리할 수 있다.

---

## 44. 설계 핵심 원칙

RPGCore 0.4.9에서 유지해야 할 핵심 원칙은 다음과 같다.

1. **RPGCore가 게임 규칙의 최종 소유자다.**
2. **외부 플러그인은 외형 / 표시 / 보조 기능으로 제한한다.**
3. **DB 작업은 가능한 한 비동기로 처리한다.**
4. **Vault 거래는 안전한 상태 전이를 사용한다.**
5. **UUID를 내부 식별자로 유지한다.**
6. **display_name은 표시용으로 분리한다.**
7. **리소스팩은 RPGCore composer가 최종 조합한다.**
8. **ItemsAdder가 최종 팩을 직접 적용하지 않는다.**
9. **기존 정상 리소스팩 구조를 임의로 정리하지 않는다.**
10. **모든 변경은 백업 → 수정 → 빌드 → 배포 → 로그 → 인게임 검증 순서로 진행한다.**
11. **백업은 `/srv/minecraft/backups`에만 생성한다.**
12. **외부 / 구매 자산은 GitHub에 무단 공개하지 않는다.**

---

## 45. 문서 기준

이 문서는 **RPGCore 0.4.9 개발 완료 시점**을 기준으로 작성되었다.

운영 경로:

```text
/srv/minecraft
```

현재 핵심 버전:

```text
RPGCore 0.4.9
Paper 26.2
Java 25
MariaDB
```

0.5.x 이후 구조 변경 시 이 문서를 복사하여 버전별 설계 문서로 유지하는 것을 권장한다.
