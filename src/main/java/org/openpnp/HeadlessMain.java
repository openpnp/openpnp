/*
 * Copyright (C) 2024 OpenPnP Contributors
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.pmw.tinylog.Logger;

/**
 * Класс для запуска OpenPnP в headless режиме без графического интерфейса.
 * Поддерживает автоматическое выполнение job файлов и скриптов.
 */
public class HeadlessMain {
    private static final String VERSION = Main.getVersion();
    private static boolean keepRunning = true;
    private static CountDownLatch shutdownLatch = new CountDownLatch(1);

    /**
     * Параметры командной строки для headless режима
     */
    public static class HeadlessArgs {
        public String jobFile = null;
        public String scriptFile = null;
        public String configDirectory = null;
        public boolean waitForCompletion = true;
        public long timeoutMinutes = 60; // таймаут по умолчанию
    }

    public static void main(String[] args) {
        Logger.info("Запуск OpenPnP в headless режиме, версия: {}", VERSION);

        // Парсинг аргументов командной строки
        HeadlessArgs headlessArgs = parseArgs(args);

        // Настройка обработчиков сигналов для graceful shutdown
        setupShutdownHooks();

        try {
            // Инициализация конфигурации
            initializeConfiguration(headlessArgs);

            // Загрузка конфигурации
            final Configuration configuration = Configuration.get();
            configuration.load();

            Locale.setDefault(configuration.getLocale());

            Logger.info("Конфигурация загружена из: {}", configuration.getConfigurationDirectory());

            // Инициализация машины
            Machine machine = configuration.getMachine();
            if (machine == null) {
                throw new RuntimeException("Не удалось загрузить конфигурацию машины");
            }

            // Добавление слушателя событий машины
            machine.addListener(new HeadlessMachineListener());

            // Запуск startup скрипта
            configuration.getScripting().on("Startup", null);
            Logger.info("Система инициализирована успешно");

            // Выполнение заданных задач
            executeRequestedTasks(headlessArgs, configuration, machine);

            // Ожидание завершения или сигнала остановки
            if (headlessArgs.waitForCompletion) {
                Logger.info("Переход в режим ожидания. Для остановки используйте Ctrl+C");
                waitForShutdown(headlessArgs.timeoutMinutes);
            }

        } catch (Exception e) {
            Logger.error("Ошибка при запуске headless режима: {}", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            shutdown();
        }

        Logger.info("Headless режим завершен");
        System.exit(0);
    }

    /**
     * Парсинг аргументов командной строки
     */
    private static HeadlessArgs parseArgs(String[] args) {
        HeadlessArgs headlessArgs = new HeadlessArgs();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "--job":
                    if (i + 1 < args.length) {
                        headlessArgs.jobFile = args[++i];
                    } else {
                        throw new IllegalArgumentException("Не указан путь к job файлу после --job");
                    }
                    break;

                case "--script":
                    if (i + 1 < args.length) {
                        headlessArgs.scriptFile = args[++i];
                    } else {
                        throw new IllegalArgumentException("Не указан путь к скрипту после --script");
                    }
                    break;

                case "--config":
                    if (i + 1 < args.length) {
                        headlessArgs.configDirectory = args[++i];
                    } else {
                        throw new IllegalArgumentException("Не указана директория конфигурации после --config");
                    }
                    break;

                case "--timeout":
                    if (i + 1 < args.length) {
                        try {
                            headlessArgs.timeoutMinutes = Long.parseLong(args[++i]);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Неверный формат таймаута: " + args[i]);
                        }
                    } else {
                        throw new IllegalArgumentException("Не указан таймаут после --timeout");
                    }
                    break;

                case "--no-wait":
                    headlessArgs.waitForCompletion = false;
                    break;

                case "--help":
                    printUsage();
                    System.exit(0);
                    break;

                default:
                    if (arg.startsWith("--")) {
                        Logger.warn("Неизвестный параметр: {}", arg);
                    }
                    break;
            }
        }

        return headlessArgs;
    }

    /**
     * Вывод справки по использованию
     */
    private static void printUsage() {
        System.out.println("OpenPnP Headless Mode - Запуск без графического интерфейса");
        System.out.println("Версия: " + VERSION);
        System.out.println();
        System.out.println("Использование:");
        System.out.println("  java -jar openpnp.jar --headless [параметры]");
        System.out.println();
        System.out.println("Параметры:");
        System.out.println("  --job <файл>        Автоматически выполнить указанный job файл");
        System.out.println("  --script <файл>     Выполнить указанный скрипт при запуске");
        System.out.println("  --config <папка>    Использовать указанную директорию конфигурации");
        System.out.println("  --timeout <минуты>  Таймаут работы в минутах (по умолчанию: 60)");
        System.out.println("  --no-wait          Не ждать завершения, выйти сразу после запуска");
        System.out.println("  --help             Показать эту справку");
        System.out.println();
        System.out.println("Примеры:");
        System.out.println("  # Запуск с выполнением job файла:");
        System.out.println("  java -jar openpnp.jar --headless --job /path/to/production.job.xml");
        System.out.println();
        System.out.println("  # Запуск с кастомным скриптом:");
        System.out.println("  java -jar openpnp.jar --headless --script /path/to/automation.js");
        System.out.println();
        System.out.println("  # Простой headless режим для API доступа:");
        System.out.println("  java -jar openpnp.jar --headless --no-wait");
    }

    /**
     * Инициализация конфигурации
     */
    private static void initializeConfiguration(HeadlessArgs args) {
        File configurationDirectory;

        if (args.configDirectory != null) {
            configurationDirectory = new File(args.configDirectory);
        } else if (System.getProperty("configDir") != null) {
            configurationDirectory = new File(System.getProperty("configDir"));
        } else {
            File homeDirectory = new File(System.getProperty("user.home"));
            configurationDirectory = new File(homeDirectory, ".openpnp2");
        }

        configurationDirectory.mkdirs();
        Configuration.initialize(configurationDirectory);

        Logger.info("Использование конфигурации из: {}", configurationDirectory.getAbsolutePath());
    }

    /**
     * Выполнение заданных задач
     */
    private static void executeRequestedTasks(HeadlessArgs args, Configuration configuration, Machine machine) {
        try {
            // Выполнение job файла если указан
            if (args.jobFile != null) {
                executeJobFile(args.jobFile, configuration, machine);
            }

            // Выполнение скрипта если указан
            if (args.scriptFile != null) {
                executeScriptFile(args.scriptFile, configuration);
            }

        } catch (Exception e) {
            Logger.error("Ошибка при выполнении задач: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Выполнение job файла
     */
    private static void executeJobFile(String jobFilePath, Configuration configuration, Machine machine)
            throws Exception {
        File jobFile = new File(jobFilePath);
        if (!jobFile.exists()) {
            throw new RuntimeException("Job файл не найден: " + jobFilePath);
        }

        Logger.info("Загрузка job файла: {}", jobFilePath);
        Job job = configuration.loadJob(jobFile);

        Logger.info("Начинаем выполнение job из файла: {}", jobFile.getName());

        // TODO: Здесь нужно добавить логику выполнения job через машину
        // Это требует изучения JobPanel.JobPlacementsTableModel и JobProcessor
        Logger.warn("ВНИМАНИЕ: Автоматическое выполнение job файлов еще не реализовано");
        Logger.info("Job загружен успешно. Для полной реализации требуется интеграция с JobProcessor");
    }

    /**
     * Выполнение скрипта
     */
    private static void executeScriptFile(String scriptFilePath, Configuration configuration) throws Exception {
        File scriptFile = new File(scriptFilePath);
        if (!scriptFile.exists()) {
            throw new RuntimeException("Скрипт не найден: " + scriptFilePath);
        }

        Logger.info("Выполнение скрипта: {}", scriptFilePath);

        // Выполнение скрипта через систему скриптов OpenPnP
        configuration.getScripting().execute(scriptFile);

        Logger.info("Скрипт выполнен успешно");
    }

    /**
     * Настройка обработчиков сигналов для graceful shutdown
     */
    private static void setupShutdownHooks() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.info("Получен сигнал завершения работы");
            keepRunning = false;
            shutdownLatch.countDown();
        }));

        // Обработка Ctrl+C
        Thread.currentThread().setUncaughtExceptionHandler((thread, exception) -> {
            Logger.error("Необработанная ошибка в потоке {}: {}", thread.getName(), exception.getMessage());
            exception.printStackTrace();
            keepRunning = false;
            shutdownLatch.countDown();
        });
    }

    /**
     * Ожидание завершения работы или таймаута
     */
    private static void waitForShutdown(long timeoutMinutes) {
        try {
            boolean completed = shutdownLatch.await(timeoutMinutes, TimeUnit.MINUTES);
            if (!completed) {
                Logger.info("Достигнут таймаут {} минут, завершаем работу", timeoutMinutes);
            }
        } catch (InterruptedException e) {
            Logger.info("Ожидание прервано");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Завершение работы и освобождение ресурсов
     */
    private static void shutdown() {
        try {
            Logger.info("Начинаем завершение работы...");

            if (Configuration.isInstanceInitialized()) {
                Configuration configuration = Configuration.get();

                // Выполнение shutdown скрипта
                try {
                    configuration.getScripting().on("Shutdown", null);
                } catch (Exception e) {
                    Logger.warn("Ошибка при выполнении shutdown скрипта: {}", e.getMessage());
                }

                // Остановка машины
                Machine machine = configuration.getMachine();
                if (machine != null && machine.isEnabled()) {
                    try {
                        machine.setEnabled(false);
                        Logger.info("Машина остановлена");
                    } catch (Exception e) {
                        Logger.warn("Ошибка при остановке машины: {}", e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            Logger.error("Ошибка при завершении работы: {}", e.getMessage());
        }
    }

    /**
     * Слушатель событий машины для headless режима
     */
    private static class HeadlessMachineListener extends MachineListener.Adapter {
        @Override
        public void machineHeadActivity(Machine machine, org.openpnp.spi.Head head) {
            // Логирование активности головок
        }

        @Override
        public void machineTargetedUserAction(Machine machine, org.openpnp.spi.HeadMountable hm, boolean jogging) {
            // Логирование действий пользователя
        }

        @Override
        public void machineActuatorActivity(Machine machine, org.openpnp.spi.Actuator actuator) {
            // Логирование активности актуаторов
        }

        @Override
        public void machineEnabled(Machine machine) {
            Logger.info("Машина включена");
        }

        @Override
        public void machineEnableFailed(Machine machine, String reason) {
            Logger.error("Не удалось включить машину. Причина: {}", reason);
        }

        @Override
        public void machineAboutToBeDisabled(Machine machine, String reason) {
            Logger.info("Машина готовится к выключению. Причина: {}", reason);
        }

        @Override
        public void machineDisabled(Machine machine, String reason) {
            Logger.info("Машина выключена. Причина: {}", reason);
        }

        @Override
        public void machineDisableFailed(Machine machine, String reason) {
            Logger.error("Не удалось выключить машину. Причина: {}", reason);
        }

        @Override
        public void machineHomed(Machine machine, boolean isHomed) {
            if (isHomed) {
                Logger.info("Машина выполнила homing");
            } else {
                Logger.info("Машина потеряла homing позицию");
            }
        }

        @Override
        public void machineBusy(Machine machine, boolean busy) {
            if (busy) {
                Logger.debug("Машина занята выполнением задачи");
            } else {
                Logger.debug("Машина свободна");
            }
        }
    }
}