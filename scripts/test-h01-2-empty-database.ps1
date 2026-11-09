$ErrorActionPreference = 'Stop'

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$SchemaDirectory = Join-Path $ProjectRoot 'database\init'
$SchemaFile = Join-Path $SchemaDirectory '01-schema.sql'
$TestContainer = 'co2nect-h012-schema-test'

if (-not (Test-Path $SchemaFile)) { throw "Script SQL não encontrado: $SchemaFile" }
& docker info *> $null
if ($LASTEXITCODE -ne 0) { throw 'Abra o Docker Desktop antes de executar este teste.' }

# Remove apenas um container temporário de teste anterior com este nome específico.
$old = & docker ps -aq --filter "name=^/$TestContainer$"
if ($old) {
    Write-Host "Removendo contêiner temporário anterior $TestContainer..."
    & docker rm -f $TestContainer | Out-Null
}

try {
    Write-Host 'Criando um PostgreSQL temporário com diretório de dados vazio...'
    $mount = "${SchemaDirectory}:/docker-entrypoint-initdb.d:ro"
    & docker run -d --rm --name $TestContainer `
        -e POSTGRES_DB=co2nect_h012_test `
        -e POSTGRES_USER=co2nect_user `
        -e POSTGRES_PASSWORD=co2nect_pass `
        -v $mount `
        postgres:16-alpine | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar o PostgreSQL temporário.' }

    $ready = $false
    for ($i = 0; $i -lt 45; $i++) {
        & docker exec $TestContainer pg_isready -U co2nect_user -d co2nect_h012_test *> $null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) {
        & docker logs --tail 80 $TestContainer
        throw 'O PostgreSQL temporário não ficou pronto.'
    }

    $tables = & docker exec $TestContainer psql -U co2nect_user -d co2nect_h012_test -Atc "SELECT to_regclass('public.sensor_device') IS NOT NULL AND to_regclass('public.sensor_reading') IS NOT NULL;"
    if ($LASTEXITCODE -ne 0 -or ([string]$tables).Trim() -ne 't') {
        throw 'Falha: sensor_device e/ou sensor_reading não foram criadas pelo script de inicialização.'
    }

    $columns = & docker exec $TestContainer psql -U co2nect_user -d co2nect_h012_test -Atc "SELECT string_agg(column_name, ',' ORDER BY ordinal_position) FROM information_schema.columns WHERE table_schema='public' AND table_name='sensor_reading';"
    if ($LASTEXITCODE -ne 0 -or -not $columns) {
        throw 'Falha: não foi possível consultar as colunas de sensor_reading.'
    }
    $requiredColumns = @('id', 'device_id', 'co2', 'temperature', 'humidity', 'measured_at', 'received_at')
    $actualColumns = ([string]$columns).Trim().Split(',')
    foreach ($column in $requiredColumns) {
        if ($actualColumns -notcontains $column) {
            throw "Falha: coluna obrigatória '$column' não existe em sensor_reading. Colunas encontradas: $columns"
        }
    }

    $indexDefinition = & docker exec $TestContainer psql -U co2nect_user -d co2nect_h012_test -Atc "SELECT indexdef FROM pg_indexes WHERE schemaname='public' AND tablename='sensor_reading' AND indexname='idx_sensor_reading_device_measured_at';"
    if ($LASTEXITCODE -ne 0 -or -not $indexDefinition) {
        throw 'Falha: índice idx_sensor_reading_device_measured_at não foi criado.'
    }
    if ([string]$indexDefinition -notmatch '\(device_id, measured_at DESC, id DESC\)') {
        throw "Falha: o índice existe, mas não tem a ordem esperada (device_id, measured_at DESC, id DESC). Definição encontrada: $indexDefinition"
    }

    Write-Host ''
    Write-Host 'H01.2: TESTE DO BANCO VAZIO APROVADO' -ForegroundColor Green
    Write-Host 'Tabelas encontradas: sensor_device e sensor_reading.'
    Write-Host "Colunas obrigatórias verificadas: $($requiredColumns -join ', ')."
    Write-Host "Índice encontrado: $indexDefinition"

    Write-Host ''
    Write-Host 'Estrutura criada:'
    & docker exec $TestContainer psql -U co2nect_user -d co2nect_h012_test -c '\dt'
    & docker exec $TestContainer psql -U co2nect_user -d co2nect_h012_test -c "SELECT indexname, indexdef FROM pg_indexes WHERE tablename='sensor_reading' ORDER BY indexname;"
} finally {
    # O container é descartável e não usa volume persistente. O banco real não é tocado.
    $state = & docker inspect --format '{{.State.Status}}' $TestContainer 2>$null
    if ($LASTEXITCODE -eq 0) {
        & docker stop $TestContainer | Out-Null
    }
}
