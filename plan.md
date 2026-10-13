# Plan: menús Bedrock, retirada de mapas y top de claims

## Objetivo

Completar la integración de los menús rediseñados con Bedrock/Geyser, retirar las integraciones opcionales de plugins de mapas sin perder funciones que siguen siendo propias del plugin, simplificar el selector de categorías de flags y añadir un menú visual con el top 10 de perfiles por chunks reclamados.

## Alcance y decisiones

- Los menús ordinarios seguirán usando inventarios Bukkit, traducidos por Geyser a la interfaz Bedrock. Las formas Bedrock nativas se reservarán para entradas de texto y confirmaciones; convertir toda la navegación a forms no encaja bien con layouts configurables y listas.
- Unificar el envío de confirmaciones para que `geyserForms` gobierne de forma consistente el flujo de ModalForm y exista un fallback de inventario para Bedrock/Java cuando Forms estén desactivados o no disponibles. Evitar confirmaciones duplicadas, en particular la ruta de ban desde Player Control.
- Eliminar BlueMap, Dynmap, Squaremap y Pl3xMap como integraciones y limpiar sus referencias de inicialización, configuración, dependencias, `soft-depend`, refrescos y documentación. Conservar PlaceholderAPI y WorldGuard, que tienen propósitos distintos.
- Conservar Change Claim Color y Claim Visibility: el color también alimenta la visualización nativa de límites, y el modo de visualización controla Display Entities/partículas (Bedrock fuerza partículas); no son exclusivamente funciones de mapas. Quitar solo sus actualizaciones de hooks de mapas y mantener sus usos internos.
- En Flags, reemplazar los tres botones de categorías por uno configurable. Cada clic avanza Resident → Trusted → Visitor → Resident; al abrir, la vista inicial continúa siendo Visitor. El botón refleja la categoría activa y mantiene la página visible al actualizar la lista.
- Añadir un botón configurable en el menú principal para abrir el top. El menú presenta las diez claims con mayor cantidad de chunks, en orden descendente, con desempate estable por nombre y propietario.
- El menú top tendrá layout configurable compatible con el validador existente y exactamente diez slots de contenido. Cada entrada será una player head con el dueño real de la claim como proveedor de skin, soporte opcional de `item_model`, y lore con posición, claim, dueño y cantidad de chunks. Las entradas serán informativas; no se presupone una acción de teleport al pulsarlas.
- Añadir en ese mismo menú un objeto separado para el visualizante, configurable (material, lore e `item_model` opcional), que muestre mediante lore la posición/ranking global del perfil de claim desde el que abrió el menú y su cantidad de chunks. Si ese perfil no tiene chunks o no está cargado, indicarlo de manera explícita; no sustituirlo silenciosamente por otro perfil del jugador.
- El acceso al leaderboard se realiza desde un botón configurable dedicado en el menú principal, usando el perfil actual como contexto para el panel del visualizante.
- Calcular el ranking desde perfiles ya cargados en cache; no añadir consultas periódicas ni tablas nuevas. Manejar explícitamente cache aún no cargado, perfiles sin chunks y empate de posiciones.

## Trabajo

1. Auditar y consolidar `GeyserFormHelper`, `ConfirmationGUI` y los flujos de confirmación de menús/comandos, respetando scheduler de Paper/Folia y evitando mutaciones si se cancela o cierra el formulario.
2. Retirar exclusivamente las integraciones de plugins de mapas y sus recursos/dependencias; revisar referencias residuales y preservar PlaceholderAPI, WorldGuard, visualización, color y visibilidad internas.
3. Reducir Flags a un único botón selector cíclico con estado y navegación Bedrock-compatible; actualizar configuración y documentación.
4. Incorporar config y botón del top en Main Menu; crear su GUI basada en el layout configurable, ordenar el ranking desde cache y renderizar diez player heads con `item_model`.
5. Actualizar `CHANGELOG.md`, `docs/CONFIGURATION.md`, documentación de comandos/integraciones y cualquier descripción de dependencias afectada.

## Validación

- Compilar y ejecutar pruebas Maven con JDK 25.
- Verificar layouts válidos e inválidos, lista vacía, menos de diez y más de diez perfiles, orden estable, desempates, owner real y `item_model`.
- Revisar flujos Geyser con Forms activos, desactivados y API ausente: texto, confirmación/cancelación y fallback; comprobar que Ban no pide confirmar dos veces.
- Revisar inicio sin BlueMap/Dynmap/Squaremap/Pl3xMap y comprobar que las funciones nativas de color y visualización siguen funcionando.
- Probar en servidor Paper/Folia y cliente Bedrock con Geyser; la compilación por sí sola no valida renderizado de cabezas/modelos ni el comportamiento real de los formularios.

## Estado de ejecución

Implementado y verificado con `mvn test` y `mvn package` bajo JDK 25. La validación en servidor Paper/Folia y cliente Bedrock/Geyser sigue pendiente porque requiere el entorno de juego.

## Siguiente etapa: perfiles, teletransporte y comandos

### Objetivo

Hacer que los límites por rango y las operaciones de perfiles funcionen con multi-perfiles, completar los spawnpoints automáticos, corregir el refresco del actionbar y endurecer los comandos de PvP para Folia. Simplificar los controles visuales: usar `/claim view` y `/claim viewmode`, retirar el menú de color y conservar el color solo por comando con una advertencia cuando Display Entity no pueda reflejarlo.

### Cambios

- Añadir `landclaim.profiles.limit.<n>` como override del máximo global, tomando el mayor valor entre configuración y permisos efectivos; `landclaim.admin` conserva bypass ilimitado. Aplicar el límite y el valor reportado en todas las rutas de creación.
- Revisar y corregir la resolución de `/claim tp` para perfiles múltiples, con sugerencias/autocompletado para dueño y perfil. Crear automáticamente un spawnpoint seguro/contextual para cada perfil y un nombre determinista `claim-<n>` cuando se cree un perfil sin nombre explícito.
- Unificar la lógica compartida de `/claim profiles` y `/claim list` para que ambas rutas expongan/seleccionen perfiles de forma coherente.
- Corregir el actionbar al entrar o salir de claims propias, ajenas y wilderness, incluyendo refresco inmediato y scheduler compatible con Folia.
- Reemplazar los iconos de libro del selector de perfiles por iconos configurables por mundo (overworld `GRASS_BLOCK`, Nether `NETHERRACK`, End `END_STONE`), con `item_model` opcional y fallback seguro.
- Rehacer `/claim pvp on|off [time]` para calcular posiciones y ejecutar acciones Bukkit exclusivamente en el scheduler de región/jugador apropiado, sin recuperar chunks desde hilos async.
- Renombrar `/claim visible` a `/claim view` y `/claim visualization` a `/claim viewmode`, manteniendo compatibilidad mediante aliases si el framework la permite sin registrar comandos ambiguos.
- Retirar Change Claim Color del menú y su configuración de menú; conservar `/claim color`. Si el perfil usa `DISPLAY_ENTITY`, informar que el color no se representa en ese modo y recomendar cambiar a partículas, sin afirmar que cambió un borde que el cliente no puede colorear.
- Actualizar mensajes, permisos descritos, documentación y `CHANGELOG.md`.

### Validación

- `mvn test package` con JDK 25, incluyendo pruebas unitarias de límite por defecto (2), permiso superior (5), permisos inválidos y precedencia del mayor límite; `git diff --check`.
- Buscar todos los puntos que crean perfiles y aplican límites; comprobar el placeholder `<limit>` con el valor efectivo.
- Probar `/claim tp` con dueño y miembros en multi-perfiles, nombres ambiguos, spawnpoint ausente y autocompletado.
- Revisar transiciones de actionbar entre perfil propio, ajeno y wilderness; revisar `/claim pvp on|off` sin operaciones Bukkit desde async.
- Verificar layouts/configuración antigua del selector de perfiles y `item_model` en los iconos por mundo.
- Validar la retirada del botón de color y el aviso del comando con Display Entity; probar Paper y Folia en servidor para los comportamientos dependientes de región.

### Estado de esta etapa

Implementación y documentación actualizadas, incluida la retirada de la GUI/configuración de cambio de color, el filtrado del autocompletado de `/claim tp`, el aviso de color solo tras una modificación válida y la apertura del selector en el scheduler del jugador. `mvn test package` pasa con JDK 25; el proyecto no contiene pruebas automatizadas (`No tests to run`). La validación de juego en Paper/Folia y Bedrock/Geyser sigue pendiente porque requiere un servidor de prueba.
