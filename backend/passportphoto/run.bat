@ECHO OFF
REM Path to the .env file
SET ENV_FILE="%~dp0.env"

REM Check if the .env file exists
IF NOT EXIST %ENV_FILE% (
    ECHO .env file not found at %ENV_FILE%
    EXIT /B 1
)

REM Read the .env file line by line
FOR /F "usebackq tokens=*" %%i IN (%ENV_FILE%) DO (
    REM Skip empty lines and lines starting with #
    echo %%i
    IF NOT "%%i"=="" IF NOT "%%i:~0,1%"=="#" (
        CALL SET "%%i"
    )
)

@REM .\mvnw package
@REM If you change pom.xml 
CALL .\mvnw clean install && CALL .\mvnw spring-boot:run