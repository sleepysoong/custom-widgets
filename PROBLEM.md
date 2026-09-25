# CustomWidgets 문제 분석 및 수정 가이드

> 기준: `main`의 `v1.0.3` (`d0b0e82`), 2026-09-24. 이 문서는 [`PROJECT.md`](PROJECT.md)의 문제 목록을 실제 수정 작업으로 옮길 수 있도록 작성했다. 아래 코드 블록 중 **현재 코드**는 저장소에서 발췌했고, **수정 예시**는 구현 방향을 설명한다. 수정 예시는 그대로 붙여 넣기 전에 현재 패키지·의존성·스키마와 맞춰야 한다.

## 먼저 읽기

이 앱의 기본 흐름은 `AI 응답 → WidgetDefinition JSON → Room 저장 → appWidgetId 연결 → Glance 렌더링`이다. 동적 데이터가 필요한 위젯은 여기에 `토큰 추출 → HTTP/MCP 조회 → 결과 저장 → Glance 갱신`이 추가되어야 한다. 현재는 두 번째 흐름이 중간에서 끊긴다.

| ID | 우선순위 | 문제 | 먼저 수정해야 하는 이유 |
| --- | --- | --- | --- |
| P01 | 최우선 | Room 1→2 마이그레이션 부재 | 기존 사용자의 위젯 데이터가 삭제될 수 있음 |
| P02 | 최우선 | 배포 서명 키가 Git에 포함됨 | 릴리스 APK의 서명 신뢰와 직결됨 |
| P03 | 높음 | API 키가 평문으로 저장될 수 있음 | 로컬 저장·백업에 비밀 정보가 남음 |
| P04 | 높음 | HTTP/MCP 값이 위젯에 전달되지 않음 | 광고하는 동적 데이터 기능이 동작하지 않음 |
| P05 | 높음 | 토큰 파서와 배터리 값이 잘못됨 | 데이터 연결 후에도 잘못된 값이 보임 |
| P06 | 높음 | Worker 예약·수동 새로고침 미완성 | 데이터와 시간 표시를 갱신할 경로가 없음 |
| P07 | 중간 | MCP 서버 재활성화·프로토콜 처리 | 서버를 다시 켤 수 없고 일부 서버 응답을 읽지 못함 |
| P08 | 중간 | DSL·미리 보기·실제 위젯 불일치 | 사용자가 본 결과와 홈 화면 결과가 달라짐 |
| P09 | 중간 | 갤러리 삭제 문구와 실제 동작 불일치 | 홈 화면 위젯이 남는데 삭제됐다고 안내함 |
| P10 | 중간 | 릴리스 전 자동 검증 부족 | `main` push가 곧 서명 APK 릴리스로 이어짐 |

**작업 의존성:** P01을 먼저 처리한다. 이후 P04와 P05의 데이터 형식을 정하고 P06의 갱신 경로를 연결한다. P07의 MCP 호출은 P04가 정한 결과 저장 방식에 연결한다. P03에서 Room 스키마를 바꾸면 P01의 마이그레이션 경로에 다음 버전 마이그레이션을 추가해야 한다. P02의 서명 키 변경 여부는 이미 설치된 APK의 업데이트 경로를 확인한 후 결정한다.

## P01. Room 업그레이드 시 사용자 데이터 삭제 가능

### 현상과 근거

이전 릴리스 `v1.0.2`의 [`WidgetDatabase.kt`](app/src/main/kotlin/com/customwidgets/app/data/local/WidgetDatabase.kt)는 스키마 버전 1이었다. 현재 버전 2에는 `mcp_servers` 테이블이 추가됐다. 과거 파일은 `git show v1.0.2:app/src/main/kotlin/com/customwidgets/app/data/local/WidgetDatabase.kt`로 확인할 수 있다.

현재 코드:

```kotlin
// app/src/main/kotlin/com/customwidgets/app/data/local/WidgetDatabase.kt
@Database(
    entities = [WidgetEntity::class, AppWidgetIdEntity::class, McpServerEntity::class],
    version = 2,
    exportSchema = false
)
abstract class WidgetDatabase : RoomDatabase()

// app/src/main/kotlin/com/customwidgets/app/di/DatabaseModule.kt
Room.databaseBuilder(context, WidgetDatabase::class.java, WidgetDatabase.DATABASE_NAME)
    .fallbackToDestructiveMigration()
    .build()
```

`fallbackToDestructiveMigration()`은 해당 버전의 마이그레이션 경로가 없을 때 DB를 새로 만든다. 이 DB에는 [`widgets`](app/src/main/kotlin/com/customwidgets/app/data/local/entity/WidgetEntity.kt), [`widget_instances`](app/src/main/kotlin/com/customwidgets/app/data/local/entity/AppWidgetIdEntity.kt), [`mcp_servers`](app/src/main/kotlin/com/customwidgets/app/data/local/entity/McpServerEntity.kt)가 함께 있으므로 기존 위젯 정의와 런처 연결이 영향을 받는다. 이미 재생성된 DB의 삭제된 데이터는 새 마이그레이션만으로 복원되지 않는다.

DB 생성 방식도 일치하지 않는다. 다음 파일은 DI의 DB 대신 자체 `Room.databaseBuilder(...).build()`를 사용한다.

- [`CustomGlanceWidget.kt`](app/src/main/kotlin/com/customwidgets/app/widget/CustomGlanceWidget.kt#L45-L55): 홈 화면 렌더링
- [`CustomWidgetProvider.kt`](app/src/main/kotlin/com/customwidgets/app/widget/CustomWidgetProvider.kt#L16-L30): 런처 위젯 삭제 알림
- [`WidgetManager.kt`](app/src/main/kotlin/com/customwidgets/app/widget/WidgetManager.kt#L61-L82): 오래된 ID 정리
- [`WidgetUpdateWorker.kt`](app/src/main/kotlin/com/customwidgets/app/widget/update/WidgetUpdateWorker.kt#L27-L41): 백그라운드 갱신

이 경로들은 버전 1 DB를 먼저 열면 마이그레이션 오류를 낼 수 있고, 현재 코드는 예외를 조용히 삼키거나 위젯을 `Not Configured`로 표시한다.

### 수정 순서

1. **버전 1→2 수동 마이그레이션을 추가한다.** 버전 1의 두 테이블은 그대로 두고 `mcp_servers`만 생성한다. 현재 Room 버전은 `2.6.1`이므로 `Migration.migrate(SupportSQLiteDatabase)` 형식을 사용한다. 아래 SQL은 [`McpServerEntity.kt`](app/src/main/kotlin/com/customwidgets/app/data/local/entity/McpServerEntity.kt)의 필드와 대조하고 Room 스키마 검증으로 확정한다.

   ```kotlin
   // 수정 예시: WidgetMigrations.kt
   val MIGRATION_1_2 = object : Migration(1, 2) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("""
               CREATE TABLE IF NOT EXISTS `mcp_servers` (
                   `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                   `name` TEXT NOT NULL,
                   `url` TEXT NOT NULL,
                   `isEnabled` INTEGER NOT NULL,
                   `apiKey` TEXT
               )
           """.trimIndent())
       }
   }
   ```

2. **모든 DB 생성 경로가 같은 마이그레이션을 사용하게 한다.** 적어도 `DatabaseModule`의 builder에 `.addMigrations(MIGRATION_1_2)`를 추가하고 파괴적 fallback을 제거한다. Glance·Provider·Manager·Worker의 직접 생성도 동일한 DB 제공 경로로 옮긴다. 앱 프로세스에서 하나의 DB 인스턴스를 공유하거나, Hilt 진입점을 통해 DI 인스턴스에 접근하는 방식 중 하나를 선택한다. 반복해서 DB를 열고 닫는 방식은 피한다.

3. **스키마 이력을 보존한다.** `exportSchema = true`와 KSP의 `room.schemaLocation`을 설정하고 생성된 스키마를 Git에 보관한다. 버전 1 스키마는 `v1.0.2` 소스에서 생성하거나 실제 버전 1 DB 파일로 테스트한다. 현재 버전 2 빌드만으로 과거 스키마가 자동 생성되지는 않는다.

4. **업그레이드 테스트를 작성한다.** 버전 1 DB에 위젯 정의 한 건과 `appWidgetId` 연결 한 건을 넣고, 버전 2로 연 뒤 두 행이 남아 있는지 확인한다. `mcp_servers` 테이블 생성과 Room 스키마 검증도 확인한다. 실제 설치된 `v1.0.2` APK에서 업데이트하는 기기 테스트가 가장 직접적인 확인이다.

### 완료 기준

- 기존 앱의 위젯 목록, 정의 JSON, `appWidgetId` 연결이 업데이트 후 그대로 남는다.
- 홈 화면 위젯을 먼저 갱신하거나 앱을 먼저 열어도 같은 결과가 나온다.
- 새 스키마 버전을 올릴 때마다 대응하는 마이그레이션과 데이터 보존 검증을 추가한다.

## P02. 배포 서명 키가 Git에 포함됨

### 현상과 근거

[`app/release.keystore`](app/release.keystore)가 Git 추적 대상이다. [`app/build.gradle.kts`](app/build.gradle.kts#L25-L58)에는 서명 파일 경로와 비밀번호가 리터럴로 있고 debug도 release 서명 설정을 쓴다. 비밀번호 값은 이 문서에 다시 기록하지 않는다.

현재 코드의 구조:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("release.keystore")
        storePassword = "<현재 저장소의 값은 문서에서 생략>"
        keyPassword = "<현재 저장소의 값은 문서에서 생략>"
    }
}
buildTypes {
    release { signingConfig = signingConfigs.getByName("release") }
    debug { signingConfig = signingConfigs.getByName("release") }
}
```

[`build-and-release.yml`](.github/workflows/build-and-release.yml#L57-L86)은 `main` push마다 이 키로 서명한 APK를 GitHub Release에 게시한다. 비밀번호만 새 값으로 바꾸거나 추적을 해제해도 이전 Git 이력에 들어 있는 키와 비밀번호는 사라지지 않는다.

### 수정 순서

1. **배포 경로를 먼저 확인한다.** GitHub Release APK를 사용자가 직접 설치·업데이트하는지, Play App Signing도 사용하는지 기록한다. 이미 배포된 APK의 서명 키를 임의로 바꾸면 기존 설치본의 업데이트가 영향을 받는다.
2. **기존 키의 취급 방안을 결정한다.** 키를 가진 사람·저장소 공개 범위·기존 릴리스 사용자를 확인한다. 유출된 키로 판단한다면 새 키 전환과 기존 설치본 업데이트 방안을 함께 계획한다. 단순 `git rm --cached`는 과거 노출을 해결하지 않는다.
3. **소스의 자격 증명을 제거한다.** keystore와 로컬 서명 설정 파일을 `.gitignore`에 추가한다. release 빌드의 키 파일 경로, store 비밀번호, alias, key 비밀번호는 보호된 CI secret 또는 로컬 비공개 설정에서 받는다. secret이 없는 로컬 debug 빌드는 가능하게 하고, release 빌드는 값이 없으면 명확히 실패하게 한다.
4. **debug에는 별도 debug 서명을 사용한다.** [`app/build.gradle.kts`](app/build.gradle.kts#L56-L58)의 debug `signingConfig` 지정을 제거하거나 별도 debug 설정을 사용한다.
5. **CI가 secret에서 키를 복원하도록 수정한다.** release 작업 전에 보호된 secret을 임시 파일로 복원하고 Gradle에 환경 변수를 전달한다. 로그·아티팩트에 keystore와 비밀번호가 남지 않는지 확인한다.

   ```yaml
   # 수정 방향 예시: .github/workflows/build-and-release.yml
   - name: Prepare release signing
     env:
       KEYSTORE_BASE64: ${{ secrets.CW_RELEASE_KEYSTORE_BASE64 }}
     run: |
       printf '%s' "$KEYSTORE_BASE64" | base64 --decode > "$RUNNER_TEMP/release.keystore"
       echo "CW_RELEASE_STORE_FILE=$RUNNER_TEMP/release.keystore" >> "$GITHUB_ENV"
   ```

   store 비밀번호·key 비밀번호·alias도 별도 secret으로 전달한다. 예시는 secret 이름과 파일 복원 방식만 보여 주며, Gradle의 release 전용 분기까지 함께 구현해야 한다.

### 완료 기준

- 현재 Git 트리에 키 파일과 비밀번호가 없고, 새 release 빌드는 보호된 CI 설정으로만 서명된다.
- debug와 release APK의 서명 키가 분리된다.
- 기존 설치본을 새 버전으로 업데이트하는 실제 시나리오가 검증되어 있다.
- 키 이력 제거가 필요하면 별도 이력 재작성 계획을 세우고 협업자에게 재클론·동기화 절차를 알린다.

## P03. API 키가 평문으로 저장될 수 있음

### 현상과 근거

[`AiConfigStore.kt`](app/src/main/kotlin/com/customwidgets/app/ai/AiConfigStore.kt#L27-L61)는 암호화 저장소를 만들지 못하면 평문 SharedPreferences로 전환한다.

```kotlin
try {
    EncryptedSharedPreferences.create(/* ... */)
} catch (_: Exception) {
    context.getSharedPreferences("${PREFS_NAME}_plain", Context.MODE_PRIVATE)
}
```

[`McpServerEntity.kt`](app/src/main/kotlin/com/customwidgets/app/data/local/entity/McpServerEntity.kt#L7-L14)는 `apiKey: String?`을 일반 Room 컬럼에 저장한다. [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml#L7-L14)은 `android:allowBackup="true"`이며 키 파일의 백업 제외 규칙이 없다.

### 수정 순서

1. AI 키 암호화 초기화 실패를 상태나 오류로 전달한다. 저장 화면이 성공을 표시하면서 평문 파일에 기록하는 경로를 없앤다. 실패 원인을 로그에 남기더라도 키 값은 기록하지 않는다. 기존 설치본에 `openai_secure_prefs_plain.xml`이 있다면 암호화 저장소로 값을 옮기고 읽기 확인 후 평문 파일을 삭제하는 경로도 필요하다.
2. MCP 키를 암호화 저장소로 옮긴다. 예를 들어 `McpSecretStore`를 만들어 서버 ID로 키를 저장·읽기·삭제하고, `McpServerRepository`가 서버 CRUD와 함께 호출하게 한다. Room 행과 비밀 저장의 일부만 성공했을 때 되돌리는 처리도 필요하다.

   ```kotlin
   // 수정 방향 예시: 역할을 분리한 인터페이스
   interface McpSecretStore {
       fun save(serverId: Long, apiKey: String)
       fun load(serverId: Long): String?
       fun delete(serverId: Long)
   }
   ```

3. 이미 Room에 저장된 평문 MCP 키를 새 저장소로 옮기는 일회성 데이터 이전을 작성한다. 이전 완료 확인 후 평문 컬럼을 정리한다. DB 구조가 바뀌면 새 버전 마이그레이션을 추가하고 P01의 업그레이드 경로와 함께 검증한다.
4. 백업 정책을 정한다. 위젯 정의 같은 사용자 데이터는 보존하면서 비밀 저장 파일을 cloud backup과 기기 간 전송에서 제외할 수 있다. 이 앱의 최소 SDK가 31이므로 `android:dataExtractionRules`를 우선 검토한다. 저장소 복원 후 키가 없는 경우에는 재입력을 안내한다.

   ```xml
   <!-- 수정 방향 예시: res/xml/data_extraction_rules.xml.
        실제 보안 저장 파일 이름을 확인한 뒤 두 영역에 같은 제외 규칙을 넣는다. -->
   <data-extraction-rules>
       <cloud-backup>
           <exclude domain="sharedpref" path="openai_secure_prefs.xml" />
           <exclude domain="sharedpref" path="openai_secure_prefs_plain.xml" />
       </cloud-backup>
       <device-transfer>
           <exclude domain="sharedpref" path="openai_secure_prefs.xml" />
           <exclude domain="sharedpref" path="openai_secure_prefs_plain.xml" />
       </device-transfer>
   </data-extraction-rules>
   ```

   Manifest의 `<application>`에 `android:dataExtractionRules="@xml/data_extraction_rules"`를 연결한다. 위 예시는 현재 AI 저장 파일만 보여 준다. MCP 키를 별도 파일로 옮기면 해당 파일도 제외한다.

### 완료 기준

- 암호화 저장소를 사용할 수 없는 환경에서 키가 평문으로 저장되지 않는다.
- MCP 서버를 추가·수정·삭제해도 DB와 비밀 저장소에 고아 키가 남지 않는다.
- 과거 버전의 키 이전, 백업·복원, 앱 재시작 후 읽기 동작을 확인한다.

## P04. HTTP/MCP 데이터 토큰이 실제 값으로 바뀌지 않음

### 현상과 근거

[`DataBindingResolver.kt`](app/src/main/kotlin/com/customwidgets/app/widget/renderer/DataBindingResolver.kt#L18-L34)는 자체 메모리 캐시를 가지며, 값이 없으면 `Loading...`을 반환한다.

```kotlin
private val httpCache = ConcurrentHashMap<String, String>()
private val mcpCache = ConcurrentHashMap<String, String>()
// ...
httpCache[cacheKey] ?: "Loading..."
mcpCache[cacheKey] ?: "Loading..."
```

[`CustomGlanceWidget.kt`](app/src/main/kotlin/com/customwidgets/app/widget/CustomGlanceWidget.kt#L60-L69)는 렌더링 때마다 `DataBindingResolver()`를 새로 만든다. `updateHttpCache()`와 `updateMcpCache()`의 호출 지점은 없다. [`HttpDataSource.fetchAndExtract()`](app/src/main/kotlin/com/customwidgets/app/widget/update/HttpDataSource.kt#L26-L59)와 [`McpClient.callTool()`](app/src/main/kotlin/com/customwidgets/app/mcp/McpClient.kt#L71-L96)도 실제 위젯 데이터 갱신 경로에서 호출되지 않는다. Worker에는 두 데이터 소스가 주입되지만 사용하지 않는다.

### 목표 데이터 흐름

```text
저장된 WidgetDefinition
  → text 노드에서 데이터 토큰 수집·중복 제거
  → HTTP/MCP 호출 및 결과 경로 추출
  → widgetEntityId + 토큰별 결과를 영속 저장
  → 해당 appWidgetId만 Glance 갱신
  → 렌더러가 저장된 결과로 토큰 치환
```

### 수정 순서

1. **토큰을 구조화한다.** 텍스트에서 원본 토큰, 데이터 소스 종류, URL/서버·도구, 인수, 결과 경로를 추출하는 파서를 별도 파일로 만든다. DSL 트리의 `Column`·`Row`·`Box`·`Clickable.child`를 모두 순회해야 한다.
2. **결과 저장 위치를 정한다.** Worker의 메모리 맵은 프로세스 재시작 후 사라진다. Room에 `widgetEntityId + 원본 토큰`을 키로 `value`, `fetchedAt`, `status`를 저장하는 방법이 단순하다. 새 테이블을 만들면 DB 버전을 올리고 P01처럼 명시적 마이그레이션을 작성한다. 동일 정의를 여러 홈 화면 인스턴스가 사용하면 값은 공유할 수 있다.

   ```kotlin
   // 수정 방향 예시: 새 Room 엔티티. 추가 시 DB 버전과 Migration(2, 3)도 수정한다.
   @Entity(primaryKeys = ["widgetEntityId", "token"], tableName = "widget_binding_values")
   data class WidgetBindingValueEntity(
       val widgetEntityId: Long,
       val token: String,         // 예: {{http:https://example.com/data:current.temp}}
       val value: String,
       val fetchedAt: Long
   )
   ```

   실제 구현에서는 `widgetEntityId` 외래 키와 삭제 시 정리 정책, 실패 상태 및 오래된 값 표시 정책도 추가한다. 위젯 JSON을 수정해 토큰이 사라진 경우 이전 토큰의 저장 행을 정리한다.
3. **Worker에서 실제 데이터를 조회한다.** HTTP 토큰은 `HttpDataSource.fetchAndExtract(url, path)`, MCP 토큰은 활성화된 서버 조회 후 `McpClient.callTool(server, tool, arguments)`을 사용한다. 호출 결과가 JSON이라면 지정한 경로의 값을 추출한다. 실패 시 마지막 성공 값을 유지할지, 오류 상태를 표시할지 정책을 정한다.
4. **렌더러에 값의 스냅샷을 전달한다.** [`CustomGlanceWidget.provideGlance()`](app/src/main/kotlin/com/customwidgets/app/widget/CustomGlanceWidget.kt#L42-L70)의 IO 구간에서 정의와 저장된 바인딩 값을 읽고, 읽은 값을 가진 resolver를 `provideContent`에 전달한다. 새 빈 캐시를 만들지 않는다. 렌더링 중 네트워크 호출은 하지 않는다.
5. **갱신 대상을 좁힌다.** 값이 바뀐 정의에 연결된 `appWidgetId`를 [`WidgetManager.updateWidgets()`](app/src/main/kotlin/com/customwidgets/app/widget/WidgetManager.kt#L30-L44)에 전달한다. 모든 위젯을 항상 갱신하는 현재 Worker 방식보다 변경 영향을 추적하기 쉽다.

### 완료 기준

- HTTP와 MCP 테스트 서버에서 가져온 값이 홈 화면 위젯에 표시된다.
- 앱 프로세스를 종료했다가 다시 시작해도 마지막 성공 값이 유지된다.
- 같은 토큰을 여러 텍스트에서 사용하면 한 번 조회한 값으로 모두 치환된다.
- 네트워크 실패·JSON 경로 누락·서버 비활성화가 무한 `Loading...` 상태로 숨겨지지 않는다.

## P05. 토큰 파서와 배터리 값이 잘못됨

### HTTP URL 파싱

현재 [`DataBindingResolver.kt`](app/src/main/kotlin/com/customwidgets/app/widget/renderer/DataBindingResolver.kt#L54-L71)의 정규식은 URL에 콜론이 없다고 가정한다.

```kotlin
val httpRegex = "\\{\\{http:([^:}]+):([^}]+)\\}\\}".toRegex()
val url = matchResult.groupValues[1]
val path = matchResult.groupValues[2]
```

프롬프트의 예시인 `{{http:https://example.com/data:current.temp}}`에서 첫 그룹은 `https`가 되고, 두 번째 그룹은 `//example.com/data:current.temp`가 된다. URL과 JSON 경로가 모두 잘못된다. 기존 토큰 형식을 유지한다면 **마지막 콜론**에서 URL과 경로를 나누는 파서가 최소 수정이다. 이때 경로에는 콜론을 허용하지 않는다고 문서화해야 한다. URL·인수에 구분자를 제한 없이 허용하려면 길이 접두어, 인코딩 또는 별도 JSON 필드가 있는 새 DSL 형식을 정의하고 기존 저장 위젯과의 호환 경로를 둔다.

```kotlin
// 수정 방향 예시: 기존 HTTP 문법만 임시로 지원한다.
fun parseHttpPayload(payload: String): Pair<String, String>? {
    val separator = payload.lastIndexOf(':')
    if (separator <= 0 || separator == payload.lastIndex) return null
    val url = payload.substring(0, separator)
    val path = payload.substring(separator + 1)
    if (!url.startsWith("https://")) return null
    return url to path
}
// payload = "https://example.com/data:current.temp"
```

이 함수는 토큰의 `{{http:`와 `}}`를 제거한 다음 호출한다. 기존 앱의 저장 JSON을 읽어야 하므로 파서 교체 때 과거 토큰 사례도 검사한다.

MCP 정규식도 `{{mcp:server:tool:path}}`처럼 인수가 생략된 3개 필드 형식에서 `path`를 세 번째 그룹(`args`)으로 읽고 실제 `path`는 빈 문자열로 둔다. [`WidgetPromptBuilder.kt`](app/src/main/kotlin/com/customwidgets/app/ai/prompt/WidgetPromptBuilder.kt#L70-L75)의 안내 문법과 파서가 같은 테스트 사례를 공유해야 한다.

### 배터리 값

현재 코드:

```kotlin
// DataBindingResolver.kt
private val batteryProvider: () -> Int = { 100 }
```

홈 화면 렌더러는 별도 공급자를 넘기지 않으므로 항상 `100%`가 표시된다. Android `BatteryManager.BATTERY_PROPERTY_CAPACITY` 등 실제 기기 값을 읽는 공급자를 렌더링 경로에 전달하고, 값 조회 실패 시 `100%`로 위장하지 않는 표현을 정한다. 시간·날짜·배터리 값은 **렌더링 순간의 스냅샷**이므로 P06의 갱신 경로도 필요하다.

```kotlin
// 수정 방향 예시: 기기 값 조회. 실제 UI에서는 null일 때 대체 문구를 사용한다.
val batteryManager = context.getSystemService(BatteryManager::class.java)
val batteryPercent: Int? = batteryManager
    ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    ?.takeIf { it in 0..100 }
```

### 완료 기준

- `https://` URL, 포트가 있는 URL, JSON 배열 경로, 빈 경로, 잘못된 토큰을 각각 검사한다.
- MCP 토큰의 인수 있음/없음 두 형식을 구분하고 프롬프트의 예제와 일치시킨다.
- 배터리 값이 기기 상태에 맞고 값 조회 실패가 명확히 표시된다.

## P06. 백그라운드 갱신과 새로고침 버튼이 연결되지 않음

### 현상과 근거

[`WidgetUpdateScheduler.kt`](app/src/main/kotlin/com/customwidgets/app/widget/update/WidgetUpdateScheduler.kt#L20-L57)는 `schedulePeriodicUpdates()`와 `triggerImmediateUpdate()`를 정의하지만 호출 지점이 없다. [`WidgetUpdateWorker.kt`](app/src/main/kotlin/com/customwidgets/app/widget/update/WidgetUpdateWorker.kt#L18-L54)는 데이터 조회 없이 모든 위젯을 다시 그린다. [`DslRenderer.kt`](app/src/main/kotlin/com/customwidgets/app/widget/renderer/DslRenderer.kt#L304-L311)의 새로고침 콜백은 비어 있다.

```kotlin
class RefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Trigger widget refresh
    }
}
```

[`CustomWidgetsApp.kt`](app/src/main/kotlin/com/customwidgets/app/CustomWidgetsApp.kt)는 `Application`만 구현한다. `@HiltWorker`의 의존성을 WorkManager가 생성할 수 있도록 `HiltWorkerFactory` 설정이 필요하다. 또한 WorkManager 기본 initializer를 Manifest에서 제거해야 앱의 사용자 정의 설정이 적용된다.

### 수정 순서

1. `CustomWidgetsApp`에 `Configuration.Provider`를 구현하고 `HiltWorkerFactory`를 WorkManager에 전달한다. 현재 WorkManager/Hilt 버전의 Kotlin 시그니처를 확인한다.

   ```kotlin
   // 수정 방향 예시: CustomWidgetsApp.kt
   @HiltAndroidApp
   class CustomWidgetsApp : Application(), Configuration.Provider {
       @Inject lateinit var workerFactory: HiltWorkerFactory

       override fun getWorkManagerConfiguration(): Configuration =
           Configuration.Builder()
               .setWorkerFactory(workerFactory)
               .build()
   }
   ```

2. `AndroidManifest.xml`에 `tools` 네임스페이스를 추가하고 WorkManager 기본 `androidx.work.WorkManagerInitializer` 메타데이터를 제거한다. 다른 App Startup initializer까지 지우지 않도록 병합된 Manifest를 확인한다.

   ```xml
   <!-- 수정 방향 예시: AndroidManifest.xml의 <application> 내부 -->
   <provider
       android:name="androidx.startup.InitializationProvider"
       android:authorities="${applicationId}.androidx-startup"
       android:exported="false"
       tools:node="merge">
       <meta-data
           android:name="androidx.work.WorkManagerInitializer"
           tools:node="remove" />
   </provider>
   ```

   최상위 `<manifest>`에 `xmlns:tools="http://schemas.android.com/tools"`가 있어야 한다.
3. 첫 홈 화면 인스턴스를 연결할 때 주기 작업을 예약하고, 마지막 인스턴스를 제거하면 필요 없는 작업을 취소한다. WorkManager 작업은 재부팅 후에도 유지되는지 확인한다. 현재 [`custom_widget_info.xml`](app/src/main/res/xml/custom_widget_info.xml#L14)의 플랫폼 갱신 주기(30분)와 중복되는 갱신을 설계한다.
4. `updateIntervalMinutes`를 실제 조회 주기 판단에 사용한다. WorkManager 주기 작업은 OS가 실행 시점을 조정하므로 분 단위의 정확한 실행을 약속하지 않는다. 하나의 전역 주기 작업으로 모든 위젯을 확인하고, 각 위젯의 마지막 조회 시각과 원하는 간격을 비교하는 방식이 관리하기 쉽다.
5. 새로고침 콜백에서 `glanceId`를 `appWidgetId`로 바꿔 **해당 인스턴스**의 일회성 작업을 예약한다. 현재 일회성 작업 이름은 전역 하나이고 정책이 `REPLACE`라서 여러 위젯을 빠르게 누르면 앞선 요청이 취소될 수 있다. 인스턴스별 고유 작업 이름을 사용한다.
6. Worker가 P04의 데이터 조회·저장까지 마친 후 위젯을 갱신하도록 한다. 네트워크 없는 상황에서도 시간·날짜·배터리 표시의 로컬 갱신이 필요한지 따로 판단한다.

### 완료 기준

- 예약 작업이 실제로 생성되고 `@HiltWorker`가 의존성 주입을 받아 실행된다.
- 새로고침 버튼을 누르면 해당 위젯의 값이 갱신된다.
- 두 위젯을 연속으로 새로고침해도 서로의 작업을 취소하지 않는다.
- 장치 재시작, 네트워크 없음, 위젯 제거 후에도 예약 상태가 의도대로 유지된다.

## P07. MCP 서버를 다시 켤 수 없고 응답 형식 처리도 제한적임

### 서버 재활성화 버그

현재 [`McpServerRepository.kt`](app/src/main/kotlin/com/customwidgets/app/mcp/McpServerRepository.kt#L29-L37)는 **이미 켜진 서버만** 읽고 그 안에서 ID를 찾는다.

```kotlin
suspend fun toggleServer(id: Long, enabled: Boolean) {
    val all = mcpServerDao.getEnabledServers()
    all.firstOrNull { it.id == id }?.let {
        mcpServerDao.updateServer(it.copy(isEnabled = enabled))
    }
}
```

비활성 서버는 목록에 없으므로 `enabled = true`가 아무 일도 하지 않는다. [`McpServerDao.kt`](app/src/main/kotlin/com/customwidgets/app/data/local/dao/McpServerDao.kt)에 ID 기준 갱신 쿼리를 추가하면 조회·수정 사이의 불필요한 단계를 없앨 수 있다.

```kotlin
// 수정 예시: McpServerDao.kt
@Query("UPDATE mcp_servers SET isEnabled = :enabled WHERE id = :id")
suspend fun setEnabled(id: Long, enabled: Boolean): Int

// McpServerRepository.kt
suspend fun toggleServer(id: Long, enabled: Boolean) {
    require(mcpServerDao.setEnabled(id, enabled) == 1) { "Server not found: $id" }
}
```

### MCP 응답 처리 범위

[`McpClient.executeRpc()`](app/src/main/kotlin/com/customwidgets/app/mcp/McpClient.kt#L98-L120)은 `Accept: application/json, text/event-stream`을 보내면서 실제 응답 본문은 항상 JSON 하나로 파싱한다.

```kotlin
addHeader("Accept", "application/json, text/event-stream")
// ...
val responseBody = response.body?.string() ?: return null
return json.decodeFromString<JsonRpcResponse>(responseBody)
```

SSE 응답이면 이 파싱은 실패한다. 현재 `tools/list` 또는 `tools/call` 실패를 빈 목록·`null`로 바꾸므로 [`McpServerViewModel.kt`](app/src/main/kotlin/com/customwidgets/app/ui/mcp/McpServerViewModel.kt#L77-L99)의 화면은 연결 실패와 도구가 없는 서버를 구분하지 못한다. 지원할 MCP 프로토콜 버전·HTTP 전송 형식을 먼저 정하고, 해당 형식의 응답·오류·세션을 처리한다. MCP 버전별 수명 주기가 다르므로 특정 핸드셰이크를 모든 서버에 일괄 적용하지 않는다.

P04의 데이터 흐름을 붙일 때는 **활성화된 서버인지 확인 → 도구 인수 구성 → `callTool` 실행 → `isError` 확인 → 텍스트/JSON 결과 경로 추출 → 값 저장** 순서로 구현한다. 서버 이름을 토큰 식별자로 쓸 경우 이름 변경·중복 이름 처리 정책도 정한다.

### 완료 기준

- 서버를 켰다 끄고 다시 켜면 DB와 UI 상태가 모두 갱신된다.
- 연결 실패, 인증 실패, 정상 연결·도구 0개가 구분되어 표시된다.
- 지원한다고 명시한 서버의 JSON 응답과 SSE 응답을 각각 처리하거나, 지원하지 않는 형식은 명확한 오류로 안내한다.
- MCP 도구 결과가 P04의 저장·렌더링 경로에 실제로 전달된다.

## P08. 위젯 DSL, 미리 보기, 실제 렌더링이 다름

### 이미지

[`WidgetDsl.kt`](app/src/main/kotlin/com/customwidgets/app/domain/model/WidgetDsl.kt#L138-L145)는 `Image.url`, `resName`, `contentScale`을 받는다. 실제 [`DslRenderer.kt`](app/src/main/kotlin/com/customwidgets/app/widget/renderer/DslRenderer.kt#L142-L153)는 `resName`이 비어 있는지에 따라 두 Android 기본 아이콘 중 하나만 고른다. [`ComposeWidgetPreview.kt`](app/src/main/kotlin/com/customwidgets/app/ui/preview/ComposeWidgetPreview.kt#L167-L175)는 고정된 카메라 이모지를 보여 준다.

```kotlin
val imageProvider = if (!node.resName.isNullOrBlank()) {
    ImageProvider(android.R.drawable.ic_menu_info_details)
} else {
    ImageProvider(android.R.drawable.ic_menu_gallery)
}
```

리소스 이미지를 지원하려면 허용한 이름을 실제 `R.drawable` ID로 매핑한다. URL 이미지를 지원하려면 네트워크 다운로드·크기 제한·디스크 캐시·실패 시 대체 이미지 처리를 P04의 Worker와 연결하고, 저장된 bitmap을 Glance에 제공한다. 지원하지 않을 기능은 [`WidgetPromptBuilder.kt`](app/src/main/kotlin/com/customwidgets/app/ai/prompt/WidgetPromptBuilder.kt#L42-L63)의 스키마에서 제거해 AI가 생성하지 않도록 한다.

### 테두리와 갱신 간격

`DslModifier.border`는 [`ComposeWidgetPreview.kt`](app/src/main/kotlin/com/customwidgets/app/ui/preview/ComposeWidgetPreview.kt#L255-L258)에 적용되지만 [`DslRenderer.toGlanceModifier()`](app/src/main/kotlin/com/customwidgets/app/widget/renderer/DslRenderer.kt#L202-L244)에서는 읽지 않는다. Glance에서 같은 모양을 구현하려면 노드 래핑 등 지원 가능한 표현을 선택하고 preview도 같은 규칙을 사용한다. 구현이 어렵다면 DSL·프롬프트·preview에서 `border`를 제거한다.

`WidgetDefinition.updateIntervalMinutes`도 현재 어디에서도 갱신 계획에 사용되지 않는다. 이 필드는 P06의 주기 판단에 연결하고 15분 미만·음수·과도한 값의 유효성 검사를 추가한다.

### 완료 기준

- 같은 JSON을 preview와 홈 화면에 표시했을 때 이미지·테두리·배경·간격이 의도한 허용 범위 안에서 일치한다.
- 지원하지 않는 속성은 AI 프롬프트와 편집 설명에서 제외된다.
- 새 DSL 속성을 추가할 때 parser, preview, Glance renderer, 저장된 JSON 호환성을 함께 확인한다.

## P09. 갤러리의 삭제 안내와 실제 동작이 다름

[`WidgetGalleryScreen.kt`](app/src/main/kotlin/com/customwidgets/app/ui/gallery/WidgetGalleryScreen.kt#L133-L145)의 대화상자는 “홈 화면 및 목록에서 삭제”한다고 안내한다. 실제 호출은 [`WidgetRepository.deleteWidget()`](app/src/main/kotlin/com/customwidgets/app/data/repository/WidgetRepository.kt#L47-L49)의 DB 행 삭제뿐이다.

```kotlin
// WidgetGalleryScreen.kt
Text("\"${target.name}\" 위젯을 홈 화면 및 목록에서 삭제하시겠습니까?")

// WidgetRepository.kt
suspend fun deleteWidget(id: Long) {
    widgetDao.deleteWidgetById(id)
}
```

DB 외래 키는 `widget_instances` 연결 행을 삭제하지만 런처에 놓인 Android 위젯 자체를 없애는 호출은 없다. 다음 업데이트 때 [`CustomGlanceWidget`](app/src/main/kotlin/com/customwidgets/app/widget/CustomGlanceWidget.kt#L60-L69)이 정의를 찾지 못해 `NotConfiguredPlaceholder`를 표시할 수 있다.

### 수정 순서

1. 삭제 동작의 의미를 정한다. 가장 안전한 기본 동작은 **저장 정의 삭제 + 연결된 홈 화면 위젯을 명시적 빈 상태로 갱신 + 런처에서 직접 제거하도록 안내**이다. 실제로 제거하지 않으면서 “홈 화면에서 삭제”라고 약속하지 않는다.
2. 삭제 전에 `getWidgetById(id)?.appWidgetIds`를 보관한다. DB 삭제 후 해당 ID만 `WidgetManager.updateWidgets()`로 다시 그린다. DB 삭제가 실패했다면 홈 화면 표시도 바꾸지 않는다.

   ```kotlin
   // 수정 방향 예시: ViewModel이 호출하는 별도 use case의 핵심 순서
   val affectedIds = widgetRepository.getWidgetById(widgetId)?.appWidgetIds.orEmpty()
   widgetRepository.deleteWidget(widgetId)
   WidgetManager.updateWidgets(context, affectedIds)
   ```

   홈 화면 갱신 실패가 발생한 경우를 사용자에게 어떻게 알릴지 정하고, 다음 시스템 갱신 시에도 정의 없음 상태가 일관되게 표시되는지 확인한다.
3. 대화상자 문구를 실제 동작에 맞춘다. 사용자가 데이터 삭제와 홈 화면 위젯 제거를 혼동하지 않도록 한다.
4. 동일 저장 정의에 홈 화면 인스턴스가 여러 개 연결된 경우를 확인한다.

### 완료 기준

- 목록 삭제 직후 모든 연결된 홈 화면 위젯의 표시가 갱신된다.
- 확인 대화상자의 설명이 실제 동작과 일치한다.
- 위젯 정의가 없는 인스턴스에 재설정 방법 또는 제거 안내가 나타난다.

## P10. 릴리스 작업에 테스트 단계가 없음

[`build-and-release.yml`](.github/workflows/build-and-release.yml#L1-L86)은 `main`에 push될 때 버전을 올리고 `:app:assembleRelease`로 APK를 만든 뒤 tag와 GitHub Release를 게시한다. 테스트 작업은 없다. 문서만 바뀌는 push도 현재 트리거 조건에 해당한다. [`app/build.gradle.kts`](app/build.gradle.kts#L82-L85)는 lint 오류가 release 빌드를 중단하지 않도록 설정되어 있다.

### 수정 순서

1. 데이터 보존, 토큰 파서, MCP 재활성화, Worker 경로에 대한 테스트를 각 문제 수정과 함께 작성한다. 현재 [`app/src/test/`](app/src/test)에는 DSL·DAO·AI 등의 테스트가 있지만 P01/P04/P06/P07의 핵심 연결은 검증하지 않는다.
2. release APK 빌드와 tag 작성 **이전**에 `:app:testDebugUnitTest`를 실행한다. 마이그레이션 기기 테스트를 추가하면 별도 Android 테스트 job을 두고 release job의 선행 조건으로 연결한다.
3. lint를 릴리스 게이트로 사용할지 결정하고, 사용한다면 오류 무시 설정을 제거한다.
4. 문서 변경이 매번 버전 증가·공개 릴리스를 만들 필요가 없다면 `paths-ignore`, 수동 `workflow_dispatch`, 또는 태그 기반 릴리스 중 팀의 배포 방식에 맞는 트리거를 선택한다.

```yaml
# 수정 방향 예시: APK 빌드와 tag 생성보다 앞에 배치
- name: Unit tests
  run: ./gradlew :app:testDebugUnitTest --no-daemon
```

### 완료 기준

- 실패한 테스트가 있는 commit에서는 서명 APK와 새 tag가 게시되지 않는다.
- 릴리스 트리거와 버전 증가 규칙이 팀의 실제 배포 절차와 일치한다.

## 문제별 확인 시나리오

| 문제 | 가장 중요한 확인 시나리오 |
| --- | --- |
| P01 | v1 DB의 위젯 1개와 `appWidgetId`를 v2로 올린 뒤 보존 확인 |
| P02 | secret 없는 debug 빌드, secret 있는 release 빌드, 기존 설치본 업데이트 확인 |
| P03 | 암호화 초기화 실패, 기존 평문 키 이전, 백업·복원 후 키 상태 확인 |
| P04 | HTTP/MCP 조회 성공·실패, 앱 프로세스 종료 후 값 유지 |
| P05 | `https://` URL·인수 없는 MCP 토큰·기기 배터리 값 확인 |
| P06 | 버튼 한 번, 위젯 두 개 연속 클릭, 재부팅·오프라인·삭제 확인 |
| P07 | MCP 서버 off→on, JSON/SSE 응답, 인증 오류 구분 |
| P08 | 동일 JSON의 preview와 홈 화면 결과 비교 |
| P09 | 목록 삭제 후 연결된 모든 홈 화면 인스턴스 표시 확인 |
| P10 | 테스트 실패를 의도적으로 만든 CI 실행에서 릴리스 미게시 확인 |

## 참고 문서

- [Android Room 데이터베이스 마이그레이션](https://developer.android.com/training/data-storage/room/migrating-db-versions): 수동 마이그레이션, 스키마 이력, 업그레이드 테스트. 이 저장소의 Room 버전은 `2.6.1`이므로 예제 코드는 해당 버전 API로 맞춘다.
- [Android Hilt와 WorkManager](https://developer.android.com/training/dependency-injection/hilt-jetpack), [WorkManager 사용자 정의 초기화](https://developer.android.com/develop/background-work/background-tasks/persistent/configuration/custom-configuration): WorkerFactory와 기본 initializer 처리.
- [Android 앱 서명](https://developer.android.com/studio/publish/app-signing): 기존 설치본 업데이트와 서명 키 관리.
- [Android 백업 보안 권장사항](https://developer.android.com/privacy-and-security/risks/backup-best-practices): 민감한 저장 파일 제외 규칙.
- [MCP 프로토콜 문서](https://modelcontextprotocol.io/specification): 지원할 버전과 HTTP 전송 형식 확인.

## 문서 작성 시 검증 범위

현재 소스, `v1.0.2`와 `v1.0.3`의 DB 스키마 이력, Gradle 설정, Manifest, 배포 워크플로를 읽어 정리했다. 이 문서 작성 과정에서 앱 코드를 수정하거나 빌드·테스트를 실행하지 않았다.
