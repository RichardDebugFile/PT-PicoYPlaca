# Pipeline CircleCI - PT-PicoYPlaca

Documentación del pipeline CI/CD definido en `.circleci/config.yml`.
Traducción del `Jenkinsfile` original al motor de CircleCI 2.1.

---

## 1. Objetivos cubiertos

| Requisito | Implementación |
|-----------|----------------|
| Configurar proyecto en CircleCI con múltiples jobs y workflows | 8 jobs + 2 workflows (`ci-pipeline`, `nightly`) |
| Investigar paralelismo en CircleCI e implementar pruebas en paralelo | Dos niveles: paralelismo de jobs en workflow + `parallelism: N` con `circleci tests split` dentro de `backend-test` |
| Configurar envío de notificaciones para diferentes estados del pipeline (acciones post) | Slack orb con `event: fail` por job y job final `notify-success` con `event: always` y bloque enriquecido |

---

## 2. Estructura general del archivo

```
parameters/      → Constantes centralizadas (imágenes, cron, parallelism)
x-anchors/       → YAML anchors para reutilizar bloques (context, filters)
orbs/            → Slack orb para notificaciones
executors/       → Java, Node y Machine (Docker build)
commands/        → Bloques reutilizables (caches Maven/npm, notify-fail)
jobs/            → 8 jobs independientes
workflows/       → ci-pipeline (push) + nightly (cron)
```

---

## 3. Pipeline parameters

Constantes no-secretas declaradas al inicio. Cambiar aquí afecta todo el pipeline.
También permiten override en runtime vía API:

```bash
curl -X POST -H "Circle-Token: $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"branch":"main","parameters":{"test_parallelism":4}}' \
  https://circleci.com/api/v2/project/gh/<org>/<repo>/pipeline
```

| Parameter | Default | Uso |
|-----------|---------|-----|
| `java_image` | `cimg/openjdk:17.0` | Imagen Docker para jobs Java |
| `node_image` | `cimg/node:18.20` | Imagen Docker para jobs Node |
| `machine_image` | `ubuntu-2204:current` | Imagen para `machine:` (Docker build) |
| `nightly_cron` | `0 6 * * *` | Cron del workflow nightly |
| `test_parallelism` | `2` | Contenedores en paralelo para `backend-test` |

---

## 4. YAML Anchors

Definidos en la clave custom `x-anchors:` (ignorada por CircleCI, parseada por YAML).

```yaml
x-anchors:
  slack_context: &slack_ctx
    context: [slack-secrets]
  deploy_branches: &deploy_filters
    filters:
      branches:
        only: [main, develop]
```

Uso en workflows:

```yaml
- backend-test:
    <<: *slack_ctx                       # inyecta context
- docker-backend:
    <<: [*slack_ctx, *deploy_filters]    # inyecta context + filtro de rama
```

Beneficio: si cambias el nombre del context, una sola edición lo propaga a todos los jobs.

---

## 5. Variables sensibles (Context)

Tokens y secretos NO van en el archivo. Viven en CircleCI UI:

`Organization Settings` → `Contexts` → `slack-secrets`

| Variable | Ejemplo |
|----------|---------|
| `SLACK_ACCESS_TOKEN` | `xoxb-...` (Bot OAuth Token de Slack app) |
| `SLACK_DEFAULT_CHANNEL` | `C0XXXXX` (Channel ID, no nombre) |

El context `slack-secrets` se inyecta en cada job que lo declara y expone estas variables como variables de entorno al runtime.

---

## 6. Jobs

| Job | Executor | Función |
|-----|----------|---------|
| `backend-test` | Java | Ejecuta tests Maven con paralelismo (split por timings) |
| `frontend-lint` | Node | `npm ci` + ESLint |
| `backend-checkstyle` | Java | Análisis estático Checkstyle (artifact XML) |
| `backend-build` | Java | `mvn package` → JAR (workspace + artifact) |
| `frontend-build` | Node | `ng build --configuration=production` → `dist/` |
| `docker-backend` | Machine | `docker build` imagen backend (solo main/develop) |
| `docker-frontend` | Machine | `docker build` imagen frontend (solo main/develop) |
| `notify-success` | base | Slack con bloque enriquecido (botón "Ver build") |

Cada job (excepto `notify-success`) incluye `notify-slack-fail` como último step. Slack orb solo dispara cuando un step previo falla.

---

## 7. Paralelismo (dos niveles)

### 7.1 Workflow-level (jobs concurrentes)

Jobs sin `requires` mutuo arrancan al mismo tiempo. Ejemplo en `ci-pipeline`:

```
ronda 1:  backend-test       ║  frontend-lint       ← arrancan a la vez
ronda 2:  backend-checkstyle (espera backend-test)
ronda 3:  backend-build      ║  frontend-build      ← paralelo
ronda 4:  docker-backend     ║  docker-frontend     ← paralelo (main/develop)
ronda 5:  notify-success
```

### 7.2 Job-level (parallelism splitting)

`backend-test` declara `parallelism: 2`. CircleCI levanta 2 contenedores idénticos. El comando:

```bash
TEST_CLASSES=$(find src/test/java -name "*Test.java" \
  | sed 's@src/test/java/@@; s@/@.@g; s@\.java$@@' \
  | circleci tests split --split-by=timings)
```

distribuye las clases de test entre los contenedores. La primera ejecución usa fallback `--split-by=name`. Las siguientes usan timings históricos para balancear el tiempo total.

Resultado: el job termina en aproximadamente la mitad del tiempo si las clases están bien balanceadas.

---

## 8. Workflows

### 8.1 `ci-pipeline`

Se dispara en cualquier push (todas las ramas). Equivalente al Multibranch Pipeline de Jenkins.

Diagrama de dependencias:

```
backend-test ──┬─→ backend-checkstyle ──┐
               │                        ├─→ backend-build ─┬─→ docker-backend ─┐
frontend-lint ─┼────────────────────────┘                  │                   ├─→ notify-success
               └─→ frontend-build ─────────────────────────┴─→ docker-frontend ┘
```

`docker-*` solo en `main` y `develop` (filtro de rama).

### 8.2 `nightly`

Se dispara por cron a las 06:00 UTC sobre `main`. Útil para detectar regresiones que no aparecieron en los pushes del día (dependencias actualizadas, drift en imágenes base, etc).

```yaml
triggers:
  - schedule:
      cron: << pipeline.parameters.nightly_cron >>
      filters:
        branches:
          only: [main]
```

---

## 9. Notificaciones Slack (acciones post)

### 9.1 Falla de cualquier job

Cada job termina con el command `notify-slack-fail`:

```yaml
notify-slack-fail:
  steps:
    - slack/notify:
        event: fail
        template: basic_fail_1
```

Slack orb dispara el mensaje solo si un step anterior falló (`event: fail`). Se envía al canal configurado en `SLACK_DEFAULT_CHANNEL` con un link directo al build roto.

### 9.2 Pipeline exitoso

Job final `notify-success` corre solo si `backend-build` y `frontend-build` pasaron. Envía un bloque Slack enriquecido (Block Kit) con:

- Header: nombre del proyecto + estado
- Campos: rama, número de build, commit, autor
- Botón: link directo al build en CircleCI

### 9.3 Estados cubiertos

| Estado | Notificación |
|--------|--------------|
| Job individual falla | `basic_fail_1` (template del orb) |
| Pipeline completo OK | Bloque custom con datos y botón |
| Pipeline parcial (algunos jobs OK, otro falla) | El job fallido emite `basic_fail_1` y `notify-success` no corre por `requires` |

---

## 10. Caches

Maven y npm cachean dependencias entre runs:

```yaml
restore_cache:
  keys:
    - v1-maven-{{ checksum "backend/pom.xml" }}
    - v1-maven-                      # fallback si pom no matchea
```

La primera key intenta hit exacto por checksum del manifiesto. Si cambia el manifiesto, cae al fallback parcial y guarda una key nueva al final del job.

---

## 11. Workspaces

`backend-build` y `frontend-build` persisten artifacts (JAR, `dist/`) en un workspace. Los jobs `docker-*` los recogen con `attach_workspace` para evitar recompilar. Independiente del cache (que es para deps de build, no para artifacts entre jobs).

---

## 12. Cómo probar el pipeline

### Validar localmente

```bash
circleci config validate .circleci/config.yml
circleci config process .circleci/config.yml > expanded.yml   # ver YAML resuelto
```

### Disparar desde push

```bash
git push origin <rama>
```

CircleCI detecta el archivo y lanza `ci-pipeline` automáticamente.

### Verificación visual en UI

1. Pipelines → última run de la rama
2. Click en un job → tab `Tests` (resultados JUnit)
3. Click `backend-test` → tab `Parallel runs` → ver 2 índices con clases distintas
4. Tab `Artifacts` → descargar JAR y `dist/`
5. Canal Slack → mensaje de éxito o fallo

### Disparar nightly manualmente

`Project Settings` → `Triggers` → workflow `nightly` → `Trigger Pipeline`.

---

## 13. Diferencias clave vs Jenkinsfile

| Concepto Jenkins | Equivalente CircleCI |
|------------------|----------------------|
| `agent any` | `executor:` |
| `stage('X') { steps { ... } }` | `jobs.X:` |
| `parallel { stage A; stage B }` | Jobs sin `requires` mutuo en workflow |
| `when { branch 'main' }` | `filters.branches.only` |
| `archiveArtifacts` | `store_artifacts` |
| `junit '*.xml'` | `store_test_results` |
| `post { always { cleanWs() } }` | No necesario (cada job arranca limpio) |
| `def runMaven()` reutilizable | `commands:` reutilizables |
| Multibranch Pipeline | Comportamiento por defecto (corre en todas las ramas salvo filtros) |

---

## 14. Próximos pasos sugeridos

- Push de imágenes Docker a registry (Docker Hub / GHCR) tras `docker-*`
- Job de deploy a entorno staging tras merge a `develop`
- Tests E2E con Cypress/Playwright en un job separado
- Análisis SonarQube como job paralelo a `backend-checkstyle`
- Notificación a otro canal Slack para fallos en `main` (canal de oncall)
