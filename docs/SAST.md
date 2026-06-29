# Análisis de Seguridad (SAST) — Dependencias

## Herramienta seleccionada: OWASP Dependency-Check

[OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/) es una
herramienta de **Análisis de Composición de Software (SCA)**, una rama del análisis
estático (SAST). Inspecciona las **dependencias de terceros** del proyecto y las
compara contra la base de datos pública de vulnerabilidades **NVD (CVE)**, reportando
qué librerías tienen fallos de seguridad conocidos.

## ¿Por qué OWASP Dependency-Check?

| Criterio | Razón |
|----------|-------|
| **Riesgo que cubre** | La mayoría de vulnerabilidades llegan por **dependencias** (Spring, Angular y sus transitivas). DC detecta esas CVEs. |
| **Gratuito y open source** | Proyecto de OWASP, sin licencia ni cuenta de pago. |
| **Ecosistema Java/Maven** | Analiza el **backend (Spring Boot)** y todas sus dependencias transitivas. |
| **Sin servicios externos** | No envía el código a la nube; descarga la base NVD y analiza localmente en el runner. |
| **Reporte rico** | Genera HTML, JSON, XML, CSV y SARIF: ideal como **artefacto** de evidencia. |
| **Integrable en CI** | Action oficial de GitHub + plugin de Maven. |

> SCA vs SAST de código: DC no busca bugs en *tu* código (eso lo haría SonarQube/CodeQL),
> sino vulnerabilidades en las **librerías** que usas — el vector de ataque más común en
> aplicaciones modernas (ej. Log4Shell). Es el análisis más relevante para un proyecto
> con muchas dependencias como este.

## Integración en el pipeline

| Elemento | Archivo | Cuándo se ejecuta |
|----------|---------|-------------------|
| **GitHub Actions** | `.github/workflows/dependency-check.yml` | en cada push, PR y manual |
| Motor | plugin oficial `org.owasp:dependency-check-maven` (v12.2.2) | dentro del workflow |

El workflow:
1. Hace checkout del repo y configura JDK 17.
2. Ejecuta el plugin de Maven `dependency-check:check` sobre `backend/pom.xml`,
   que resuelve el árbol de dependencias del backend y lo evalúa contra el NVD.
3. Genera el reporte en `backend/target/` en formato `ALL`
   (HTML, JSON, XML, CSV, SARIF).
4. Publica `dependency-check-report.*` como **artefacto** descargable.

> El escaneo cubre el **backend (Maven)**, donde están las dependencias del servidor
> (Spring Boot y transitivas). Para incluir el frontend (npm) se puede añadir un paso
> con la CLI de Dependency-Check apuntando a `frontend/package-lock.json`.

## Requisito: NVD API Key

Dependency-Check descarga la base de CVEs del NVD. Sin una API key el proceso es muy
lento y suele fallar por *rate-limiting*. La key es **gratuita e inmediata**:

1. Solicitarla en https://nvd.nist.gov/developers/request-an-api-key (llega por correo).
2. Guardarla como **secret** del repositorio con el nombre `NVD_API_KEY`:
   - GitHub → Settings → Secrets and variables → Actions → *New repository secret*
   - O por CLI: `gh secret set NVD_API_KEY`

El workflow la consume con `--nvdApiKey ${{ secrets.NVD_API_KEY }}`.

## Retroalimentación de la herramienta (artefacto)

Cada ejecución publica el artefacto **`dependency-check-report`** con el reporte en
varios formatos. El `dependency-check-report.html` es el más legible: lista cada
dependencia vulnerable, su CVE, la severidad (CVSS) y la recomendación de actualización.

## Cómo reproducir el escaneo localmente

```bash
# Backend (plugin de Maven), requiere la NVD API key:
mvn -f backend/pom.xml org.owasp:dependency-check-maven:12.2.2:check \
  -Dformat=ALL -DnvdApiKey=<TU_API_KEY>
# Reporte en: backend/target/dependency-check-report.html

# Opcional, incluir el frontend con la CLI:
dependency-check --project PT-PicoYPlaca --scan frontend \
  --format ALL --out reports --nvdApiKey <TU_API_KEY>
```

> Para convertir el pipeline en un *gate* (que falle si hay CVEs graves) se agrega
> `--failOnCVSS 7` a los argumentos del workflow.
