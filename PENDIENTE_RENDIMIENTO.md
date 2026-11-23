# Pendiente de rendimiento

El punto 1 ya está hecho: abrir o filtrar Cuestionarios y Combos ya no reescribe estados. El estado cambia al asignar, quitar, reciclar o borrar en la jornada.

El punto 2 ya está hecho: el listado y los filtros traen la página en tres consultas (preguntas, jornada e historial) en vez de repetirlas por cada fila.

Queda este.

## 3. No volver a bajar la lista al guardar

Al guardar, el navegador pide otra vez la página entera. El guardado ya tiene la fila nueva: hay que pintarla en la tabla y no llamar a la recarga.

Sitios en el navegador: el guardado de preguntas (`recargarConFiltros`), cuestionarios y combos. Misma idea si jornadas, concursantes o programas recargan el listado completo después de guardar una sola ficha.
