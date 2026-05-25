# PT-PicoYPlaca - Despliegue Kubernetes (Minikube)

Despliegue del proyecto en un clúster Kubernetes local usando Minikube.
Equivalente al `docker-compose.yml` pero orquestado por Kubernetes.

---

## Archivos

| Archivo | Recurso | Propósito |
|---------|---------|-----------|
| `namespace.yaml` | Namespace | Aísla el proyecto (`picoyplaca`) |
| `backend-deployment.yaml` | Deployment + ReplicaSet | 2 réplicas de la API Spring Boot |
| `backend-service.yaml` | Service (ClusterIP) | DNS interno `backend:8080` |
| `frontend-deployment.yaml` | Deployment + ReplicaSet | 2 réplicas de Angular + nginx |
| `frontend-service.yaml` | Service (NodePort) | Acceso externo en puerto `30080` |
| `backend-hpa.yaml` | HorizontalPodAutoscaler | Auto-escalado 2-5 pods según CPU/mem |
| `deploy.ps1` | Script | Build + apply automatizado |

---

## Conceptos clave

### Deployment

Objeto de alto nivel que **declara** el estado deseado de la aplicación. Gestiona ReplicaSets internamente.

**Usos**:
- **Rolling updates** sin downtime (`strategy.type: RollingUpdate`)
- **Rollback** a versión previa: `kubectl rollout undo deployment/backend`
- **Pause/resume** despliegues
- **Escalado declarativo**: cambiar `replicas:` y aplicar
- **Historial** de revisiones (`kubectl rollout history`)

### ReplicaSet

Garantiza que **N pods idénticos** estén corriendo en todo momento.

**Usos**:
- **Auto-recreación** si un pod muere o el nodo falla
- **Escalado horizontal** (manual con `kubectl scale` o automático con HPA)
- **Distribución** entre nodos del clúster
- **Reemplazo controlado** durante actualizaciones

**Nota importante**: nunca se crea un ReplicaSet directamente. El Deployment lo crea y gestiona automáticamente.

### Diferencia Deployment vs ReplicaSet

| Aspecto | ReplicaSet | Deployment |
|---------|------------|------------|
| Replica pods | Sí | Sí (via RS) |
| Rolling update | No | Sí |
| Rollback | No | Sí |
| Historial | No | Sí |
| Uso directo | Raro | Estándar |

---

## Pre-requisitos

```powershell
# Instalar minikube
winget install Kubernetes.minikube
# o: choco install minikube
# o: scoop install minikube

# Verificar
minikube version
kubectl version --client
```

---

## Despliegue

### Opción A — Script automatizado

```powershell
pwsh -File k8s/deploy.ps1
```

### Opción B — Pasos manuales

```powershell
# 1. Arrancar minikube
minikube start --driver=docker --memory=4096 --cpus=2
minikube addons enable metrics-server

# 2. Apuntar Docker al daemon de minikube
& minikube docker-env --shell powershell | Invoke-Expression

# 3. Build imágenes dentro del cluster
docker build -t picoyplaca-backend:latest ./backend
docker build -t picoyplaca-frontend:latest ./frontend

# 4. Aplicar manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/ -n picoyplaca

# 5. Verificar
kubectl get pods,svc,deploy,rs -n picoyplaca
```

### Acceso al frontend

```powershell
minikube service frontend -n picoyplaca
```

Abre el navegador en la URL asignada (puerto 30080).

---

## Evidencia de Pods y Services funcionando

```powershell
# Ver todos los recursos
kubectl get all -n picoyplaca

# Pods con detalle
kubectl get pods -n picoyplaca -o wide

# Services con endpoints
kubectl get svc,endpoints -n picoyplaca

# Logs en tiempo real
kubectl logs -f -l app=backend -n picoyplaca

# Describir un pod específico
kubectl describe pod -l app=backend -n picoyplaca
```

Salida esperada:

```
NAME                            READY   STATUS    RESTARTS   AGE
pod/backend-7d4f8c9b5-abc12     1/1     Running   0          2m
pod/backend-7d4f8c9b5-def34     1/1     Running   0          2m
pod/frontend-6b8c7d6f4-xyz56    1/1     Running   0          2m
pod/frontend-6b8c7d6f4-uvw78    1/1     Running   0          2m

NAME                TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)
service/backend     ClusterIP   10.96.123.45    <none>        8080/TCP
service/frontend    NodePort    10.96.234.56    <none>        80:30080/TCP

NAME                       READY   UP-TO-DATE   AVAILABLE
deployment.apps/backend    2/2     2            2
deployment.apps/frontend   2/2     2            2

NAME                                  DESIRED   CURRENT   READY
replicaset.apps/backend-7d4f8c9b5     2         2         2
replicaset.apps/frontend-6b8c7d6f4    2         2         2
```

---

## Evidencia de escalamiento

### Escalado manual

```powershell
# Subir a 5 réplicas
kubectl scale deployment backend --replicas=5 -n picoyplaca

# Ver el cambio en tiempo real
kubectl get pods -n picoyplaca -w

# Bajar a 1 réplica
kubectl scale deployment backend --replicas=1 -n picoyplaca

# Verificar ReplicaSet actualizado
kubectl get rs -n picoyplaca
```

### Escalado automático (HPA)

```powershell
# Aplicar HPA (ya incluido en deploy.ps1)
kubectl apply -f k8s/backend-hpa.yaml

# Ver estado del HPA
kubectl get hpa -n picoyplaca

# Generar carga para forzar escalado
kubectl run -n picoyplaca load --rm -it --image=busybox -- /bin/sh -c "while true; do wget -q -O- http://backend:8080/actuator/health; done"

# En otra terminal, ver cómo escala
kubectl get hpa -n picoyplaca -w
kubectl get pods -n picoyplaca -w
```

### Auto-recuperación (test de resiliencia)

```powershell
# Listar pods
kubectl get pods -n picoyplaca

# Matar un pod del backend
kubectl delete pod <nombre-pod-backend> -n picoyplaca

# Verificar que el ReplicaSet crea uno nuevo automaticamente
kubectl get pods -n picoyplaca -w
```

El ReplicaSet detecta que faltan pods y crea uno nuevo para mantener `replicas: 2`.

---

## Rolling update

```powershell
# Cambiar imagen
kubectl set image deployment/backend backend=picoyplaca-backend:v2 -n picoyplaca

# Ver progreso del rollout
kubectl rollout status deployment/backend -n picoyplaca

# Historial
kubectl rollout history deployment/backend -n picoyplaca

# Rollback
kubectl rollout undo deployment/backend -n picoyplaca
```

---

## Limpieza

```powershell
# Borrar todo el namespace (incluye todos los recursos dentro)
kubectl delete namespace picoyplaca

# Detener minikube
minikube stop

# Borrar el cluster completo
minikube delete
```

---

## Mapeo docker-compose → Kubernetes

| docker-compose | Kubernetes |
|----------------|------------|
| `services.backend.build` | Build manual + `image:` en Deployment |
| `services.backend.ports` | `Service` (ClusterIP/NodePort) |
| `services.backend.environment` | `env:` en container |
| `services.backend.mem_limit` | `resources.limits.memory` |
| `services.backend.healthcheck` | `livenessProbe` + `readinessProbe` |
| `services.frontend.depends_on` | No existe → `readinessProbe` + reintentos |
| `networks` | Mismo namespace + DNS interno |
| `docker-compose up` | `kubectl apply -f k8s/` |
| `docker-compose scale` | `kubectl scale` o HPA |
