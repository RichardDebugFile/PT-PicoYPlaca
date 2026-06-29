# Análisis de Seguridad (SAST) — Escaneo de Secretos

## Herramienta seleccionada: Gitleaks

[Gitleaks](https://github.com/gitleaks/gitleaks) es una herramienta de análisis
estático (SAST) especializada en la **detección de secretos** (claves de API, tokens,
contraseñas, claves privadas, *connection strings*) tanto en el código como en **todo
el historial de Git**.

## ¿Por qué Gitleaks?

| Criterio | Razón |
|----------|-------|
| **Tipo de riesgo** | El mayor riesgo de este repo (Terraform, Docker, Kubernetes) es **filtrar credenciales** de la nube. Gitleaks ataca exactamente eso. |
| **Gratuito y open source** | Sin costo de licencia para cuentas personales/académicas. |
| **Escanea el historial** | No solo el último commit: revisa los 21+ commits previos en busca de secretos ya introducidos. |
| **Rápido** | Escaneo completo del repo en < 1 s (Go, sin dependencias). |
| **Nativo de CI/CD** | Integración directa con GitHub Actions, CircleCI y pre-commit. |
| **Configurable** | `.gitleaks.toml` permite extender reglas y definir *allowlists* de falsos positivos. |

> Nota: Gitleaks cubre la categoría de SAST **"secret scanning"**. Para un análisis SAST
> de vulnerabilidades de código (inyección, XSS, etc.) se complementaría con herramientas
> como SonarQube o CodeQL; aquí el foco es la fuga de credenciales, el riesgo principal
> de una base de código orientada a infraestructura.

## Integración en el pipeline (defensa en profundidad)

| Capa | Archivo | Cuándo se ejecuta |
|------|---------|-------------------|
| **GitHub Actions** (principal) | `.github/workflows/gitleaks.yml` | en cada push y Pull Request |
| **CircleCI** | job `secret-scan` en `.circleci/config.yml` | en cada ejecución del pipeline |
| **pre-commit** (local) | `.pre-commit-config.yaml` | antes de cada commit en la máquina del dev |
| **Configuración** | `.gitleaks.toml` | reglas por defecto + allowlist |

La capa **principal** es GitHub Actions: corre del lado del servidor y **no se puede
omitir**, a diferencia del hook local de pre-commit (saltable con `--no-verify`).

## Retroalimentación de la herramienta (artefacto)

El workflow de GitHub Actions genera un reporte en formato **SARIF**
(`gitleaks-report.sarif`) y lo publica como **artefacto** descargable de cada ejecución.
Ese archivo es la retroalimentación de la herramienta: lista las posibles fugas
(ubicación, regla, commit). Si no hay hallazgos, el reporte queda vacío y el pipeline
pasa en verde.

## Cómo reproducir el escaneo localmente

```bash
# Descargar gitleaks (o usar el binario del release v8.30.1)
gitleaks detect --source . --config .gitleaks.toml --redact --verbose

# Generar el reporte como artefacto
gitleaks detect --source . --config .gitleaks.toml \
  --report-format sarif --report-path gitleaks-report.sarif
```

## Resultado del escaneo inicial

```
21 commits scanned.
scanned ~949.73 KB in 832ms
no leaks found
```

El repositorio está limpio: no se detectaron secretos en el historial.
