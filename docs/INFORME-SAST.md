# Informe — Análisis de Seguridad SAST con Snyk

**Asignatura:** Ingeniería de Software / DevOps
**Práctica:** Integración de una herramienta SAST en el pipeline de CI
**Autor:** Ricardo Hidalgo Pachacama
**Universidad:** Universidad de Las Américas (UDLA)
**Repositorio:** https://github.com/RichardDebugFile/PT-PicoYPlaca (público)

---

## 1. Objetivo

Seleccionar una herramienta **SAST** y añadirla al **pipeline de Integración Continua**
del proyecto PT-PicoYPlaca, ejecutarla sobre el código y documentar sus resultados como
evidencia y artefacto.

## 2. Herramienta seleccionada: Snyk

Se eligió **Snyk** (módulo *Snyk Open Source*, análisis de composición de software / SCA),
que detecta vulnerabilidades conocidas en las **dependencias de terceros** del proyecto.

**Por qué Snyk:**
- El mayor riesgo de este proyecto (Spring Boot + sus transitivas) son las **CVEs de
  dependencias**; Snyk las detecta e indica a qué versión actualizar.
- **Fiable en CI:** consulta su propia base vía API en segundos, sin descargar la base
  completa del NVD (que es lenta e inestable — devuelve HTTP 503).
- **Gratuito** para uso académico, con Action oficial de GitHub y reporte **SARIF**.

> La justificación completa está en [`docs/SAST.md`](SAST.md).

## 3. Integración en el pipeline

Workflow de **GitHub Actions**: [`.github/workflows/snyk.yml`](../.github/workflows/snyk.yml).

Se ejecuta en cada `push`, `pull_request` y manualmente. Pasos:
1. Checkout del repo y JDK 17.
2. Instala el **CLI de Snyk** (`npm install -g snyk`).
3. Ejecuta `snyk test --file=backend/pom.xml --sarif-file-output=snyk-backend.sarif`.
4. Publica el reporte **SARIF** como **artefacto** (`snyk-report`).

Autenticación mediante el secret `SNYK_TOKEN` (token de la cuenta gratuita de Snyk).

## 4. Ejecución y resultados

La corrida del pipeline finalizó en **verde**. Resultado del escaneo:

```
Tested 60 dependencies for known issues, found 89 issues, 89 vulnerable paths.
```

**Hallazgos destacados** (de mayor severidad):

| Severidad | Vulnerabilidad | Dependencia |
|-----------|----------------|-------------|
| 🔴 Critical | Authentication Bypass Using an Alternate Path or Channel | `spring-boot-actuator@3.2.3` |
| 🔴 Critical | Authentication Bypass Using an Alternate Path or Channel | `spring-boot-actuator-autoconfigure@3.2.3` |
| 🟠 High | Incorrect Authorization | `spring-core@6.1.4` |
| 🟠 High | Uncontrolled Recursion | `commons-lang3@3.13.0` |
| 🟠 High | Allocation of Resources Without Limits (ReDoS) | `spring-core@6.1.4` |
| 🟡 Medium | Regular Expression Denial of Service (ReDoS) | `spring-core@6.1.4` |

**Remediación recomendada:** actualizar Spring Boot a una versión parcheada (la 3.2.3 es
antigua) y `commons-lang3` a ≥ 3.18, según indica el propio reporte de Snyk.

**Artefacto generado:** `snyk-report` — archivo `snyk-backend.sarif` (~39 KB) con los 89
hallazgos (ubicación, identificador SNYK/CVE, severidad y versión que corrige).

## 5. Evidencias

> Insertar 3 capturas en `docs/capturas/` con estos nombres.

### 5.1 Repositorio público
![Repositorio público](capturas/01-repo-publico.png)
*El repositorio en GitHub con la etiqueta **Public**.*

### 5.2 Pipeline ejecutado + hallazgos
![Pipeline Snyk en verde con hallazgos](capturas/02-pipeline-hallazgos.png)
*La corrida del workflow de Snyk en **verde**, con el paso del escaneo mostrando
`Tested 60 dependencies ... found 89 issues`.*

### 5.3 Reporte SAST como artefacto
![Artefacto snyk-report](capturas/03-artefacto.png)
*La sección **Artifacts** de la corrida con el reporte `snyk-report` descargable.*

## 6. Conclusiones

- Se integró exitosamente **Snyk** en el pipeline de CI (GitHub Actions); el análisis
  corre automáticamente en cada cambio.
- El escaneo detectó **89 vulnerabilidades** en 60 dependencias, incluidas **2 críticas**
  de *Authentication Bypass* en Spring Boot Actuator — evidencia del valor del análisis SCA.
- La retroalimentación de la herramienta queda como **artefacto SARIF** descargable, con la
  ruta de remediación (versiones a actualizar).
- Se valoró primero OWASP Dependency-Check, pero su dependencia de la API del NVD (caídas
  503, descarga de ~360k CVEs) lo hacía inestable en CI; **Snyk** resultó fiable y rápido.

## 7. Referencias

- Snyk Docs. https://docs.snyk.io
- Snyk GitHub Actions. https://github.com/snyk/actions
- Base de vulnerabilidades de Snyk. https://security.snyk.io
