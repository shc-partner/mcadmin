# GitHub 관리 기준

## 포함 권장

```text
RPGCore/src/
RPGCore/build.gradle.kts
RPGCore/settings.gradle.kts
RPGCore/gradle.properties
RPGCore/gradle/
RPGCore/gradlew
RPGCore/gradlew.bat
scripts/
docs/
README.md
```

## 제외 권장

```text
server/
backups/
logs/
crash-reports/
.gradle/
build/
database.yml
secret.yml
.env
resourcepacks/source-models/
resourcepacks/*.zip
*.class
*.log
```

Gradle wrapper JAR은 예외적으로 저장소에 포함할 수 있습니다.

```text
RPGCore/gradle/wrapper/gradle-wrapper.jar
```

## 커밋 전 점검

```bash
git status
git diff
```

민감정보 점검:

```bash
grep -RIn   --exclude-dir=.git   -Ei 'password|api[_-]?key|access[_-]?token|secret|private[_-]?key|rcon'   .
```

운영 소스에서는 `git add .`보다 변경 파일을 확인한 뒤 필요한 파일만 추가하는 방식을 권장합니다.
