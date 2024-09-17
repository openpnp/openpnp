@echo off

set archi=%PROCESSOR_ARCHITECTURE%

:run_application
if not x%archi:86=%==x%archi% java --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED --add-opens=java.desktop.java.awt.color=ALL-UNNAMED -jar target\openpnp-gui-0.0.1-alpha-SNAPSHOT.jar
if not x%archi:64=%==x%archi% java --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.desktop.java.awt=ALL-UNNAMED --add-opens=java.desktop.java.awt.color=ALL-UNNAMED -jar target\openpnp-gui-0.0.1-alpha-SNAPSHOT.jar

set exit_code=%ERRORLEVEL%
if %exit_code% == 127 (
    echo Application exited with code 127. Restarting...
    goto run_application
)