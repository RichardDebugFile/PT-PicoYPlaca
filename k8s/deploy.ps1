# ============================================================================
# PT-PicoYPlaca - Script de despliegue Minikube
# ============================================================================
# Uso: pwsh -File k8s/deploy.ps1

$ErrorActionPreference = "Stop"

Write-Host "==> Verificando minikube..." -ForegroundColor Cyan
$status = minikube status --format='{{.Host}}' 2>$null
if ($status -ne "Running") {
    Write-Host "Arrancando minikube..." -ForegroundColor Yellow
    minikube start --driver=docker --memory=4096 --cpus=2
    minikube addons enable metrics-server
}

Write-Host "==> Apuntando docker CLI al daemon de minikube..." -ForegroundColor Cyan
& minikube -p minikube docker-env --shell powershell | Invoke-Expression

Write-Host "==> Build de imagenes dentro de minikube..." -ForegroundColor Cyan
docker build -t picoyplaca-backend:latest ./backend
docker build -t picoyplaca-frontend:latest ./frontend

Write-Host "==> Aplicando manifests..." -ForegroundColor Cyan
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/ -n picoyplaca

Write-Host "==> Esperando que los pods esten Ready..." -ForegroundColor Cyan
kubectl wait --for=condition=ready pod -l app=backend -n picoyplaca --timeout=180s
kubectl wait --for=condition=ready pod -l app=frontend -n picoyplaca --timeout=120s

Write-Host "==> Estado del cluster:" -ForegroundColor Green
kubectl get all -n picoyplaca

Write-Host "==> URL frontend:" -ForegroundColor Green
minikube service frontend -n picoyplaca --url
