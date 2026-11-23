# Pendiente de rendimiento

El punto 1 ya está hecho: abrir o filtrar Cuestionarios y Combos ya no reescribe estados. El estado cambia al asignar, quitar, reciclar o borrar en la jornada.

Quedan estos dos, en este orden.

## 2. Traer la página en una consulta

Hoy el listado pide los identificadores de la página y, por cada uno, vuelve a la base de datos: preguntas, jornada e historial. Una página de 25 son unas 75 consultas. Combos repite el mismo patrón en `mapearComboListado`.

Hay que sustituir ese bucle por una consulta de la página que ya traiga las preguntas y la jornada. El historial de reutilizado, si sigue haciendo falta en la tabla, en la misma consulta o en una sola para los 25 identificadores.

Sitios:

- `CuestionarioService.obtenerTodosPaginados` y `filtrarCuestionarios`, que llaman a `obtenerCuestionarioConSlots` por cada fila.
- `ComboService.obtenerTodosPaginados` y `filtrarCombos`, que llaman a `mapearComboListado` por cada fila.

## 3. No volver a bajar la lista al guardar

Al guardar, el navegador pide otra vez la página entera. El guardado ya tiene la fila nueva: hay que pintarla en la tabla y no llamar a la recarga.

Sitios en el navegador: el guardado de preguntas (`recargarConFiltros`), cuestionarios y combos. Misma idea si jornadas, concursantes o programas recargan el listado completo después de guardar una sola ficha.
