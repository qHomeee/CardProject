# Проверка существующей Android-основы

Дата: 4 ноября 2026 года. Проверяется исходный шаблон `KotlinProject/`; код приложения и зависимости на этапе практики 3 не менялись.

## Среда

- Windows, JBR 25.0.3 из Android Studio.
- Gradle Wrapper 9.6.0, AGP 9.4.1, compileSdk и targetSdk 37, minSdk 24.
- Фактический `kotlin-compiler-embeddable` — 2.2.10. Compose compiler plugin — 2.2.10, Compose BOM — 2026.02.01.
- Запуск в офлайн-режиме использовал уже имеющиеся SDK и кеши Gradle. На чистой машине сначала потребуется загрузить SDK и зависимости; наличие файлов version catalog само по себе не гарантирует сборку без сети.

## Команды

В PowerShell из `X:\vs code\CartProject\KotlinProject`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat --offline --no-daemon help --console=plain
.\gradlew.bat --offline --no-daemon :app:assembleDebug :app:testDebugUnitTest --console=plain
.\gradlew.bat --offline --no-daemon :app:lintDebug --console=plain
.\gradlew.bat --offline --no-daemon :app:dependencyInsight --dependency kotlin-compiler-embeddable --configuration kotlinCompilerClasspath --console=plain
```

Все четыре запуска завершились `BUILD SUCCESSFUL`.

## Результаты и пределы проверки

- Создан `KotlinProject/app/build/outputs/apk/debug/app-debug.apk`: это шаблонный debug APK, не релиз «Запомни».
- XML в `KotlinProject/app/build/test-results/testDebugUnitTest/` подтверждает один тест `ExampleUnitTest.addition_isCorrect`, 0 failures, 0 errors, 0 skipped. Он проверяет `2 + 2 = 4`, а не требования MVP.
- Отчёт `KotlinProject/app/build/reports/lint-results-debug.sarif`: 8 результатов уровня warning — 1 `RedundantLabel` и 7 `UnusedResources`; ошибок нет. HTML-версия расположена рядом в `lint-results-debug.html`.
- Предупреждения исходных ресурсов будут разобраны при работе над темой и ресурсами. Они не подавлялись; ktlint пока не подключён.
- Unit-тесты предметной логики, ViewModel, Kover, серверные и инструментальные/UI-тесты ещё не реализованы и на этом шаге не запускались.
- Успех сборки текущего шаблона не подтверждает совместимость Room, Koin, Ktor и других ещё не добавленных библиотек. Их точные версии проверяются при подключении.
- Результаты сборки и отчёты находятся в игнорируемом `build/` и в Git не добавляются.

## Проверка документа

`docs/tools/build_stack.py` успешно создал DOCX с пятью таблицами, восемью критериями серверного сравнения, десятью рисками и 28 официальными гиперссылками. Итоговый рендерер сформировал шесть страниц в `.tmp/stack-render-final/`; все шесть PNG визуально проверены. Обрезаний, наложений и лишних пустых страниц нет. Промежуточные PDF и PNG для QA в Git не добавляются.
