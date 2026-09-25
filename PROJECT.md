# CustomWidgets 프로젝트 분석

> 분석 기준: `main`의 `v1.0.3` (`d0b0e82`). 이 문서는 현재 코드의 동작과 확인된 개선 과제를 기록한다.

## 프로젝트 목적

CustomWidgets는 사용자가 자연어로 설명한 Android 홈 화면 위젯을 생성하는 앱이다. AI가 반환한 JSON을 앱의 위젯 DSL로 해석하고, 사용자가 미리 보기와 JSON 편집을 거쳐 저장하면 Jetpack Glance가 홈 화면에 표시한다.

| 항목 | 현재 구성 |
| --- | --- |
| 플랫폼 | Android, Kotlin, 단일 `:app` 모듈 |
| UI | Jetpack Compose, Material 3, 폴더블 화면 대응 |
| 홈 화면 위젯 | Jetpack Glance, AppWidget Provider |
| AI | OpenAI Chat Completions API, OkHttp, SSE 스트리밍 |
| 저장소 | Room, SharedPreferences |
| 백그라운드 작업 | WorkManager 코드 존재 |
| SDK | `minSdk 31`, `targetSdk 35`, `compileSdk 35` |

## 주요 사용자 흐름

1. `CreateWidgetScreen`에서 크기와 설명을 선택한다.
2. `WidgetGenerationService`가 프롬프트를 구성하고 `AiService`를 통해 JSON을 스트리밍으로 받는다.
3. `WidgetDefinition.fromJson()`이 응답을 파싱하고 DSL의 중첩 깊이와 노드 수를 검사한다.
4. 사용자가 미리 보거나 JSON을 수정한 뒤 `WidgetRepository`를 통해 Room에 저장한다.
5. 런처에서 추가한 위젯은 `WidgetConfigureActivity`가 Android `appWidgetId`와 저장된 정의를 연결한다.
6. `CustomGlanceWidget`이 정의를 읽고 `DslRenderer`로 렌더링한다.

## 코드 구조

| 영역 | 주요 코드 | 역할 |
| --- | --- | --- |
| 화면 | [`ui/`](app/src/main/kotlin/com/customwidgets/app/ui), [`MainActivity.kt`](app/src/main/kotlin/com/customwidgets/app/MainActivity.kt) | 생성, 갤러리, 상세, MCP 서버, API 설정 |
| DSL | [`WidgetDsl.kt`](app/src/main/kotlin/com/customwidgets/app/domain/model/WidgetDsl.kt) | 위젯 노드, 스타일, 액션, JSON 검증 |
| AI | [`ai/`](app/src/main/kotlin/com/customwidgets/app/ai) | 프롬프트, API 호출, 응답 파싱 |
| 저장 | [`data/`](app/src/main/kotlin/com/customwidgets/app/data) | Room 엔티티·DAO·Repository |
| 위젯 | [`widget/`](app/src/main/kotlin/com/customwidgets/app/widget) | Glance 렌더링, 인스턴스 갱신, 데이터 바인딩 |
| MCP | [`mcp/`](app/src/main/kotlin/com/customwidgets/app/mcp) | 서버 설정, 도구 목록 조회, 도구 호출 함수 |
| 배포 | [빌드 설정](app/build.gradle.kts), [GitHub Actions](.github/workflows/build-and-release.yml) | 버전 증가, 서명 APK 빌드, GitHub Release |

## 현재 구현 상태

| 기능 | 상태 | 코드에서 확인한 동작 |
| --- | --- | --- |
| AI 위젯 생성·JSON 편집 | 구현 | AI 응답을 DSL로 파싱하고 저장할 수 있다. |
| 갤러리·런처 위젯 연결 | 구현 | 정의와 `appWidgetId`를 Room에 저장한다. |
| 시간·날짜 토큰 | 부분 구현 | 렌더링 시 값을 계산하며 위젯 XML에 30분 갱신 주기가 설정되어 있다. |
| 배터리 토큰 | 미완성 | 기본 공급자가 항상 `100`을 반환한다. |
| HTTP·MCP 데이터 토큰 | 미완성 | 렌더러가 새 빈 캐시를 사용하며 데이터를 채우는 호출 경로가 없다. |
| MCP 서버 관리 | 부분 구현 | 서버 저장과 `tools/list` 조회는 있으나 위젯 데이터 조회에 도구 호출이 연결되지 않았다. |
| 수동 새로고침 | 미완성 | `RefreshCallback`이 비어 있다. |
| 이미지 노드 | 부분 구현 | 이미지 URL과 리소스 이름 대신 Android 기본 아이콘을 표시한다. |

## 확인된 문제와 우선순위

### P0 — 기존 사용자 데이터 보존

현재 Room 스키마는 버전 2이며, 이전 코드의 스키마는 버전 1이었다. 명시적인 `Migration(1, 2)` 없이 [`fallbackToDestructiveMigration()`](app/src/main/kotlin/com/customwidgets/app/di/DatabaseModule.kt)을 사용하므로, 이전 버전에서 업데이트한 사용자의 위젯 정의와 인스턴스 연결이 삭제될 수 있다. 일부 위젯 코드가 DB를 직접 생성할 때는 같은 fallback 설정도 사용하지 않는다. [스키마 정의](app/src/main/kotlin/com/customwidgets/app/data/local/WidgetDatabase.kt)와 모든 DB 생성 경로를 함께 정리해야 한다.

### P0 — 배포 서명 키 관리

[`app/release.keystore`](app/release.keystore)가 Git에 추적되고, [빌드 설정](app/build.gradle.kts)에 서명 비밀번호가 들어 있다. debug와 release가 같은 키를 사용한다. [배포 워크플로](.github/workflows/build-and-release.yml)는 `main`에 push될 때 이 키로 APK를 서명하고 GitHub Release에 게시한다. 키와 비밀번호를 저장소에서 분리하고, 이미 배포한 APK의 업데이트 호환성을 고려해 키 대응 방안을 정해야 한다.

### P1 — 실제 데이터 바인딩 연결

[`DataBindingResolver`](app/src/main/kotlin/com/customwidgets/app/widget/renderer/DataBindingResolver.kt)의 HTTP·MCP 캐시는 값을 주입받을 수 있지만 호출자가 없다. [`CustomGlanceWidget`](app/src/main/kotlin/com/customwidgets/app/widget/CustomGlanceWidget.kt)은 렌더링할 때마다 새 resolver를 만든다. 따라서 해당 토큰은 `Loading...`으로 표시된다. HTTP 토큰 정규식은 `https:`의 콜론을 구분자로 읽어 URL도 잘못 분리한다. [`WidgetUpdateWorker`](app/src/main/kotlin/com/customwidgets/app/widget/update/WidgetUpdateWorker.kt)는 데이터 소스를 주입받지만 실제 조회에 사용하지 않는다.

### P1 — 갱신 경로 완성

[`WidgetUpdateScheduler`](app/src/main/kotlin/com/customwidgets/app/widget/update/WidgetUpdateScheduler.kt)의 예약 함수는 호출되지 않으며 DSL의 `updateIntervalMinutes`도 사용되지 않는다. [`RefreshCallback`](app/src/main/kotlin/com/customwidgets/app/widget/renderer/DslRenderer.kt)은 동작이 없다. Worker를 예약할 경우에는 [`CustomWidgetsApp`](app/src/main/kotlin/com/customwidgets/app/CustomWidgetsApp.kt)에 `HiltWorkerFactory`를 연결하는 WorkManager 설정도 필요하다. 현재 위젯 XML의 [`updatePeriodMillis`](app/src/main/res/xml/custom_widget_info.xml)는 30분으로 고정되어 있다.

### P1 — API 키 보호

[`AiConfigStore`](app/src/main/kotlin/com/customwidgets/app/ai/AiConfigStore.kt)는 암호화 저장소를 열지 못하면 평문 SharedPreferences로 전환한다. [MCP 서버 API 키](app/src/main/kotlin/com/customwidgets/app/data/local/entity/McpServerEntity.kt)는 Room에 평문으로 저장된다. [Manifest](app/src/main/AndroidManifest.xml)는 앱 백업을 허용한다. 두 키의 저장·백업 정책을 통일하고 암호화 실패를 사용자에게 드러내야 한다.

### P2 — 기능 일치와 사용성

- [`McpServerRepository.toggleServer()`](app/src/main/kotlin/com/customwidgets/app/mcp/McpServerRepository.kt)는 활성화된 서버만 조회하므로 서버를 끈 뒤 다시 켤 수 없다.
- [`McpClient.callTool()`](app/src/main/kotlin/com/customwidgets/app/mcp/McpClient.kt)은 구현되어 있지만 호출 지점이 없다. 생성 프롬프트가 안내하는 MCP 토큰과 실제 동작이 일치하지 않는다.
- [`DslRenderer`](app/src/main/kotlin/com/customwidgets/app/widget/renderer/DslRenderer.kt)는 이미지 URL·리소스 이름 및 DSL의 `border`를 실제 위젯에 반영하지 않는다. 미리 보기와 홈 화면의 결과가 달라질 수 있다.
- [갤러리에서 정의를 삭제](app/src/main/kotlin/com/customwidgets/app/data/repository/WidgetRepository.kt)하면 연결 정보는 삭제되지만 홈 화면 인스턴스의 처리 정책이 없다. 다음 렌더링에서 설정되지 않은 위젯 화면이 나타날 수 있다.

## 권장 작업 순서

1. 버전 1→2 Room 마이그레이션을 구현하고 기존 DB 데이터로 업그레이드 경로를 확인한다.
2. 배포 서명 키를 보호된 저장소로 옮기고 기존 배포본의 서명 키 대응을 결정한다.
3. HTTP·MCP 조회 결과를 위젯 렌더링에 전달하고 배터리 값을 실제 기기에서 읽는다.
4. Worker 예약, Hilt WorkerFactory, 새로고침 액션, 위젯별 갱신 주기를 연결한다.
5. MCP 재활성화와 DSL 렌더링 누락을 수정하고 화면 미리 보기와 실제 위젯을 비교한다.
6. 릴리스 전에 자동화된 테스트 단계를 배포 워크플로에 추가한다.

## 개발 및 검증

프로젝트는 Gradle Wrapper를 포함한다. 로컬 디버그 APK는 `./gradlew :app:assembleDebug`, 단위 테스트는 `./gradlew :app:testDebugUnitTest`로 실행할 수 있다. 테스트 소스는 DSL, AI 통신, DAO, MCP, 바인딩, 생성 흐름 등을 다룬다.

이 문서는 소스와 Git 이력을 읽어 작성했다. 이번 문서 작성 과정에서는 빌드와 테스트를 실행하지 않았다.
