@echo off
REM Compile TV Live ^& Replay en TVLive.exe (Windows)
python -m pip install -r requirements-dev.txt || exit /b 1
python -m PyInstaller tvlive.spec --clean --noconfirm || exit /b 1
echo.
echo Executable genere dans dist\TVLive.exe
pause
