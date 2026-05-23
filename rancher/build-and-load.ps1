# ──────────────────────────────────────────────────────────────────────────────
# build-and-load.ps1 — Сборка образа Pet Gym и загрузка в Rancher Desktop VM
#
# ЗАЧЕМ ЭТОТ СКРИПТ:
#   `docker build` кладёт образ в Docker Desktop (Windows хост).
#   k3s (Kubernetes внутри Rancher Desktop) использует Docker ВНУТРИ Linux VM.
#   Это два разных Docker daemon — два разных хранилища образов!
#   imagePullPolicy: Never + образ только в Docker Desktop = ErrImageNeverPull.
#
#   Скрипт решает проблему:
#     1. Собрать образ в Docker Desktop
#     2. Экспортировать в .tar файл
#     3. Загрузить .tar в Docker daemon Linux VM (через rdctl shell)
#
# ИСПОЛЬЗОВАНИЕ:
#   .\rancher\build-and-load.ps1             # Сборка + загрузка (первый деплой)
#   .\rancher\build-and-load.ps1 -Restart    # Сборка + загрузка + перезапуск Pod-а
#
# ТРЕБОВАНИЯ:
#   - Rancher Desktop запущен и работает
#   - kubectl context = rancher-desktop (проверить: kubectl config current-context)
#   - Namespace pet-gym существует (для -Restart): kubectl apply -f rancher/k8s/
# ──────────────────────────────────────────────────────────────────────────────

param(
    # -Restart: после загрузки образа перезапустить Deployment pet-gym-app в K8s.
    # Используй при обновлении кода: .\rancher\build-and-load.ps1 -Restart
    [switch]$Restart
)

# ──── Конфигурация образа ──────────────────────────────────────────────────────
# Имя и тег должны совпадать с image: в rancher/k8s/06-app.yaml
$ImageName = "pet-gym"     # image: pet-gym:1.0.0 в 06-app.yaml
$ImageTag  = "1.0.0"       # Тег версии
$FullImage = "${ImageName}:${ImageTag}"    # Итоговое имя: pet-gym:1.0.0

# Временный .tar файл в системной директории Temp
# $env:TEMP = C:\Users\<username>\AppData\Local\Temp (переменная окружения Windows)
$TarPath = "$env:TEMP\${ImageName}.tar"

# Тот же путь но в формате Linux VM (Rancher Desktop монтирует C:\ как /mnt/c/)
# WSL интеграция: C:\Users\MaxxPC\AppData\Local\Temp → /mnt/c/Users/MaxxPC/AppData/Local/Temp
$VMTarPath = "/mnt/c/Users/$env:USERNAME/AppData/Local/Temp/${ImageName}.tar"

# ──────────────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  Pet Gym — сборка и загрузка образа в K8s"     -ForegroundColor Cyan
Write-Host "════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  Образ: $FullImage"                             -ForegroundColor White
Write-Host ""

# ──── Шаг 1: Сборка Docker образа ────────────────────────────────────────────
Write-Host "[1/3] Сборка образа $FullImage..." -ForegroundColor Yellow

# --provenance=false ОБЯЗАТЕЛЕН!
# Без него BuildKit создаёт multi-platform manifest list (специальный формат).
# k3s с imagePullPolicy: Never не умеет работать с manifest list → ErrImageNeverPull.
# С --provenance=false создаётся обычный одиночный образ.
#
# Контекст сборки = . (корень проекта).
# Dockerfile в корне проекта использует исходники src/ и файлы Gradle из корня.
docker build --provenance=false -t $FullImage .

# $LASTEXITCODE — код завершения последней команды (0 = успех)
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ОШИБКА: Сборка провалилась!" -ForegroundColor Red
    Write-Host "Проверь: Dockerfile в корне проекта? Gradle wrapper есть?" -ForegroundColor Red
    exit 1
}
Write-Host "OK: Образ $FullImage собран" -ForegroundColor Green

# ──── Шаг 2: Экспорт образа в .tar файл ──────────────────────────────────────
Write-Host ""
Write-Host "[2/3] Экспорт образа в $TarPath..." -ForegroundColor Yellow

# docker save: упаковывает образ со всеми слоями в один .tar архив
# -o: output file (путь к файлу)
docker save $FullImage -o $TarPath

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ОШИБКА: Экспорт провалился!" -ForegroundColor Red
    exit 1
}

# Показываем размер .tar файла (для контроля — Spring Boot JAR ~50-80МБ)
$SizeMB = [math]::Round((Get-Item $TarPath).Length / 1MB, 1)
Write-Host "OK: Экспортировано $SizeMB МБ" -ForegroundColor Green

# ──── Шаг 3: Загрузка .tar в Docker daemon Linux VM ──────────────────────────
Write-Host ""
Write-Host "[3/3] Загрузка образа в Rancher Desktop VM..." -ForegroundColor Yellow

# rdctl shell: выполняет команду внутри Linux VM Rancher Desktop
# docker load: загружает .tar архив в Docker daemon VM
# < $VMTarPath: перенаправляем .tar как stdin для docker load
# /mnt/c/: C:\ Windows диск, смонтированный в Linux VM через WSL2
rdctl shell -- sh -c "docker load < $VMTarPath"

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ОШИБКА: Загрузка в VM провалилась!" -ForegroundColor Red
    Write-Host "Rancher Desktop запущен? (проверить: rdctl version)" -ForegroundColor Red
    exit 1
}
Write-Host "OK: Образ загружен в VM" -ForegroundColor Green

# Верификация: проверяем что образ реально виден внутри VM
Write-Host ""
Write-Host "Проверка наличия образа в VM:" -ForegroundColor Cyan
rdctl shell -- sh -c "docker images $ImageName"

# ──── Шаг 4 (опциональный): Перезапуск Deployment ────────────────────────────
if ($Restart) {
    Write-Host ""
    Write-Host "[4/4] Перезапуск Deployment pet-gym-app..." -ForegroundColor Yellow

    # rollout restart: K8s создаёт новые Pod-ы с новым образом, потом удаляет старые.
    # Это zero-downtime обновление (если replicas > 1 и readinessProbe настроены).
    # При replicas=1 будет кратковременная недоступность.
    kubectl rollout restart deployment/pet-gym-app -n pet-gym

    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "ОШИБКА: Не удалось перезапустить Deployment!" -ForegroundColor Red
        Write-Host "Namespace pet-gym существует? kubectl apply -f rancher/k8s/" -ForegroundColor Red
        exit 1
    }

    # Ждём пока rollout завершится (новый Pod запустится и пройдёт readinessProbe)
    Write-Host "Ожидание готовности Pod-а (таймаут 3 минуты)..." -ForegroundColor Yellow
    kubectl rollout status deployment/pet-gym-app -n pet-gym --timeout=180s

    if ($LASTEXITCODE -eq 0) {
        Write-Host "OK: Deployment обновлён, Pod готов!" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "ПРЕДУПРЕЖДЕНИЕ: Таймаут ожидания. Pod ещё загружается." -ForegroundColor Yellow
        Write-Host "Проверь статус: kubectl get pods -n pet-gym" -ForegroundColor Yellow
        Write-Host "Логи: kubectl logs -n pet-gym deployment/pet-gym-app" -ForegroundColor Yellow
    }
}

# ──── Итог ────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "  Готово! Образ $FullImage в VM." -ForegroundColor Green
Write-Host "════════════════════════════════════════════════" -ForegroundColor Green
Write-Host ""

if (-not $Restart) {
    Write-Host "Следующие шаги:" -ForegroundColor Yellow
    Write-Host "  Первый деплой:   kubectl apply -f rancher/k8s/" -ForegroundColor White
    Write-Host "  Обновить стек:   .\rancher\build-and-load.ps1 -Restart" -ForegroundColor White
    Write-Host ""
    Write-Host "После деплоя:" -ForegroundColor Yellow
    Write-Host "  Статус Pod-ов:   kubectl get all -n pet-gym" -ForegroundColor White
    Write-Host "  Логи Spring:     kubectl logs -n pet-gym deployment/pet-gym-app -f" -ForegroundColor White
    Write-Host "  Приложение:      http://localhost:30091" -ForegroundColor White
    Write-Host "  Prometheus:      http://localhost:30903" -ForegroundColor White
    Write-Host "  Grafana:         http://localhost:30304 (admin/admin)" -ForegroundColor White
    Write-Host "  K8s Dashboard:   https://localhost:30443" -ForegroundColor White
}
