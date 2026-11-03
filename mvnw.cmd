@echo off
rem Maven Wrapper simplificado de APOLO (Windows).
rem Descarga Maven la primera vez (necesita internet solo ese dia) y lo reutiliza despues.
setlocal
set "MAVEN_VERSION=3.9.9"
set "DIST_DIR=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MVN_CMD=%DIST_DIR%\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd"

if not exist "%MVN_CMD%" (
    echo Descargando Maven %MAVEN_VERSION% ^(solo la primera vez^)...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; New-Item -ItemType Directory -Force -Path '%DIST_DIR%' | Out-Null; $zip='%DIST_DIR%\maven.zip'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile $zip; Expand-Archive -Path $zip -DestinationPath '%DIST_DIR%' -Force; Remove-Item $zip"
    if errorlevel 1 exit /b 1
)

call "%MVN_CMD%" %*
exit /b %ERRORLEVEL%
