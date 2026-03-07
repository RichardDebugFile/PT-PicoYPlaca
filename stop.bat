@echo off
chcp 65001 >nul
title PT-PicoYPlaca — Deteniendo...

echo.
echo  Deteniendo y eliminando contenedores...
echo.

docker compose down

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo  [ERROR] Algo salio mal al detener los contenedores.
    echo  Revisa el estado con:  docker compose ps
    echo.
    pause
    exit /b 1
)

echo.
echo  [OK] Contenedores detenidos correctamente.
echo.
pause
