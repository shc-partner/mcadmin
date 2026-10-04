# RPGCore 데이터베이스 구조

## 1. 기본

Database:

```text
MariaDB
```

RPGCore는 기능별 Repository를 통해 DB에 접근한다.

## 2. 식별 원칙

```text
player_uuid   영구 식별자
player_name   계정명
display_name  표시용 이름
```

내부 식별과 DB 관계는 UUID를 기준으로 유지한다.

## 3. Repository 규칙

- SQL은 Repository에 둔다.
- Listener에서 SQL을 직접 실행하지 않는다.
- PreparedStatement를 사용한다.
- PK / UNIQUE KEY를 활용해 중복을 방지한다.
- 반복 실행 가능성이 있는 INSERT는 idempotent하게 설계한다.
- Bukkit 객체는 DB 비동기 스레드에서 직접 변경하지 않는다.

## 4. 주요 데이터 영역

현재 DB는 다음 범주의 데이터를 저장한다.

```text
플레이어 기본 데이터
레벨 / 경험치
직업
아이템 / 장비 정의
상점 / 경제 기록
던전 보상
가구 상점
칭호
칭호 획득 / 장착
업적 진행도
경마 / 슬롯 거래
기타 복구 / pending 데이터
```

세부 테이블 SQL은 실제 운영 DB schema를 기준으로 관리한다.

## 5. 진행도 공통 구조

현재 구현된 누적 진행도 구조:

```text
rpg_player_achievement_progress
```

핵심 필드:

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

이 구조는 향후 공통 progression 엔진으로 일반화할 수 있다.

## 6. 스키마 변경

DB 수정 전 순서:

```text
현재 schema 확인
→ 필요한 변경만 SQL 작성
→ HeidiSQL에서 적용
→ 실제 row / index 확인
→ RPGCore 코드 적용
→ 테스트
```

이미 존재하는 컬럼을 추측으로 다시 추가하지 않는다.
