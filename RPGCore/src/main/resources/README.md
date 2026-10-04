# src/main/resources

RPGCore 런타임 리소스와 설정 파일을 관리한다.

주요 파일:

```text
plugin.yml
config.yml
database.yml
```

## plugin.yml

Paper/Bukkit 플러그인 메타데이터와 명령/권한을 정의한다.

버전은 빌드 설정과 일치시킵니다.

## config.yml

RPGCore의 일반 설정을 관리한다.

운영 설정을 수정할 때는 실제 코드에서 읽는 키를 먼저 확인한다.

## database.yml

운영 DB 접속 정보가 포함되므로 GitHub에 올리지 않는다.

Git 저장소에는 다음 파일만 둔다.

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

## 관리 원칙

- secret 값 커밋 금지
- 사용되지 않는 설정을 추측으로 삭제하지 않음
- 설정 키 변경 시 Java 코드와 함께 수정
- 운영 파일 수정 전 백업
