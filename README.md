# TaskFlow

Sistema de gerenciamento de funcionários e férias com autenticação e autorização baseada em roles.

## 📋 Índice

- [Sobre o Projeto](#sobre-o-projeto)
- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [Pré-requisitos](#pré-requisitos)
- [Como Executar](#como-executar)
- [Dados Iniciais](#dados-iniciais)
- [Collections HTTP](#collections-http)
- [Documentação da API (Swagger)](#documentação-da-api-swagger)
- [Estrutura do Projeto](#estrutura-do-projeto)

## 🚀 Sobre o Projeto

TaskFlow é uma aplicação completa para gerenciamento de funcionários e solicitações de férias, com controle de acesso baseado em três níveis de permissão:

- **ADMIN**: Acesso total ao sistema
- **MANAGER**: Gerencia equipes e aprova férias
- **EMPLOYEE**: Acessa seus próprios dados e solicita férias

## 🛠️ Tecnologias Utilizadas

### Backend
- Java 17
- Spring Boot 3.x
- Spring Security
- PostgreSQL 16
- Swagger/OpenAPI 3.0

### Frontend
- React
- Nginx

### Infraestrutura
- Docker
- Docker Compose

## 📦 Pré-requisitos

- Docker Desktop instalado e em execução
- Portas disponíveis: 8080 (Backend), 80 (Frontend), 5432 (PostgreSQL)

## 🐳 Como Executar

### Usando Docker (Recomendado)

O projeto inclui um script auxiliar `docker.sh` que facilita o gerenciamento dos containers Docker.

#### Comandos Disponíveis

```bash
# Iniciar os containers
./docker.sh start

# Parar os containers
./docker.sh stop

# Reiniciar os containers
./docker.sh restart

# Visualizar logs de todos os containers
./docker.sh logs

# Visualizar logs de um container específico
./docker.sh logs backend
./docker.sh logs frontend
./docker.sh logs postgres

# Listar containers em execução
./docker.sh ps

# Construir as imagens dos containers
./docker.sh build

# Parar e remover containers, volumes e imagens
./docker.sh clean

# Verificar se o Docker está rodando
./docker.sh check_docker

# Exibir ajuda
./docker.sh help
```

#### Primeira Execução

```bash
# 1. Clone o repositório e navegue até a pasta
cd taskflow

# 2. Inicie os containers
./docker.sh start
```

Aguarde alguns instantes para que todos os serviços sejam iniciados. O backend estará disponível em:
- **Backend API**: http://localhost:8080
- **Frontend**: http://localhost:80
- **PostgreSQL**: localhost:5432

### Variáveis de Ambiente

O arquivo `.env` na raiz do projeto contém as configurações do Docker Compose

# Spring Boot Configuration
SPRING_PROFILES_ACTIVE=docker

# Application Ports
BACKEND_PORT=8080
FRONTEND_PORT=80
```

## 👥 Dados Iniciais

Quando a aplicação é inicializada, são carregados automaticamente dados de exemplo no banco de dados:

### Credenciais de Acesso

**Senha padrão para todos os usuários**: `@@Senha123`

**Login**: Use o email do usuário

### Usuários Pré-cadastrados

#### Administrador
- **Email**: `lionel.messi@example.com`
- **Senha**: `@@Senha123`
- **Role**: ADMIN

#### Gerente
- **Email**: `cristiano.ronaldo@example.com`
- **Senha**: `@@Senha123`
- **Role**: MANAGER

#### Funcionários
Diversos funcionários são carregados automaticamente com nomes de jogadores de futebol famosos. Todos utilizam a senha `@@Senha123` e o formato de email: `nome.sobrenome@example.com`

Exemplos:
- `neymar.junior@example.com`
- `kylian.mbappe@example.com`
- `kevin.debruyne@example.com`
- `erling.haaland@example.com`
- E muitos outros...

## 📡 Collections HTTP

O projeto inclui um arquivo `collections.http` localizado em `taskflow-backend/collections.http` com as principais requisições HTTP para testar a API.

### Como Usar

1. Abra o arquivo `collections.http` em uma IDE compatível (IntelliJ IDEA, VS Code com extensão REST Client)
2. Execute a requisição de login primeiro
3. Copie o token JWT retornado
4. Abra o arquivo `http-client.env.json`
5. Adicione o token na variável `auth.token`:

```json
{
  "dev": {
    "host.url": "http://localhost:8080",
    "auth.token": "Bearer SEU_TOKEN_AQUI"
  }
}
```

### Endpoints Disponíveis

#### Autenticação
- `POST /auth/login` - Realizar login e obter token JWT

#### Employees (Funcionários)
- `POST /employees` - Criar novo funcionário
- `GET /employees` - Listar funcionários (com paginação)
- `GET /employees/{id}` - Buscar funcionário por ID
- `GET /employees/by-first-name/{firstName}` - Buscar funcionário por primeiro nome
- `GET /employees/by-email/{email}` - Buscar funcionário por email
- `PATCH /employees/{id}` - Atualizar funcionário
- `DELETE /employees/{id}` - Deletar funcionário

#### Vacations (Férias)
- `POST /vacations` - Criar solicitação de férias
- `GET /vacations` - Listar solicitações de férias
- `GET /vacations/{id}` - Buscar solicitação de férias por ID
- `PATCH /vacations/{id}/decision` - Aprovar/Rejeitar solicitação de férias
- `DELETE /vacations/{id}` - Deletar solicitação de férias

### Exemplo de Requisições

#### Login
```http
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "email": "lionel.messi@example.com",
  "password": "@@Senha123"
}
```

#### Criar Funcionário
```http
POST http://localhost:8080/employees
Content-Type: application/json
Authorization: Bearer SEU_TOKEN_AQUI

{
  "firstName": "João",
  "lastName": "Silva",
  "email": "joao.silva@example.com",
  "role": "EMPLOYEE",
  "managerId": "ID_DO_MANAGER"
}
```

#### Solicitar Férias
```http
POST http://localhost:8080/vacations
Content-Type: application/json
Authorization: Bearer SEU_TOKEN_AQUI

{
  "startDate": "2026-07-01",
  "endDate": "2026-07-15",
  "requestReason": "Férias de Verão"
}
```

## 📚 Documentação da API (Swagger)

A documentação completa da API está disponível através do Swagger UI.

### Acesso

Com a aplicação rodando, acesse:

**URL**: http://localhost:8080/swagger-ui/index.html

### Recursos do Swagger

- **Visualização completa** de todos os endpoints disponíveis
- **Schemas** detalhados de requisição e resposta
- **Teste interativo** de endpoints diretamente pela interface
- **Autenticação JWT** integrada - clique no botão "Authorize" e adicione seu token

### Como Usar o Swagger

1. Acesse http://localhost:8080/swagger-ui/index.html
2. Faça login através do endpoint `/auth/login` ou use o `collections.http`
3. Copie o token JWT retornado
4. Clique no botão **"Authorize"** no topo da página do Swagger
5. Cole o token no formato: `Bearer SEU_TOKEN_AQUI`
6. Clique em "Authorize" e depois "Close"
7. Agora você pode testar todos os endpoints autenticados diretamente pelo Swagger

## 📁 Estrutura do Projeto

```
taskflow/
├── taskflow-backend/          # Backend Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   ├── build.gradle
│   ├── Dockerfile
│   └── collections.http       # Coleção de requisições HTTP
│
├── taskflow-frontend/         # Frontend React (outro repositorio)
│   └── taskflow/
│       ├── src/
│       ├── public/
│       ├── Dockerfile
│       └── nginx.conf
│
├── docker-compose.yml         # Configuração Docker Compose
├── docker.sh                  # Script auxiliar para Docker
├── .env                       # Variáveis de ambiente
├── create-admin.sh            # Script para criar admin
└── README.md                  # Este arquivo
```

## 🔐 Segurança

- Autenticação via JWT (JSON Web Token)
- Senhas criptografadas com BCrypt
- Autorização baseada em roles (ADMIN, MANAGER, EMPLOYEE)
- Validação de dados em todas as requisições

## 🤝 Contribuindo

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 👨‍💻 Autor

Desenvolvido com ❤️ por Jorge

