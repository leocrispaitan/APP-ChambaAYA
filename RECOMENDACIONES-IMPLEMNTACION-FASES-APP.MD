--- RECOEMNCIONES DE LA APP COMPLETO:


flujo de registro por fases y el documento de ChambAYA, te recomiendo no meter absolutamente todo dentro de users. La colección users debe ser el núcleo de la cuenta; las publicaciones, postulaciones, comentarios, reportes, favoritos, etc. deben ser colecciones independientes relacionadas por uid.

Esto además evita que el documento del usuario crezca demasiado: Firestore tiene un límite de 1 MiB por documento, por lo que no conviene guardar listas enormes de comentarios, likes, publicaciones o postulaciones dentro de users.

Tu documento plantea justamente trabajadores, contratantes, publicaciones, postulaciones, reputación, chat, notificaciones, geolocalización, reportes e historial.

1. Arquitectura completa que te recomiendo

La estructura general de Firestore sería:

Firestore
│
├── users
│
├── email_verifications
│
├── publications
│
├── applications
│
├── jobs
│
├── ratings
│
├── comments
│
├── publication_likes
│
├── publication_saves
│
├── publication_reports
│
├── user_reports
│
├── hidden_publications
│
├── conversations
│
├── messages
│
├── notifications
│
├── workplaces
│
├── user_blocks
│
└── categories

Y las imágenes NO van en Firestore.

Firebase Storage
│
├── users/
│   └── {uid}/
│       ├── profile/
│       └── documents/
│
├── employers/
│   └── {uid}/
│       └── workplace/
│
└── publications/
    └── {publicationId}/
        ├── image_1
        ├── image_2
        └── image_3

En Firestore solamente guardas las URLs/rutas de esas imágenes.

Importante para tu proyecto actual: Firebase Cloud Storage ya no funciona en proyectos nuevos con el plan Spark; para usar Storage debes tener Blaze, aunque puede haber uso sin costo dentro de las cuotas correspondientes. Firestore Standard sí tiene cuota gratuita, actualmente 1 GiB almacenado, 50.000 lecturas/día, 20.000 escrituras/día y 20.000 eliminaciones/día.

2. users — LA COLECCIÓN PRINCIPAL

Esta es la que tú estás viendo actualmente:

users
   └── {uid}

Y tu documento actual:

email
emailVerified
otpVerified
registrationStatus
role
uid
verifiedAt

está bien como inicio, pero yo lo ampliaría de esta manera.

users/{uid}
{
  "uid": "firebase-auth-uid",

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
    "documentNumber": "********",
    "identityVerified": true,
    "verifiedAt": "Timestamp"
  },

  "profile": {
    "firstName": "Maria",
    "lastName": "Sanchez",
    "fullName": "Maria Sanchez",

    "username": "maria_sanchez_ayacucho",

    "usernameNormalized": "maria_sanchez_ayacucho",

    "phone": "+51XXXXXXXXX",

    "profilePhotoUrl": "https://...",

    "profilePhotoPath": "users/UID/profile/profile.jpg",

    "profilePhotoSource": "GOOGLE",

    "bio": "",

    "district": "Carmen Alto",

    "province": "Huamanga",

    "department": "Ayacucho",

    "country": "Peru"
  },

  "location": {
    "latitude": -13.16,
    "longitude": -74.22,

    "district": "Carmen Alto",

    "locationPermission": true
  },

  "worker": {
    "enabled": true,

    "experienceYears": 4,

    "specialties": [
      "Albanileria",
      "Pintura",
      "Jardineria"
    ],

    "skills": [
      "Pintura interior",
      "Pintura exterior",
      "Acabados"
    ],

    "workCount": 28,

    "ratingAverage": 4.9,

    "ratingCount": 18,

    "profileCompleted": 90
  },

  "employer": {
    "enabled": false,

    "employerType": null,

    "businessName": null,

    "sector": null,

    "ruc": null,

    "workplaceId": null,

    "publishedCount": 0,

    "hiredCount": 0,

    "ratingAverage": 0,

    "ratingCount": 0
  },

  "privacy": {
    "showPhone": false,
    "showExactAddress": false,
    "showEmail": false
  },

  "statistics": {
    "applicationsCount": 0,
    "publicationsCount": 0,
    "completedJobsCount": 0,
    "savedPublicationsCount": 0,
    "receivedRatingsCount": 0
  },

  "createdAt": "Timestamp",

  "updatedAt": "Timestamp",

  "lastLoginAt": "Timestamp"
}
3. ¿Por qué roles y no solamente role?

Actualmente tienes:

"role": "TRABAJADOR"

Yo cambiaría a:

"roles": [
  "TRABAJADOR"
]

y:

"activeRole": "TRABAJADOR"

Porque posteriormente podrías tener:

"roles": [
  "TRABAJADOR",
  "CONTRATANTE"
]

Entonces el mismo usuario puede hacer:

👷 Trabajador
        ↓
Buscar chambas
Postular
Trabajar
Calificar

        +

📢 Contratante
        ↓
Publicar
Buscar trabajadores
Contratar
Calificar

Eso resuelve exactamente el caso que mencionaste: un trabajador también puede publicar una oferta, pero primero debe completar los requisitos de contratante.

4. Fases de registro

Tu flujo quedaría muy limpio.

FASE 1 — Identidad
Trabajador
"identity": {
  "documentType": "DNI",
  "documentNumber": "********",
  "identityVerified": true
}

La API de DNI devuelve los datos necesarios:

Nombre
Apellidos
Nombre completo
DNI

No necesitas pedir nuevamente al usuario todo eso.

Contratante

Puede elegir:

○ DNI
○ RUC

Por lo tanto:

"identity": {
  "documentType": "RUC",
  "documentNumber": "********",
  "identityVerified": true
}

Pero no asumiría que todo contratante es empresa.

Podrías tener:

PERSONA
EMPRESA
NEGOCIO
INDEPENDIENTE
5. FASE 2 — Credenciales

Aquí mantienes:

"auth": {
  "provider": "EMAIL",
  "email": "usuario@gmail.com",
  "emailVerified": false,
  "otpVerified": false
}

O:

"auth": {
  "provider": "GOOGLE",
  "email": "usuario@gmail.com",
  "emailVerified": true,
  "otpVerified": true
}
MUY IMPORTANTE

Nunca guardes la contraseña en Firestore.

Firebase Authentication debe manejarla.

En Firestore solamente:

"provider": "EMAIL"

o:

"provider": "GOOGLE"
6. FASE 3 — OTP

Tu colección:

email_verifications

puede quedar así:

{
  "uid": "UID",

  "email": "usuario@gmail.com",

  "otpHash": "HASH_DEL_CODIGO",

  "status": "PENDING",

  "attempts": 0,

  "maxAttempts": 5,

  "expiresAt": "Timestamp",

  "createdAt": "Timestamp",

  "usedAt": null
}

No guardaría:

"otp": "123456"

en texto plano.

Una vez validado:

otpVerified = true

y:

registrationStatus = VERIFIED
7. FASE 4 — Usuario creado

Entonces tienes:

users/{uid}

con:

{
  "registrationStatus": "VERIFIED",
  "accountStatus": "ACTIVE",
  "roles": ["TRABAJADOR"]
}

Y no obligas al usuario a rellenar 30 campos durante el registro.

Eso es exactamente lo que te recomiendo.

8. Perfil después del registro

Después de entrar a la aplicación aparece:

Completa tu perfil

90% completo

como en tu diseño.

Entonces puede completar:

Trabajador
Foto
Nombre de usuario
Teléfono
Distrito
Experiencia
Especialidades
Habilidades
Descripción
Disponibilidad
Experiencia laboral
Trabajos realizados

Pero el registro inicial solamente necesita lo indispensable.

9. Imagen de perfil

Aquí tienes tres posibilidades:

Google
"profilePhotoSource": "GOOGLE",
"profilePhotoUrl": "URL_GOOGLE"
Foto predeterminada
"profilePhotoSource": "DEFAULT",
"profilePhotoUrl": "URL_DEFAULT"
Foto subida por usuario
"profilePhotoSource": "CUSTOM",
"profilePhotoUrl": "URL_STORAGE",
"profilePhotoPath": "users/UID/profile/profile.jpg"

Así puedes permitir:

Cambiar foto de perfil

sin modificar toda la cuenta.

10. Perfil de contratante

Si activa el rol:

"roles": [
  "TRABAJADOR",
  "CONTRATANTE"
]

se habilita:

"employer": {
  "enabled": true,

  "employerType": "EMPRESA",

  "businessName": "Constructora Andina",

  "commercialName": "Constructora Andina",

  "sector": "CONSTRUCCION",

  "ruc": "20XXXXXXXXX",

  "description": "Empresa dedicada a proyectos de construcción.",

  "publishedCount": 15,

  "hiredCount": 12,

  "ratingAverage": 4.8,

  "ratingCount": 20
}

Si es una persona:

"employerType": "PERSONA"

y:

"ruc": null
11. workplaces

Para el lugar del establecimiento.

Esto es especialmente importante para tu idea.

workplaces
   └── {workplaceId}
{
  "workplaceId": "WORKPLACE_ID",

  "ownerUid": "UID",

  "name": "Ferreteria El Sol",

  "type": "LOCAL_COMERCIAL",

  "sector": "FERRETERIA",

  "description": "Venta de materiales de construccion.",

  "address": "Av. Principal 123",

  "district": "Carmen Alto",

  "province": "Huamanga",

  "department": "Ayacucho",

  "location": {
    "latitude": -13.16,
    "longitude": -74.22
  },

  "photoUrl": "https://...",

  "photoPath": "employers/UID/workplace/photo.jpg",

  "verified": false,

  "createdAt": "Timestamp",

  "updatedAt": "Timestamp"
}
12. publications — OFERTAS DE TRABAJO

Esta será una de las colecciones principales.

publications
   └── {publicationId}

Ejemplo:

{
  "publicationId": "PUB001",

  "ownerUid": "UID_CONTRATANTE",

  "status": "ACTIVE",

  "visibility": "PUBLIC",

  "type": "JOB_OFFER",

  "title": "Se requiere pintor profesional",

  "description": "Se requiere pintor con experiencia...",

  "category": "PINTURA",

  "subcategory": "PINTURA_INTERIOR",

  "skillsRequired": [
    "Pintura interior",
    "Pintura exterior"
  ],

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

    "latitude": -13.16,
    "longitude": -74.22,

    "exactAddress": "..."
  },

  "workplaceId": "WORKPLACE_ID",

  "images": [
    {
      "url": "https://...",
      "storagePath": "publications/PUB001/image1.jpg"
    }
  ],

  "publisher": {
    "uid": "UID_CONTRATANTE",
    "name": "Constructora Andina",
    "username": "constructora_andina",
    "photoUrl": "https://...",
    "verified": true,
    "employerType": "EMPRESA",
    "sector": "CONSTRUCCION"
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
Un detalle muy importante

Aunque publisher provenga de users, sí conviene copiar esos datos en la publicación.

¿Por qué?

Si mañana la empresa cambia:

Constructora Andina

por:

Constructora Ayacucho

no quieres que todas las publicaciones antiguas cambien automáticamente.

Por eso guardas un snapshot del publicador.

13. Publicación solamente con texto o con fotografías

Tu idea puede funcionar así:

Opción A
Título
Descripción
Opción B
Título
Descripción
Foto
Opción C
Título
Descripción
Foto 1
Foto 2
Foto 3

Por ejemplo:

"images": []

si no hay fotografías.

O:

"images": [
  {
    "url": "https://...",
    "storagePath": "publications/PUB001/1.jpg"
  },
  {
    "url": "https://...",
    "storagePath": "publications/PUB001/2.jpg"
  }
]
14. applications — POSTULACIONES

Cuando María pulsa:

Postular

creas:

applications
   └── {applicationId}
{
  "applicationId": "APP001",

  "publicationId": "PUB001",

  "workerUid": "UID_TRABAJADOR",

  "employerUid": "UID_CONTRATANTE",

  "status": "PENDING",

  "worker": {
    "name": "Maria Sanchez",
    "username": "maria_sanchez_ayacucho",
    "photoUrl": "https://...",
    "experienceYears": 4,
    "ratingAverage": 4.9
  },

  "message": "Estoy interesada en el trabajo.",

  "createdAt": "Timestamp",

  "updatedAt": "Timestamp"
}

Estados:

PENDING
ACCEPTED
REJECTED
CANCELLED
WITHDRAWN
COMPLETED

Esto alimenta:

Mis postulaciones

15. jobs — TRABAJOS REALIZADOS

Cuando una postulación es aceptada, puedes crear un trabajo:

jobs
   └── {jobId}
{
  "jobId": "JOB001",

  "publicationId": "PUB001",

  "workerUid": "UID_TRABAJADOR",

  "employerUid": "UID_CONTRATANTE",

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

Cuando termina:

COMPLETED

y entonces se habilita la calificación.

16. ratings — CALIFICACIONES

No pondría todas las calificaciones dentro de users.

Crear:

ratings
   └── {ratingId}
{
  "ratingId": "RATING001",

  "jobId": "JOB001",

  "publicationId": "PUB001",

  "fromUid": "UID_EVALUADOR",

  "toUid": "UID_EVALUADO",

  "rating": 5,

  "comment": "Muy responsable y puntual.",

  "type": "USER",

  "createdAt": "Timestamp"
}

Puedes tener:

Trabajador → Contratante
Contratante → Trabajador

Esto está alineado con tu sistema planteado de que ambas personas se califican después del trabajo.

17. comments — COMENTARIOS

Para comentarios de publicaciones:

comments
   └── {commentId}
{
  "commentId": "COMMENT001",

  "publicationId": "PUB001",

  "authorUid": "UID",

  "authorName": "Maria Sanchez",

  "authorUsername": "maria_sanchez_ayacucho",

  "authorPhotoUrl": "https://...",

  "text": "¿El trabajo sigue disponible?",

  "status": "VISIBLE",

  "createdAt": "Timestamp",

  "updatedAt": "Timestamp"
}
18. publication_likes

No hagas:

"likedBy": [
  "uid1",
  "uid2",
  "uid3",
  ...
]

porque puede crecer demasiado.

Mejor:

publication_likes
   └── {publicationId_uid}

Ejemplo:

{
  "publicationId": "PUB001",

  "userUid": "UID001",

  "createdAt": "Timestamp"
}

Y en publications solamente:

"statistics": {
  "likes": 132
}
19. publication_saves

Para:

🔖 Guardar

publication_saves
   └── {publicationId_uid}
{
  "publicationId": "PUB001",

  "userUid": "UID001",

  "createdAt": "Timestamp"
}

Así puedes tener:

Mis publicaciones guardadas

20. hidden_publications

Para:

No me interesa

hidden_publications
   └── {userUid_publicationId}
{
  "userUid": "UID001",

  "publicationId": "PUB001",

  "createdAt": "Timestamp",

  "reason": "NOT_INTERESTED"
}

Entonces esa publicación deja de aparecerle a ese usuario.

21. publication_reports

Para el menú que aparece en tu segunda imagen:

🚨 Denunciar publicación

publication_reports
   └── {reportId}
{
  "reportId": "REPORT001",

  "publicationId": "PUB001",

  "reporterUid": "UID001",

  "reason": "FRAUD",

  "description": "La oferta parece falsa.",

  "status": "PENDING",

  "createdAt": "Timestamp"
}

Categorías:

FRAUD
SCAM
INAPPROPRIATE
FALSE_INFORMATION
SPAM
DANGEROUS_JOB
OTHER
22. user_reports

También debes permitir:

Denunciar usuario

{
  "reportId": "REPORT_USER001",

  "reportedUid": "UID_PROBLEMATICO",

  "reporterUid": "UID001",

  "reason": "SCAM",

  "description": "...",

  "status": "PENDING",

  "createdAt": "Timestamp"
}

Esto es diferente de denunciar una publicación.

23. user_blocks

Para:

Bloquear usuario

user_blocks
   └── {blockerUid_blockedUid}
{
  "blockerUid": "UID001",

  "blockedUid": "UID002",

  "createdAt": "Timestamp"
}
24. conversations

Para el chat:

conversations
   └── {conversationId}
{
  "conversationId": "CONV001",

  "participants": [
    "UID001",
    "UID002"
  ],

  "publicationId": "PUB001",

  "lastMessage": "Hola, ¿sigue disponible?",

  "lastMessageAt": "Timestamp",

  "createdAt": "Timestamp"
}
25. messages
messages
   └── {messageId}
{
  "conversationId": "CONV001",

  "senderUid": "UID001",

  "receiverUid": "UID002",

  "type": "TEXT",

  "text": "Hola, ¿sigue disponible?",

  "read": false,

  "createdAt": "Timestamp"
}

Después puedes añadir:

TEXT
IMAGE
LOCATION
SYSTEM

Esto coincide con tu idea de que puedan conversar, compartir ubicación, negociar precio y acordar horario.

26. notifications
notifications
   └── {notificationId}
{
  "notificationId": "NOTIF001",

  "recipientUid": "UID001",

  "type": "NEW_APPLICATION",

  "title": "Nueva postulación",

  "message": "Un trabajador ha postulado a tu oferta.",

  "publicationId": "PUB001",

  "senderUid": "UID002",

  "read": false,

  "createdAt": "Timestamp"
}

Otros tipos:

NEW_APPLICATION
APPLICATION_ACCEPTED
APPLICATION_REJECTED
NEW_MESSAGE
NEW_RATING
NEW_COMMENT
NEW_LIKE
PUBLICATION_EXPIRING
NEW_NEARBY_PUBLICATION

Tu documento también contempla notificaciones de nuevas ofertas cercanas.

27. categories

En lugar de escribir las categorías manualmente por todas partes:

categories
{
  "id": "PINTURA",

  "name": "Pintura",

  "icon": "format_paint",

  "active": true,

  "order": 3
}

Y podrías tener:

ALBANILERIA
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

Son las categorías planteadas en tu documento.

28. Estructura final completa

En resumen, yo dejaría tu Firestore así:

📁 users
   └── {uid}

📁 email_verifications
   └── {verificationId}

📁 workplaces
   └── {workplaceId}

📁 publications
   └── {publicationId}

📁 applications
   └── {applicationId}

📁 jobs
   └── {jobId}

📁 ratings
   └── {ratingId}

📁 comments
   └── {commentId}

📁 publication_likes
   └── {publicationId_uid}

📁 publication_saves
   └── {publicationId_uid}

📁 hidden_publications
   └── {userUid_publicationId}

📁 publication_reports
   └── {reportId}

📁 user_reports
   └── {reportId}

📁 user_blocks
   └── {blockerUid_blockedUid}

📁 conversations
   └── {conversationId}

📁 messages
   └── {messageId}

📁 notifications
   └── {notificationId}

📁 categories
   └── {categoryId}
29. Firebase Storage

Las imágenes:

Perfil trabajador
users/{uid}/profile/profile.jpg
Perfil contratante
users/{uid}/profile/profile.jpg
Lugar/establecimiento
users/{uid}/workplace/workplace.jpg
Publicaciones
publications/{publicationId}/image_1.jpg
publications/{publicationId}/image_2.jpg
publications/{publicationId}/image_3.jpg

Y en Firestore:

{
  "url": "https://...",
  "storagePath": "publications/PUB001/image_1.jpg"
}

No guardes el archivo/base64 dentro de Firestore. Firestore está pensado para datos; las imágenes deben estar en Storage. Además, el documento de Firestore tiene un máximo de 1 MiB.

30. Algo que cambiaría de tu Firebase actual

Actualmente tienes:

users
 └── UID
      ├── email
      ├── emailVerified
      ├── otpVerified
      ├── registrationStatus
      ├── role
      ├── uid
      └── verifiedAt

No necesitas borrarlo.

Puedes evolucionarlo hacia:

users
 └── UID
      │
      ├── uid
      ├── accountStatus
      ├── registrationStatus
      ├── roles[]
      ├── activeRole
      │
      ├── auth{}
      │
      ├── identity{}
      │
      ├── profile{}
      │
      ├── location{}
      │
      ├── worker{}
      │
      ├── employer{}
      │
      ├── privacy{}
      │
      ├── statistics{}
      │
      ├── createdAt
      ├── updatedAt
      └── lastLoginAt

Y luego las demás colecciones se relacionan mediante:

uid
publicationId
applicationId
jobId
conversationId
31. El flujo completo de tu usuario quedaría así
REGISTRO
   │
   ├── FASE 1
   │     DNI / RUC
   │     ↓
   │     Validación de identidad
   │
   ├── FASE 2
   │     Google
   │       o
   │     Correo + contraseña
   │
   ├── FASE 3
   │     OTP
   │     ↓
   │     correo verificado
   │
   └── FASE 4
         Crear users/{uid}
              │
              ↓
        ENTRAR A CHAMBAYA
              │
       ┌──────┴───────┐
       ↓              ↓
   TRABAJADOR     CONTRATANTE
       │              │
       ↓              ↓
   Completar       Completar
    perfil          perfil
       │              │
       ↓              ↓
 Buscar ofertas    Publicar
       │              │
    Postular       Postulantes
       │              │
       └──────┬───────┘
              ↓
            CHAT
              ↓
           TRABAJO
              ↓
         FINALIZADO
              ↓
       ⭐ CALIFICACIONES
              ↓
          REPUTACIÓN

Y eso encaja con el objetivo original de ChambAYA: conectar a quienes necesitan trabajadores con quienes buscan trabajos temporales, incorporando perfiles, ubicación, postulaciones, reputación, chat y seguridad.

Mi recomendación concreta para tu implementación ahora

No intentes crear las 17 colecciones manualmente de golpe en Firebase Console. Empieza solamente con:

1. users
2. email_verifications
3. categories
4. publications
5. applications
6. workplaces

y deja preparadas conceptualmente:

ratings
comments
likes
saves
reports
blocks
conversations
messages
notifications
jobs

Firestore crea las colecciones automáticamente cuando tu aplicación escribe el primer documento. Así puedes desarrollar fase por fase sin tener una consola llena de documentos vacíos.

Y para tu fase actual, la colección users debería ser la prioridad: ahí tienes que dejar bien diseñada desde ahora la compatibilidad con TRABAJADOR + CONTRATANTE, aunque inicialmente el usuario solo se registre como trabajador.