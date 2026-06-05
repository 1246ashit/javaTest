@echo off
REM ============================================================
REM  TextCombat - quick start script
REM  Launches the Spring Boot backend and the Vite frontend,
REM  each in its own terminal window.
REM ============================================================

REM Always run relative to this script's location, no matter where it's called from.
cd /d "%~dp0"

echo ============================================================
echo  TextCombat dev launcher
echo ============================================================
echo.
echo  Backend : textcombat-api  (Spring Boot, HTTPS https://localhost:8443)
echo  Frontend: textcombat-web  (Vite, http://localhost:5173)
echo.
echo  NOTE: The backend needs these external services running first:
echo        - PostgreSQL  (source of truth)
echo        - Redis       (room state + Redisson lock)
echo        - Kafka       (lobby event broadcast)
echo  If any of them is down, the backend will fail to start.
echo.

REM ---- Sanity check: backend .env must exist (POSTGRES_PASSWORD / JWT_SECRET) ----
if not exist "textcombat-api\.env" (
    echo [WARN] textcombat-api\.env not found.
    echo        Create it with POSTGRES_PASSWORD and JWT_SECRET before starting the backend.
    echo.
)

REM ---- Frontend dependencies: install on first run ----
if not exist "textcombat-web\node_modules" (
    echo [INFO] Frontend dependencies not found. Running "npm install"...
    pushd "textcombat-web"
    call npm install
    popd
    echo.
)

REM ---- Launch backend in a new window (uses globally installed Maven) ----
REM NOTE: the project's Maven wrapper (.mvn/wrapper) is incomplete, so we call
REM       global "mvn" directly. Make sure Maven is on your PATH.
echo [INFO] Starting backend...
start "TextCombat API" cmd /k "cd /d "%~dp0textcombat-api" && mvn spring-boot:run"

REM ---- Launch frontend in a new window ----
echo [INFO] Starting frontend...
start "TextCombat Web" cmd /k "cd /d "%~dp0textcombat-web" && npm run dev"

echo.
echo [DONE] Two windows opened. Backend takes a bit longer to boot.
echo        Open the frontend at: http://localhost:5173
echo        (Close either window to stop that process.)
echo.
pause
