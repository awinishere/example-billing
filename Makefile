MVN ?= ./mvnw
NAME ?=

.DEFAULT_GOAL := help
.PHONY: help module build test run tree clean db-up db-down

help:
	@echo "make module NAME=<module-name>    generate a Spring Boot + JPA + Lombok module"
	@echo "make build                        ./mvnw -DskipTests package (all modules)"
	@echo "make test                         ./mvnw test (all modules)"
	@echo "make run NAME=<module-name>       run one module with its dependencies, loading only <module>/.env"
	@echo "make tree NAME=<module-name>      dependency:tree of a single module"
	@echo "make db-up                        start docker compose dev services"
	@echo "make db-down                      stop docker compose dev services"
	@echo "make clean                        ./mvnw clean"

module:
	@test -n "$(NAME)" || { echo "usage: make module NAME=example-billing"; exit 2; }
	@sh scripts/new-module.sh "$(NAME)"

build:
	@$(MVN) -DskipTests package

test:
	@$(MVN) test

run:
	@test -n "$(NAME)" || { echo "usage: make run NAME=wonderwrite-authentication"; exit 2; }
	@test -f "$(NAME)/.env" || { echo "missing $(NAME)/.env - each module owns its own env file"; exit 2; }
	@set -a; . "$(NAME)/.env"; set +a; \
	$(MVN) -q -pl $(NAME) -am -DskipTests install && $(MVN) -pl $(NAME) spring-boot:run

tree:
	@test -n "$(NAME)" || { echo "usage: make tree NAME=example-billing"; exit 2; }
	@$(MVN) -pl $(NAME) -am dependency:tree

clean:
	@$(MVN) clean

db-up:
	@docker compose --env-file wonderwrite-authentication/.env up -d

db-down:
	@docker compose --env-file wonderwrite-authentication/.env down
