set -a
source .env
set +a


./mvnw clean install && echo "First command successful, running second command..." && ./mvnw spring-boot:run