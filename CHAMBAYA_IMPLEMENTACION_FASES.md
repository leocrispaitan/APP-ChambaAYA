# ChambAYA --- Plan maestro de implementación con Claude Code / Codex

## Objetivo

Implementar ChambAYA de forma incremental, segura y profesional usando
**Firebase Authentication, Cloud Firestore y Cloudinary (almacenamiento de imágenes)**,
trabajando fase por fase.

La estrategia es:

1.  Terminar y dejar sólida la creación de usuarios.
2.  Mostrar y completar los datos desde el perfil.
3.  Incorporar el rol CONTRATANTE sin romper TRABAJADOR.
4.  Crear lugares/establecimientos.
5.  Crear publicaciones de trabajo.
6.  Crear postulaciones y contratación.
7.  Crear historial y trabajos realizados.
8.  Crear calificaciones y reputación.
9.  Crear comentarios, likes, guardados, ocultar y denuncias.
10. Crear chat y notificaciones.
11. Aplicar seguridad de Firestore y del preset de subida de Cloudinary.
12. Probar todo el flujo de extremo a extremo.

> **Regla fundamental:** cada fase debe ser implementada, compilada y
> probada antes de pasar a la siguiente. No modificar funcionalidades
> que todavía no corresponden a la fase actual.

------------------------------------------------------------------------

# 0. Arquitectura general

## Roles

Un usuario tendrá una sola cuenta Firebase Authentication y un único
`uid`.

Puede tener uno o ambos roles:

``` text
TRABAJADOR
CONTRATANTE
```

Por ello se utilizará:

``` json
{
  "roles": ["TRABAJADOR"],
  "activeRole": "TRABAJADOR"
}
```

o posteriormente:

``` json
{
  "roles": ["TRABAJADOR", "CONTRATANTE"],
  "activeRole": "TRABAJADOR"
}
```

No crear dos cuentas para una misma persona.

------------------------------------------------------------------------

# 1. Estructura general de Firestore

``` text
users
email_verifications
categories
workplaces
publications
applications
jobs
ratings
comments
publication_likes
publication_saves
hidden_publications
publication_reports
user_reports
user_blocks
conversations
messages
notifications
```

## Cloudinary (almacenamiento de imágenes)

Las imágenes y archivos no se almacenarán directamente en Firestore.
Se subirán a Cloudinary usando carpetas equivalentes a:

``` text
users/{uid}/profile/profile

users/{uid}/workplace/photo_1

publications/{publicationId}/image_1
publications/{publicationId}/image_2
publications/{publicationId}/image_3
```

La subida se hace desde Android con el SDK de Cloudinary usando un
**unsigned upload preset** (configurado en el dashboard, restringido
por carpeta, tamaño y tipo de archivo).

Firestore solamente guardará:

``` text
url
publicId
```

### Credenciales Cloudinary

``` text
API Key: 619274453168488
API Secret: W3mN_1hUVkUbkaU-s1Dhaezx25Q
Cloud Name: vtmk2tgh
```

Nota: el API Secret solo se usa en un backend/Cloud Function para
operaciones firmadas (por ejemplo borrar una imagen). Nunca debe
incluirse en el código de la app Android ni subirse a un repositorio
público; el Cloud Name y el nombre del upload preset (unsigned) son
los únicos datos que sí van dentro de la app.

------------------------------------------------------------------------

# 2. Reglas para Claude Code / Codex

Utilizar estas reglas en TODAS las fases:

``` text
1. Antes de modificar código, inspecciona la estructura actual del proyecto.
2. No reemplaces funcionalidades existentes que ya funcionan.
3. Reutiliza las clases, repositorios, ViewModels, Activities, Fragments y layouts existentes cuando corresponda.
4. No dupliques lógica.
5. Mantén arquitectura limpia y consistente con el proyecto actual.
6. Firebase Authentication maneja contraseñas. Nunca guardar contraseñas en Firestore.
7. Nunca guardar imágenes como Base64 dentro de Firestore.
8. Las imágenes deben subirse a Cloudinary (unsigned upload preset) y Firestore solamente guardará `url`/`publicId`.
9. Usar uid de Firebase Authentication como identificador principal del usuario.
10. No almacenar DNI/RUC como información pública.
11. Validar permisos y datos también mediante Firestore Security Rules, no solamente desde Android.
12. Usar Timestamp de Firestore para fechas.
13. Mantener nombres de campos consistentes.
14. Evitar listas que puedan crecer indefinidamente dentro de un documento.
15. Para likes, guardados, denuncias, comentarios, etc., utilizar colecciones independientes.
16. No implementar fases futuras antes de terminar la fase actual.
17. Después de cada cambio ejecutar build/tests correspondientes.
18. Si detectas un conflicto con código existente, explicar primero el conflicto y elegir la solución que preserve la funcionalidad actual.
```

------------------------------------------------------------------------

# FASE 1 --- REGISTRO Y CREACIÓN DEL USUARIO

## Objetivo

Completar el flujo:

``` text
FASE 1
DNI/RUC
↓
Validación de identidad

FASE 2
Google o correo + contraseña
↓
Credenciales

FASE 3
OTP
↓
Correo verificado

FASE 4
Crear users/{uid}
↓
Usuario registrado
```

No pedir todos los datos del perfil durante el registro.

El usuario completará posteriormente su perfil desde `Mi Perfil`.

------------------------------------------------------------------------

## 1.1 Datos mínimos de registro

### Trabajador

``` text
DNI
Correo o Google
Contraseña si corresponde
OTP
Rol
```

### Contratante

``` text
DNI o RUC
Correo o Google
Contraseña si corresponde
OTP
Rol
```

El contratante no debe estar obligado a tener RUC.

Puede ser:

``` text
PERSONA
EMPRESA
NEGOCIO
INDEPENDIENTE
```

------------------------------------------------------------------------

## 1.2 Colección users

Crear:

``` text
users/{uid}
```

Documento inicial:

``` json
{
  "uid": "UID_FIREBASE",

  "accountStatus": "ACTIVE",

  "registrationStatus": "VERIFIED",

  "roles": [
    "TRABAJADOR"
  ],

  "activeRole": "TRABAJADOR",

  "auth": {
    "provider": "EMAIL",
    "email": "usuario@gmail.com",
    "emailVerified": true,
    "otpVerified": true
  },

  "identity": {
    "documentType": "DNI",
    "documentNumber": "DNI_VALIDADO",
    "identityVerified": true,
    "verifiedAt": "Timestamp"
  },

  "createdAt": "Timestamp",
  "updatedAt": "Timestamp",
  "lastLoginAt": "Timestamp"
}
```

Para Google:

``` json
{
  "auth": {
    "provider": "GOOGLE",
    "email": "usuario@gmail.com",
    "emailVerified": true,
    "otpVerified": true
  }
}
```

------------------------------------------------------------------------

## 1.3 OTP

Colección:

``` text
email_verifications
```

Documento:

``` json
{
  "uid": "UID",
  "email": "usuario@gmail.com",
  "otpHash": "HASH",
  "status": "PENDING",
  "attempts": 0,
  "maxAttempts": 5,
  "expiresAt": "Timestamp",
  "createdAt": "Timestamp",
  "usedAt": null
}
```

No guardar el OTP numérico en texto plano.

------------------------------------------------------------------------

## 1.4 Prompt para Claude Code / Codex --- FASE 1

``` text
Estoy desarrollando ChambAYA en Android con Firebase.

Necesito implementar SOLAMENTE la FASE 1 del plan de registro.

Primero inspecciona todo el proyecto actual y determina:
- Activity/Fragment del registro.
- ViewModels.
- Repositories.
- Firebase Authentication.
- Firestore.
- código existente de validación DNI/RUC.
- flujo actual de Google.
- flujo actual de correo/contraseña.
- OTP existente.
- modelos relacionados con User.

IMPORTANTE:
No reemplaces ni rompas la validación DNI/RUC que ya funciona.
No implementes todavía perfil completo, publicaciones, chat, postulaciones ni otras fases.

Objetivo de esta fase:

1. Mantener la FASE 1 actual de validación DNI/RUC.
2. Mantener la FASE 2 actual de credenciales.
3. Mantener la FASE 3 actual de OTP.
4. Al finalizar correctamente el OTP, crear/actualizar users/{uid}.
5. Usar Firebase Authentication como fuente de identidad.
6. Nunca guardar contraseñas en Firestore.
7. Usar el uid de Firebase Auth como ID del documento users.
8. Crear los campos:
   - uid
   - accountStatus
   - registrationStatus
   - roles
   - activeRole
   - auth.provider
   - auth.email
   - auth.emailVerified
   - auth.otpVerified
   - identity.documentType
   - identity.documentNumber
   - identity.identityVerified
   - identity.verifiedAt
   - createdAt
   - updatedAt
   - lastLoginAt
9. Si el usuario es TRABAJADOR:
   roles = ["TRABAJADOR"]
10. Si es CONTRATANTE:
   roles = ["CONTRATANTE"]
11. No obligar al usuario a rellenar nombre de usuario, experiencia, habilidades, dirección exacta, fotos personalizadas, etc. durante este registro.
12. Esos datos se completarán después desde Mi Perfil.

Implementa con el patrón arquitectónico existente del proyecto.

Después:
- compila el proyecto;
- corrige errores;
- verifica el flujo completo;
- documenta exactamente qué archivos modificaste;
- muestra la estructura final de users;
- no avances a la FASE 2.
```

------------------------------------------------------------------------

# FASE 2 --- PERFIL DEL USUARIO

## Objetivo

Después de registrarse, el usuario entra a la aplicación.

Desde:

``` text
Mi Perfil
```

puede completar los datos que no eran necesarios durante el registro.

------------------------------------------------------------------------

# 2.1 Perfil base

Agregar a `users/{uid}`:

``` json
{
  "profile": {
    "firstName": "Maria",
    "lastName": "Sanchez",
    "fullName": "Maria Sanchez",
    "username": "maria_sanchez_ayacucho",
    "usernameNormalized": "maria_sanchez_ayacucho",
    "phone": "+51XXXXXXXXX",
    "profilePhotoUrl": "",
    "profilePhotoPath": "",
    "profilePhotoSource": "DEFAULT",
    "bio": "",
    "district": "Carmen Alto",
    "province": "Huamanga",
    "department": "Ayacucho",
    "country": "Peru"
  }
}
```

------------------------------------------------------------------------

# 2.2 Foto de perfil

Fuentes:

``` text
GOOGLE
DEFAULT
CUSTOM
```

Ejemplo:

``` json
{
  "profilePhotoSource": "CUSTOM",
  "profilePhotoUrl": "https://res.cloudinary.com/.../profile.jpg",
  "profilePhotoPublicId": "users/UID/profile/profile"
}
```

Cloudinary (carpeta):

``` text
users/{uid}/profile/profile
```

------------------------------------------------------------------------

# 2.3 Perfil de trabajador

Agregar:

``` json
{
  "worker": {
    "enabled": true,
    "experienceYears": 4,
    "specialties": [
      "Albañilería",
      "Pintura",
      "Jardinería"
    ],
    "skills": [
      "Pintura interior",
      "Pintura exterior",
      "Acabados"
    ],
    "workCount": 0,
    "ratingAverage": 0,
    "ratingCount": 0,
    "profileCompleted": 0
  }
}
```

------------------------------------------------------------------------

# 2.4 Privacidad

``` json
{
  "privacy": {
    "showPhone": false,
    "showExactAddress": false,
    "showEmail": false
  }
}
```

El DNI, correo y dirección exacta no deben mostrarse públicamente por
defecto.

------------------------------------------------------------------------

# 2.5 Estadísticas

``` json
{
  "statistics": {
    "applicationsCount": 0,
    "publicationsCount": 0,
    "completedJobsCount": 0,
    "savedPublicationsCount": 0,
    "receivedRatingsCount": 0
  }
}
```

------------------------------------------------------------------------

## Prompt FASE 2

``` text
Implementa SOLAMENTE la FASE 2 de ChambAYA: PERFIL DEL USUARIO.

Primero inspecciona el código actual y utiliza la estructura existente.

No modificar el flujo de registro ya terminado.

Necesito que Mi Perfil cargue users/{uid} desde Firestore.

Mostrar:
- nombre completo
- username
- foto
- experiencia
- trabajos realizados
- calificación
- especialidades
- habilidades
- distrito
- descripción

Agregar edición de:
- username
- teléfono
- foto
- descripción
- distrito
- experiencia
- especialidades
- habilidades

La foto personalizada debe subirse a Cloudinary (unsigned upload
preset) a la carpeta:
users/{uid}/profile/profile

Firestore solo debe almacenar:
profilePhotoUrl
profilePhotoPublicId
profilePhotoSource

Implementar actualización parcial del documento.

No permitir que el usuario modifique:
- uid
- verification status
- identity verification
- registrationStatus
- emailVerified
- otpVerified

Validar username como único y normalizado.

Mantener el diseño actual de Mi Perfil y adaptarlo sin destruir la UI existente.

Compilar y probar.

No implementar todavía publicaciones, chat, postulaciones ni rol contratante completo.
```

------------------------------------------------------------------------

# FASE 3 --- ACTIVAR ROL CONTRATANTE

## Objetivo

Un trabajador registrado puede convertirse también en contratante.

No crear otra cuenta.

Cambiar:

``` json
{
  "roles": [
    "TRABAJADOR"
  ]
}
```

a:

``` json
{
  "roles": [
    "TRABAJADOR",
    "CONTRATANTE"
  ]
}
```

------------------------------------------------------------------------

# 3.1 Datos employer

``` json
{
  "employer": {
    "enabled": true,
    "employerType": "PERSONA",
    "businessName": "",
    "commercialName": "",
    "sector": "",
    "ruc": null,
    "workplaceId": null,
    "publishedCount": 0,
    "hiredCount": 0,
    "ratingAverage": 0,
    "ratingCount": 0
  }
}
```

------------------------------------------------------------------------

# 3.2 Requisitos para activar contratante

``` text
Identidad verificada
Correo verificado
Perfil básico completo
Teléfono verificado
Tipo de contratante
```

RUC:

``` text
opcional para persona
obligatorio si se declara empresa cuando corresponda
```

No asumir que todos los contratantes tienen RUC.

------------------------------------------------------------------------

## Prompt FASE 3

``` text
Implementa SOLAMENTE la FASE 3: ACTIVACIÓN DEL ROL CONTRATANTE.

No crear una segunda cuenta.

Un mismo uid puede tener:
["TRABAJADOR"]
o:
["TRABAJADOR", "CONTRATANTE"]

Agregar activeRole para permitir cambiar el modo.

Crear employer con:
- enabled
- employerType
- businessName
- commercialName
- sector
- ruc
- workplaceId
- publishedCount
- hiredCount
- ratingAverage
- ratingCount

Crear una pantalla para activar/completar el perfil de contratante.

Permitir:
PERSONA
EMPRESA
NEGOCIO
INDEPENDIENTE

DNI o RUC según corresponda.

No eliminar la información del trabajador.

Probar:
1. trabajador solamente;
2. contratante solamente;
3. trabajador + contratante;
4. cambio de activeRole.

No implementar publicaciones todavía.
```

------------------------------------------------------------------------

# FASE 4 --- LUGAR / ESTABLECIMIENTO

## Colección

``` text
workplaces/{workplaceId}
```

``` json
{
  "workplaceId": "ID",
  "ownerUid": "UID",

  "name": "Ferretería El Sol",

  "type": "LOCAL_COMERCIAL",

  "sector": "FERRETERIA",

  "description": "",

  "address": "",

  "district": "Carmen Alto",
  "province": "Huamanga",
  "department": "Ayacucho",

  "location": {
    "latitude": 0,
    "longitude": 0
  },

  "photoUrl": "",
  "photoPath": "",

  "verified": false,

  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```

Tipos:

``` text
VIVIENDA
LOCAL_COMERCIAL
EMPRESA
TALLER
RESTAURANTE
OBRA
CAMPO
OTRO
```

------------------------------------------------------------------------

## Prompt FASE 4

``` text
Implementa SOLAMENTE la FASE 4: WORKPLACES.

Crear colección workplaces.

Permitir al contratante:
- crear lugar;
- editar lugar;
- agregar nombre;
- tipo;
- sector;
- descripción;
- dirección;
- distrito;
- ubicación;
- fotografía.

La fotografía debe subirse a Cloudinary (unsigned upload preset).

Guardar en Firestore solamente `url` y `publicId`.

Relacionar:
users/{uid}.employer.workplaceId

No implementar publicaciones todavía.
```

------------------------------------------------------------------------

# FASE 5 --- PUBLICACIONES

## Colección

``` text
publications/{publicationId}
```

Campos principales:

``` json
{
  "publicationId": "PUB001",
  "ownerUid": "UID",

  "status": "ACTIVE",
  "visibility": "PUBLIC",

  "type": "JOB_OFFER",

  "title": "Se requiere pintor profesional",

  "description": "Descripción del trabajo",

  "category": "PINTURA",

  "subcategory": "PINTURA_INTERIOR",

  "skillsRequired": [],

  "payment": {
    "amount": 100,
    "currency": "PEN",
    "period": "DAY",
    "negotiable": false
  },

  "schedule": {
    "startDate": "Timestamp",
    "endDate": "Timestamp",
    "startTime": "08:00",
    "endTime": "17:00"
  },

  "workersNeeded": 2,
  "workersHired": 0,

  "location": {
    "district": "Carmen Alto",
    "province": "Huamanga",
    "department": "Ayacucho",
    "latitude": 0,
    "longitude": 0,
    "exactAddress": ""
  },

  "workplaceId": "WORKPLACE_ID",

  "images": [],

  "publisher": {
    "uid": "UID",
    "name": "",
    "username": "",
    "photoUrl": "",
    "verified": false,
    "employerType": "",
    "sector": ""
  },

  "statistics": {
    "views": 0,
    "likes": 0,
    "comments": 0,
    "shares": 0,
    "saves": 0,
    "applications": 0
  },

  "featured": false,

  "createdAt": "Timestamp",
  "updatedAt": "Timestamp",
  "expiresAt": "Timestamp"
}
```

------------------------------------------------------------------------

# FASE 5.1 --- Fotografías de publicación

Cloudinary (carpeta):

``` text
publications/{publicationId}/image_1
publications/{publicationId}/image_2
publications/{publicationId}/image_3
```

Firestore:

``` json
{
  "images": [
    {
      "url": "https://res.cloudinary.com/.../image_1.jpg",
      "publicId": "publications/PUB001/image_1"
    }
  ]
}
```

Máximo recomendado inicialmente: 3 imágenes.

La publicación también debe funcionar sin imágenes.

------------------------------------------------------------------------

# FASE 5.2 --- Datos automáticos

Al crear publicación, tomar automáticamente desde el perfil:

``` text
nombre del contratante
username
foto
sector
tipo de contratante
verificación
workplace
ubicación
```

No pedirlos nuevamente si ya existen.

------------------------------------------------------------------------

## Prompt FASE 5

``` text
Implementa SOLAMENTE la FASE 5: PUBLICACIONES DE TRABAJO.

Crear publications/{publicationId}.

El contratante debe poder:
- crear publicación;
- editar publicación;
- eliminar/archivar publicación;
- pausar publicación;
- reactivar publicación;
- finalizar publicación.

Campos:
title
description
category
subcategory
skillsRequired
payment
schedule
workersNeeded
location
workplaceId
images
publisher
statistics
status
createdAt
updatedAt
expiresAt

Al crear la publicación, cargar automáticamente desde users/{uid}:
- nombre;
- username;
- foto;
- sector;
- tipo de contratante;
- verificación.

No pedir nuevamente información que ya existe.

Permitir publicación:
- solo texto;
- texto + imágenes.

Imágenes a Cloudinary (unsigned upload preset).

No implementar todavía postulaciones.
```

------------------------------------------------------------------------

# FASE 6 --- BUSCAR Y FILTRAR PUBLICACIONES

Implementar:

``` text
Buscar por texto
Categoría
Distrito
Pago
Fecha
Distancia
Estado
```

La pantalla principal debe mostrar:

``` text
Publicaciones cercanas
```

y permitir:

``` text
Ver publicación
Ver perfil del contratante
Postular
Guardar
Me gusta
Compartir
No me interesa
Denunciar
```

La aplicación ya contempla búsquedas por distrito, categoría, sueldo,
fecha y distancia. fileciteturn0file0L35-L42

------------------------------------------------------------------------

# FASE 7 --- POSTULACIONES

Colección:

``` text
applications/{applicationId}
```

``` json
{
  "applicationId": "APP001",
  "publicationId": "PUB001",
  "workerUid": "UID_WORKER",
  "employerUid": "UID_EMPLOYER",

  "status": "PENDING",

  "worker": {
    "name": "",
    "username": "",
    "photoUrl": "",
    "experienceYears": 0,
    "ratingAverage": 0
  },

  "message": "",

  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```

Estados:

``` text
PENDING
ACCEPTED
REJECTED
CANCELLED
WITHDRAWN
```

------------------------------------------------------------------------

## Prompt FASE 7

``` text
Implementa SOLAMENTE postulaciones.

El trabajador puede pulsar POSTULAR.

Crear applications/{applicationId}.

Evitar postulaciones duplicadas para el mismo:
workerUid + publicationId.

El contratante puede:
- ver postulantes;
- ver perfil;
- aceptar;
- rechazar.

El trabajador puede:
- ver mis postulaciones;
- retirar una postulación cuando corresponda;
- consultar estado.

Actualizar contadores de publication.

Crear notificaciones básicas cuando:
- se recibe postulación;
- se acepta;
- se rechaza.

No implementar todavía calificaciones.
```

------------------------------------------------------------------------

# FASE 8 --- JOBS / TRABAJOS REALIZADOS

Cuando una postulación sea aceptada:

``` text
jobs/{jobId}
```

``` json
{
  "jobId": "JOB001",
  "publicationId": "PUB001",
  "workerUid": "UID_WORKER",
  "employerUid": "UID_EMPLOYER",

  "status": "IN_PROGRESS",

  "startedAt": "Timestamp",
  "completedAt": null,

  "agreedPayment": {
    "amount": 100,
    "currency": "PEN"
  },

  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```

Estados:

``` text
ACCEPTED
IN_PROGRESS
COMPLETED
CANCELLED
```

------------------------------------------------------------------------

# FASE 9 --- CALIFICACIONES Y REPUTACIÓN

Colección:

``` text
ratings/{ratingId}
```

``` json
{
  "ratingId": "RATING001",

  "jobId": "JOB001",
  "publicationId": "PUB001",

  "fromUid": "UID_EVALUADOR",
  "toUid": "UID_EVALUADO",

  "rating": 5,

  "comment": "Muy responsable y puntual.",

  "createdAt": "Timestamp"
}
```

Permitir:

``` text
Trabajador → Contratante
Contratante → Trabajador
```

No permitir calificar un trabajo que no se haya realizado/completado.

------------------------------------------------------------------------

## Prompt FASE 9

``` text
Implementa el sistema de reputación.

Solo permitir calificar después de completar el job.

Crear ratings/{ratingId}.

Un usuario no puede calificar dos veces el mismo job en la misma dirección.

Actualizar:
ratingAverage
ratingCount

Mostrar:
- estrellas;
- promedio;
- cantidad;
- comentarios.

Aplicar tanto a trabajadores como contratantes.
```

------------------------------------------------------------------------

# FASE 10 --- COMENTARIOS

Colección:

``` text
comments/{commentId}
```

``` json
{
  "commentId": "COMMENT001",
  "publicationId": "PUB001",

  "authorUid": "UID",

  "authorName": "",
  "authorUsername": "",
  "authorPhotoUrl": "",

  "text": "",

  "status": "VISIBLE",

  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```

Permitir:

``` text
comentar
editar propio comentario
eliminar propio comentario
reportar comentario
```

------------------------------------------------------------------------

# FASE 11 --- LIKES, GUARDADOS Y COMPARTIR

## Likes

``` text
publication_likes
```

``` json
{
  "publicationId": "PUB001",
  "userUid": "UID",
  "createdAt": "Timestamp"
}
```

## Guardados

``` text
publication_saves
```

``` json
{
  "publicationId": "PUB001",
  "userUid": "UID",
  "createdAt": "Timestamp"
}
```

## No me interesa

``` text
hidden_publications
```

``` json
{
  "publicationId": "PUB001",
  "userUid": "UID",
  "reason": "NOT_INTERESTED",
  "createdAt": "Timestamp"
}
```

## Compartir

El compartir por WhatsApp no necesita una colección obligatoriamente.

Usar Android Share Intent.

Registrar `shares` solamente si quieres estadísticas.

------------------------------------------------------------------------

# FASE 12 --- DENUNCIAS Y SEGURIDAD SOCIAL

## Denunciar publicación

``` text
publication_reports
```

``` json
{
  "reportId": "REPORT001",
  "publicationId": "PUB001",
  "reporterUid": "UID",
  "reason": "FRAUD",
  "description": "",
  "status": "PENDING",
  "createdAt": "Timestamp"
}
```

## Denunciar usuario

``` text
user_reports
```

``` json
{
  "reportId": "REPORT001",
  "reportedUid": "UID",
  "reporterUid": "UID",
  "reason": "SCAM",
  "description": "",
  "status": "PENDING",
  "createdAt": "Timestamp"
}
```

## Bloquear

``` text
user_blocks
```

``` json
{
  "blockerUid": "UID_A",
  "blockedUid": "UID_B",
  "createdAt": "Timestamp"
}
```

------------------------------------------------------------------------

# FASE 13 --- CHAT

Colección:

``` text
conversations/{conversationId}
```

``` json
{
  "conversationId": "CONV001",

  "participants": [
    "UID_A",
    "UID_B"
  ],

  "publicationId": "PUB001",

  "lastMessage": "Hola, ¿sigue disponible?",

  "lastMessageAt": "Timestamp",

  "createdAt": "Timestamp"
}
```

Mensajes:

``` text
conversations/{conversationId}/messages/{messageId}
```

``` json
{
  "senderUid": "UID",
  "type": "TEXT",
  "text": "Hola",
  "read": false,
  "createdAt": "Timestamp"
}
```

Tipos futuros:

``` text
TEXT
IMAGE
LOCATION
SYSTEM
```

------------------------------------------------------------------------

# FASE 14 --- NOTIFICACIONES

Colección:

``` text
notifications/{notificationId}
```

``` json
{
  "notificationId": "NOTIF001",

  "recipientUid": "UID",

  "type": "NEW_APPLICATION",

  "title": "Nueva postulación",

  "message": "Un trabajador postuló a tu oferta.",

  "senderUid": "UID",

  "publicationId": "PUB001",

  "read": false,

  "createdAt": "Timestamp"
}
```

Tipos:

``` text
NEW_APPLICATION
APPLICATION_ACCEPTED
APPLICATION_REJECTED
NEW_MESSAGE
NEW_RATING
NEW_COMMENT
NEW_LIKE
NEW_NEARBY_PUBLICATION
PUBLICATION_EXPIRING
```

------------------------------------------------------------------------

# FASE 15 --- CATEGORÍAS

Colección:

``` text
categories/{categoryId}
```

``` json
{
  "id": "PINTURA",
  "name": "Pintura",
  "icon": "format_paint",
  "active": true,
  "order": 3
}
```

Categorías iniciales:

``` text
ALBAÑILERIA
CONSTRUCCION
PINTURA
CARPINTERIA
ELECTRICIDAD
GASFITERIA
JARDINERIA
LIMPIEZA
MUDANZAS
DELIVERY
COCINA
AYUDANTE_GENERAL
CUIDADO_NINOS
CUIDADO_ADULTOS
AGRICULTURA
GANADERIA
COSTURA
VENTAS
OTROS
```

------------------------------------------------------------------------

# FASE 16 --- REGLAS DE SEGURIDAD FIRESTORE

Antes de producción, revisar todas las reglas.

Principios:

``` text
users:
- el usuario puede leer/modificar solamente campos permitidos de su propio perfil;
- no puede modificar uid;
- no puede modificar verificaciones desde Android.

publications:
- solamente contratantes habilitados pueden crear;
- solamente el propietario puede editar/eliminar;
- publicaciones públicas pueden ser leídas.

applications:
- trabajador puede crear su propia postulación;
- contratante puede consultar postulaciones de sus publicaciones;
- trabajador puede consultar sus propias postulaciones.

ratings:
- solamente participantes del job;
- solamente después de completar.

reports:
- usuario autenticado puede crear;
- usuario normal no puede modificar el estado de moderación.

messages:
- solamente participantes pueden leer/escribir.

Cloudinary:
- usar un unsigned upload preset dedicado por tipo de recurso (perfil, workplace, publicación);
- restringir el preset por carpeta, tamaño máximo y tipo de archivo desde el dashboard;
- no exponer el API Secret en la app; operaciones destructivas (borrar imagen) deben pasar por un backend/Cloud Function firmado;
- validar en Firestore Security Rules que el `publicId`/`url` guardado pertenezca a la carpeta esperada del uid/publicationId del usuario autenticado.
```

------------------------------------------------------------------------

# FASE 17 --- PERFIL PÚBLICO

Separar:

``` text
Mi Perfil
```

de:

``` text
Perfil Público
```

## Mi Perfil

Puede mostrar:

``` text
Editar perfil
Completar perfil
Mis postulaciones
Mis trabajos
Mis publicaciones
Guardados
Configuración
Cambiar modo
```

## Perfil público del trabajador

Mostrar:

``` text
Foto
Nombre
Username
Verificación
Calificación
Trabajos realizados
Experiencia
Especialidades
Habilidades
Distrito
Comentarios/reputación
```

No mostrar:

``` text
DNI
contraseña
email privado
dirección exacta
datos internos
```

## Perfil público del contratante

Mostrar:

``` text
Foto
Nombre/empresa
Username
Verificación
Sector
Calificación
Trabajos publicados
Contrataciones
Lugar/establecimiento cuando corresponda
Reputación
```

------------------------------------------------------------------------

# FASE 18 --- COMPLETAR PERFIL

Implementar porcentaje:

``` text
profileCompleted
```

Ejemplo:

``` text
90%
```

Calcular según campos realmente completados.

No guardar manualmente porcentajes inconsistentes.

Ejemplo de campos:

``` text
Foto
Username
Teléfono
Distrito
Descripción
Experiencia
Especialidades
Habilidades
```

El porcentaje se recalcula cuando cambian los datos.

------------------------------------------------------------------------

# FASE 19 --- HISTORIAL

## Trabajador

Mostrar:

``` text
Mis postulaciones
Mis trabajos
Trabajos completados
Trabajos cancelados
Calificaciones recibidas
```

## Contratante

Mostrar:

``` text
Mis publicaciones
Publicaciones activas
Publicaciones finalizadas
Postulantes
Contrataciones
Trabajadores contratados
Calificaciones recibidas
```

------------------------------------------------------------------------

# FASE 20 --- PRUEBA COMPLETA DEL SISTEMA

Crear dos usuarios de prueba.

## Usuario A

``` text
TRABAJADOR
```

## Usuario B

``` text
CONTRATANTE
```

Probar:

``` text
A se registra
↓
A verifica OTP
↓
A completa perfil
↓
B se registra
↓
B activa contratante
↓
B crea workplace
↓
B publica oferta
↓
A encuentra oferta
↓
A guarda oferta
↓
A da like
↓
A comenta
↓
A postula
↓
B recibe notificación
↓
B revisa perfil A
↓
B acepta
↓
Se crea job
↓
Chat
↓
Trabajo realizado
↓
Job COMPLETED
↓
A califica B
↓
B califica A
↓
Se actualiza reputación
```

------------------------------------------------------------------------

# Arquitectura final

``` text
                         CHAMBAYA
                            │
              ┌─────────────┴─────────────┐
              │                           │
          FIREBASE AUTH              FIRESTORE
              │                           │
             uid                          │
              │                           ├── users
              │                           ├── workplaces
              │                           ├── publications
              │                           ├── applications
              │                           ├── jobs
              │                           ├── ratings
              │                           ├── comments
              │                           ├── likes
              │                           ├── saves
              │                           ├── reports
              │                           ├── blocks
              │                           ├── conversations
              │                           ├── messages
              │                           ├── notifications
              │                           └── categories
              │
              │
              └──────────────────────┐
                                     │
                                CLOUDINARY
                                     │
                                     ├── Fotos de perfil
                                     ├── Fotos de lugares
                                     └── Fotos de publicaciones
```

------------------------------------------------------------------------

# Orden recomendado de implementación

``` text
FASE 1   Registro
         ↓
FASE 2   Perfil
         ↓
FASE 3   Rol Contratante
         ↓
FASE 4   Workplace
         ↓
FASE 5   Publicaciones
         ↓
FASE 6   Búsqueda/Filtros
         ↓
FASE 7   Postulaciones
         ↓
FASE 8   Jobs
         ↓
FASE 9   Calificaciones
         ↓
FASE 10  Comentarios
         ↓
FASE 11  Likes/Guardados/Ocultar
         ↓
FASE 12  Denuncias/Bloqueos
         ↓
FASE 13  Chat
         ↓
FASE 14  Notificaciones
         ↓
FASE 15  Categorías
         ↓
FASE 16  Security Rules
         ↓
FASE 17  Perfil público
         ↓
FASE 18  Completar perfil
         ↓
FASE 19  Historial
         ↓
FASE 20  Pruebas integrales
```

------------------------------------------------------------------------

# Regla final para el desarrollo

No pedirle a Claude Code o Codex:

``` text
"Implementa toda la aplicación ChambAYA."
```

Es demasiado grande y puede modificar partes que ya funcionan.

Usar siempre:

``` text
"Implementa SOLAMENTE la FASE X..."
```

y después comprobar:

``` text
BUILD
↓
TEST
↓
FIREBASE
↓
UI
↓
DATOS
↓
SECURITY
↓
SIGUIENTE FASE
```

De esta forma ChambAYA puede crecer progresivamente sin tener que
rehacer el registro cuando posteriormente agregues publicaciones,
contratación, reputación, chat y demás funcionalidades.
