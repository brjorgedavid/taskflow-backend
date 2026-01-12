#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"

DOCKER_CMD=""
if command -v docker &> /dev/null; then
    DOCKER_CMD="docker"
elif [ -f "/usr/local/bin/docker" ]; then
    DOCKER_CMD="/usr/local/bin/docker"
elif [ -f "/Applications/Docker.app/Contents/Resources/bin/docker" ]; then
    DOCKER_CMD="/Applications/Docker.app/Contents/Resources/bin/docker"
else
    echo -e "\033[0;31m[ERROR]\033[0m Docker não encontrado. Por favor, certifique-se de que o Docker Desktop está instalado."
    exit 1
fi

print_success() {
    echo -e "\033[0;32m[SUCCESS]\033[0m $1"
}

print_error() {
    echo -e "\033[0;31m[ERROR]\033[0m $1"
}

print_info() {
    echo -e "\033[0;34m[INFO]\033[0m $1"
}

check_docker() {
    if ! $DOCKER_CMD info > /dev/null 2>&1; then
        print_error "Docker não está rodando. Por favor, inicie o Docker e tente novamente."
        exit 1
    fi
}

start_containers() {
    print_info "Iniciando containers..."
    check_docker
    $DOCKER_CMD compose -f "$DOCKER_COMPOSE_FILE" up -d
    print_success "Containers iniciados com sucesso!"
    print_info "Backend: http://localhost:8080"
    print_info "Frontend: http://localhost:80"
    print_info "PostgreSQL: localhost:5432"
}

stop_containers() {
    print_info "Parando containers..."
    check_docker
    $DOCKER_CMD compose -f "$DOCKER_COMPOSE_FILE" down
    print_success "Containers parados com sucesso!"
}

restart_containers() {
    print_info "Reiniciando containers..."
    stop_containers
    start_containers
}

view_logs() {
    check_docker
    if [ -z "$2" ]; then
        print_info "Visualizando logs de todos os containers..."
        $DOCKER_CMD compose -f "$DOCKER_COMPOSE_FILE" logs -f
    else
        print_info "Visualizando logs do container: $2"
        $DOCKER_CMD compose -f "$DOCKER_COMPOSE_FILE" logs -f "$2"
    fi
}

list_containers() {
    check_docker
    print_info "Containers em execução:"
    $DOCKER_CMD compose -f "$DOCKER_COMPOSE_FILE" ps
}

build_containers() {
    print_info "Construindo containers..."
    check_docker
    $DOCKER_CMD compose -f "$DOCKER_COMPOSE_FILE" build
    print_success "Containers construídos com sucesso!"
}

clean_all() {
    print_info "Parando e removendo todos os containers, volumes e imagens..."
    check_docker
    $DOCKER_CMD compose -f "$DOCKER_COMPOSE_FILE" down -v --rmi all
    print_success "Limpeza completa realizada!"
}

show_help() {
    echo "Uso: ./docker.sh [COMANDO]"
    echo ""
    echo "Comandos disponíveis:"
    echo "  start           - Inicia os containers"
    echo "  stop            - Para os containers"
    echo "  restart         - Reinicia os containers"
    echo "  logs [service]  - Visualiza os logs (opcionalmente de um serviço específico)"
    echo "  ps              - Lista os containers em execução"
    echo "  build           - Constrói as imagens dos containers"
    echo "  clean           - Para e remove containers, volumes e imagens"
    echo "  check_docker    - Verifica se o Docker está rodando"
    echo "  help            - Mostra esta mensagem de ajuda"
}

case "$1" in
    start)
        start_containers
        ;;
    stop)
        stop_containers
        ;;
    restart)
        restart_containers
        ;;
    logs)
        view_logs "$@"
        ;;
    ps)
        list_containers
        ;;
    build)
        build_containers
        ;;
    clean)
        clean_all
        ;;
    check_docker)
        check_docker
        print_success "Docker está rodando!"
        ;;
    help|--help|-h|"")
        show_help
        ;;
    *)
        print_error "Comando desconhecido: $1"
        echo ""
        show_help
        exit 1
        ;;
esac

