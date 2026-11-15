@echo off
rem Crea el keystore con el que se firman las versiones release de Días de Furia.
rem Solo hay que hacerlo una vez. Te pedirá una contrasena y tus datos (nombre, etc.).
setlocal
set KEYTOOL=keytool
if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\keytool.exe" set KEYTOOL="%ProgramFiles%\Android\Android Studio\jbr\bin\keytool.exe"

if not exist firma mkdir firma
if exist firma\dias-de-furia.jks (
    echo Ya existe firma\dias-de-furia.jks. No se sobrescribe.
    goto fin
)

%KEYTOOL% -genkeypair -v -keystore firma\dias-de-furia.jks -alias diasdefuria -keyalg RSA -keysize 4096 -validity 10000

echo.
echo Listo. Ahora copia firma.properties.ejemplo como firma.properties y pon tus contrasenas.
:fin
pause
