# RPGCore

Paper 26.2 기반 Minecraft RPG 서버의 핵심 커스텀 플러그인입니다.

## 역할

RPGCore는 다음 계층을 직접 소유합니다.

- 플레이어 영구 데이터
- RPG 상태 및 규칙
- 전투/스탯 계산
- 기능별 Repository / Service
- 명령 및 Paper 이벤트 처리
- 외부 플러그인 연동 제어

게임 콘텐츠의 세부 규칙보다는 개발 구조상 RPGCore가 최종 데이터 소유자라는 점이 중요합니다.

## 진입점

```text
com.hcs.rpgcore.RPGCorePlugin
```

주요 초기화 책임:

```text
설정 로드
→ DB 연결
→ Repository 생성
→ Service 생성
→ Listener / Command 등록
→ 외부 Plugin Hook
→ 반복 Task 시작
```

## 주요 문서

- `ARCHITECTURE.md`
- `DATABASE.md`
- `BUILD_DEPLOY.md`
- `MODULES.md`
