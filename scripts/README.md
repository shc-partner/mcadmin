# scripts

Minecraft 서버 운영 및 배포 보조 스크립트 문서이다.

주요 역할:

- Paper 다운로드/갱신 보조
- 서버 시작 보조
- RPGCore 리소스팩 조합
- 리소스팩 SHA1 계산
- `server.properties` 갱신
- 반복 운영 작업 자동화

## 원칙

- 운영 파일을 수정하는 스크립트는 실행 전 대상 파일 존재 여부를 검사한다.
- 예상 상태와 다르면 중단한다.
- 변경 전에 `/srv/minecraft/backups`에 백업한다.
- 운영 경로를 추측하지 않고 절대 경로를 명시한다.
- secret 값을 스크립트에 하드코딩하지 않는 방향을 우선한다.

리소스팩 관련 세부 구조는 `RESOURCEPACK_PIPELINE.md`를 참고한다.
