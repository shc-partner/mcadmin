# RPGCore 빌드 및 배포

## 1. 빌드

```bash
cd /srv/minecraft/plugins-source/RPGCore
./gradlew clean build
```

결과:

```text
build/libs/RPGCore-0.4.9.jar
```

## 2. 운영 위치

```text
/srv/minecraft/server/plugins/RPGCore-0.4.9.jar
```

## 3. 배포 원칙

```text
1. 기존 운영 JAR 백업
2. 서버 중지
3. 새 JAR 배포
4. 필요 시 SHA256 비교
5. 서버 시작
6. startup log 확인
7. 변경 기능 인게임 검증
```

백업:

```text
/srv/minecraft/backups
```

## 4. 로그 확인

```bash
sudo journalctl -u minecraft --no-pager | tail -200
```

필터:

```bash
sudo journalctl -u minecraft --no-pager | grep -Ei 'RPGCore|ERROR|WARN|Exception'
```

최소 확인:

```text
RPGCore enable 성공
DB 연결 성공
Listener 등록 정상
Command 등록 정상
외부 hook 상태
SQLException 없음
ClassNotFoundException 없음
NoSuchMethodError 없음
```

## 5. 배포 완료 기준

`BUILD SUCCESSFUL`만으로 완료 처리하지 않는다.

다음까지 확인해야 한다.

```text
컴파일 성공
→ 서버 시작 성공
→ 관련 로그 정상
→ 변경 기능 실제 동작 확인
```
