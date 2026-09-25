# Liquid Glass verification record

날짜: 2026-09-25 · 변경 기준: 현재 작업 트리

## 성공한 확인

| 확인 | 명령/증거 | 결과 |
|---|---|---|
| Backdrop AAR compile SDK 요구 | `:app:checkDebugAarMetadata` 및 `:app:checkReleaseAarMetadata` (AAR 검사를 끄는 Gradle task 없음) | 통과, compileSdk 37 |
| 앱 Kotlin + KSP/Hilt | `:app:compileDebugKotlin`, `:app:compileReleaseKotlin` | 통과 |
| 최신 debug APK | `:app:assembleDebug` | 성공, `app/build/outputs/apk/debug/app-debug.apk` 갱신 |
| 최신 release APK | `:app:assembleRelease` (max-workers=1, Gradle heap 1536 MiB) | 성공, `app/build/outputs/apk/release/app-release.apk` 갱신 |
| Compose 계측 테스트 APK | 최신 `:app:assembleDebugAndroidTest` | 계측 코드 컴파일 및 APK 생성. Compose v2 rule로 이행하라는 deprecation warning 2건; 테스트 실행 여부와는 별개 |
| Compose dependency resolution | `:app:dependencies --configuration debugRuntimeClasspath` | BOM에서 AndroidX Compose 1.12.1 선택; Backdrop 2.0.1의 Android variant가 요구한 1.12.0은 BOM으로 1.12.1에 정렬; duplicate-class 체크 통과 |
| JVM 단위 테스트 | `:app:testDebugUnitTest` | 48 tests, 실패 0. `GlassPolicyTest` 2개 포함 |
| lint | 이전 `:app:lintDebug` 실행 기록 | 주로 기존 미사용 리소스·오래된 종속성·구두점 경고. 최종 화면 변경 뒤에는 재실행하지 않음 |
| patch whitespace | `git diff --check` | 통과 |

계측 테스트는 `GlassRenderingTest`(host/panel 표시, 재생성, controls, 입력, modal, 효과 모드와 slider semantics) 및 `StartupRenderingTest`(MainActivity 첫 화면/재생성)을 포함한다. 이들은 코드에 들어 있고 APK에서 컴파일됐으나 아래 장치 문제로 실행 결과는 아직 없다.

최종 APK 생성 전 로컬 메모리 한도 때문에 `mergeExtDexDebug`가 768 MiB Gradle heap으로 실패했고, debug/release를 2048 MiB heap에서 한꺼번에 패키징할 때 프로세스가 종료됐다. 캐시를 활용해 debug를 따로 생성하고 release는 1536 MiB heap으로 따로 생성해 두 APK 모두 성공했다. 일반 개발 PC/CI에서는 프로젝트 기본 `org.gradle.jvmargs`를 사용하고, 메모리 제한이 작은 환경에서는 변형을 나눠 빌드한다.

## 에뮬레이터 실행 시도

Android 13 API 33 x86_64 SDK image로 AVD를 만들었다. `/dev/kvm`이 없어 `-accel off`의 QEMU/TCG CPU 렌더링을 사용했다. 해상도 480×800, SwiftShader, 2 GiB 설정의 첫 cold boot는 약 305초 뒤 `sys.boot_completed=1`까지 도달했다. 그러나 APK 설치가 끝나기 전에 컨테이너 OOM이 QEMU를 종료했고 ADB 연결이 사라졌다. 다음 부팅은 1.5 GiB와 1 GiB로 낮추고 카메라를 끈 상태로 시도했지만 `system_server` 초기화 중 cgroup OOM으로 종료됐다. 이 기간에 별도의 고메모리 Git 작업이 겹쳤다.

따라서 앱 설치·Activity 실행·계측 테스트는 AVD에서 완료하지 못했다. API 33 에뮬레이터에서 부팅 플래그가 한 번 확인된 사실을 앱 렌더 성공으로 간주하지 않는다. 현재 실행 중인 Android 기기는 없다.

따라서 현재 증거는 **최신 debug/release APK 패키징과 소스 컴파일 증거**이며 RenderThread 생존·효과 합성·시각 대비를 확인한 런타임 증거는 아니다. 계측 테스트 PASS라고 보고하지 않는다.

## 남은 장치 검증

KVM 가속을 사용할 수 있는 호스트 또는 실제 Android 기기를 연결한 뒤 아래를 실행한다. 이 컨테이너에서 QEMU cold boot와 Gradle 작업을 동시에 실행하지 않는다.

```bash
adb devices
./gradlew :app:connectedDebugAndroidTest
adb logcat -d -s AndroidRuntime RenderThread
```

계측 테스트가 완료되면 HTML/XML 결과를 확인하고, MainActivity와 debug Glass Catalog에서 실제 스크린샷을 저장한다. 특히 API 31/32의 blur, API 33+ lens, Full/Reduced/Off, Dialog, 1.5×·2× 글꼴, TalkBack, IME와 접힘/펼침을 확인한다. Emulator PASS는 실기기 GPU 성능·접근성을 대신하지 않는다.
