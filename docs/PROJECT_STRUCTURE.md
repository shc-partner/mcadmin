# 프로젝트 구조

## 1. 전체 서버 구조

운영 기준 루트는 다음과 같습니다.

```text
/srv/minecraft
```

주요 디렉토리:

```text
/srv/minecraft/
├── server/                     # Paper 운영 서버
├── plugins-source/
│   └── RPGCore/                # RPGCore 개발 소스
├── resourcepacks/
│   ├── rpgcore/                # 최종 리소스팩 소스
│   ├── source-models/          # 외부/원본 모델
│   └── rpgcore-0.4.9.zip       # 접속 시 배포되는 최종 팩
├── backups/                    # 모든 백업
├── scripts/                    # 운영 보조 스크립트
└── redeploy-resourcepack.py
```

## 2. 개발과 운영 분리

개발 소스:

```text
/srv/minecraft/plugins-source/RPGCore
```

운영 JAR:

```text
/srv/minecraft/server/plugins/RPGCore-0.4.9.jar
```

리소스팩 소스:

```text
/srv/minecraft/resourcepacks/rpgcore
```

최종 배포 리소스팩:

```text
/srv/minecraft/resourcepacks/rpgcore-0.4.9.zip
```

소스, 빌드 결과물, 운영 결과물을 같은 역할로 취급하지 않는다.

## 3. RPGCore Gradle 구조

```text
RPGCore/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/hcs/rpgcore/
│       └── resources/
├── gradle/
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
└── gradlew.bat
```

## 4. 문서 구조 원칙

루트 문서는 프로젝트 전체를 설명한다.

`RPGCore/` 문서는 Java 플러그인 개발 구조를 설명한다.

`scripts/` 문서는 운영 스크립트와 리소스팩 배포 파이프라인을 설명한다.

`src/main/resources/README.md`는 설정 파일 관리 기준을 설명한다.

`src/main/java/com/hcs/rpgcore/README.md`는 Java 패키지 내부의 역할 분리를 설명한다.
