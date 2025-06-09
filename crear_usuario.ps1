$body = @{
    nombre = "admin"
    password = "admin"
    rol = "direccion"
} | ConvertTo-Json

$headers = @{
    'Content-Type' = 'application/json'
}

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/usuarios/crear" -Method POST -Body $body -Headers $headers
    Write-Host "Usuario creado exitosamente:" -ForegroundColor Green
    Write-Host ($response | ConvertTo-Json -Depth 10) -ForegroundColor Yellow
} catch {
    Write-Host "Error al crear usuario:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
} 