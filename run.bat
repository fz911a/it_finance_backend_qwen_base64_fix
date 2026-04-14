@echo off
chcp 65001 >nul
call mvn org.springframework.boot:spring-boot-maven-plugin:3.5.0:run
