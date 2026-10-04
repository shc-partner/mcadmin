# RPGCore 0.4.9

Minecraft Paper 26.2 기반 RPG 서버 프로젝트의 개발 문서 루트입니다.

## 기준 환경

- OS: Ubuntu 24.04
- Java: Temurin JDK 25
- Paper: 26.2 계열
- RPGCore: 0.4.9
- Database: MariaDB
- Project root: `/srv/minecraft`
- RPGCore source: `/srv/minecraft/plugins-source/RPGCore`

## 문서 구조

```text
/
├── README.md
├── docs/
├── RPGCore/
│   ├── README.md
│   ├── ARCHITECTURE.md
│   ├── DATABASE.md
│   ├── BUILD_DEPLOY.md
│   ├── MODULES.md
│   └── src/
│       └── main/
│           ├── resources/
│           │   └── README.md
│           └── java/
│               └── com/hcs/rpgcore/
│                   └── README.md
└── scripts/
    ├── README.md
    └── RESOURCEPACK_PIPELINE.md
```

## 개발 원칙

RPGCore가 핵심 게임 규칙과 영구 데이터를 소유하고, 외부 플러그인은 NPC, HUD, 모델, 경제 API, 리소스 생성 등 보조 계층으로 사용합니다.

운영 변경은 다음 순서를 기본으로 합니다.

```text
현재 상태 확인
→ 백업
→ 최소 수정
→ 빌드
→ 배포
→ 서버 로그 확인
→ 인게임 검증
```

모든 백업은 `/srv/minecraft/backups` 아래에 생성합니다.

## 시작 지점

- 전체 개발 구조: `docs/PROJECT_STRUCTURE.md`
- 운영/개발 원칙: `docs/DEVELOPMENT_GUIDE.md`
- RPGCore 내부 구조: `RPGCore/ARCHITECTURE.md`
- DB 구조: `RPGCore/DATABASE.md`
- 빌드/배포: `RPGCore/BUILD_DEPLOY.md`
- 패키지 역할: `RPGCore/MODULES.md`
- 리소스팩 파이프라인: `scripts/RESOURCEPACK_PIPELINE.md`
