@echo off
echo.
echo [1/3] Checking GitHub login status...
gh auth status >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ! You are not logged in to GitHub yet.
    echo.
    echo I am opening the browser for you. 
    echo The 8-character code has been copied to your clipboard!
    echo Please paste it into the browser when prompted.
    echo.
    gh auth login --web -h GitHub.com -p https --clipboard
)

echo.
echo [2/3] Adding your changes...
git add .

echo.
echo [3/3] Sending code to techmaster300/andrdscren...
git commit -m "Update from sync tool" >nul 2>&1
git push origin main

echo.
echo Done! Your code is on GitHub.
echo You can check the build here: https://github.com/techmaster300/andrdscren/actions
pause
