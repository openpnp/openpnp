@echo off
setlocal enabledelayedexpansion

REM OpenPnP Headless Mode Launcher for Windows
REM Скрипт для запуска OpenPnP без графического интерфейса

REM Настройки по умолчанию
set "SCRIPT_DIR=%~dp0"
set "JAVA_OPTS="
set "CONFIG_DIR="
set "JOB_FILE="
set "SCRIPT_FILE="
set "TIMEOUT="
set "NO_WAIT=false"
set "ADDITIONAL_ARGS="
set "JAVA_CMD="
set "JAR_FILE="

REM Функция вывода справки
:show_help
echo OpenPnP Headless Mode Launcher for Windows
echo Скрипт для запуска OpenPnP в режиме без графического интерфейса
echo.
echo Использование:
echo   %~nx0 [параметры]
echo.
echo Параметры:
echo   -j, --job FILE        Автоматически выполнить указанный job файл
echo   -s, --script FILE     Выполнить указанный скрипт при запуске
echo   -c, --config DIR      Использовать указанную директорию конфигурации
echo   -t, --timeout MIN     Таймаут работы в минутах (по умолчанию: 60)
echo   -n, --no-wait         Не ждать завершения, выйти сразу после запуска
echo   -m, --memory SIZE     Размер heap памяти JVM (например: 2g, 1024m)
echo   -D, --java-prop       Дополнительные Java свойства (-Dname=value)
echo   -h, --help            Показать эту справку
echo   -v, --version         Показать версию
echo.
echo Переменные окружения:
echo   JAVA_HOME             Путь к установке Java
echo   OPENPNP_JAR           Путь к OpenPnP JAR файлу
echo   OPENPNP_CONFIG        Директория конфигурации по умолчанию
echo.
echo Примеры:
echo   # Запуск с выполнением job файла:
echo   %~nx0 --job C:\path\to\production.job.xml
echo.
echo   # Запуск с кастомным скриптом и увеличенной памятью:
echo   %~nx0 --script automation.js --memory 4g
echo.
echo   # Простой headless режим для API доступа:
echo   %~nx0 --no-wait --config C:\custom\config
goto :eof

REM Функция логирования
:log
echo [%date% %time%] %~1
goto :eof

:log_warn
echo [%date% %time%] WARN: %~1
goto :eof

:log_error
echo [%date% %time%] ERROR: %~1
goto :eof

REM Функция поиска Java
:find_java
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
        call :log "Используется Java из JAVA_HOME: !JAVA_CMD!"
        goto :check_java_version
    )
)

REM Поиск Java в PATH
java -version >nul 2>&1
if !errorlevel! equ 0 (
    set "JAVA_CMD=java"
    call :log "Используется Java из PATH"
    goto :check_java_version
)

call :log_error "Java не найдена. Установите Java или установите переменную JAVA_HOME"
exit /b 1

:check_java_version
REM Проверка версии Java (упрощенная проверка для Windows)
for /f "tokens=3" %%i in ('"%JAVA_CMD%" -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION_STRING=%%i
)
REM Удаляем кавычки
set JAVA_VERSION_STRING=!JAVA_VERSION_STRING:"=!
REM Извлекаем первую цифру версии
for /f "tokens=1 delims=." %%a in ("!JAVA_VERSION_STRING!") do set JAVA_MAJOR_VERSION=%%a

if !JAVA_MAJOR_VERSION! LSS 8 (
    call :log_error "Требуется Java 8 или выше. Найдена версия: !JAVA_MAJOR_VERSION!"
    exit /b 1
)

call :log "Java версия проверена: !JAVA_MAJOR_VERSION!"
goto :eof

REM Функция поиска OpenPnP JAR файла
:find_openpnp_jar
if defined OPENPNP_JAR (
    if exist "%OPENPNP_JAR%" (
        set "JAR_FILE=%OPENPNP_JAR%"
        call :log "Используется JAR из переменной OPENPNP_JAR: !JAR_FILE!"
        goto :eof
    )
)

if exist "%SCRIPT_DIR%openpnp.jar" (
    set "JAR_FILE=%SCRIPT_DIR%openpnp.jar"
    call :log "Найден JAR файл: !JAR_FILE!"
    goto :eof
)

if exist "%SCRIPT_DIR%target\openpnp.jar" (
    set "JAR_FILE=%SCRIPT_DIR%target\openpnp.jar"
    call :log "Найден JAR файл: !JAR_FILE!"
    goto :eof
)

if exist "%SCRIPT_DIR%build\libs\openpnp.jar" (
    set "JAR_FILE=%SCRIPT_DIR%build\libs\openpnp.jar"
    call :log "Найден JAR файл: !JAR_FILE!"
    goto :eof
)

call :log_error "OpenPnP JAR файл не найден. Возможные пути:"
call :log_error "  - %SCRIPT_DIR%openpnp.jar"
call :log_error "  - %SCRIPT_DIR%target\openpnp.jar"
call :log_error "  - %SCRIPT_DIR%build\libs\openpnp.jar"
call :log_error "  - Установите переменную OPENPNP_JAR"
exit /b 1

REM Функция проверки файлов
:validate_files
if defined JOB_FILE (
    if not exist "%JOB_FILE%" (
        call :log_error "Job файл не найден: %JOB_FILE%"
        exit /b 1
    )
)

if defined SCRIPT_FILE (
    if not exist "%SCRIPT_FILE%" (
        call :log_error "Скрипт не найден: %SCRIPT_FILE%"
        exit /b 1
    )
)

if defined CONFIG_DIR (
    if not exist "%CONFIG_DIR%" (
        call :log_warn "Директория конфигурации не существует, будет создана: %CONFIG_DIR%"
        mkdir "%CONFIG_DIR%" 2>nul
        if !errorlevel! neq 0 (
            call :log_error "Не удалось создать директорию конфигурации: %CONFIG_DIR%"
            exit /b 1
        )
    )
)
goto :eof

REM Парсинг аргументов командной строки
:parse_args
if "%~1"=="" goto :args_parsed

if "%~1"=="-j" goto :set_job
if "%~1"=="--job" goto :set_job
if "%~1"=="-s" goto :set_script
if "%~1"=="--script" goto :set_script
if "%~1"=="-c" goto :set_config
if "%~1"=="--config" goto :set_config
if "%~1"=="-t" goto :set_timeout
if "%~1"=="--timeout" goto :set_timeout
if "%~1"=="-n" goto :set_no_wait
if "%~1"=="--no-wait" goto :set_no_wait
if "%~1"=="-m" goto :set_memory
if "%~1"=="--memory" goto :set_memory
if "%~1"=="-D" goto :set_java_prop
if "%~1"=="--java-prop" goto :set_java_prop
if "%~1"=="-h" goto :show_help_and_exit
if "%~1"=="--help" goto :show_help_and_exit
if "%~1"=="-v" goto :show_version
if "%~1"=="--version" goto :show_version

REM Неизвестный параметр - добавляем к дополнительным аргументам
set "ADDITIONAL_ARGS=%ADDITIONAL_ARGS% %~1"
shift
goto :parse_args

:set_job
shift
set "JOB_FILE=%~1"
shift
goto :parse_args

:set_script
shift
set "SCRIPT_FILE=%~1"
shift
goto :parse_args

:set_config
shift
set "CONFIG_DIR=%~1"
shift
goto :parse_args

:set_timeout
shift
set "TIMEOUT=%~1"
shift
goto :parse_args

:set_no_wait
set "NO_WAIT=true"
shift
goto :parse_args

:set_memory
shift
set "JAVA_OPTS=%JAVA_OPTS% -Xmx%~1"
shift
goto :parse_args

:set_java_prop
shift
set "JAVA_OPTS=%JAVA_OPTS% -D%~1"
shift
goto :parse_args

:show_help_and_exit
call :show_help
exit /b 0

:show_version
call :find_java
if !errorlevel! neq 0 exit /b !errorlevel!
call :find_openpnp_jar
if !errorlevel! neq 0 exit /b !errorlevel!
"%JAVA_CMD%" -jar "%JAR_FILE%" --version
exit /b !errorlevel!

:args_parsed

REM Основная логика запуска
:main
call :log "Запуск OpenPnP в headless режиме..."

REM Поиск и проверка зависимостей
call :find_java
if !errorlevel! neq 0 exit /b !errorlevel!

call :find_openpnp_jar
if !errorlevel! neq 0 exit /b !errorlevel!

call :validate_files
if !errorlevel! neq 0 exit /b !errorlevel!

REM Формирование аргументов для OpenPnP
set "OPENPNP_ARGS=--headless"

if defined JOB_FILE (
    set "OPENPNP_ARGS=!OPENPNP_ARGS! --job "!JOB_FILE!""
    call :log "Job файл: !JOB_FILE!"
)

if defined SCRIPT_FILE (
    set "OPENPNP_ARGS=!OPENPNP_ARGS! --script "!SCRIPT_FILE!""
    call :log "Скрипт: !SCRIPT_FILE!"
)

if defined CONFIG_DIR (
    set "OPENPNP_ARGS=!OPENPNP_ARGS! --config "!CONFIG_DIR!""
    call :log "Конфигурация: !CONFIG_DIR!"
) else if defined OPENPNP_CONFIG (
    set "OPENPNP_ARGS=!OPENPNP_ARGS! --config "!OPENPNP_CONFIG!""
    call :log "Конфигурация (из переменной): !OPENPNP_CONFIG!"
)

if defined TIMEOUT (
    set "OPENPNP_ARGS=!OPENPNP_ARGS! --timeout !TIMEOUT!"
    call :log "Таймаут: !TIMEOUT! минут"
)

if "!NO_WAIT!"=="true" (
    set "OPENPNP_ARGS=!OPENPNP_ARGS! --no-wait"
    call :log "Режим: без ожидания завершения"
)

REM Настройка Java параметров по умолчанию
echo !JAVA_OPTS! | findstr /C:"-Xmx" >nul
if !errorlevel! neq 0 (
    set "JAVA_OPTS=!JAVA_OPTS! -Xmx2g"
)

REM Дополнительные системные параметры
set "JAVA_OPTS=!JAVA_OPTS! -Djava.awt.headless=true"
set "JAVA_OPTS=!JAVA_OPTS! -Dfile.encoding=UTF-8"

REM Формирование полной команды
set "FULL_CMD="!JAVA_CMD!" !JAVA_OPTS! -jar "!JAR_FILE!" !OPENPNP_ARGS! !ADDITIONAL_ARGS!"

call :log "Запуск команды: !FULL_CMD!"
call :log "Для остановки используйте Ctrl+C"
echo.

REM Запуск OpenPnP
if "!NO_WAIT!"=="true" (
    start "OpenPnP Headless" !FULL_CMD!
    call :log "OpenPnP запущен в фоновом режиме"
) else (
    !FULL_CMD!
    set "EXIT_CODE=!errorlevel!"
    
    if !EXIT_CODE! equ 0 (
        call :log "OpenPnP завершился успешно"
    ) else (
        call :log_error "OpenPnP завершился с ошибкой (код: !EXIT_CODE!)"
    )
    
    exit /b !EXIT_CODE!
)

goto :eof

REM Точка входа скрипта
call :parse_args %*
call :main 