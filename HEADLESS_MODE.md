# OpenPnP Headless Mode

## Описание

Headless режим позволяет запускать OpenPnP без графического интерфейса пользователя, что идеально подходит для:

- **Автоматизации производства** - запуск задач по расписанию
- **Серверного развертывания** - работа на серверах без GUI
- **Интеграции с другими системами** - через API и скрипты
- **Удаленного управления** - управление через SSH или другие протоколы
- **Пакетной обработки** - выполнение множества задач последовательно

## Возможности headless режима

✅ **Автоматическое выполнение job файлов**  
✅ **Запуск скриптов при старте**  
✅ **Полная поддержка конфигурации машины**  
✅ **Логирование всех операций**  
✅ **Graceful shutdown по сигналам**  
✅ **Мониторинг состояния машины**  
✅ **Таймауты и контроль времени выполнения**  
✅ **Кроссплатформенность** (Windows, Linux, macOS)

## Установка и настройка

### Системные требования

- Java 8 или выше
- OpenPnP JAR файл
- Сконфигурированная машина

### Подготовка

1. Убедитесь, что Java установлена:
   ```bash
   java -version
   ```

2. Скопируйте скрипты запуска в директорию с OpenPnP:
   - `run-headless.sh` (для Linux/macOS)  
   - `run-headless.bat` (для Windows)

3. Сделайте shell скрипт исполняемым (Linux/macOS):
   ```bash
   chmod +x run-headless.sh
   ```

## Использование

### Базовый запуск

**Linux/macOS:**
```bash
./run-headless.sh
```

**Windows:**
```cmd
run-headless.bat
```

### Параметры командной строки

| Параметр | Описание | Пример |
|----------|----------|---------|
| `-j, --job FILE` | Автоматически выполнить job файл | `--job production.job.xml` |
| `-s, --script FILE` | Выполнить скрипт при запуске | `--script automation.js` |
| `-c, --config DIR` | Директория конфигурации | `--config /custom/config` |
| `-t, --timeout MIN` | Таймаут работы в минутах | `--timeout 120` |
| `-n, --no-wait` | Не ждать завершения | `--no-wait` |
| `-m, --memory SIZE` | Размер heap памяти JVM | `--memory 4g` |
| `-D, --java-prop` | Java системные свойства | `-D configDir=/custom` |
| `-h, --help` | Показать справку | `--help` |
| `-v, --version` | Показать версию | `--version` |

### Переменные окружения

| Переменная | Описание | Пример |
|------------|----------|---------|
| `JAVA_HOME` | Путь к установке Java | `/usr/lib/jvm/java-11` |
| `OPENPNP_JAR` | Путь к JAR файлу | `/opt/openpnp/openpnp.jar` |
| `OPENPNP_CONFIG` | Директория конфигурации по умолчанию | `/etc/openpnp` |

## Примеры использования

### 1. Запуск с выполнением job файла

**Linux/macOS:**
```bash
./run-headless.sh --job /path/to/production.job.xml --timeout 60
```

**Windows:**
```cmd
run-headless.bat --job "C:\Production\board.job.xml" --timeout 60
```

### 2. Запуск с кастомным скриптом

```bash
./run-headless.sh --script automation.js --memory 4g --config /custom/config
```

### 3. Фоновый режим для API доступа

```bash
./run-headless.sh --no-wait --config /production/config
```

### 4. Запуск с дополнительными Java параметрами

```bash
./run-headless.sh -D configDir=/custom -D headless.timeout=3600 --memory 8g
```

## Интеграция со скриптами

### Startup скрипт

При запуске headless режима автоматически выполняется событие `Startup`, которое можно использовать для инициализации:

```javascript
// scripts/Events/Startup.js
print("OpenPnP запущен в headless режиме");

// Инициализация машины
var machine = config.getMachine();
machine.setEnabled(true);

// Ваша логика инициализации
initializeCustomFeatures();
```

### Shutdown скрипт

При завершении работы выполняется событие `Shutdown`:

```javascript
// scripts/Events/Shutdown.js
print("Завершение работы headless режима");

// Безопасное отключение машины
var machine = config.getMachine();
if (machine.isEnabled()) {
    machine.setEnabled(false);
}

// Ваша логика завершения
cleanupResources();
```

### Автоматизация задач

Создайте скрипт для автоматического выполнения задач:

```javascript
// automation.js
function runProductionCycle() {
    var machine = config.getMachine();
    
    try {
        // Включение машины
        machine.setEnabled(true);
        
        // Выполнение homing
        machine.home();
        
        // Загрузка и выполнение job
        var jobFile = new java.io.File("/production/current.job.xml");
        var job = config.loadJob(jobFile);
        
        // TODO: Добавить логику выполнения job
        print("Job загружен: " + jobFile.getName());
        
        print("Производственный цикл завершен успешно");
        
    } catch (e) {
        print("Ошибка: " + e.message);
        throw e;
    } finally {
        // Безопасное отключение
        machine.setEnabled(false);
    }
}

// Запуск
runProductionCycle();
```

## Мониторинг и логирование

### Логи

Все логи сохраняются в файл `OpenPnP.log` в директории конфигурации:
- События машины
- Ошибки выполнения
- Статус операций
- Диагностическая информация

### Мониторинг состояния

Headless режим предоставляет детальную информацию о состоянии машины:

```
[2024-01-15 10:30:15] Машина включена
[2024-01-15 10:30:16] Машина выполнила homing
[2024-01-15 10:30:17] Машина занята выполнением задачи
[2024-01-15 10:35:42] Машина свободна
```

### Контроль процесса

**Linux/macOS:**
```bash
# Проверка запущенного процесса
ps aux | grep openpnp

# Остановка через сигнал
kill -TERM <PID>

# Принудительная остановка
kill -9 <PID>
```

**Windows:**
```cmd
# Проверка запущенного процесса
tasklist | findstr java

# Остановка процесса
taskkill /F /PID <PID>
```

## Устранение неполадок

### Частые проблемы

**1. Java не найдена**
```
ERROR: Java не найдена. Установите Java или установите переменную JAVA_HOME
```
*Решение:* Установите Java 8+ или настройте `JAVA_HOME`

**2. JAR файл не найден**
```
ERROR: OpenPnP JAR файл не найден
```
*Решение:* Укажите путь через переменную `OPENPNP_JAR` или поместите JAR в директорию скрипта

**3. Ошибка конфигурации**
```
ERROR: Не удалось загрузить конфигурацию машины
```
*Решение:* Проверьте файл `machine.xml` в директории конфигурации

**4. Недостаточно памяти**
```
OutOfMemoryError: Java heap space
```
*Решение:* Увеличьте память через параметр `--memory 4g`

### Диагностика

Включите подробное логирование:

```bash
./run-headless.sh --java-prop "tinylog.level=DEBUG" --job production.job.xml
```

Проверьте конфигурацию без выполнения задач:

```bash
./run-headless.sh --no-wait --timeout 1
```

## Безопасность

### Рекомендации

1. **Ограничьте права доступа** к директории конфигурации
2. **Используйте отдельного пользователя** для запуска headless режима
3. **Настройте firewall** если используется сетевое взаимодействие
4. **Регулярно обновляйте** OpenPnP и Java
5. **Мониторьте логи** на предмет подозрительной активности

### Изоляция

Запускайте в изолированной среде:

```bash
# Создание отдельного пользователя
sudo useradd -r -s /bin/false openpnp

# Запуск от имени пользователя
sudo -u openpnp ./run-headless.sh --config /etc/openpnp
```

## Интеграция с системами автоматизации

### systemd (Linux)

Создайте service файл `/etc/systemd/system/openpnp-headless.service`:

```ini
[Unit]
Description=OpenPnP Headless Mode
After=network.target

[Service]
Type=simple
User=openpnp
Group=openpnp
WorkingDirectory=/opt/openpnp
ExecStart=/opt/openpnp/run-headless.sh --config /etc/openpnp --no-wait
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Активация:
```bash
sudo systemctl enable openpnp-headless
sudo systemctl start openpnp-headless
```

### Docker

Создайте Dockerfile:

```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app
COPY openpnp.jar .
COPY run-headless.sh .
COPY config/ ./config/

RUN chmod +x run-headless.sh

CMD ["./run-headless.sh", "--config", "./config", "--no-wait"]
```

### Windows Service

Используйте NSSM (Non-Sucking Service Manager) для создания Windows службы.

## Производительность

### Оптимизация JVM

```bash
# Рекомендуемые параметры для production
./run-headless.sh \
  --memory 4g \
  -D XX:+UseG1GC \
  -D XX:MaxGCPauseMillis=200 \
  -D server
```

### Мониторинг ресурсов

Следите за использованием:
- Памяти JVM
- CPU нагрузкой  
- Дискового пространства для логов
- Сетевой активностью

## Поддержка и сообщество

- **Документация:** [openpnp.org](http://openpnp.org)
- **Форум:** [OpenPnP Community](https://groups.google.com/group/openpnp)
- **GitHub:** [openpnp/openpnp](https://github.com/openpnp/openpnp)
- **Wiki:** [OpenPnP Wiki](https://github.com/openpnp/openpnp/wiki)

## Лицензия

Headless режим распространяется под той же лицензией, что и OpenPnP - GNU General Public License v3.0. 