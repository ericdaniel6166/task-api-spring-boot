.PHONY: up down run test build compile

up:
	docker compose up -d

down:
	docker compose down -v

run:
	./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

build:
	./mvnw clean package -DskipTests

test:
	./mvnw test

compile:
	./mvnw compile
