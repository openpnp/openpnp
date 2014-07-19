@echo off

set archi=%PROCESSOR_ARCHITECTURE%

if not x%archi:86=%==x%archi% java -Djava.library.path=lib\native\win32 -jar target\openpnp-gui-0.0.1-alpha-SNAPSHOT.jar
if not x%archi:64=%==x%archi% java -Djava.library.path=lib\native\win64 -jar target\openpnp-gui-0.0.1-alpha-SNAPSHOT.jar


