@echo off
call gradlew.bat spotlessApply
for /f "tokens=*" %%G in ('git diff --cached --name-only --diff-filter=ACM') do git add "%%G"
