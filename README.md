# PT-PicoYPlaca

Aplicacion web de dos capas para validar si un vehiculo puede circular segun la normativa de **Pico y Placa** vigente en el Distrito Metropolitano de Quito, Ecuador (Res. AQ-013-2023 &mdash; AMT Quito).

---

## Arquitectura

```
┌─────────────────────┐         HTTP/REST          ┌──────────────────────┐
│   Frontend          │ ─────────────────────────> │   Backend            │
│   Angular 18        │ <───────────────────────── │   Spring Boot 3.2.x  │
│   Puerto: 4200/80   │         JSON               │   Puerto: 8080       │
└─────────────────────┘                            └──────────────────────┘
```

| Capa     | Tecnologia           | Puerto |
|----------|----------------------|--------|
| Frontend | Angular 18 + Angular Material | 4200 (dev) / 80 (prod) |
| Backend  | Spring Boot 3.2 + Java 17     | 8080 |

---

## Requisitos previos

| Herramienta  | Version minima | Necesario para |
|--------------|----------------|----------------|
| Java JDK     | 17             | Backend (sin Docker) |
| Maven        | 3.9            | Backend (sin Docker) |
| Node.js      | 18+            | Frontend (sin Docker) |
| npm          | 9+             | Frontend (sin Docker) |
| Angular CLI  | 18             | Frontend (sin Docker) |
| Docker       | 24             | Despliegue con Docker |
| Docker Compose | 2.x          | Despliegue con Docker |

---

## Opcion 1: Script de un clic (Windows)

Si estas en Windows con Docker Desktop instalado, simplemente haz doble clic en:

| Script | Accion |
|--------|--------|
| `start.bat` | Compila, inicia los contenedores y abre el navegador automaticamente |
| `stop.bat`  | Detiene y elimina los contenedores |

---

## Opcion 2: Ejecutar con Docker Compose (recomendado para produccion)

```bash
# 1. Clonar el repositorio
git clone https://github.com/TU_USUARIO/PT-PicoYPlaca.git
cd PT-PicoYPlaca

# 2. Levantar backend + frontend
docker compose up --build

# 3. Abrir en el navegador
#    Frontend: http://localhost
#    Backend API: http://localhost:8080
#    Swagger UI: http://localhost:8080/swagger-ui.html
```

Para detener los servicios:
```bash
docker compose down
```

---

## Opcion 3: Ejecutar en modo desarrollo (sin Docker)

### Backend

```bash
cd backend

# Compilar y ejecutar
mvn spring-boot:run

# La API estara disponible en:
#   http://localhost:8080/api/v1/pico-placa/verificar
#   http://localhost:8080/swagger-ui.html
```

### Frontend

```bash
cd frontend

# Instalar dependencias
npm install

# Ejecutar en modo desarrollo (ng no esta instalado globalmente: usar npx)
npx @angular/cli@18 serve

# Abrir: http://localhost:4200
```

---

## Opcion 4: Build manual de produccion (avanzado)

### Backend

```bash
cd backend
mvn clean package -DskipTests
java -jar target/picoyplaca-backend-1.0.0.jar --spring.profiles.active=prod
```

### Frontend

```bash
cd frontend
npm install
npm run build -- --configuration=production
# Archivos generados en: dist/frontend/browser/
# Sirvalos con cualquier servidor HTTP estatico (nginx, Apache, etc.)
```

---

## Normativa implementada

> **Alcance geografico:** Esta aplicacion implementa la normativa del **Distrito Metropolitano de Quito (DMQ)**
> segun la Resolucion AQ-013-2023 de la AMT Quito. Otras ciudades del Ecuador (Guayaquil, Cuenca, etc.)
> tienen sus propias reglas de Pico y Placa con horarios y digitos distintos; esta herramienta **no aplica** para ellas.

| Dia        | Digitos restringidos | Horario manana | Horario tarde |
|------------|----------------------|----------------|---------------|
| Lunes      | 1, 2                 | 06:00 - 09:30  | 16:00 - 20:00 |
| Martes     | 3, 4                 | 06:00 - 09:30  | 16:00 - 20:00 |
| Miercoles  | 5, 6                 | 06:00 - 09:30  | 16:00 - 20:00 |
| Jueves     | 7, 8                 | 06:00 - 09:30  | 16:00 - 20:00 |
| Viernes    | 9, 0                 | 06:00 - 09:30  | 16:00 - 20:00 |
| Sabado     | Sin restriccion      | &mdash;         | &mdash;       |
| Domingo    | Sin restriccion      | &mdash;         | &mdash;       |

> Se toma el **ultimo digito numerico** de la placa (ignorando letras al final).
> Ejemplo: placa `ABC-1234` -> ultimo digito: **4**

---

## Endpoints de la API

### POST /api/v1/pico-placa/verificar

```json
// Request
{
  "placa": "ABC-1234",
  "fechaHora": "2026-03-05T08:00:00"
}

// Response 200 OK
{
  "placa": "ABC-1234",
  "fechaHora": "2026-03-05T08:00:00",
  "diaSemana": "JUEVES",
  "puedeCircular": false,
  "mensaje": "El vehiculo con placa ABC-1234 NO puede circular...",
  "digitosRestringidosHoy": "7 y 8",
  "franjaHorariaRestriccion": "06:00 - 09:30"
}
```

### GET /api/v1/pico-placa/reglas

Retorna las reglas de restriccion configuradas en el sistema.

Documentacion interactiva (Swagger UI): `http://localhost:8080/swagger-ui.html`

---

## Ejecutar tests

```bash
cd backend
mvn test
```

---

## Estructura del repositorio

```
PT-PicoYPlaca/
├── backend/                  <- Spring Boot 3.2 + Maven
│   ├── src/
│   │   ├── main/java/com/picoyplaca/
│   │   │   ├── PicoYPlacaApplication.java
│   │   │   ├── controller/PicoPlacaController.java
│   │   │   ├── service/PicoPlacaService.java
│   │   │   ├── dto/           (ConsultaRequest, ConsultaResponse, ErrorResponse, ReglasResponse)
│   │   │   ├── exception/     (PlacaInvalidaException, FechaAnteriorException, GlobalExceptionHandler)
│   │   │   └── config/        (CorsConfig, OpenApiConfig)
│   │   └── test/java/com/picoyplaca/
│   │       ├── service/PicoPlacaServiceTest.java      (58 tests unitarios)
│   │       ├── controller/PicoPlacaControllerTest.java (15 tests @WebMvcTest)
│   │       └── integration/PicoPlacaIntegrationTest.java (16 tests @SpringBootTest)
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                 <- Angular 18 + Angular Material
│   ├── src/
│   │   ├── app/
│   │   │   ├── components/
│   │   │   │   ├── consulta-form/
│   │   │   │   └── resultado/
│   │   │   ├── services/pico-placa.service.ts
│   │   │   ├── models/pico-placa.model.ts
│   │   │   └── environments/
│   │   └── index.html
│   ├── Dockerfile
│   └── nginx.conf
├── docker-compose.yml
├── start.bat                 <- Inicia todo con un doble clic (Windows)
├── stop.bat                  <- Detiene los contenedores
├── .gitignore
└── README.md
```

---

## Consideraciones de produccion

- **CORS**: configurable via variable de entorno `CORS_ALLOWED_ORIGINS`
- **Perfil Spring**: activa `prod` automaticamente via Docker Compose
- **Imagen Docker**: multi-stage build para reducir tamano final (~200MB backend, ~50MB frontend)
- **Nginx**: proxy inverso integrado; los requests a `/api/` se reenvian al backend sin exponer el puerto 8080 publicamente
- **Seguridad HTTP**: headers `X-Content-Type-Options`, `X-Frame-Options`, `X-XSS-Protection` en nginx
- **Health check**: Docker Compose verifica el estado del backend antes de levantar el frontend

---

## Licencia

MIT
