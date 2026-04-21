@echo off
chcp 65001 >nul
cd /d "%~dp0"
call mvn org.springframework.boot:spring-boot-maven-plugin:3.5.0:run
