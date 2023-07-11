@echo off
echo.
echo [��Ϣ] ���Web���̣�����dist�ļ���
echo.

%~d0
cd %~dp0

cd ..
npm run build:prod

pause