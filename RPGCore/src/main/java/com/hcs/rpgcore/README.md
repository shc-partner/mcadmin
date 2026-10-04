# com.hcs.rpgcore

RPGCore 0.4.9의 Java 소스 루트 패키지입니다.

> 현재 실제 패키지 경로는 `com.hcs.rpgcore`입니다.  
> 문서 정리만을 위해 `com.shc.rpgcore`로 변경하지 않습니다.  
> `com.shc.rpgcore`로 바꾸려면 Java package rename과 import, plugin main class 등 전체 변경 작업이 필요합니다.

## 기본 책임 분리

```text
Listener    이벤트 수신
Command     명령 처리
Service     비즈니스 로직
Repository  DB 접근
Factory     객체 생성
Registry    정의 등록/조회
Task        스케줄 작업
State/DTO   상태 전달
```

## 권장 호출 방향

```text
Listener / Command
        ↓
Service
        ↓
Repository
```

## 개발 규칙

- Listener에 SQL을 넣지 않습니다.
- Repository에 플레이어 UI 로직을 넣지 않습니다.
- DB 작업은 가능한 한 비동기로 처리합니다.
- Bukkit 객체 변경은 메인 스레드에서 수행합니다.
- 기능 추가 전 기존 유사 모듈의 구조를 먼저 확인합니다.
- 임시 백업 파일을 이 디렉토리에 남기지 않습니다.
