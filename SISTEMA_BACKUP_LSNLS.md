# 🗄️ Sistema de Backup Automático - LSNLS

## 🎯 **PROPÓSITO**

Sistema completo de copias de seguridad automáticas para la base de datos MySQL de LSNLS, que ejecuta backups diarios a las 22:00 (10 PM) hora española y mantiene un historial de 1 semana.

---

## 📋 **CARACTERÍSTICAS PRINCIPALES**

### ✅ **Backup Automático Diario**
- **Horario**: Todos los días a las 22:00 (10 PM) hora española
- **Frecuencia**: Automático sin intervención manual
- **Retención**: 7 días (1 semana)
- **Compresión**: Automática en formato ZIP

### ✅ **Backup Manual**
- **API REST**: Endpoints para crear backups manuales
- **Scripts**: Scripts de Windows para backup/restauración
- **Interfaz**: Control desde la aplicación web

### ✅ **Gestión Inteligente**
- **Rotación automática**: Elimina backups antiguos
- **Compresión**: Reduce espacio en disco
- **Logs detallados**: Registro completo de operaciones
- **Validación**: Verificación de integridad

---

## 🔧 **COMPONENTES IMPLEMENTADOS**

### **1. Scripts de Windows**

#### **backup_database.bat**
```bash
# Script principal de backup
- Ejecuta mysqldump con opciones optimizadas
- Comprime automáticamente en ZIP
- Limpia backups antiguos (7 días)
- Genera logs detallados
- Manejo de errores robusto
```

#### **restore_database.bat**
```bash
# Script de restauración
- Restaura desde archivo .sql o .zip
- Crea backup de seguridad antes de restaurar
- Validación de archivos
- Confirmación de usuario
```

#### **setup_scheduled_task.bat**
```bash
# Configuración de tarea programada
- Crea tarea programada de Windows
- Ejecuta como SYSTEM (permisos elevados)
- Configuración automática
```

### **2. Servicio Spring Boot**

#### **BackupService.java**
```java
@Service
public class BackupService {
    // Backup automático programado
    @Scheduled(cron = "0 0 22 * * ?", zone = "Europe/Madrid")
    public void executeScheduledBackup()
    
    // Backup manual
    public String createBackup()
    
    // Limpieza automática
    public void cleanupOldBackups()
    
    // Restauración
    public boolean restoreBackup(String fileName)
}
```

#### **BackupController.java**
```java
@RestController
@RequestMapping("/api/backup")
public class BackupController {
    // Listar backups
    @GetMapping
    public ResponseEntity<List<BackupInfo>> listBackups()
    
    // Crear backup manual
    @POST("/create")
    public ResponseEntity<Map<String, Object>> createBackup()
    
    // Restaurar backup
    @POST("/restore/{fileName}")
    public ResponseEntity<Map<String, Object>> restoreBackup()
}
```

---

## 🚀 **INSTALACIÓN Y CONFIGURACIÓN**

### **Paso 1: Configurar Tarea Programada**

```bash
# Ejecutar como administrador
cd lsnls/backup
setup_scheduled_task.bat
```

**Resultado esperado:**
```
✅ Tarea programada creada exitosamente
Nombre de la tarea: LSNLS_DailyBackup
Programación: Diaria a las 22:00 (10 PM)
```

### **Paso 2: Verificar Configuración**

```bash
# Verificar tarea programada
schtasks /query /tn "LSNLS_DailyBackup"

# Ejecutar backup manual de prueba
backup_database.bat
```

### **Paso 3: Configurar Spring Boot**

**application.properties:**
```properties
# Configuración de backup
lsnls.backup.directory=./backup
lsnls.backup.retention-days=7

# Habilitar scheduling
spring.task.scheduling.pool.size=5
```

---

## 📊 **ESTRUCTURA DE ARCHIVOS**

```
lsnls/
├── backup/
│   ├── backup_database.bat          # Script principal
│   ├── restore_database.bat         # Script de restauración
│   ├── setup_scheduled_task.bat     # Configuración tarea
│   ├── backups/                     # Directorio de backups
│   │   ├── lsnls_backup_20241201_220000.zip
│   │   ├── lsnls_backup_20241202_220000.zip
│   │   └── ...
│   └── logs/                        # Logs de operaciones
│       ├── backup_20241201_220000.log
│       └── ...
└── src/main/java/com/lsnls/
    ├── service/
    │   └── BackupService.java       # Servicio de backup
    └── controller/
        └── BackupController.java    # API de backup
```

---

## 🔌 **API REST - ENDPOINTS**

### **Listar Backups**
```http
GET /api/backup
Authorization: Bearer {token}
```

**Respuesta:**
```json
[
  {
    "fileName": "lsnls_backup_20241201_220000.zip",
    "createdAt": "2024-12-01T22:00:00",
    "size": 1048576,
    "compressed": true,
    "formattedSize": "1.0 MB"
  }
]
```

### **Crear Backup Manual**
```http
POST /api/backup/create
Authorization: Bearer {token}
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Backup creado exitosamente",
  "fileName": "lsnls_backup_20241201_220000.sql"
}
```

### **Restaurar Backup**
```http
POST /api/backup/restore/{fileName}
Authorization: Bearer {token}
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Base de datos restaurada exitosamente",
  "fileName": "lsnls_backup_20241201_220000.zip"
}
```

### **Estado del Sistema**
```http
GET /api/backup/status
Authorization: Bearer {token}
```

**Respuesta:**
```json
{
  "totalBackups": 7,
  "totalSize": 7340032,
  "formattedTotalSize": "7.0 MB",
  "latestBackup": {
    "fileName": "lsnls_backup_20241201_220000.zip",
    "createdAt": "2024-12-01T22:00:00",
    "size": 1048576,
    "compressed": true
  }
}
```

---

## 🛠️ **COMANDOS ÚTILES**

### **Gestión de Tareas Programadas**
```bash
# Ver tarea programada
schtasks /query /tn "LSNLS_DailyBackup"

# Ejecutar manualmente
schtasks /run /tn "LSNLS_DailyBackup"

# Eliminar tarea
schtasks /delete /tn "LSNLS_DailyBackup" /f

# Ver todas las tareas
schtasks /query
```

### **Backup Manual**
```bash
# Ejecutar backup manual
cd lsnls/backup
backup_database.bat

# Restaurar desde backup
restore_database.bat lsnls_backup_20241201_220000.zip
```

### **Gestión de Archivos**
```bash
# Ver backups disponibles
dir lsnls\backup\backups\*.zip

# Ver logs
dir lsnls\backup\logs\*.log

# Limpiar manualmente (más de 7 días)
forfiles /p "lsnls\backup\backups" /s /m *.zip /d -7 /c "cmd /c del @path"
```

---

## 📈 **MONITOREO Y LOGS**

### **Logs de Backup**
```
[01/12/2024 22:00:00] ========================================
[01/12/2024 22:00:00] INICIANDO BACKUP AUTOMÁTICO LSNLS
[01/12/2024 22:00:00] Base de datos: lsnls
[01/12/2024 22:00:00] Archivo: backups\lsnls_backup_20241201_220000.sql
[01/12/2024 22:00:00] ========================================
[01/12/2024 22:00:00] Ejecutando mysqldump...
[01/12/2024 22:00:05] ✅ BACKUP COMPLETADO EXITOSAMENTE
[01/12/2024 22:00:05] Archivo creado: backups\lsnls_backup_20241201_220000.sql
[01/12/2024 22:00:05] Tamaño del backup: 1048576 bytes
[01/12/2024 22:00:06] Comprimiendo backup...
[01/12/2024 22:00:07] ✅ Backup comprimido: backups\lsnls_backup_20241201_220000.zip
[01/12/2024 22:00:07] Limpiando backups antiguos...
[01/12/2024 22:00:08] ✅ Limpieza completada
[01/12/2024 22:00:08] ========================================
[01/12/2024 22:00:08] BACKUP FINALIZADO
[01/12/2024 22:00:08] ========================================
```

### **Logs de Spring Boot**
```
2024-12-01 22:00:00.000  INFO 1234 --- [scheduling-1] c.l.s.BackupService : 🔄 Iniciando backup automático programado
2024-12-01 22:00:00.001  INFO 1234 --- [scheduling-1] c.l.s.BackupService : 📦 Iniciando creación de backup
2024-12-01 22:00:05.123  INFO 1234 --- [scheduling-1] c.l.s.BackupService : ✅ Backup creado exitosamente: lsnls_backup_20241201_220000.sql (1048576 bytes)
2024-12-01 22:00:06.456  INFO 1234 --- [scheduling-1] c.l.s.BackupService : 📦 Backup comprimido: lsnls_backup_20241201_220000.zip
2024-12-01 22:00:07.789  INFO 1234 --- [scheduling-1] c.l.s.BackupService : 🧹 Iniciando limpieza de backups antiguos (retención: 7 días)
2024-12-01 22:00:08.012  INFO 1234 --- [scheduling-1] c.l.s.BackupService : ✅ Limpieza completada. 2 archivos eliminados
2024-12-01 22:00:08.013  INFO 1234 --- [scheduling-1] c.l.s.BackupService : ✅ Backup automático completado: lsnls_backup_20241201_220000.sql
```

---

## 🔒 **SEGURIDAD Y PERMISOS**

### **Permisos Requeridos**
- ✅ **Administrador**: Para configurar tarea programada
- ✅ **MySQL**: Usuario con permisos de backup/restore
- ✅ **Sistema de archivos**: Escritura en directorio de backup
- ✅ **Spring Security**: Solo usuarios autorizados

### **Configuración de Seguridad**
```properties
# Solo usuarios con rol VERIFICACION o DIRECCION
@PreAuthorize("@authorizationService.canValidate()")

# Backup de seguridad antes de restaurar
- Crea backup automático antes de restaurar
- Previene pérdida de datos
```

---

## 🚨 **SOLUCIÓN DE PROBLEMAS**

### **Error: mysqldump no encontrado**
```bash
# Solución: Agregar MySQL al PATH
set PATH=%PATH%;C:\Program Files\MySQL\MySQL Server 8.0\bin
```

### **Error: Permisos insuficientes**
```bash
# Solución: Ejecutar como administrador
# Clic derecho → "Ejecutar como administrador"
```

### **Error: Tarea programada no ejecuta**
```bash
# Verificar tarea
schtasks /query /tn "LSNLS_DailyBackup"

# Verificar logs del sistema
eventvwr.msc → Windows Logs → Application
```

### **Error: Espacio insuficiente**
```bash
# Limpiar backups manualmente
forfiles /p "lsnls\backup\backups" /s /m *.zip /d -3 /c "cmd /c del @path"
```

---

## 📊 **ESTADÍSTICAS Y MÉTRICAS**

### **Tamaños Típicos**
- **Base de datos pequeña**: 1-5 MB
- **Base de datos mediana**: 5-50 MB
- **Base de datos grande**: 50+ MB

### **Tiempos de Ejecución**
- **Backup**: 30 segundos - 5 minutos
- **Compresión**: 10-30 segundos
- **Limpieza**: 5-10 segundos

### **Espacio en Disco**
- **7 días de retención**: ~50-500 MB
- **Compresión**: Reduce 60-80% del tamaño
- **Logs**: ~1-5 MB por mes

---

## ✅ **VERIFICACIÓN DE FUNCIONAMIENTO**

### **Checklist de Verificación**
- ✅ Tarea programada configurada
- ✅ Script de backup ejecuta correctamente
- ✅ Directorio de backup creado
- ✅ Logs generados
- ✅ API de backup responde
- ✅ Limpieza automática funciona
- ✅ Restauración funciona

### **Pruebas Recomendadas**
1. **Backup manual**: `backup_database.bat`
2. **API de backup**: `POST /api/backup/create`
3. **Restauración**: `restore_database.bat backup_file.zip`
4. **Limpieza**: `POST /api/backup/cleanup`

---

## 🎯 **CONCLUSIÓN**

El sistema de backup automático de LSNLS está **completamente implementado** y proporciona:

- ✅ **Backup automático diario** a las 22:00 hora española
- ✅ **Retención de 1 semana** con limpieza automática
- ✅ **Compresión automática** para ahorrar espacio
- ✅ **API REST completa** para gestión
- ✅ **Scripts de Windows** para operaciones manuales
- ✅ **Logs detallados** para monitoreo
- ✅ **Seguridad integrada** con Spring Security

El sistema está **listo para producción** y garantiza la protección completa de los datos de LSNLS. 🛡️ 