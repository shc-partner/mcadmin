# 리소스팩 배포 파이프라인

## 1. 최종 소유자

최종 리소스팩 소스:

```text
/srv/minecraft/resourcepacks/rpgcore
```

접속자가 다운로드하는 최종 파일:

```text
/srv/minecraft/resourcepacks/rpgcore-0.4.9.zip
```

RPGCore가 최종 리소스팩의 소유자입니다.

## 2. 관리 스크립트

```text
/srv/minecraft/redeploy-resourcepack.py
```

## 3. 파이프라인

```text
RPGCore Source Pack
        +
BetterHud
        +
BetterModel / ModelEngine
        +
Mythic / Armor Resources
        +
검증된 ItemsAdder Output
        ↓
Merge
        ↓
Atlas / Overlay 검증
        ↓
ZIP 생성
        ↓
SHA1 계산
        ↓
server.properties 갱신
```

## 4. ItemsAdder

ItemsAdder는 최종 팩을 직접 배포하지 않습니다.

사용:

```text
/iazip --uncompressed --apply-to none
```

그 결과에서 필요한 리소스를 검증 후 RPGCore source pack으로 병합합니다.

금지:

```text
ItemsAdder auto apply
ItemsAdder auto hosting
일반 /iazip로 운영 팩 덮어쓰기
```

## 5. Paper 26.2

리소스팩 format 기준:

```text
88
```

Overlay 예:

```text
ia_overlay_26_2_plus
```

## 6. Atlas 관리

- 기존 atlas source 보존
- sprite ID 충돌 확인
- 외부 생성 atlas 전체 덮어쓰기 금지
- 대규모 JSON 정리 금지
- 최종 ZIP에서 실제 병합 결과 재검증

## 7. 배포 전 백업

리소스팩 변경 전 백업:

```text
/srv/minecraft/backups/resourcepack-redeploy/<timestamp>
```

최종 ZIP과 관련 설정을 백업한 뒤 재배포합니다.
