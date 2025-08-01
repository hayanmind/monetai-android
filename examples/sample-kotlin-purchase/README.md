# Monetai Sample Kotlin Purchase

이 샘플 앱은 MonetaiSDK와 RevenueCat을 통합하여 인앱 구매 기능을 구현한 Android 앱입니다.

## 기능

- MonetaiSDK 초기화 및 설정
- RevenueCat을 통한 인앱 구매
- 사용자 예측 기능
- 이벤트 로깅
- 할인 정보 표시
- 구독 상태 확인

## 설정

1. `Constants.kt` 파일에서 SDK 키와 사용자 ID를 설정하세요:

   ```kotlin
   const val SDK_KEY = "your-sdk-key"
   const val USER_ID = "your-user-id"
   const val REVENUE_CAT_API_KEY = "your-revenuecat-api-key"
   ```

2. RevenueCat 대시보드에서 제품을 설정하세요.

3. 앱을 빌드하고 실행하세요.

## 사용법

1. 앱을 실행하면 MonetaiSDK가 자동으로 초기화됩니다.
2. "Predict User" 버튼을 눌러 사용자 예측을 실행하세요.
3. 테스트 이벤트 버튼들을 눌러 다양한 이벤트를 로깅하세요.
4. 제품 목록에서 "Buy" 버튼을 눌러 구매를 테스트하세요.

## 의존성

- MonetaiSDK
- RevenueCat Purchases SDK
- AndroidX 라이브러리들
- Kotlin Coroutines
