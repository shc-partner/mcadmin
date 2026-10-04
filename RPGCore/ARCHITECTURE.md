# RPGCore 아키텍처

## 1. 기본 계층

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

표시나 Bukkit 상태 변경이 필요하면 DB 작업 후 메인 스레드로 돌아옵니다.

## 2. Listener

Listener는 Paper 이벤트를 수신하고 최소한의 조건 확인 후 Service를 호출한다.

Listener에 SQL이나 긴 비즈니스 로직을 직접 넣지 않는다.

## 3. Service

Service는 기능의 핵심 규칙과 상태 전이를 담당한다.

주요 역할:

- 조건 판단
- 여러 Repository 조합
- 외부 API 호출 조정
- 성공/실패 결과 처리
- 후속 기능 연결

## 4. Repository

Repository는 DB 접근을 담당한다.

```text
SELECT
INSERT
UPDATE
DELETE
상태 전이
```

플레이어 메시지, GUI, 월드 변경은 Repository에서 처리하지 않는다.

## 5. Factory / Registry / State

Factory:
- ItemStack 등 Bukkit 객체 생성

Registry:
- 정의 등록 및 조회

State / DTO:
- 런타임 상태 및 데이터 전달

## 6. 의존성 주입

권장 방향:

```text
RPGCorePlugin
    ↓
Repository
    ↓
Service
    ↓
Listener / Command
```

필요한 의존성만 생성자로 전달한다.

하위 Service가 `RPGCorePlugin` 전체를 무조건 참조하는 구조는 피한다.

## 7. 비동기 처리

DB:

```text
runTaskAsynchronously
```

Bukkit 객체 변경:

```text
main thread
```

대표 흐름:

```text
Main Thread
→ 게임 상태 확인
→ Async DB 작업
→ Main Thread 복귀
→ 메시지 / Inventory / Entity 처리
```

## 8. 외부 플러그인 경계

```text
RPGCore         핵심 규칙 / 데이터
Vault           경제 API
Citizens        NPC 표현
PlaceholderAPI  값 노출
BetterHud       HUD 표현
BetterModel     모델 표현
ItemsAdder      리소스 생성 보조
Mythic*         몹 / 방어구 표현 및 보조
```

외부 플러그인이 RPGCore 핵심 DB의 원본이 되지 않도록 유지한다.

## 9. 안전한 거래 상태

경제 기능은 상태 전이를 DB에 기록한다.

```text
PREPARED
DEBIT_IN_FLIGHT
DEBIT_CONFIRMED
CANCELLED
REFUND_PENDING
```

목적:

- 비정상 종료 대응
- 중복 차감 방지
- 거래 추적
- 환불 판단
