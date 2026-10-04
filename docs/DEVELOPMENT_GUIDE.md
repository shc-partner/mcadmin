# 개발 가이드

## 1. 변경 전 확인

실제 파일과 호출 구조를 먼저 확인한 뒤 수정한다.

추측으로 클래스, 패키지, DB 컬럼, 리소스팩 구조를 변경하지 않는다.

## 2. 최소 변경 원칙

운영 중인 0.4.9에서는 기능 추가나 버그 수정 시 가능한 한 기존 구조를 유지한다.

금지에 가까운 작업:

- 근거 없는 대규모 package rename
- 사용 여부를 확인하지 않은 파일 삭제
- 정상 리소스팩 JSON 일괄 삭제
- 외부 플러그인 생성물을 무조건 덮어쓰기
- 소스 폴더 안에 `.bak` 파일 장기 보관

## 3. 백업

모든 백업 위치:

```text
/srv/minecraft/backups
```

예:

```text
/srv/minecraft/backups/<작업명>-YYYYMMDD-HHMMSS/
```

## 4. 기본 개발 순서

```text
현재 상태 확인
→ 백업
→ 수정
→ ./gradlew clean build
→ build success 확인
→ 운영 JAR 교체
→ 서버 재시작
→ journal 확인
→ 인게임 검증
```

## 5. Git 관리

Git에는 개발 산출물만 포함한다.

제외 대상:

```text
server/
backups/
.gradle/
build/
database.yml
secret.yml
.env
*.log
resourcepacks/source-models/
resourcepacks/*.zip
구매/재배포 제한 자산
```

실제 DB 비밀번호와 운영 인증정보는 절대 커밋하지 않는다.
