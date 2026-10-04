# RPGCore 모듈 구조

게임 내부 수치나 콘텐츠 상세 설명 없이 Java 패키지의 개발 책임만 정리합니다.

```text
boss/          보스 관련 공통 처리
check/         수표/골드 교환 계열
classjob/      직업 선택 및 직업 상태
combat/        전투 판정 및 피해 계산
command/       명령어
craft/         장비 제작
database/      공통 DB 연결
dismantle/     장비 분해
dungeon/       던전 상태 / 진행 / 입장 / 보상 연계
elixir/        소비형 효과 계열
enchant/       인챈트 관련 기능
furniture/     가구 상점 및 구매 처리
horse/         경마 관련 거래/상태
hud/           HUD 데이터 및 갱신
item/          커스텀 아이템 정의 / 생성 / 강화
level/         레벨 / 경험치
listener/      공통 이벤트 보호/제어
locator/       위치 표시 계열
mana/          마나 상태 / 회복
market/        가격/시장 데이터
mob/           커스텀 몹 / 보스 동작
placeholder/   PlaceholderAPI 노출
player/        플레이어 저장 / 접속 / 이름표
runtime/       런타임 플레이어 상태
shop/          상점 / 경제
skill/         스킬 정의 / 실행 / 쿨다운 / 버프
slot/          슬롯머신 거래/상태
starter/       신규 플레이어 진입
stat/          RPG 스탯 계산 / 장비 스탯
storage/       서버 저장소 기능
title/         칭호 / 업적 진행도
```

## 신규 기능 추가 기준

필요에 따라 다음 구성을 사용합니다.

```text
<Feature>Listener.java
<Feature>Service.java
<Feature>Repository.java
<Feature>State.java
<Feature>Definition.java
```

모든 기능에 모든 파일이 필요한 것은 아닙니다.

- 이벤트만 필요: Listener
- 규칙 필요: Service
- DB 필요: Repository
- 상태 필요: State / DTO
- 등록 데이터 필요: Definition / Registry
