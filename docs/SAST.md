# Análisis de Seguridad (SAST) — Dependencias

## Herramienta seleccionada: Snyk

[Snyk](https://snyk.io/) es una plataforma de seguridad para desarrolladores. Su
módulo **Snyk Open Source (SCA)** analiza las **dependencias de terceros** del proyecto
y las compara contra la **base de vulnerabilidades de Snyk** (Snyk Intel), reportando
qué librerías tienen fallos de seguridad conocidos y cómo remediarlos.

## ¿Por qué Snyk?

| Criterio | Razón |
|----------|-------|
| **Riesgo que cubre** | La mayoría de vulnerabilidades llegan por **dependencias** (Spring y sus transitivas). Snyk las detecta y propone la versión que las corrige. |
| **Fiable y rápido** | Consulta la base de Snyk vía API en segundos; **no descarga** la base completa del NVD (que es lenta e inestable). |
| **Plan gratuito** | El tier *Free* alcanza para un proyecto académico (tests de Open Source). |
| **Remediación** | No solo lista la CVE: indica a qué versión actualizar para resolverla. |
| **Integrable en CI** | Action oficial de GitHub (`snyk/actions`) + dashboard web. |
| **Reporte SARIF** | Genera un reporte estándar SARIF, ideal como **artefacto** de evidencia. |

> Se eligió Snyk frente a un escáner basado en el NVD (como OWASP Dependency-Check)
> por **fiabilidad en CI**: la API pública del NVD sufre caídas (HTTP 503) y la primera
> sincronización descarga ~360k CVEs, lo que hace el pipeline lento e inestable. Snyk
> mantiene su propia base curada y responde por API en segundos.

## Integración en el pipeline

| Elemento | Archivo | Cuándo se ejecuta |
|----------|---------|-------------------|
| **GitHub Actions** | `.github/workflows/snyk.yml` | en cada push, PR y manual |
| Motor | Action oficial `snyk/actions/maven` | dentro del workflow |

El workflow:
1. Hace checkout del repo y configura JDK 17.
2. Ejecuta `snyk test` sobre `backend/pom.xml` (resuelve el árbol de dependencias
   del backend y lo evalúa contra la base de Snyk).
3. Genera un reporte **SARIF** (`snyk-backend.sarif`).
4. Publica ese reporte como **artefacto** descargable (`snyk-report`).

> El escaneo cubre el **backend (Maven)**. Para incluir el frontend (npm) se añade
> un paso con `snyk/actions/node` apuntando a `frontend/`.

## Requisito: SNYK_TOKEN

Snyk se autentica con un token de API (cuenta gratuita):

1. Crear cuenta en https://snyk.io (se puede entrar con la cuenta de GitHub).
2. Obtener el token: **Account settings → Auth Token (API token)**.
3. Guardarlo como **secret** del repositorio con el nombre `SNYK_TOKEN`:
   - GitHub → Settings → Secrets and variables → Actions → *New repository secret*
   - O por CLI: `gh secret set SNYK_TOKEN`

El workflow lo consume con `env: SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}`.

## Retroalimentación de la herramienta (artefacto)

Cada ejecución publica el artefacto **`snyk-report`** con el archivo `snyk-backend.sarif`,
que lista cada dependencia vulnerable, su identificador (CVE/SNYK-ID), la severidad y la
versión que corrige el problema. El dashboard de Snyk (https://app.snyk.io) muestra lo
mismo de forma visual.

## Cómo reproducir el escaneo localmente

```bash
# Instalar la CLI y autenticar (una vez):
npm install -g snyk
snyk auth                       # abre el navegador, o: snyk auth <TU_TOKEN>

# Escanear el backend:
snyk test --file=backend/pom.xml

# Generar el reporte SARIF:
snyk test --file=backend/pom.xml --sarif-file-output=snyk-backend.sarif
```

> Para convertir el pipeline en un *gate* (que falle si hay vulnerabilidades graves)
> se quita `continue-on-error` y se usa `--severity-threshold=high`.
