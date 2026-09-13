Por favor, ayúdame a rediseñar la interfaz de usuario (UI) de la sección de listado de ofertas de trabajo en mi aplicación Android (ChambAYA). 

CONTEXTO DE LA APLICACIÓN:
ChambAYA es una plataforma móvil enfocada en conectar de manera rápida y organizada a personas que ofrecen y buscan trabajos temporales e informales en Ayacucho (construcción, limpieza, delivery, etc.). Actualmente, en la vista principal (`fragmento_chambas.xml`), las tarjetas de empleo (`item_job_card.xml`) se muestran en una cuadrícula de dos columnas.

OBJETIVO DEL REDISEÑO (SOLO FRONTEND):
Quiero rediseñar exclusivamente el diseño de las tarjetas de trabajo (`item_job_card.xml`) y su contenedor en el RecyclerView para que tengan un estilo visual idéntico al de una publicación de Instagram, mostrando **una sola publicación por fila** en lugar de dos. 

No se debe modificar ninguna otra sección de la vista superior (barra de búsqueda, ubicación, categorías o filtros de estado). Solo enfócate en las tarjetas de empleo.

ESTRUCTURA VISUAL REQUERIDA PARA CADA TARJETA (Estilo Publicación de Instagram):

1. Cabecera de la tarjeta:
   - Foto de perfil circular del usuario que publica la oferta.
   - Nombre del perfil / contratante y un ícono de verificado azul.
   - Lugar de publicación o distrito (ej. "Ayacucho Centro", "Carmen Alto").
   - Fecha y hora de la publicación (o tiempo transcurrido).
   - Menú de opciones (ícono de tres puntos verticales) alineado a la derecha, tal como en el diseño de referencia.
   - Botón "Postular" en la parte superior derecha (reemplazando el botón tradicional de "Seguir").
   - Nombre del usuario y descripción corta del trabajo u oferta (ej. "Se necesita ayudante de albañilería...").

2. Contenido central (Multimedia / Detalles):
   - Imagen principal de la oferta de trabajo (opcional: si el empleador sube una foto o imagen descriptiva del trabajo; si no hay imagen, mostrar una estructura limpia con el título del puesto, categoría de trabajo, sueldo por día y breve descripción).

3. Barra de interacciones (Inferior a la imagen/detalles):
   - Ícono de corazón (Me gusta / Guardar en favoritos).
   - Ícono de comentarios.
   - Ícono de compartir.
   - Ícono de guardar (marcador) alineado hacia la extrema derecha.

4. Pie de tarjeta (Texto):
   - Contador de "Me gusta".


REQUISITOS TÉCNICOS:
- Trabajar únicamente en el diseño de la interfaz (`fragmento_chambas.xml` e `item_job_card.xml`). 
- No implementar lógica de backend por ahora, solo diseño frontend limpio, optimizado y adaptable (ConstraintLayout / MaterialCardView según convenga).
- Mantener una estética moderna, profesional y alineada con Material Design.