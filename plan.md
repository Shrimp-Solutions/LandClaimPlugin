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
