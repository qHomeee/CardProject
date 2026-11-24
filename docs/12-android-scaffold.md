# Практика 12 Каркас приложения Запомни

Проект перенесён в папку **android**. При запуске открывается экран «Мои наборы». Первый список приходит из источника данных через сценарий чтения и ViewModel. Название приложения и текст экрана хранятся в strings.xml.

## Границы слоёв

| Часть | Ответственность | Пример |
| --- | --- | --- |
| :domain | Модели и сценарии на Kotlin/JVM. Список наблюдается через Flow. | Deck, LocalDeckStore, ObserveMyDecks |
| data в :app | Реализует контракт чтения. Пока возвращает пустой список. | EmptyLocalDeckStore |
| presentation в :app | Преобразует поток в UiState и показывает загрузку или список. | MyDecksViewModel, MyDecksScreen |
| di в :app | Связывает хранилище, сценарий чтения и ViewModel. | AppModule |

Koin запускается из **ZapomniApplication**, указанного в манифесте. Activity получает ViewModel через Koin. Compose собирает StateFlow с учётом жизненного цикла. Подключение Room потребует реализации LocalDeckStore и замены привязки в AppModule.

## Версии зависимостей

Точные версии задаёт **gradle/libs.versions.toml**. Сборка использует Gradle 9.6.0, AGP 9.4.1 и Kotlin 2.2.10. Модуль :domain собирается в JVM 11. Для потока используется Coroutines 1.10.2, для внедрения зависимостей — [Koin 4.1.1 с обычным DSL](https://insert-koin.io/docs/4.1/reference/koin-android/viewmodel/).

<!-- pagebreak -->

## Сборка и проверка

Откройте android в Android Studio и дождитесь синхронизации Gradle. Подготовьте SDK API 37 и JDK 25 для Gradle. Команды ниже запускаются из папки android в PowerShell.

1. **.\gradlew.bat :domain:build :app:assembleDebug** собирает доменный JAR и debug APK. APK находится в app/build/outputs/apk/debug.
2. **.\gradlew.bat :app:testDebugUnitTest** запускает JVM-тест. Тест получает ViewModel из Koin и дожидается состояния с пустым списком.
3. **.\gradlew.bat :ktlintCheck :app:lintDebug** проверяет Kotlin и Android. [ktlint 1.7.1](https://github.com/ktlint/ktlint/releases/tag/1.7.1) читает настройки из .editorconfig. Задача :check также запускает ktlint.
4. **.\gradlew.bat :app:connectedDebugAndroidTest** проверяет появление экрана «Мои наборы» на подключённом устройстве или эмуляторе.

## Результаты

Собраны JAR, debug APK и APK инструментального теста. Прошёл один JVM-тест графа зависимостей. ktlint завершился без замечаний и обнаружил ошибки форматирования в контрольном примере.

Android Lint завершился без ошибок. В отчёте 17 предупреждений, из них семь относятся к неиспользуемым ресурсам шаблона и десять к версиям зависимостей. Инструментальный тест собран. Для его запуска требуется устройство или настроенный эмулятор.
