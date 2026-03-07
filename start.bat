@echo off
chcp 65001 >nul
title PT-PicoYPlaca — Iniciando...

echo.
echo  =====================================================
echo    PT-PicoYPlaca — Verificador Pico y Placa (Quito)
echo  =====================================================
echo.

REM --- Verificar que Docker esta corriendo ---
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo  [ERROR] Docker no esta en ejecucion.
    echo.
    echo  Por favor abre Docker Desktop, espera a que termine
    echo  de iniciar y vuelve a ejecutar este script.
    echo.
    pause
    exit /b 1
)
echo  [OK] Docker en ejecucion.
echo.

REM --- Construir e iniciar los contenedores en segundo plano ---
echo  Construyendo e iniciando contenedores...
echo  (la primera vez puede tardar varios minutos mientras se descargan las imagenes base)
echo.

docker compose up --build -d

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo  [ERROR] Fallo al iniciar los contenedores.
    echo  Revisa los mensajes de error arriba.
    echo  Para ver logs detallados ejecuta:  docker compose logs
    echo.
    pause
    exit /b 1
)

echo.
echo  Contenedores iniciados. Esperando que los servicios esten listos...
echo.

REM --- Esperar hasta que el frontend responda (maximo ~2 minutos) ---
set /a intentos=0
:wait_loop
    set /a intentos+=1
    if %intentos% GTR 40 (
        echo  [ADVERTENCIA] El servicio tarda mas de lo esperado.
        echo  Verifica el estado con:  docker compose ps
        echo  O revisa los logs con:   docker compose logs
        goto abrir
    )
    timeout /t 3 /nobreak >nul
    REM Intenta una peticion HTTP al frontend; sale con 0 si responde 200
    powershell -Command ^
        "try { $r = Invoke-WebRequest -Uri 'http://localhost' -UseBasicParsing -TimeoutSec 2; exit ($r.StatusCode -ne 200) } catch { exit 1 }" ^
        >nul 2>&1
    if %ERRORLEVEL% NEQ 0 goto wait_loop

:abrir
echo.
echo  =====================================================
echo    Servicios disponibles:
echo.
echo    Frontend  :  http://localhost
echo    API REST  :  http://localhost:8080
echo    Swagger UI:  http://localhost:8080/swagger-ui.html
echo  =====================================================
echo.

start http://localhost

echo  Abriendo el navegador...
echo.
echo  Para detener los servicios ejecuta stop.bat
echo  o escribe:  docker compose down
echo.
pause
