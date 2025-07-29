#!/bin/bash

# OpenPnP Headless Mode Launcher
# Скрипт для запуска OpenPnP без графического интерфейса
# Поддерживает Linux и macOS

# Настройки по умолчанию
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_OPTS=""
CONFIG_DIR=""
JOB_FILE=""
SCRIPT_FILE=""
TIMEOUT=""
NO_WAIT=false
ADDITIONAL_ARGS=""

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Функция вывода справки
show_help() {
    echo -e "${BLUE}OpenPnP Headless Mode Launcher${NC}"
    echo "Скрипт для запуска OpenPnP в режиме без графического интерфейса"
    echo ""
    echo "Использование:"
    echo "  $0 [параметры]"
    echo ""
    echo "Параметры:"
    echo "  -j, --job FILE        Автоматически выполнить указанный job файл"
    echo "  -s, --script FILE     Выполнить указанный скрипт при запуске"
    echo "  -c, --config DIR      Использовать указанную директорию конфигурации"
    echo "  -t, --timeout MIN     Таймаут работы в минутах (по умолчанию: 60)"
    echo "  -n, --no-wait         Не ждать завершения, выйти сразу после запуска"
    echo "  -m, --memory SIZE     Размер heap памяти JVM (например: 2g, 1024m)"
    echo "  -D, --java-prop       Дополнительные Java свойства (-Dname=value)"
    echo "  -h, --help            Показать эту справку"
    echo "  -v, --version         Показать версию"
    echo ""
    echo "Переменные окружения:"
    echo "  JAVA_HOME             Путь к установке Java"
    echo "  OPENPNP_JAR           Путь к OpenPnP JAR файлу"
    echo "  OPENPNP_CONFIG        Директория конфигурации по умолчанию"
    echo ""
    echo "Примеры:"
    echo "  # Запуск с выполнением job файла:"
    echo "  $0 --job /path/to/production.job.xml"
    echo ""
    echo "  # Запуск с кастомным скриптом и увеличенной памятью:"
    echo "  $0 --script automation.js --memory 4g"
    echo ""
    echo "  # Простой headless режим для API доступа:"
    echo "  $0 --no-wait --config /custom/config"
}

# Функция логирования
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARN:${NC} $1"
}

log_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"
}

# Функция поиска Java
find_java() {
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        JAVA_CMD="$JAVA_HOME/bin/java"
    elif command -v java >/dev/null 2>&1; then
        JAVA_CMD="java"
    else
        log_error "Java не найдена. Установите Java или установите переменную JAVA_HOME"
        exit 1
    fi

    # Проверка версии Java
    JAVA_VERSION=$("$JAVA_CMD" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 8 ]; then
        log_error "Требуется Java 8 или выше. Найдена версия: $JAVA_VERSION"
        exit 1
    fi

    log "Используется Java: $JAVA_CMD (версия $JAVA_VERSION)"
}

# Функция поиска OpenPnP JAR файла
find_openpnp_jar() {
    if [ -n "$OPENPNP_JAR" ] && [ -f "$OPENPNP_JAR" ]; then
        JAR_FILE="$OPENPNP_JAR"
    elif [ -f "$SCRIPT_DIR/openpnp.jar" ]; then
        JAR_FILE="$SCRIPT_DIR/openpnp.jar"
    elif [ -f "$SCRIPT_DIR/target/openpnp.jar" ]; then
        JAR_FILE="$SCRIPT_DIR/target/openpnp.jar"
    elif [ -f "$SCRIPT_DIR/build/libs/openpnp.jar" ]; then
        JAR_FILE="$SCRIPT_DIR/build/libs/openpnp.jar"
    else
        log_error "OpenPnP JAR файл не найден. Возможные пути:"
        log_error "  - $SCRIPT_DIR/openpnp.jar"
        log_error "  - $SCRIPT_DIR/target/openpnp.jar" 
        log_error "  - $SCRIPT_DIR/build/libs/openpnp.jar"
        log_error "  - Установите переменную OPENPNP_JAR"
        exit 1
    fi

    log "Используется JAR файл: $JAR_FILE"
}

# Функция проверки файлов
validate_files() {
    if [ -n "$JOB_FILE" ] && [ ! -f "$JOB_FILE" ]; then
        log_error "Job файл не найден: $JOB_FILE"
        exit 1
    fi

    if [ -n "$SCRIPT_FILE" ] && [ ! -f "$SCRIPT_FILE" ]; then
        log_error "Скрипт не найден: $SCRIPT_FILE"
        exit 1
    fi

    if [ -n "$CONFIG_DIR" ] && [ ! -d "$CONFIG_DIR" ]; then
        log_warn "Директория конфигурации не существует, будет создана: $CONFIG_DIR"
        mkdir -p "$CONFIG_DIR" || {
            log_error "Не удалось создать директорию конфигурации: $CONFIG_DIR"
            exit 1
        }
    fi
}

# Функция настройки сигналов
setup_signal_handlers() {
    trap 'log "Получен сигнал завершения, останавливаем OpenPnP..."; kill $OPENPNP_PID 2>/dev/null; exit 0' SIGINT SIGTERM
}

# Парсинг аргументов командной строки
while [[ $# -gt 0 ]]; do
    case $1 in
        -j|--job)
            JOB_FILE="$2"
            shift 2
            ;;
        -s|--script)
            SCRIPT_FILE="$2"
            shift 2
            ;;
        -c|--config)
            CONFIG_DIR="$2"
            shift 2
            ;;
        -t|--timeout)
            TIMEOUT="$2"
            shift 2
            ;;
        -n|--no-wait)
            NO_WAIT=true
            shift
            ;;
        -m|--memory)
            JAVA_OPTS="$JAVA_OPTS -Xmx$2"
            shift 2
            ;;
        -D|--java-prop)
            JAVA_OPTS="$JAVA_OPTS -D$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        -v|--version)
            find_java
            find_openpnp_jar
            "$JAVA_CMD" -jar "$JAR_FILE" --version
            exit 0
            ;;
        *)
            ADDITIONAL_ARGS="$ADDITIONAL_ARGS $1"
            shift
            ;;
    esac
done

# Основная логика запуска
main() {
    log "Запуск OpenPnP в headless режиме..."

    # Поиск и проверка зависимостей
    find_java
    find_openpnp_jar
    validate_files

    # Формирование аргументов для OpenPnP
    OPENPNP_ARGS="--headless"
    
    if [ -n "$JOB_FILE" ]; then
        OPENPNP_ARGS="$OPENPNP_ARGS --job \"$JOB_FILE\""
        log "Job файл: $JOB_FILE"
    fi
    
    if [ -n "$SCRIPT_FILE" ]; then
        OPENPNP_ARGS="$OPENPNP_ARGS --script \"$SCRIPT_FILE\""
        log "Скрипт: $SCRIPT_FILE"
    fi
    
    if [ -n "$CONFIG_DIR" ]; then
        OPENPNP_ARGS="$OPENPNP_ARGS --config \"$CONFIG_DIR\""
        log "Конфигурация: $CONFIG_DIR"
    elif [ -n "$OPENPNP_CONFIG" ]; then
        OPENPNP_ARGS="$OPENPNP_ARGS --config \"$OPENPNP_CONFIG\""
        log "Конфигурация (из переменной): $OPENPNP_CONFIG"
    fi
    
    if [ -n "$TIMEOUT" ]; then
        OPENPNP_ARGS="$OPENPNP_ARGS --timeout $TIMEOUT"
        log "Таймаут: $TIMEOUT минут"
    fi
    
    if [ "$NO_WAIT" = true ]; then
        OPENPNP_ARGS="$OPENPNP_ARGS --no-wait"
        log "Режим: без ожидания завершения"
    fi

    # Настройка Java параметров по умолчанию
    if [[ "$JAVA_OPTS" != *"-Xmx"* ]]; then
        JAVA_OPTS="$JAVA_OPTS -Xmx2g"
    fi
    
    # Дополнительные системные параметры
    JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true"
    JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"

    # Настройка обработчиков сигналов
    setup_signal_handlers

    # Формирование полной команды
    FULL_CMD="\"$JAVA_CMD\" $JAVA_OPTS -jar \"$JAR_FILE\" $OPENPNP_ARGS $ADDITIONAL_ARGS"

    log "Запуск команды: $FULL_CMD"
    log "Для остановки используйте Ctrl+C"
    echo ""

    # Запуск OpenPnP
    eval $FULL_CMD &
    OPENPNP_PID=$!

    # Ожидание завершения если требуется
    if [ "$NO_WAIT" = false ]; then
        wait $OPENPNP_PID
        EXIT_CODE=$?
        
        if [ $EXIT_CODE -eq 0 ]; then
            log "OpenPnP завершился успешно"
        else
            log_error "OpenPnP завершился с ошибкой (код: $EXIT_CODE)"
        fi
        
        exit $EXIT_CODE
    else
        log "OpenPnP запущен в фоновом режиме (PID: $OPENPNP_PID)"
        echo $OPENPNP_PID > "$SCRIPT_DIR/openpnp-headless.pid"
        log "PID сохранен в файл: $SCRIPT_DIR/openpnp-headless.pid"
    fi
}

# Проверка на то, что скрипт не запущен как source
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi 