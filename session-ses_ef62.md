# Análisis integral de proyecto

**Session ID:** ses_ef623f52cffeJtD5i4nlbjDOUS
**Created:** 4/10/2026, 21:19:38
**Updated:** 4/10/2026, 21:25:55

---

## User

Hola quiero que analices este proyecto de inicio a fin 

---

## Assistant (Build · Muse Spark 1.3 Free · 6.7s)

_Thinking:_



Voy a analizar tu proyecto ChambaAYA de inicio a fin.

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA</path>
<type>directory</type>
<entries>
.firebaserc
.git/
.gitignore
.gradle/
.idea/
.kotlin/
app/
build.gradle.kts
CHAMBAYA_IMPLEMENTACION_FASES.md
FASE-1-REANUDACION-REGISTRO.md
FASE-1-REGISTRO-COMPLETO.md
firebase.json
firestore.indexes.json
firestore.rules
functions/
gradle.properties
gradle/
gradlew
gradlew.bat
local.properties
README.md
RECOMENDACIONES-IMPLEMNTACION-FASES-APP.MD
settings.gradle.kts

(23 entries)
</entries>
```

---

## Assistant (Build · Muse Spark 1.3 Free · 3.6s)

_Thinking:_



**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\README.md"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\README.md</path>
<type>file</type>
<content>
1: # APP-ChambaAYA

(End of file - total 1 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\CHAMBAYA_IMPLEMENTACION_FASES.md"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\CHAMBAYA_IMPLEMENTACION_FASES.md</path>
<type>file</type>
<content>
1: # ChambAYA --- Plan maestro de implementación con Claude Code / Codex
2: 
3: ## Objetivo
4: 
5: Implementar ChambAYA de forma incremental, segura y profesional usando
6: **Firebase Authentication, y Cloudinary (almacenamiento de imágenes)**,
7: trabajando fase por fase.
8: 
9: La estrategia es:
10: 
11: 1.  Terminar y dejar sólida la creación de usuarios.
12: 2.  Mostrar y completar los datos desde el perfil.
13: 3.  Incorporar el rol CONTRATANTE sin romper TRABAJADOR.
14: 4.  Crear lugares/establecimientos.
15: 5.  Crear publicaciones de trabajo.
16: 6.  Crear postulaciones y contratación.
17: 7.  Crear historial y trabajos realizados.
18: 8.  Crear calificaciones y reputación.
19: 9.  Crear comentarios, likes, guardados, ocultar y denuncias.
20: 10. Crear chat y notificaciones.
21: 11. Aplicar seguridad de Firestore y del preset de subida de Cloudinary.
22: 12. Probar todo el flujo de extremo a extremo.
23: 
24: > **Regla fundamental:** cada fase debe ser implementada, compilada y
25: > probada antes de pasar a la siguiente. No modificar funcionalidades
26: > que todavía no corresponden a la fase actual.
27: 
28: ------------------------------------------------------------------------
29: 
30: # 0. Arquitectura general
31: 
32: ## Roles
33: 
34: Un usuario tendrá una sola cuenta Firebase Authentication y un único
35: `uid`.
36: 
37: Puede tener uno o ambos roles:
38: 
39: ``` text
40: TRABAJADOR
41: CONTRATANTE
42: ```
43: 
44: Por ello se utilizará:
45: 
46: ``` json
47: {
48:   "roles": ["TRABAJADOR"],
49:   "activeRole": "TRABAJADOR"
50: }
51: ```
52: 
53: o posteriormente:
54: 
55: ``` json
56: {
57:   "roles": ["TRABAJADOR", "CONTRATANTE"],
58:   "activeRole": "TRABAJADOR"
59: }
60: ```
61: 
62: No crear dos cuentas para una misma persona.
63: 
64: ------------------------------------------------------------------------
65: 
66: # 1. Estructura general de Firestore
67: 
68: ``` text
69: users
70: email_verifications
71: categories
72: workplaces
73: publications
74: applications
75: jobs
76: ratings
77: comments
78: publication_likes
79: publication_saves
80: hidden_publications
81: publication_reports
82: user_reports
83: user_blocks
84: conversations
85: messages
86: notifications
87: ```
88: 
89: ## Cloudinary (almacenamiento de imágenes)
90: 
91: Las imágenes y archivos no se almacenarán directamente en Firestore.
92: Se subirán a Cloudinary usando carpetas equivalentes a:
93: 
94: ``` text
95: users/{uid}/profile/profile
96: 
97: users/{uid}/workplace/photo_1
98: 
99: publications/{publicationId}/image_1
100: publications/{publicationId}/image_2
101: publications/{publicationId}/image_3
102: ```
103: 
104: La subida se hace desde Android con el SDK de Cloudinary usando un
105: **unsigned upload preset** (configurado en el dashboard, restringido
106: por carpeta, tamaño y tipo de archivo).
107: 
108: Firestore solamente guardará:
109: 
110: ``` text
111: url
112: publicId
113: ```
114: 
115: ### Credenciales Cloudinary
116: 
117: ``` text
118: API Key: 619274453168488
119: API Secret: W3mN_1hUVkUbkaU-s1Dhaezx25Q
120: Cloud Name: vtmk2tgh
121: ```
122: 
123: Nota: el API Secret solo se usa en un backend/Cloud Function para
124: operaciones firmadas (por ejemplo borrar una imagen). Nunca debe
125: incluirse en el código de la app Android ni subirse a un repositorio
126: público; el Cloud Name y el nombre del upload preset (unsigned) son
127: los únicos datos que sí van dentro de la app.
128: 
129: ------------------------------------------------------------------------
130: 
131: # 2. Reglas para Claude Code / Codex
132: 
133: Utilizar estas reglas en TODAS las fases:
134: 
135: ``` text
136: 1. Antes de modificar código, inspecciona la estructura actual del proyecto.
137: 2. No reemplaces funcionalidades existentes que ya funcionan.
138: 3. Reutiliza las clases, repositorios, ViewModels, Activities, Fragments y layouts existentes cuando corresponda.
139: 4. No dupliques lógica.
140: 5. Mantén arquitectura limpia y consistente con el proyecto actual.
141: 6. Firebase Authentication maneja contraseñas. Nunca guardar contraseñas en Firestore.
142: 7. Nunca guardar imágenes como Base64 dentro de Firestore.
143: 8. Las imágenes deben subirse a Cloudinary (unsigned upload preset) y Firestore solamente guardará `url`/`publicId`.
144: 9. Usar uid de Firebase Authentication como identificador principal del usuario.
145: 10. No almacenar DNI/RUC como información pública.
146: 11. Validar permisos y datos también mediante Firestore Security Rules, no solamente desde Android.
147: 12. Usar Timestamp de Firestore para fechas.
148: 13. Mantener nombres de campos consistentes.
149: 14. Evitar listas que puedan crecer indefinidamente dentro de un documento.
150: 15. Para likes, guardados, denuncias, comentarios, etc., utilizar colecciones independientes.
151: 16. No implementar fases futuras antes de terminar la fase actual.
152: 17. Después de cada cambio ejecutar build/tests correspondientes.
153: 18. Si detectas un conflicto con código existente, explicar primero el conflicto y elegir la solución que preserve la funcionalidad actual.
154: ```
155: 
156: ------------------------------------------------------------------------
157: 
158: # FASE 1 --- REGISTRO Y CREACIÓN DEL USUARIO
159: 
160: ## Objetivo
161: 
162: Completar el flujo:
163: 
164: ``` text
165: FASE 1
166: DNI/RUC
167: ↓
168: Validación de identidad
169: 
170: FASE 2
171: Google o correo + contraseña
172: ↓
173: Credenciales
174: 
175: FASE 3
176: OTP
177: ↓
178: Correo verificado
179: 
180: FASE 4
181: Crear users/{uid}
182: ↓
183: Usuario registrado
184: ```
185: 
186: No pedir todos los datos del perfil durante el registro.
187: 
188: El usuario completará posteriormente su perfil desde `Mi Perfil`.
189: 
190: ------------------------------------------------------------------------
191: 
192: ## 1.1 Datos mínimos de registro
193: 
194: ### Trabajador
195: 
196: ``` text
197: DNI
198: Correo o Google
199: Contraseña si corresponde
200: OTP
201: Rol
202: ```
203: 
204: ### Contratante
205: 
206: ``` text
207: DNI o RUC
208: Correo o Google
209: Contraseña si corresponde
210: OTP
211: Rol
212: ```
213: 
214: El contratante no debe estar obligado a tener RUC.
215: 
216: Puede ser:
217: 
218: ``` text
219: PERSONA
220: EMPRESA
221: NEGOCIO
222: INDEPENDIENTE
223: ```
224: 
225: ------------------------------------------------------------------------
226: 
227: ## 1.2 Colección users
228: 
229: Crear:
230: 
231: ``` text
232: users/{uid}
233: ```
234: 
235: Documento inicial:
236: 
237: ``` json
238: {
239:   "uid": "UID_FIREBASE",
240: 
241:   "accountStatus": "ACTIVE",
242: 
243:   "registrationStatus": "VERIFIED",
244: 
245:   "roles": [
246:     "TRABAJADOR"
247:   ],
248: 
249:   "activeRole": "TRABAJADOR",
250: 
251:   "auth": {
252:     "provider": "EMAIL",
253:     "email": "usuario@gmail.com",
254:     "emailVerified": true,
255:     "otpVerified": true
256:   },
257: 
258:   "identity": {
259:     "documentType": "DNI",
260:     "documentNumber": "DNI_VALIDADO",
261:     "identityVerified": true,
262:     "verifiedAt": "Timestamp"
263:   },
264: 
265:   "createdAt": "Timestamp",
266:   "updatedAt": "Timestamp",
267:   "lastLoginAt": "Timestamp"
268: }
269: ```
270: 
271: Para Google:
272: 
273: ``` json
274: {
275:   "auth": {
276:     "provider": "GOOGLE",
277:     "email": "usuario@gmail.com",
278:     "emailVerified": true,
279:     "otpVerified": true
280:   }
281: }
282: ```
283: 
284: ------------------------------------------------------------------------
285: 
286: ## 1.3 OTP
287: 
288: Colección:
289: 
290: ``` text
291: email_verifications
292: ```
293: 
294: Documento:
295: 
296: ``` json
297: {
298:   "uid": "UID",
299:   "email": "usuario@gmail.com",
300:   "otpHash": "HASH",
301:   "status": "PENDING",
302:   "attempts": 0,
303:   "maxAttempts": 5,
304:   "expiresAt": "Timestamp",
305:   "createdAt": "Timestamp",
306:   "usedAt": null
307: }
308: ```
309: 
310: No guardar el OTP numérico en texto plano.
311: 
312: ------------------------------------------------------------------------
313: 
314: ## 1.4 Prompt para Claude Code / Codex --- FASE 1
315: 
316: ``` text
317: Estoy desarrollando ChambAYA en Android con Firebase.
318: 
319: Necesito implementar SOLAMENTE la FASE 1 del plan de registro.
320: 
321: Primero inspecciona todo el proyecto actual y determina:
322: - Activity/Fragment del registro.
323: - ViewModels.
324: - Repositories.
325: - Firebase Authentication.
326: - Firestore.
327: - código existente de validación DNI/RUC.
328: - flujo actual de Google.
329: - flujo actual de correo/contraseña.
330: - OTP existente.
331: - modelos relacionados con User.
332: 
333: IMPORTANTE:
334: No reemplaces ni rompas la validación DNI/RUC que ya funciona.
335: No implementes todavía perfil completo, publicaciones, chat, postulaciones ni otras fases.
336: 
337: Objetivo de esta fase:
338: 
339: 1. Mantener la FASE 1 actual de validación DNI/RUC.
340: 2. Mantener la FASE 2 actual de credenciales.
341: 3. Mantener la FASE 3 actual de OTP.
342: 4. Al finalizar correctamente el OTP, crear/actualizar users/{uid}.
343: 5. Usar Firebase Authentication como fuente de identidad.
344: 6. Nunca guardar contraseñas en Firestore.
345: 7. Usar el uid de Firebase Auth como ID del documento users.
346: 8. Crear los campos:
347:    - uid
348:    - accountStatus
349:    - registrationStatus
350:    - roles
351:    - activeRole
352:    - auth.provider
353:    - auth.email
354:    - auth.emailVerified
355:    - auth.otpVerified
356:    - identity.documentType
357:    - identity.documentNumber
358:    - identity.identityVerified
359:    - identity.verifiedAt
360:    - createdAt
361:    - updatedAt
362:    - lastLoginAt
363: 9. Si el usuario es TRABAJADOR:
364:    roles = ["TRABAJADOR"]
365: 10. Si es CONTRATANTE:
366:    roles = ["CONTRATANTE"]
367: 11. No obligar al usuario a rellenar nombre de usuario, experiencia, habilidades, dirección exacta, fotos personalizadas, etc. durante este registro.
368: 12. Esos datos se completarán después desde Mi Perfil.
369: 
370: Implementa con el patrón arquitectónico existente del proyecto.
371: 
372: Después:
373: - compila el proyecto;
374: - corrige errores;
375: - verifica el flujo completo;
376: - documenta exactamente qué archivos modificaste;
377: - muestra la estructura final de users;
378: - no avances a la FASE 2.
379: ```
380: 
381: ------------------------------------------------------------------------
382: 
383: # FASE 2 --- PERFIL DEL USUARIO
384: 
385: ## Objetivo
386: 
387: Después de registrarse, el usuario entra a la aplicación.
388: 
389: Desde:
390: 
391: ``` text
392: Mi Perfil
393: ```
394: 
395: puede completar los datos que no eran necesarios durante el registro.
396: 
397: ------------------------------------------------------------------------
398: 
399: # 2.1 Perfil base
400: 
401: Agregar a `users/{uid}`:
402: 
403: ``` json
404: {
405:   "profile": {
406:     "firstName": "Maria",
407:     "lastName": "Sanchez",
408:     "fullName": "Maria Sanchez",
409:     "username": "maria_sanchez_ayacucho",
410:     "usernameNormalized": "maria_sanchez_ayacucho",
411:     "phone": "+51XXXXXXXXX",
412:     "profilePhotoUrl": "",
413:     "profilePhotoPath": "",
414:     "profilePhotoSource": "DEFAULT",
415:     "bio": "",
416:     "district": "Carmen Alto",
417:     "province": "Huamanga",
418:     "department": "Ayacucho",
419:     "country": "Peru"
420:   }
421: }
422: ```
423: 
424: ------------------------------------------------------------------------
425: 
426: # 2.2 Foto de perfil
427: 
428: Fuentes:
429: 
430: ``` text
431: GOOGLE
432: DEFAULT
433: CUSTOM
434: ```
435: 
436: Ejemplo:
437: 
438: ``` json
439: {
440:   "profilePhotoSource": "CUSTOM",
441:   "profilePhotoUrl": "https://res.cloudinary.com/.../profile.jpg",
442:   "profilePhotoPublicId": "users/UID/profile/profile"
443: }
444: ```
445: 
446: Cloudinary (carpeta):
447: 
448: ``` text
449: users/{uid}/profile/profile
450: ```
451: 
452: ------------------------------------------------------------------------
453: 
454: # 2.3 Perfil de trabajador
455: 
456: Agregar:
457: 
458: ``` json
459: {
460:   "worker": {
461:     "enabled": true,
462:     "experienceYears": 4,
463:     "specialties": [
464:       "Albañilería",
465:       "Pintura",
466:       "Jardinería"
467:     ],
468:     "skills": [
469:       "Pintura interior",
470:       "Pintura exterior",
471:       "Acabados"
472:     ],
473:     "workCount": 0,
474:     "ratingAverage": 0,
475:     "ratingCount": 0,
476:     "profileCompleted": 0
477:   }
478: }
479: ```
480: 
481: ------------------------------------------------------------------------
482: 
483: # 2.4 Privacidad
484: 
485: ``` json
486: {
487:   "privacy": {
488:     "showPhone": false,
489:     "showExactAddress": false,
490:     "showEmail": false
491:   }
492: }
493: ```
494: 
495: El DNI, correo y dirección exacta no deben mostrarse públicamente por
496: defecto.
497: 
498: ------------------------------------------------------------------------
499: 
500: # 2.5 Estadísticas
501: 
502: ``` json
503: {
504:   "statistics": {
505:     "applicationsCount": 0,
506:     "publicationsCount": 0,
507:     "completedJobsCount": 0,
508:     "savedPublicationsCount": 0,
509:     "receivedRatingsCount": 0
510:   }
511: }
512: ```
513: 
514: ------------------------------------------------------------------------
515: 
516: ## Prompt FASE 2
517: 
518: ``` text
519: Implementa SOLAMENTE la FASE 2 de ChambAYA: PERFIL DEL USUARIO.
520: 
521: Primero inspecciona el código actual y utiliza la estructura existente.
522: 
523: No modificar el flujo de registro ya terminado.
524: 
525: Necesito que Mi Perfil cargue users/{uid} desde Firestore.
526: 
527: Mostrar:
528: - nombre completo
529: - username
530: - foto
531: - experiencia
532: - trabajos realizados
533: - calificación
534: - especialidades
535: - habilidades
536: - distrito
537: - descripción
538: 
539: Agregar edición de:
540: - username
541: - teléfono
542: - foto
543: - descripción
544: - distrito
545: - experiencia
546: - especialidades
547: - habilidades
548: 
549: La foto personalizada debe subirse a Cloudinary (unsigned upload
550: preset) a la carpeta:
551: users/{uid}/profile/profile
552: 
553: Firestore solo debe almacenar:
554: profilePhotoUrl
555: profilePhotoPublicId
556: profilePhotoSource
557: 
558: Implementar actualización parcial del documento.
559: 
560: No permitir que el usuario modifique:
561: - uid
562: - verification status
563: - identity verification
564: - registrationStatus
565: - emailVerified
566: - otpVerified
567: 
568: Validar username como único y normalizado.
569: 
570: Mantener el diseño actual de Mi Perfil y adaptarlo sin destruir la UI existente.
571: 
572: Compilar y probar.
573: 
574: No implementar todavía publicaciones, chat, postulaciones ni rol contratante completo.
575: ```
576: 
577: ------------------------------------------------------------------------
578: 
579: # FASE 3 --- ACTIVAR ROL CONTRATANTE
580: 
581: ## Objetivo
582: 
583: Un trabajador registrado puede convertirse también en contratante.
584: 
585: No crear otra cuenta.
586: 
587: Cambiar:
588: 
589: ``` json
590: {
591:   "roles": [
592:     "TRABAJADOR"
593:   ]
594: }
595: ```
596: 
597: a:
598: 
599: ``` json
600: {
601:   "roles": [
602:     "TRABAJADOR",
603:     "CONTRATANTE"
604:   ]
605: }
606: ```
607: 
608: ------------------------------------------------------------------------
609: 
610: # 3.1 Datos employer
611: 
612: ``` json
613: {
614:   "employer": {
615:     "enabled": true,
616:     "employerType": "PERSONA",
617:     "businessName": "",
618:     "commercialName": "",
619:     "sector": "",
620:     "ruc": null,
621:     "workplaceId": null,
622:     "publishedCount": 0,
623:     "hiredCount": 0,
624:     "ratingAverage": 0,
625:     "ratingCount": 0
626:   }
627: }
628: ```
629: 
630: ------------------------------------------------------------------------
631: 
632: # 3.2 Requisitos para activar contratante
633: 
634: ``` text
635: Identidad verificada
636: Correo verificado
637: Perfil básico completo
638: Teléfono verificado
639: Tipo de contratante
640: ```
641: 
642: RUC:
643: 
644: ``` text
645: opcional para persona
646: obligatorio si se declara empresa cuando corresponda
647: ```
648: 
649: No asumir que todos los contratantes tienen RUC.
650: 
651: ------------------------------------------------------------------------
652: 
653: ## Prompt FASE 3
654: 
655: ``` text
656: Implementa SOLAMENTE la FASE 3: ACTIVACIÓN DEL ROL CONTRATANTE.
657: 
658: No crear una segunda cuenta.
659: 
660: Un mismo uid puede tener:
661: ["TRABAJADOR"]
662: o:
663: ["TRABAJADOR", "CONTRATANTE"]
664: 
665: Agregar activeRole para permitir cambiar el modo.
666: 
667: Crear employer con:
668: - enabled
669: - employerType
670: - businessName
671: - commercialName
672: - sector
673: - ruc
674: - workplaceId
675: - publishedCount
676: - hiredCount
677: - ratingAverage
678: - ratingCount
679: 
680: Crear una pantalla para activar/completar el perfil de contratante.
681: 
682: Permitir:
683: PERSONA
684: EMPRESA
685: NEGOCIO
686: INDEPENDIENTE
687: 
688: DNI o RUC según corresponda.
689: 
690: No eliminar la información del trabajador.
691: 
692: Probar:
693: 1. trabajador solamente;
694: 2. contratante solamente;
695: 3. trabajador + contratante;
696: 4. cambio de activeRole.
697: 
698: No implementar publicaciones todavía.
699: ```
700: 
701: ------------------------------------------------------------------------
702: 
703: # FASE 4 --- LUGAR / ESTABLECIMIENTO
704: 
705: ## Colección
706: 
707: ``` text
708: workplaces/{workplaceId}
709: ```
710: 
711: ``` json
712: {
713:   "workplaceId": "ID",
714:   "ownerUid": "UID",
715: 
716:   "name": "Ferretería El Sol",
717: 
718:   "type": "LOCAL_COMERCIAL",
719: 
720:   "sector": "FERRETERIA",
721: 
722:   "description": "",
723: 
724:   "address": "",
725: 
726:   "district": "Carmen Alto",
727:   "province": "Huamanga",
728:   "department": "Ayacucho",
729: 
730:   "location": {
731:     "latitude": 0,
732:     "longitude": 0
733:   },
734: 
735:   "photoUrl": "",
736:   "photoPath": "",
737: 
738:   "verified": false,
739: 
740:   "createdAt": "Timestamp",
741:   "updatedAt": "Timestamp"
742: }
743: ```
744: 
745: Tipos:
746: 
747: ``` text
748: VIVIENDA
749: LOCAL_COMERCIAL
750: EMPRESA
751: TALLER
752: RESTAURANTE
753: OBRA
754: CAMPO
755: OTRO
756: ```
757: 
758: ------------------------------------------------------------------------
759: 
760: ## Prompt FASE 4
761: 
762: ``` text
763: Implementa SOLAMENTE la FASE 4: WORKPLACES.
764: 
765: Crear colección workplaces.
766: 
767: Permitir al contratante:
768: - crear lugar;
769: - editar lugar;
770: - agregar nombre;
771: - tipo;
772: - sector;
773: - descripción;
774: - dirección;
775: - distrito;
776: - ubicación;
777: - fotografía
778: - fecha y hora de publicacion - automatico sin escribir manuealmente
779: 
780: La fotografía debe subirse a Cloudinary (unsigned upload preset).
781: 
782: Guardar en Firestore solamente `url` y `publicId`.
783: 
784: Relacionar:
785: users/{uid}.employer.workplaceId
786: 
787: No implementar publicaciones todavía.
788: ```
789: 
790: ------------------------------------------------------------------------
791: 
792: # FASE 5 --- PUBLICACIONES
793: 
794: ## Colección
795: 
796: ``` text
797: publications/{publicationId}
798: ```
799: 
800: Campos principales:
801: 
802: ``` json
803: {
804:   "publicationId": "PUB001",
805:   "ownerUid": "UID",
806: 
807:   "status": "ACTIVE",
808:   "visibility": "PUBLIC",
809: 
810:   "type": "JOB_OFFER",
811: 
812:   "title": "Se requiere pintor profesional",
813: 
814:   "description": "Descripción del trabajo",
815: 
816:   "category": "PINTURA",
817: 
818:   "subcategory": "PINTURA_INTERIOR",
819: 
820:   "skillsRequired": [],
821: 
822:   "payment": {
823:     "amount": 100,
824:     "currency": "PEN",
825:     "period": "DAY",
826:     "negotiable": false
827:   },
828: 
829:   "schedule": {
830:     "startDate": "Timestamp",
831:     "endDate": "Timestamp",
832:     "startTime": "08:00",
833:     "endTime": "17:00"
834:   },
835: 
836:   "workersNeeded": 2,
837:   "workersHired": 0,
838: 
839:   "location": {
840:     "district": "Carmen Alto",
841:     "province": "Huamanga",
842:     "department": "Ayacucho",
843:     "latitude": 0,
844:     "longitude": 0,
845:     "exactAddress": ""
846:   },
847: 
848:   "workplaceId": "WORKPLACE_ID",
849: 
850:   "images": [],
851: 
852:   "publisher": {
853:     "uid": "UID",
854:     "name": "",
855:     "username": "",
856:     "photoUrl": "",
857:     "verified": false,
858:     "employerType": "",
859:     "sector": ""
860:   },
861: 
862:   "statistics": {
863:     "views": 0,
864:     "likes": 0,
865:     "comments": 0,
866:     "shares": 0,
867:     "saves": 0,
868:     "applications": 0
869:   },
870: 
871:   "featured": false,
872: 
873:   "createdAt": "Timestamp",
874:   "updatedAt": "Timestamp",
875:   "expiresAt": "Timestamp"
876: }
877: ```
878: 
879: ------------------------------------------------------------------------
880: 
881: # FASE 5.1 --- Fotografías de publicación
882: 
883: Cloudinary (carpeta):
884: 
885: ``` text
886: publications/{publicationId}/image_1
887: publications/{publicationId}/image_2
888: publications/{publicationId}/image_3
889: ```
890: 
891: Firestore:
892: 
893: ``` json
894: {
895:   "images": [
896:     {
897:       "url": "https://res.cloudinary.com/.../image_1.jpg",
898:       "publicId": "publications/PUB001/image_1"
899:     }
900:   ]
901: }
902: ```
903: 
904: Máximo recomendado inicialmente: 3 imágenes.
905: 
906: La publicación también debe funcionar sin imágenes.
907: 
908: ------------------------------------------------------------------------
909: 
910: # FASE 5.2 --- Datos automáticos
911: 
912: Al crear publicación, tomar automáticamente desde el perfil:
913: 
914: ``` text
915: nombre del contratante
916: username
917: foto
918: sector
919: tipo de contratante
920: verificación
921: workplace
922: ubicación
923: ```
924: 
925: No pedirlos nuevamente si ya existen.
926: 
927: ------------------------------------------------------------------------
928: 
929: ## Prompt FASE 5
930: 
931: ``` text
932: Implementa SOLAMENTE la FASE 5: PUBLICACIONES DE TRABAJO.
933: 
934: Crear publications/{publicationId}.
935: 
936: El contratante debe poder:
937: - crear publicación;
938: - editar publicación;
939: - eliminar/archivar publicación;
940: - pausar publicación;
941: - reactivar publicación;
942: - finalizar publicación.
943: 
944: Campos:
945: title
946: description
947: category
948: subcategory
949: skillsRequired
950: payment
951: schedule
952: workersNeeded
953: location
954: workplaceId
955: images
956: publisher
957: statistics
958: status
959: createdAt
960: updatedAt
961: expiresAt
962: 
963: Al crear la publicación, cargar automáticamente desde users/{uid}:
964: - nombre;
965: - username;
966: - foto;
967: - sector;
968: - tipo de contratante;
969: - verificación.
970: 
971: No pedir nuevamente información que ya existe.
972: 
973: Permitir publicación:
974: - solo texto;
975: - texto + imágenes.
976: 
977: Imágenes a Cloudinary (unsigned upload preset).
978: 
979: No implementar todavía postulaciones.
980: ```
981: 
982: ------------------------------------------------------------------------
983: 
984: # FASE 6 --- BUSCAR Y FILTRAR PUBLICACIONES
985: 
986: Implementar:
987: 
988: ``` text
989: Buscar por texto
990: Categoría
991: Distrito
992: Pago
993: Fecha
994: Distancia
995: Estado
996: ```
997: 
998: La pantalla principal debe mostrar:
999: 
1000: ``` text
1001: Publicaciones cercanas
1002: ```
1003: 
1004: y permitir:
1005: 
1006: ``` text
1007: Ver publicación
1008: Ver perfil del contratante
1009: Postular
1010: Guardar
1011: Me gusta
1012: Compartir
1013: No me interesa
1014: Denunciar
1015: ```
1016: 
1017: La aplicación ya contempla búsquedas por distrito, categoría, sueldo,
1018: fecha y distancia. fileciteturn0file0L35-L42
1019: 
1020: ------------------------------------------------------------------------
1021: 
1022: # FASE 7 --- POSTULACIONES
1023: 
1024: Colección:
1025: 
1026: ``` text
1027: applications/{applicationId}
1028: ```
1029: 
1030: ``` json
1031: {
1032:   "applicationId": "APP001",
1033:   "publicationId": "PUB001",
1034:   "workerUid": "UID_WORKER",
1035:   "employerUid": "UID_EMPLOYER",
1036: 
1037:   "status": "PENDING",
1038: 
1039:   "worker": {
1040:     "name": "",
1041:     "username": "",
1042:     "photoUrl": "",
1043:     "experienceYears": 0,
1044:     "ratingAverage": 0
1045:   },
1046: 
1047:   "message": "",
1048: 
1049:   "createdAt": "Timestamp",
1050:   "updatedAt": "Timestamp"
1051: }
1052: ```
1053: 
1054: Estados:
1055: 
1056: ``` text
1057: PENDING
1058: ACCEPTED
1059: REJECTED
1060: CANCELLED
1061: WITHDRAWN
1062: ```
1063: 
1064: ------------------------------------------------------------------------
1065: 
1066: ## Prompt FASE 7
1067: 
1068: ``` text
1069: Implementa SOLAMENTE postulaciones.
1070: 
1071: El trabajador puede pulsar POSTULAR.
1072: 
1073: Crear applications/{applicationId}.
1074: 
1075: Evitar postulaciones duplicadas para el mismo:
1076: workerUid + publicationId.
1077: 
1078: El contratante puede:
1079: - ver postulantes;
1080: - ver perfil;
1081: - aceptar;
1082: - rechazar.
1083: 
1084: El trabajador puede:
1085: - ver mis postulaciones;
1086: - retirar una postulación cuando corresponda;
1087: - consultar estado.
1088: 
1089: Actualizar contadores de publication.
1090: 
1091: Crear notificaciones básicas cuando:
1092: - se recibe postulación;
1093: - se acepta;
1094: - se rechaza.
1095: 
1096: No implementar todavía calificaciones.
1097: ```
1098: 
1099: ------------------------------------------------------------------------
1100: 
1101: # FASE 8 --- JOBS / TRABAJOS REALIZADOS
1102: 
1103: Cuando una postulación sea aceptada:
1104: 
1105: ``` text
1106: jobs/{jobId}
1107: ```
1108: 
1109: ``` json
1110: {
1111:   "jobId": "JOB001",
1112:   "publicationId": "PUB001",
1113:   "workerUid": "UID_WORKER",
1114:   "employerUid": "UID_EMPLOYER",
1115: 
1116:   "status": "IN_PROGRESS",
1117: 
1118:   "startedAt": "Timestamp",
1119:   "completedAt": null,
1120: 
1121:   "agreedPayment": {
1122:     "amount": 100,
1123:     "currency": "PEN"
1124:   },
1125: 
1126:   "createdAt": "Timestamp",
1127:   "updatedAt": "Timestamp"
1128: }
1129: ```
1130: 
1131: Estados:
1132: 
1133: ``` text
1134: ACCEPTED
1135: IN_PROGRESS
1136: COMPLETED
1137: CANCELLED
1138: ```
1139: 
1140: ------------------------------------------------------------------------
1141: 
1142: # FASE 9 --- CALIFICACIONES Y REPUTACIÓN
1143: 
1144: Colección:
1145: 
1146: ``` text
1147: ratings/{ratingId}
1148: ```
1149: 
1150: ``` json
1151: {
1152:   "ratingId": "RATING001",
1153: 
1154:   "jobId": "JOB001",
1155:   "publicationId": "PUB001",
1156: 
1157:   "fromUid": "UID_EVALUADOR",
1158:   "toUid": "UID_EVALUADO",
1159: 
1160:   "rating": 5,
1161: 
1162:   "comment": "Muy responsable y puntual.",
1163: 
1164:   "createdAt": "Timestamp"
1165: }
1166: ```
1167: 
1168: Permitir:
1169: 
1170: ``` text
1171: Trabajador → Contratante
1172: Contratante → Trabajador
1173: ```
1174: 
1175: No permitir calificar un trabajo que no se haya realizado/completado.
1176: 
1177: ------------------------------------------------------------------------
1178: 
1179: ## Prompt FASE 9
1180: 
1181: ``` text
1182: Implementa el sistema de reputación.
1183: 
1184: Solo permitir calificar después de completar el job.
1185: 
1186: Crear ratings/{ratingId}.
1187: 
1188: Un usuario no puede calificar dos veces el mismo job en la misma dirección.
1189: 
1190: Actualizar:
1191: ratingAverage
1192: ratingCount
1193: 
1194: Mostrar:
1195: - estrellas;
1196: - promedio;
1197: - cantidad;
1198: - comentarios.
1199: 
1200: Aplicar tanto a trabajadores como contratantes.
1201: ```
1202: 
1203: ------------------------------------------------------------------------
1204: 
1205: # FASE 10 --- COMENTARIOS
1206: 
1207: Colección:
1208: 
1209: ``` text
1210: comments/{commentId}
1211: ```
1212: 
1213: ``` json
1214: {
1215:   "commentId": "COMMENT001",
1216:   "publicationId": "PUB001",
1217: 
1218:   "authorUid": "UID",
1219: 
1220:   "authorName": "",
1221:   "authorUsername": "",
1222:   "authorPhotoUrl": "",
1223: 
1224:   "text": "",
1225: 
1226:   "status": "VISIBLE",
1227: 
1228:   "createdAt": "Timestamp",
1229:   "updatedAt": "Timestamp"
1230: }
1231: ```
1232: 
1233: Permitir:
1234: 
1235: ``` text
1236: comentar
1237: editar propio comentario
1238: eliminar propio comentario
1239: reportar comentario
1240: ```
1241: 
1242: ------------------------------------------------------------------------
1243: 
1244: # FASE 11 --- LIKES, GUARDADOS Y COMPARTIR DENUNCIAR , NO QUIERO , VER ESTO, CAMPORTIR, CALIFICAR PUBLICACION, NO ME INTERESA
1245: 
1246: ## Likes
1247: 
1248: ``` text
1249: publication_likes
1250: ```
1251: 
1252: ``` json
1253: {
1254:   "publicationId": "PUB001",
1255:   "userUid": "UID",
1256:   "createdAt": "Timestamp"
1257: }
1258: ```
1259: 
1260: ## Guardados
1261: 
1262: ``` text
1263: publication_saves
1264: ```
1265: 
1266: ``` json
1267: {
1268:   "publicationId": "PUB001",
1269:   "userUid": "UID",
1270:   "createdAt": "Timestamp"
1271: }
1272: ```
1273: 
1274: ## No me interesa
1275: 
1276: ``` text
1277: hidden_publications
1278: ```
1279: 
1280: ``` json
1281: {
1282:   "publicationId": "PUB001",
1283:   "userUid": "UID",
1284:   "reason": "NOT_INTERESTED",
1285:   "createdAt": "Timestamp"
1286: }
1287: ```
1288: 
1289: ## Compartir
1290: 
1291: El compartir por WhatsApp no necesita una colección obligatoriamente.
1292: 
1293: Usar Android Share Intent.
1294: 
1295: Registrar `shares` solamente si quieres estadísticas.
1296: 
1297: ------------------------------------------------------------------------
1298: 
1299: # FASE 12 --- DENUNCIAS Y SEGURIDAD SOCIAL
1300: 
1301: ## Denunciar publicación
1302: 
1303: ``` text
1304: publication_reports
1305: ```
1306: 
1307: ``` json
1308: {
1309:   "reportId": "REPORT001",
1310:   "publicationId": "PUB001",
1311:   "reporterUid": "UID",
1312:   "reason": "FRAUD",
1313:   "description": "",
1314:   "status": "PENDING",
1315:   "createdAt": "Timestamp"
1316: }
1317: ```
1318: 
1319: ## Denunciar usuario
1320: 
1321: ``` text
1322: user_reports
1323: ```
1324: 
1325: ``` json
1326: {
1327:   "reportId": "REPORT001",
1328:   "reportedUid": "UID",
1329:   "reporterUid": "UID",
1330:   "reason": "SCAM",
1331:   "description": "",
1332:   "status": "PENDING",
1333:   "createdAt": "Timestamp"
1334: }
1335: ```
1336: 
1337: ## Bloquear
1338: 
1339: ``` text
1340: user_blocks
1341: ```
1342: 
1343: ``` json
1344: {
1345:   "blockerUid": "UID_A",
1346:   "blockedUid": "UID_B",
1347:   "createdAt": "Timestamp"
1348: }
1349: ```
1350: 
1351: ------------------------------------------------------------------------
1352: 
1353: # FASE 13 --- CHAT
1354: 
1355: Colección:
1356: 
1357: ``` text
1358: conversations/{conversationId}
1359: ```
1360: 
1361: ``` json
1362: {
1363:   "conversationId": "CONV001",
1364: 
1365:   "participants": [
1366:     "UID_A",
1367:     "UID_B"
1368:   ],
1369: 
1370:   "publicationId": "PUB001",
1371: 
1372:   "lastMessage": "Hola, ¿sigue disponible?",
1373: 
1374:   "lastMessageAt": "Timestamp",
1375: 
1376:   "createdAt": "Timestamp"
1377: }
1378: ```
1379: 
1380: Mensajes:
1381: 
1382: ``` text
1383: conversations/{conversationId}/messages/{messageId}
1384: ```
1385: 
1386: ``` json
1387: {
1388:   "senderUid": "UID",
1389:   "type": "TEXT",
1390:   "text": "Hola",
1391:   "read": false,
1392:   "createdAt": "Timestamp"
1393: }
1394: ```
1395: 
1396: Tipos futuros:
1397: 
1398: ``` text
1399: TEXT
1400: IMAGE
1401: LOCATION
1402: SYSTEM
1403: ```
1404: 
1405: ------------------------------------------------------------------------
1406: 
1407: # FASE 14 --- NOTIFICACIONES
1408: 
1409: Colección:
1410: 
1411: ``` text
1412: notifications/{notificationId}
1413: ```
1414: 
1415: ``` json
1416: {
1417:   "notificationId": "NOTIF001",
1418: 
1419:   "recipientUid": "UID",
1420: 
1421:   "type": "NEW_APPLICATION",
1422: 
1423:   "title": "Nueva postulación",
1424: 
1425:   "message": "Un trabajador postuló a tu oferta.",
1426: 
1427:   "senderUid": "UID",
1428: 
1429:   "publicationId": "PUB001",
1430: 
1431:   "read": false,
1432: 
1433:   "createdAt": "Timestamp"
1434: }
1435: ```
1436: 
1437: Tipos:
1438: 
1439: ``` text
1440: NEW_APPLICATION
1441: APPLICATION_ACCEPTED
1442: APPLICATION_REJECTED
1443: NEW_MESSAGE
1444: NEW_RATING
1445: NEW_COMMENT
1446: NEW_LIKE
1447: NEW_NEARBY_PUBLICATION
1448: PUBLICATION_EXPIRING
1449: ```
1450: 
1451: ------------------------------------------------------------------------
1452: 
1453: # FASE 15 --- CATEGORÍAS
1454: 
1455: Colección:
1456: 
1457: ``` text
1458: categories/{categoryId}
1459: ```
1460: 
1461: ``` json
1462: {
1463:   "id": "PINTURA",
1464:   "name": "Pintura",
1465:   "icon": "format_paint",
1466:   "active": true,
1467:   "order": 3
1468: }
1469: ```
1470: 
1471: Categorías iniciales:
1472: 
1473: ``` text
1474: ALBAÑILERIA
1475: CONSTRUCCION
1476: PINTURA
1477: CARPINTERIA
1478: ELECTRICIDAD
1479: GASFITERIA
1480: JARDINERIA
1481: LIMPIEZA
1482: MUDANZAS
1483: DELIVERY
1484: COCINA
1485: AYUDANTE_GENERAL
1486: CUIDADO_NINOS
1487: CUIDADO_ADULTOS
1488: AGRICULTURA
1489: GANADERIA
1490: COSTURA
1491: VENTAS
1492: OTROS
1493: ```
1494: 
1495: ------------------------------------------------------------------------
1496: 
1497: # FASE 16 --- REGLAS DE SEGURIDAD FIRESTORE
1498: 
1499: Antes de producción, revisar todas las reglas.
1500: 
1501: Principios:
1502: 
1503: ``` text
1504: users:
1505: - el usuario puede leer/modificar solamente campos permitidos de su propio perfil;
1506: - no puede modificar uid;
1507: - no puede modificar verificaciones desde Android.
1508: 
1509: publications:
1510: - solamente contratantes habilitados pueden crear;
1511: - solamente el propietario puede editar/eliminar;
1512: - publicaciones públicas pueden ser leídas.
1513: 
1514: applications:
1515: - trabajador puede crear su propia postulación;
1516: - contratante puede consultar postulaciones de sus publicaciones;
1517: - trabajador puede consultar sus propias postulaciones.
1518: 
1519: ratings:
1520: - solamente participantes del job;
1521: - solamente después de completar.
1522: 
1523: reports:
1524: - usuario autenticado puede crear;
1525: - usuario normal no puede modificar el estado de moderación.
1526: 
1527: messages:
1528: - solamente participantes pueden leer/escribir.
1529: 
1530: Cloudinary:
1531: - usar un unsigned upload preset dedicado por tipo de recurso (perfil, workplace, publicación);
1532: - restringir el preset por carpeta, tamaño máximo y tipo de archivo desde el dashboard;
1533: - no exponer el API Secret en la app; operaciones destructivas (borrar imagen) deben pasar por un backend/Cloud Function firmado;
1534: - validar en Firestore Security Rules que el `publicId`/`url` guardado pertenezca a la carpeta esperada del uid/publicationId del usuario autenticado.
1535: ```
1536: 
1537: ------------------------------------------------------------------------
1538: 
1539: # FASE 17 --- PERFIL PÚBLICO
1540: 
1541: Separar:
1542: 
1543: ``` text
1544: Mi Perfil
1545: ```
1546: 
1547: de:
1548: 
1549: ``` text
1550: Perfil Público
1551: ```
1552: 
1553: ## Mi Perfil
1554: 
1555: Puede mostrar:
1556: 
1557: ``` text
1558: Editar perfil
1559: Completar perfil
1560: Mis postulaciones
1561: Mis trabajos
1562: Mis publicaciones
1563: Guardados
1564: Configuración
1565: Cambiar modo
1566: ```
1567: 
1568: ## Perfil público del trabajador
1569: 
1570: Mostrar:
1571: 
1572: ``` text
1573: Foto
1574: Nombre
1575: Username
1576: Verificación
1577: Calificación
1578: Trabajos realizados
1579: Experiencia
1580: Especialidades
1581: Habilidades
1582: Distrito
1583: Comentarios/reputación
1584: ```
1585: 
1586: No mostrar:
1587: 
1588: ``` text
1589: DNI
1590: contraseña
1591: email privado
1592: dirección exacta
1593: datos internos
1594: ```
1595: 
1596: ## Perfil público del contratante
1597: 
1598: Mostrar:
1599: 
1600: ``` text
1601: Foto
1602: Nombre/empresa
1603: Username
1604: Verificación
1605: Sector
1606: Calificación
1607: Trabajos publicados
1608: Contrataciones
1609: Lugar/establecimiento cuando corresponda
1610: Reputación
1611: ```
1612: 
1613: ------------------------------------------------------------------------
1614: 
1615: # FASE 18 --- COMPLETAR PERFIL
1616: 
1617: Implementar porcentaje:
1618: 
1619: ``` text
1620: profileCompleted
1621: ```
1622: 
1623: Ejemplo:
1624: 
1625: ``` text
1626: 90%
1627: ```
1628: 
1629: Calcular según campos realmente completados.
1630: 
1631: No guardar manualmente porcentajes inconsistentes.
1632: 
1633: Ejemplo de campos:
1634: 
1635: ``` text
1636: Foto
1637: Username
1638: Teléfono
1639: Distrito
1640: Descripción
1641: Experiencia
1642: Especialidades
1643: Habilidades
1644: ```
1645: 
1646: El porcentaje se recalcula cuando cambian los datos.
1647: 
1648: ------------------------------------------------------------------------
1649: 
1650: # FASE 19 --- HISTORIAL
1651: 
1652: ## Trabajador
1653: 
1654: Mostrar:
1655: 
1656: ``` text
1657: Mis postulaciones
1658: Mis trabajos
1659: Trabajos completados
1660: Trabajos cancelados
1661: Calificaciones recibidas
1662: ```
1663: 
1664: ## Contratante
1665: 
1666: Mostrar:
1667: 
1668: ``` text
1669: Mis publicaciones
1670: Publicaciones activas
1671: Publicaciones finalizadas
1672: Postulantes
1673: Contrataciones
1674: Trabajadores contratados
1675: Calificaciones recibidas
1676: ```
1677: 
1678: ------------------------------------------------------------------------
1679: 
1680: # FASE 20 --- PRUEBA COMPLETA DEL SISTEMA
1681: 
1682: Crear dos usuarios de prueba.
1683: 
1684: ## Usuario A
1685: 
1686: ``` text
1687: TRABAJADOR
1688: ```
1689: 
1690: ## Usuario B
1691: 
1692: ``` text
1693: CONTRATANTE
1694: ```
1695: 
1696: Probar:
1697: 
1698: ``` text
1699: A se registra
1700: ↓
1701: A verifica OTP
1702: ↓
1703: A completa perfil
1704: ↓
1705: B se registra
1706: ↓
1707: B activa contratante
1708: ↓
1709: B crea workplace
1710: ↓
1711: B publica oferta
1712: ↓
1713: A encuentra oferta
1714: ↓
1715: A guarda oferta
1716: ↓
1717: A da like
1718: ↓
1719: A comenta
1720: ↓
1721: A postula
1722: ↓
1723: B recibe notificación
1724: ↓
1725: B revisa perfil A
1726: ↓
1727: B acepta
1728: ↓
1729: Se crea job
1730: ↓
1731: Chat
1732: ↓
1733: Trabajo realizado
1734: ↓
1735: Job COMPLETED
1736: ↓
1737: A califica B
1738: ↓
1739: B califica A
1740: ↓
1741: Se actualiza reputación
1742: ```
1743: 
1744: ------------------------------------------------------------------------
1745: 
1746: # Arquitectura final
1747: 
1748: ``` text
1749:                          CHAMBAYA
1750:                             │
1751:               ┌─────────────┴─────────────┐
1752:               │                           │
1753:           FIREBASE AUTH              FIRESTORE
1754:               │                           │
1755:              uid                          │
1756:               │                           ├── users
1757:               │                           ├── workplaces
1758:               │                           ├── publications
1759:               │                           ├── applications
1760:               │                           ├── jobs
1761:               │                           ├── ratings
1762:               │                           ├── comments
1763:               │                           ├── likes
1764:               │                           ├── saves
1765:               │                           ├── reports
1766:               │                           ├── blocks
1767:               │                           ├── conversations
1768:               │                           ├── messages
1769:               │                           ├── notifications
1770:               │                           └── categories
1771:               │
1772:               │
1773:               └──────────────────────┐
1774:                                      │
1775:                                 CLOUDINARY
1776:                                      │
1777:                                      ├── Fotos de perfil
1778:                                      ├── Fotos de lugares
1779:                                      └── Fotos de publicaciones
1780: ```
1781: 
1782: ------------------------------------------------------------------------
1783: 
1784: # Orden recomendado de implementación
1785: 
1786: ``` text
1787: FASE 1   Registro
1788:          ↓
1789: FASE 2   Perfil
1790:          ↓
1791: FASE 3   Rol Contratante
1792:          ↓
1793: FASE 4   Workplace
1794:          ↓
1795: FASE 5   Publicaciones
1796:          ↓
1797: FASE 6   Búsqueda/Filtros
1798:          ↓
1799: FASE 7   Postulaciones
1800:          ↓
1801: FASE 8   Jobs
1802:          ↓
1803: FASE 9   Calificaciones
1804:          ↓
1805: FASE 10  Comentarios
1806:          ↓
1807: FASE 11  Likes/Guardados/Ocultar
1808:          ↓
1809: FASE 12  Denuncias/Bloqueos
1810:          ↓
1811: FASE 13  Chat
1812:          ↓
1813: FASE 14  Notificaciones
1814:          ↓
1815: FASE 15  Categorías
1816:          ↓
1817: FASE 16  Security Rules
1818:          ↓
1819: FASE 17  Perfil público
1820:          ↓
1821: FASE 18  Completar perfil
1822:          ↓
1823: FASE 19  Historial
1824:          ↓
1825: FASE 20  Pruebas integrales
1826: ```
1827: 
1828: ------------------------------------------------------------------------
1829: 
1830: # Regla final para el desarrollo
1831: 
1832: No pedirle a Claude Code o Codex:
1833: 
1834: ``` text
1835: "Implementa toda la aplicación ChambAYA."
1836: ```
1837: 
1838: Es demasiado grande y puede modificar partes que ya funcionan.
1839: 
1840: Usar siempre:
1841: 
1842: ``` text
1843: "Implementa SOLAMENTE la FASE X..."
1844: ```
1845: 
1846: y después comprobar:
1847: 
1848: ``` text
1849: BUILD
1850: ↓
1851: TEST
1852: ↓
1853: FIREBASE
1854: ↓
1855: UI
1856: ↓
1857: DATOS
1858: ↓
1859: SECURITY
1860: ↓
1861: SIGUIENTE FASE
1862: ```
1863: 
1864: De esta forma ChambAYA puede crecer progresivamente sin tener que
1865: rehacer el registro cuando posteriormente agregues publicaciones,
1866: contratación, reputación, chat y demás funcionalidades.

(End of file - total 1866 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\FASE-1-REGISTRO-COMPLETO.md"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\FASE-1-REGISTRO-COMPLETO.md</path>
<type>file</type>
<content>
1: # FASE 1 — Registro Completo (implementación)
2: 
3: **Estado:** implementada y compilada (`:app:assembleDebug` → BUILD SUCCESSFUL)
4: **Alcance:** solo FASE 1 del plan maestro `CHAMBAYA_IMPLEMENTACION_FASES.md`.
5: No se implementó FASE 2 ni superior (perfil completo, contratante, publicaciones, chat).
6: 
7: ---
8: 
9: ## 1. Problema que se corrigió
10: 
11: Antes, `users/{uid}` solo guardaba el correo. El **nombre oficial (RENIEC/SUNAT)** y el
12: **DNI/RUC validado** se mostraban en pantalla, pero vivían únicamente en `TextView`s:
13: 
14: - se perdían al recrear la Activity (rotación o muerte de proceso),
15: - nunca llegaban a Firestore,
16: - el documento se armaba "a mano" en **tres** lugares distintos
17:   (`RegistroActivity`, fallback de OTP y Cloud Function), con esquemas incompatibles.
18: 
19: Ahora hay una **capa de datos única** (`data/`) y el documento `users/{uid}` se crea
20: una sola vez, con el nombre oficial y el documento de identidad.
21: 
22: ---
23: 
24: ## 2. Archivos modificados
25: 
26: ### Nuevos (`app/src/main/java/com/proyecto/chambaya/data/`)
27: 
28: | Archivo | Responsabilidad |
29: |---|---|
30: | `model/RegistrationModels.kt` | Constantes de dominio (`UserRoles`, `IdentityDocumentTypes`, `AuthProviders`, `AuthMethods`, `EmailVerificationMethods`, `AccountStatuses`, `RegistrationStatuses`, `IdentitySources`, `ProfilePhotoSources`) + `ValidatedIdentity`, `RegistrationDraft`, `PendingRegistration` |
31: | `model/IdentityNameParser.kt` | Separa `nombre_completo` de RENIEC en `firstName` / `lastName` y aplica `Display Case` |
32: | `remote/IdentityValidationService.kt` | Consultation HTTP a RENIEC/SUNAT. Mismo `baseUrl`, mismo token y mismo timeout (10 s) que ya usaba la Activity. Devuelve `IdentityValidationResult.Success / Rejected / ServiceError / NetworkError` |
33: | `local/PendingRegistrationStore.kt` | Persistencia del registro en curso en `SharedPreferences` (borrador sin `uid` + pendiente con `uid`) |
34: | `repository/RegistrationRepository.kt` | **Única** fuente de verdad de `users/{uid}`: `finalizeRegistration()`, `touchLastLogin()`, `buildRegistrationDocument()`, `buildRefreshDocument()` |
35: 
36: ### Modificados
37: 
38: | Archivo | Cambio |
39: |---|---|
40: | `RegistroActivity.kt` | Nuevo estado (`validatedIdentity`, `googleDisplayName/PhotoUrl`); consultas RENIEC/SUNAT vía `IdentityValidationService` + `lifecycleScope`; escritura de `users/{uid}` solo vía `RegistrationRepository`; payload `identity`/`profile` enviado a la Cloud Function; `PendingRegistrationStore` para sobrevivir rotación y muerte de proceso; se eliminó la escritura duplicada a `users` en el fallback de OTP |
41: | `LoginActivity.kt` | Lee `auth.email`, `activeRole` y `roles` con *fallback* a los campos raíz legacy; acepta `accountStatus == ACTIVE`; actualiza `lastLoginAt` al entrar |
42: | `res/values/strings.xml` | `register_step5_subtitle`, `register_btn_finish` ("Entrar a ChambAYA") y nueva clave `register_btn_saving` |
43: | `firestore.rules` | Reglas endurecidas (ver sección 4) |
44: | `functions/index.js` | `verifyEmailOtp` recibe y escribe la nueva estructura + espejo legacy; valida el formato del documento; preserva `createdAt` |
45: 
46: ---
47: 
48: ## 3. Estructura final de `users/{uid}`
49: 
50: ```
51: users/{uid}
52: ├── uid                      "el-uid-de-firebase-auth"
53: ├── accountStatus            "ACTIVE"
54: ├── registrationStatus       "VERIFIED"
55: ├── roles                    [ "TRABAJADOR" ]  |  [ "CONTRATANTE" ]
56: ├── activeRole               "TRABAJADOR"    |  "CONTRATANTE"
57: │
58: ├── auth
59: │   ├── provider             "EMAIL" | "GOOGLE"
60: │   ├── email                "usuario@correo.com"      (normalizado en minúsculas)
61: │   ├── emailVerified        true
62: │   ├── otpVerified          true  (solo si el correo se confirmó con el OTP de ChambAYA)
63: │   └── verificationMethod   "CHAMBAYA_OTP" | "FIREBASE_EMAIL_LINK"
64: │
65: ├── identity
66: │   ├── documentType         "DNI" | "RUC"
67: │   ├── documentNumber       "72345678" | "20123456789"   (validado en RENIEC/SUNAT)
68: │   ├── documentNumberMasked "****5678" | "***6789"       (para vistas públicas)
69: │   ├── identityVerified     true
70: │   ├── verifiedWith         "RENIEC" | "SUNAT"
71: │   ├── identityName         "JUAN CARLOS PEREZ"  |  "EMPRESA SAC S.A.C."
72: │   ├── identityStatus       "HABIDO / ACTIVO"  (RUC)
73: │   ├── location             "Lima, Lima"        (RENIEC)
74: │   └── verifiedAt           Timestamp
75: │
76: ├── profile
77: │   ├── firstName            "Juan Carlos"
78: │   ├── lastName             "Perez"
79: │   ├── fullName             "JUAN CARLOS PEREZ"   (nombre oficial del padrón)
80: │   ├── profilePhotoUrl      "https://lh3.googleusercontent.com/..."
81: │   ├── profilePhotoPublicId ""
82: │   ├── profilePhotoSource   "GOOGLE" | "DEFAULT"
83: │   └── country              "Peru"
84: │
85: ├── createdAt                Timestamp
86: ├── updatedAt                Timestamp
87: └── lastLoginAt              Timestamp
88: │
89: └── ESPEJO LEGACY (raíz) — se conserva para no romper lecturas ya existentes
90:     ├── email                "usuario@correo.com"
91:     ├── role                 "TRABAJADOR" | "CONTRATANTE"
92:     ├── emailVerified        true
93:     ├── otpVerified          true | false
94:     ├── authMethod           "EMAIL_PASSWORD" | "GOOGLE"
95:     └── verifiedAt           Timestamp
96: ```
97: 
98: ### Reglas de la FASE 1
99: 
100: - **Nunca** se guarda la contraseña: vive en Firebase Authentication.
101: - **Nunca** se guardan imágenes ni Base64: solo la URL de Google.
102: - **No** se crea `employer` (pertenece a FASE 3).
103: - **No** se crea `profile.username` (pertenece a FASE 2).
104: - `identity.documentNumber` es de **solo lectura del propietario**; para exponerlo
105:   públicamente se usará `identity.documentNumberMasked`.
106: - Para RUC: `profile.firstName`/`lastName` van vacíos y `profile.fullName` es la razón social.
107: - `auth.otpVerified` es `true` solo si el correo se confirmó con el OTP de ChambAYA.
108:   La ruta Google+correo verificado por Firebase usa `FIREBASE_EMAIL_LINK`.
109: 
110: ---
111: 
112: ## 4. Reglas de seguridad (`firestore.rules`)
113: 
114: ```text
115: users/{uid}
116:   allow list:   if false                                   # sin listados públicos
117:   allow get:    if request.auth.uid == uid
118:   allow create: if es el propio uid
119:                 && registrationStatus == 'VERIFIED'
120:                 && accountStatus == 'ACTIVE'
121:                 && roles/activeRole coherentes
122:                 && auth válido (provider, email, emailVerified)
123:                 && identity válido (DNI 8 dígitos / RUC 11 dígitos,
124:                                     verifiedWith RENIEC|SUNAT, identityName)
125:   allow update: (a) solo ['updatedAt','lastLoginAt','lastSeenAt'], o
126:                 (b) migración única si el documento no tenía bloque `identity`, o
127:                 (c) completar el registro si aún no estaba VERIFIED
128:                     (el número de documento no puede cambiar)
129:   allow delete: if false
130: 
131: email_verifications/{uid}
132:   read:  solo el dueño
133:   create/update:  sesión OTP válida y `verified == false` (reenvío)
134:   update:  única transición permitida `verified: false -> true`
135: ```
136: 
137: ---
138: 
139: ## 5. Flujo de escritura (único)
140: 
141: ```
142: Paso 1  DNI/RUC  ──► IdentityValidationService ──► ValidatedIdentity
143:                                         │
144:                                         ▼
145:                         PendingRegistrationStore (SharedPreferences)
146:                                         │
147: Paso 2  Correo+contraseña | Google ─────┤
148:                                         ▼
149: Paso 3  OTP ChambAYA | correo Firebase  │   ← el Cloud Function puede
150:                                         │      escribir el mismo documento
151:                                         ▼
152:                      RegistrationRepository.finalizeRegistration()
153:                                         ▼
154:                               users/{uid} (completo)
155:                                         │
156: Paso 4  Resumen ──► "Entrar a ChambAYA" ──► LoginActivity
157:                        (limpia el store)
158: ```
159: 
160: - El **fallback de OTP directo contra Firestore** (plan Spark) ya **no** escribe `users`:
161:   solo invalida el OTP. La escritura autoritativa es la del repositorio.
162: - `finalizeRegistration()` es **idempotente**: si el documento ya tiene `identity` y
163:   `profile`, solo refresca `updatedAt`/`lastLoginAt` (respeta las Rules). Si quedó
164:   incompleto (versión previa de la app o Cloud Function sin `identity`), completa campos.
165: - `LoginActivity` llama a `touchLastLogin(uid)`, que solo escribe auditoría.
166: 
167: ---
168: 
169: ## 6. Verificación realizada
170: 
171: | Prueba | Resultado |
172: |---|---|
173: | `:app:compileDebugKotlin` | OK |
174: | `:app:assembleDebug` (limpio) | **BUILD SUCCESSFUL** — `app/build/outputs/apk/debug/app-debug.apk` |
175: | `node --check functions/index.js` | OK |
176: | 36 pruebas unitarias de los helpers de `functions/index.js` (`normalizeIdentity`, `buildUserDocument`, `maskDocumentNumber`) | **TODAS PASARON** |
177: 
178: Las pruebas cubrieron: rechazo de DNI de 7 dígitos, RUC de 10 dígitos, letras en el
179: número, nombre demasiado corto, `identity` nulo, tipo de documento desconocido,
180: enmascarado, coherencia `roles`/`activeRole`, preservación de `createdAt`, rol inválido
181: → `TRABAJADOR`, y ausencia de contraseñas en el documento.
182: 
183: ---
184: 
185: ## 7. Pendiente para FASE 16 (deuda de seguridad conocida)
186: 
187: Estas decisiones se tomaron para **no romper** el funcionamiento actual, pero deben
188: corregirse cuando la app pase a plan Blaze con Functions desplegadas:
189: 
190: 1. **El token de RENIEC/SUNAT está en el cliente** (`IdentityValidationService` y el
191:    companion object de `RegistroActivity`). Cualquiera con el APK puede escribir un DNI
192:    falso. Debe moverse a una Cloud Function.
193: 2. **El hash del OTP se calcula en el cliente** con una sal embebida. Debe validarse
194:    solo en backend.
195: 3. **No hay unicidad global de DNI/RUC**: dos cuentas pueden registrarse con el mismo
196:    documento. Requiere una colección de reservas (`identity_reservations`) con
197:    transacción, o un claim por documento.
198: 4. `email_verifications` es accesible por el cliente (compatible con Spark). Con Blaze
199:    debe pasar a ser de solo lectura/escritura del backend.
200: 
201: ---
202: 
203: ## 8. Cómo probar el resultado
204: 
205: 1. Registrarse como **Trabajador** con DNI real → tarjeta "✓ Verificación confirmada con RENIEC".
206: 2. Continuar con correo + contraseña o Google.
207: 3. Verificar el OTP (o el correo de Firebase).
208: 4. En Firestore → `users/{uid}`:
209:    - `identity.identityName` debe coincidir con el nombre de RENIEC.
210:    - `identity.documentNumber` = el DNI consultado.
211:    - `profile.fullName` = el nombre oficial.
212:    - `auth.email` en minúsculas, `auth.emailVerified: true`.
213:    - `roles: ["TRABAJADOR"]`, `activeRole: "TRABAJADOR"`.
214: 5. Repetir como **Contratante** con un RUC ACTIVO/HABIDO →
215:    `identity.verifiedWith: "SUNAT"`, `profile.fullName` = razón social.
216: 6. Entrar a la app → `lastLoginAt` se actualiza.
217: 7. Matar la app en el paso 1 y reabrirla → la tarjeta de verificado sigue poblada
218:    (gracias a `PendingRegistrationStore`).

(End of file - total 218 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\FASE-1-REANUDACION-REGISTRO.md"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\FASE-1-REANUDACION-REGISTRO.md</path>
<type>file</type>
<content>
1: # FASE 1 — Reanudación de registros a medias
2: 
3: **Estado:** implementado y compilado (`:app:assembleDebug` → BUILD SUCCESSFUL)
4: **Alcance:** corrección del flujo de registro entre los sub-pasos 1 (DNI/RUC), 2
5: (credenciales) y 3 (verificación). No cambia el modelo final de `users/{uid}`, no toca
6: la validación DNI/RUC (RENIEC/SUNAT) ni introduce nada de FASE 2 o superior.
7: 
8: ---
9: 
10: ## 1. El bug
11: 
12: La cuenta de Firebase Authentication se creaba en el **sub-paso 2**, pero el documento
13: `users/{uid}` solo se escribía al confirmar la verificación (**sub-paso 4**). Durante esa
14: ventana la app no distinguía dos situaciones muy distintas y las trataba igual:
15: 
16: | Situación real | Qué hacía la app antes |
17: |---|---|
18: | Cuenta con registro **completo** | «Ya existe una cuenta registrada» → correcto |
19: | Cuenta creada en Auth, **sin** `users/{uid}` (el usuario cerró la app antes del OTP) | «Ya existe una cuenta registrada» → **bloqueo incorrecto** |
20: 
21: Como el DNI/RUC del sub-paso 1 solo se conservaba en el borrador local y el pendiente
22: ligado al `uid` se guardaba **únicamente al finalizar**, el usuario que cerraba la app en
23: el sub-paso 3 perdía además la posibilidad de recuperar su identidad validada.
24: 
25: ---
26: 
27: ## 2. Solución
28: 
29: ### 2.1 Se consulta el estado real de `users/{uid}` antes de bloquear
30: 
31: `RegistrationRepository.fetchRegistrationState(uid)` devuelve un estado de cuatro valores:
32: 
33: | Estado | Significado |
34: |---|---|
35: | `MISSING` | No existe `users/{uid}` (cuenta huérfana de Auth) |
36: | `INCOMPLETE` | Existe, pero el registro no se completó |
37: | `COMPLETE` | `registrationStatus ∈ {VERIFIED, COMPLETED}` o `accountStatus == ACTIVE` |
38: | `UNKNOWN` | No se pudo leer (sin red / sin permiso): no se afirma nada |
39: 
40: El criterio de `COMPLETE` es **exactamente el mismo que usa `LoginActivity`** para dar
41: acceso a la app. Esto garantiza el invariante:
42: 
43: > Si el usuario puede entrar por login, no se le puede ofrecer un registro nuevo.
44: 
45: ### 2.2 La contraseña escrita es la prueba de titularidad
46: 
47: En el sub-paso 2, cuando Firebase responde `email-already-in-use`, ya no se muestra el
48: error. Se intenta `signInWithEmailAndPassword` con la contraseña que el usuario acaba de
49: escribir:
50: 
51: - **La contraseña coincide** → la cuenta es del usuario, ya hay sesión y por tanto ya puede
52:   leer su `users/{uid}` (las reglas solo permiten leerlo al dueño) → se decide con el
53:   estado real: `COMPLETE` → «ya está registrada» + ir a login; `MISSING`/`INCOMPLETE` →
54:   **se retoma el registro**.
55: - **La contraseña no coincide** → no se intenta leer el documento de otra cuenta
56:   (Firestore lo prohíbe) → diálogo «esa cuenta ya existe, la contraseña no coincide» con
57:   acceso al inicio de sesión. No se filtra información: es el mismo dato que ya revelaba
58:   el error de Firebase.
59: 
60: ### 2.3 El DNI/RUC se guarda ligado a la cuenta en el sub-paso 2
61: 
62: `persistPendingAccountForResume()` se llama **en el momento en que Auth crea la cuenta**
63: (antes de enviar el correo o el OTP), no al final. `PendingRegistrationStore` guarda:
64: 
65: - el **borrador** (rol + identidad, sin `uid`) — ya existía;
66: - el **pendiente** ligado al `uid` (rol, correo, proveedor, método de acceso, identidad);
67: - un **índice correo → uid**, para recuperar el registro aunque el usuario haya perdido la
68:   sesión de Firebase.
69: 
70: Así el DNI/RUC sobrevive al cierre de la app y no hay que volver a escribirlo.
71: 
72: ### 2.4 Reanudación explícita
73: 
74: - `offerToResumePendingRegistration()`: al abrir la app, si la sesión actual tiene un
75:   registro a medias, aparece el diálogo «Retoma tu registro» con acceso directo al
76:   sub-paso 3. Solo se ofrece con `savedInstanceState == null` (no en rotación) y nunca
77:   con estado `COMPLETE` o `UNKNOWN`.
78: - `resumeAtVerificationStep()`: lleva al sub-paso 3 conservando la verificación obligatoria.
79:   Si Firebase ya tiene el correo verificado de un intento anterior, la verificación ya está
80:   cumplida y se cierra el registro con `checkEmailVerificationAndProceed()`.
81: - `adoptStoredIdentityIfMissing()`: si la identidad no está en memoria, la recupera del
82:   pendiente y repinta la tarjeta de DNI/RUC y el rol.
83: 
84: ---
85: 
86: ## 3. Archivos modificados
87: 
88: | Archivo | Cambio | Por qué |
89: |---|---|---|
90: | `data/repository/RegistrationRepository.kt` | Nuevo `enum UserRegistrationState` (`MISSING`, `INCOMPLETE`, `COMPLETE`, `UNKNOWN`), `fetchRegistrationState(uid)` e `isRegistered(snapshot)` | Necesario para distinguir «cuenta registrada» de «registro a medias» con el mismo criterio que el login |
91: | `data/model/RegistrationModels.kt` | `RegistrationStatuses.COMPLETED` y `RegistrationStatuses.REGISTERED` | El literal `"COMPLETED"` estaba hardcodeado en `LoginActivity`; ahora hay una única fuente |
92: | `data/local/PendingRegistrationStore.kt` | `savePending()` normaliza el correo y escribe un índice `correo → uid`; nuevo `loadPendingByEmail()` | Recuperar el registro por correo aunque no exista sesión de Auth |
93: | `RegistroActivity.kt` | `handleExistingAccountOnEmailRegister()`, `handleExistingAccountOnGoogleRegister()`, `resolveRegistrationAfterAuth()`, `resumeRegistrationOrAskIdentity()`, `persistPendingAccountForResume()`, `adoptStoredIdentityIfMissing()`, `showResumeRegistrationDialog()`, `resumeAtVerificationStep()`, `showAlreadyRegisteredDialog()`, `showExistingAccountDialog()`, `offerToResumePendingRegistration()`; `restoreRegistrationDraft()` refactorizado en `restoreRoleIntoUi()` + `applyIdentityIntoUi()` | Núcleo de la corrección. El refactor evita duplicar el pintado de DNI/RUC y rol |
94: | `LoginActivity.kt` | `EXTRA_PREFILL_EMAIL` + `prefillEmailFromIntent()` | Al detectar «cuenta ya registrada», el correo llega precargado en el login |
95: 
96: **No se modificaron:** `firestore.rules`, `functions/index.js`, `strings.xml` ni ningún
97: layout. La validación DNI/RUC, el modelo de `users/{uid}` y la colección
98: `email_verifications` quedan intactos.
99: 
100: ---
101: 
102: ## 4. Tabla de decisión (sub-paso 2)
103: 
104: | Caso | Resultado |
105: |---|---|
106: | Correo nuevo, correo+contraseña | Se crea la cuenta, se guarda el pendiente, se envía la verificación → sub-paso 3 |
107: | Correo nuevo, Google | Se crea la cuenta, se guarda el pendiente → modal de éxito → sub-paso 3 |
108: | Correo existente **completo**, misma contraseña | «Esta cuenta ya está registrada» → **Iniciar sesión** (con correo precargado) |
109: | Correo existente **completo**, Google | «Esta cuenta ya está registrada» → **Iniciar sesión** |
110: | **Registro a medias**, misma contraseña | «Retoma tu registro» → sub-paso 3, sin volver a crear la cuenta ni escribir el DNI |
111: | **Registro a medias**, Google (misma cuenta) | «Retoma tu registro» → se reenvía el OTP → sub-paso 3 |
112: | **Registro a medias**, Google (correo de una cuenta con contraseña) | Aviso: esa cuenta se creó con correo y contraseña, que inicie sesión para completarla |
113: | Correo existente con **otra** contraseña | «Esa cuenta ya existe, la contraseña no coincide» → **Ir a iniciar sesión** / **Usar otro correo** |
114: 
115: ---
116: 
117: ## 5. Verificación obligatoria
118: 
119: No se saltó ningún control: `updateStep(4)` sigue bloqueado mientras `!isOtpVerified`, y el
120: documento `users/{uid}` en su forma final **solo** se escribe por
121: `RegistrationRepository.finalizeRegistration()` después de que la verificación responde
122: correctamente (OTP de ChambAYA para Google, enlace de Firebase para correo+contraseña).
123: 
124: ## 6. Seguridad
125: 
126: - La reanudación **no permite tomar cuentas ajenas**: exige la contraseña correcta o la
127:   credencial de Google del propio usuario.
128: - Sin sesión no se lee ningún documento ajeno (reglas de Firestore: `users` es
129:   legible solo por el dueño, `list: if false`).
130: - El DNI/RUC se muestra enmascarado en los diálogos (`maskEmail`, y el documento solo se
131:   completa si el usuario lo tiene en memoria).
132: - La conclusión «ya está registrada» solo se afirma con una lectura exitosa
133:   (`UNKNOWN` no afirma nada).
134: 
135: ## 7. Limitaciones conocidas
136: 
137: - El pendiente local es de **un solo slot**: si el usuario abandona un registro y empieza
138:   otro con otro correo, el anterior deja de ser recuperable localmente (el documento en
139:   Firestore, si llegó a escribirse, no se ve afectado).
140: - El estado `INCOMPLETE` cubre documentos antiguos: un usuario previo a la FASE 1 con
141:   `registrationStatus == "VERIFIED"` pero sin bloque `identity` se considera `COMPLETE`
142:   (puede entrar por login, luego no debe poder re-registrarse). La migración de esos
143:   documentos no forma parte de esta corrección.
144: - Si un documento existente tiene `identity` y todavía no está `VERIFIED`, las reglas
145:   impiden cambiar el `documentNumber`; el cliente muestra «No pudimos guardar tu cuenta»
146:   y el usuario debe reintentar con el mismo documento.
147: 
148: ## 8. Cómo probar en un dispositivo
149: 
150: Caso 1 — registro a medias con correo y contraseña (el bug reportado)
151: 1. Registro nuevo: rol → DNI (válido en RENIEC) → correo + contraseña.
152: 2. Cierra la app **antes** de confirmar la verificación.
153: 3. Reabre la app: aparece «Retoma tu registro» → *Continuar verificación* → sub-paso 3
154:    con el DNI ya validado. O, si se descarta el diálogo, repite DNI + mismo correo +
155:    misma contraseña: no aparece «cuenta ya registrada», aparece «Retoma tu registro».
156: 4. Confirma la verificación → se crea `users/{uid}` con `identity` y `profile`.
157: 
158: Caso 2 — registro a medias con Google
159: 1. Mismo proceso por el botón de Google, cerrando antes del OTP.
160: 2. Al volver, «Continuar con Google» con la misma cuenta → «Retoma tu registro» → llega
161:    al sub-paso 3 con un OTP nuevo.
162: 
163: Caso 3 — cuenta ya completa (no debe cambiar el comportamiento)
164: 1. Entra con una cuenta ya registrada: «Esta cuenta ya está registrada» → *Iniciar
165:    sesión* → el login aparece con el correo precargado.
166: 2. Repetido por Google: mismo mensaje.
167: 
168: Caso 4 — credenciales que no coinciden
169: 1. Con una cuenta existente, escribe el mismo correo con otra contraseña válida de 8+
170:    caracteres → «Esa cuenta ya existe, la contraseña no coincide», sin crear nada.

(End of file - total 170 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\RECOMENDACIONES-IMPLEMNTACION-FASES-APP.MD"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\RECOMENDACIONES-IMPLEMNTACION-FASES-APP.MD</path>
<type>file</type>
<content>
1: --- RECOEMNCIONES DE LA APP COMPLETO:
2: 
3: 
4: flujo de registro por fases y el documento de ChambAYA, te recomiendo no meter absolutamente todo dentro de users. La colección users debe ser el núcleo de la cuenta; las publicaciones, postulaciones, comentarios, reportes, favoritos, etc. deben ser colecciones independientes relacionadas por uid.
5: 
6: Esto además evita que el documento del usuario crezca demasiado: Firestore tiene un límite de 1 MiB por documento, por lo que no conviene guardar listas enormes de comentarios, likes, publicaciones o postulaciones dentro de users.
7: 
8: Tu documento plantea justamente trabajadores, contratantes, publicaciones, postulaciones, reputación, chat, notificaciones, geolocalización, reportes e historial.
9: 
10: 1. Arquitectura completa que te recomiendo
11: 
12: La estructura general de Firestore sería:
13: 
14: Firestore
15: │
16: ├── users
17: │
18: ├── email_verifications
19: │
20: ├── publications
21: │
22: ├── applications
23: │
24: ├── jobs
25: │
26: ├── ratings
27: │
28: ├── comments
29: │
30: ├── publication_likes
31: │
32: ├── publication_saves
33: │
34: ├── publication_reports
35: │
36: ├── user_reports
37: │
38: ├── hidden_publications
39: │
40: ├── conversations
41: │
42: ├── messages
43: │
44: ├── notifications
45: │
46: ├── workplaces
47: │
48: ├── user_blocks
49: │
50: └── categories
51: 
52: Y las imágenes NO van en Firestore.
53: 
54: Firebase Storage
55: │
56: ├── users/
57: │   └── {uid}/
58: │       ├── profile/
59: │       └── documents/
60: │
61: ├── employers/
62: │   └── {uid}/
63: │       └── workplace/
64: │
65: └── publications/
66:     └── {publicationId}/
67:         ├── image_1
68:         ├── image_2
69:         └── image_3
70: 
71: En Firestore solamente guardas las URLs/rutas de esas imágenes.
72: 
73: Importante para tu proyecto actual: Firebase Cloud Storage ya no funciona en proyectos nuevos con el plan Spark; para usar Storage debes tener Blaze, aunque puede haber uso sin costo dentro de las cuotas correspondientes. Firestore Standard sí tiene cuota gratuita, actualmente 1 GiB almacenado, 50.000 lecturas/día, 20.000 escrituras/día y 20.000 eliminaciones/día.
74: 
75: 2. users — LA COLECCIÓN PRINCIPAL
76: 
77: Esta es la que tú estás viendo actualmente:
78: 
79: users
80:    └── {uid}
81: 
82: Y tu documento actual:
83: 
84: email
85: emailVerified
86: otpVerified
87: registrationStatus
88: role
89: uid
90: verifiedAt
91: 
92: está bien como inicio, pero yo lo ampliaría de esta manera.
93: 
94: users/{uid}
95: {
96:   "uid": "firebase-auth-uid",
97: 
98:   "accountStatus": "ACTIVE",
99: 
100:   "registrationStatus": "VERIFIED",
101: 
102:   "roles": [
103:     "TRABAJADOR"
104:   ],
105: 
106:   "activeRole": "TRABAJADOR",
107: 
108:   "auth": {
109:     "provider": "EMAIL",
110:     "email": "usuario@gmail.com",
111:     "emailVerified": true,
112:     "otpVerified": true
113:   },
114: 
115:   "identity": {
116:     "documentType": "DNI",
117:     "documentNumber": "********",
118:     "identityVerified": true,
119:     "verifiedAt": "Timestamp"
120:   },
121: 
122:   "profile": {
123:     "firstName": "Maria",
124:     "lastName": "Sanchez",
125:     "fullName": "Maria Sanchez",
126: 
127:     "username": "maria_sanchez_ayacucho",
128: 
129:     "usernameNormalized": "maria_sanchez_ayacucho",
130: 
131:     "phone": "+51XXXXXXXXX",
132: 
133:     "profilePhotoUrl": "https://...",
134: 
135:     "profilePhotoPath": "users/UID/profile/profile.jpg",
136: 
137:     "profilePhotoSource": "GOOGLE",
138: 
139:     "bio": "",
140: 
141:     "district": "Carmen Alto",
142: 
143:     "province": "Huamanga",
144: 
145:     "department": "Ayacucho",
146: 
147:     "country": "Peru"
148:   },
149: 
150:   "location": {
151:     "latitude": -13.16,
152:     "longitude": -74.22,
153: 
154:     "district": "Carmen Alto",
155: 
156:     "locationPermission": true
157:   },
158: 
159:   "worker": {
160:     "enabled": true,
161: 
162:     "experienceYears": 4,
163: 
164:     "specialties": [
165:       "Albanileria",
166:       "Pintura",
167:       "Jardineria"
168:     ],
169: 
170:     "skills": [
171:       "Pintura interior",
172:       "Pintura exterior",
173:       "Acabados"
174:     ],
175: 
176:     "workCount": 28,
177: 
178:     "ratingAverage": 4.9,
179: 
180:     "ratingCount": 18,
181: 
182:     "profileCompleted": 90
183:   },
184: 
185:   "employer": {
186:     "enabled": false,
187: 
188:     "employerType": null,
189: 
190:     "businessName": null,
191: 
192:     "sector": null,
193: 
194:     "ruc": null,
195: 
196:     "workplaceId": null,
197: 
198:     "publishedCount": 0,
199: 
200:     "hiredCount": 0,
201: 
202:     "ratingAverage": 0,
203: 
204:     "ratingCount": 0
205:   },
206: 
207:   "privacy": {
208:     "showPhone": false,
209:     "showExactAddress": false,
210:     "showEmail": false
211:   },
212: 
213:   "statistics": {
214:     "applicationsCount": 0,
215:     "publicationsCount": 0,
216:     "completedJobsCount": 0,
217:     "savedPublicationsCount": 0,
218:     "receivedRatingsCount": 0
219:   },
220: 
221:   "createdAt": "Timestamp",
222: 
223:   "updatedAt": "Timestamp",
224: 
225:   "lastLoginAt": "Timestamp"
226: }
227: 3. ¿Por qué roles y no solamente role?
228: 
229: Actualmente tienes:
230: 
231: "role": "TRABAJADOR"
232: 
233: Yo cambiaría a:
234: 
235: "roles": [
236:   "TRABAJADOR"
237: ]
238: 
239: y:
240: 
241: "activeRole": "TRABAJADOR"
242: 
243: Porque posteriormente podrías tener:
244: 
245: "roles": [
246:   "TRABAJADOR",
247:   "CONTRATANTE"
248: ]
249: 
250: Entonces el mismo usuario puede hacer:
251: 
252: 👷 Trabajador
253:         ↓
254: Buscar chambas
255: Postular
256: Trabajar
257: Calificar
258: 
259:         +
260: 
261: 📢 Contratante
262:         ↓
263: Publicar
264: Buscar trabajadores
265: Contratar
266: Calificar
267: 
268: Eso resuelve exactamente el caso que mencionaste: un trabajador también puede publicar una oferta, pero primero debe completar los requisitos de contratante.
269: 
270: 4. Fases de registro
271: 
272: Tu flujo quedaría muy limpio.
273: 
274: FASE 1 — Identidad
275: Trabajador
276: "identity": {
277:   "documentType": "DNI",
278:   "documentNumber": "********",
279:   "identityVerified": true
280: }
281: 
282: La API de DNI devuelve los datos necesarios:
283: 
284: Nombre
285: Apellidos
286: Nombre completo
287: DNI
288: 
289: No necesitas pedir nuevamente al usuario todo eso.
290: 
291: Contratante
292: 
293: Puede elegir:
294: 
295: ○ DNI
296: ○ RUC
297: 
298: Por lo tanto:
299: 
300: "identity": {
301:   "documentType": "RUC",
302:   "documentNumber": "********",
303:   "identityVerified": true
304: }
305: 
306: Pero no asumiría que todo contratante es empresa.
307: 
308: Podrías tener:
309: 
310: PERSONA
311: EMPRESA
312: NEGOCIO
313: INDEPENDIENTE
314: 5. FASE 2 — Credenciales
315: 
316: Aquí mantienes:
317: 
318: "auth": {
319:   "provider": "EMAIL",
320:   "email": "usuario@gmail.com",
321:   "emailVerified": false,
322:   "otpVerified": false
323: }
324: 
325: O:
326: 
327: "auth": {
328:   "provider": "GOOGLE",
329:   "email": "usuario@gmail.com",
330:   "emailVerified": true,
331:   "otpVerified": true
332: }
333: MUY IMPORTANTE
334: 
335: Nunca guardes la contraseña en Firestore.
336: 
337: Firebase Authentication debe manejarla.
338: 
339: En Firestore solamente:
340: 
341: "provider": "EMAIL"
342: 
343: o:
344: 
345: "provider": "GOOGLE"
346: 6. FASE 3 — OTP
347: 
348: Tu colección:
349: 
350: email_verifications
351: 
352: puede quedar así:
353: 
354: {
355:   "uid": "UID",
356: 
357:   "email": "usuario@gmail.com",
358: 
359:   "otpHash": "HASH_DEL_CODIGO",
360: 
361:   "status": "PENDING",
362: 
363:   "attempts": 0,
364: 
365:   "maxAttempts": 5,
366: 
367:   "expiresAt": "Timestamp",
368: 
369:   "createdAt": "Timestamp",
370: 
371:   "usedAt": null
372: }
373: 
374: No guardaría:
375: 
376: "otp": "123456"
377: 
378: en texto plano.
379: 
380: Una vez validado:
381: 
382: otpVerified = true
383: 
384: y:
385: 
386: registrationStatus = VERIFIED
387: 7. FASE 4 — Usuario creado
388: 
389: Entonces tienes:
390: 
391: users/{uid}
392: 
393: con:
394: 
395: {
396:   "registrationStatus": "VERIFIED",
397:   "accountStatus": "ACTIVE",
398:   "roles": ["TRABAJADOR"]
399: }
400: 
401: Y no obligas al usuario a rellenar 30 campos durante el registro.
402: 
403: Eso es exactamente lo que te recomiendo.
404: 
405: 8. Perfil después del registro
406: 
407: Después de entrar a la aplicación aparece:
408: 
409: Completa tu perfil
410: 
411: 90% completo
412: 
413: como en tu diseño.
414: 
415: Entonces puede completar:
416: 
417: Trabajador
418: Foto
419: Nombre de usuario
420: Teléfono
421: Distrito
422: Experiencia
423: Especialidades
424: Habilidades
425: Descripción
426: Disponibilidad
427: Experiencia laboral
428: Trabajos realizados
429: 
430: Pero el registro inicial solamente necesita lo indispensable.
431: 
432: 9. Imagen de perfil
433: 
434: Aquí tienes tres posibilidades:
435: 
436: Google
437: "profilePhotoSource": "GOOGLE",
438: "profilePhotoUrl": "URL_GOOGLE"
439: Foto predeterminada
440: "profilePhotoSource": "DEFAULT",
441: "profilePhotoUrl": "URL_DEFAULT"
442: Foto subida por usuario
443: "profilePhotoSource": "CUSTOM",
444: "profilePhotoUrl": "URL_STORAGE",
445: "profilePhotoPath": "users/UID/profile/profile.jpg"
446: 
447: Así puedes permitir:
448: 
449: Cambiar foto de perfil
450: 
451: sin modificar toda la cuenta.
452: 
453: 10. Perfil de contratante
454: 
455: Si activa el rol:
456: 
457: "roles": [
458:   "TRABAJADOR",
459:   "CONTRATANTE"
460: ]
461: 
462: se habilita:
463: 
464: "employer": {
465:   "enabled": true,
466: 
467:   "employerType": "EMPRESA",
468: 
469:   "businessName": "Constructora Andina",
470: 
471:   "commercialName": "Constructora Andina",
472: 
473:   "sector": "CONSTRUCCION",
474: 
475:   "ruc": "20XXXXXXXXX",
476: 
477:   "description": "Empresa dedicada a proyectos de construcción.",
478: 
479:   "publishedCount": 15,
480: 
481:   "hiredCount": 12,
482: 
483:   "ratingAverage": 4.8,
484: 
485:   "ratingCount": 20
486: }
487: 
488: Si es una persona:
489: 
490: "employerType": "PERSONA"
491: 
492: y:
493: 
494: "ruc": null
495: 11. workplaces
496: 
497: Para el lugar del establecimiento.
498: 
499: Esto es especialmente importante para tu idea.
500: 
501: workplaces
502:    └── {workplaceId}
503: {
504:   "workplaceId": "WORKPLACE_ID",
505: 
506:   "ownerUid": "UID",
507: 
508:   "name": "Ferreteria El Sol",
509: 
510:   "type": "LOCAL_COMERCIAL",
511: 
512:   "sector": "FERRETERIA",
513: 
514:   "description": "Venta de materiales de construccion.",
515: 
516:   "address": "Av. Principal 123",
517: 
518:   "district": "Carmen Alto",
519: 
520:   "province": "Huamanga",
521: 
522:   "department": "Ayacucho",
523: 
524:   "location": {
525:     "latitude": -13.16,
526:     "longitude": -74.22
527:   },
528: 
529:   "photoUrl": "https://...",
530: 
531:   "photoPath": "employers/UID/workplace/photo.jpg",
532: 
533:   "verified": false,
534: 
535:   "createdAt": "Timestamp",
536: 
537:   "updatedAt": "Timestamp"
538: }
539: 12. publications — OFERTAS DE TRABAJO
540: 
541: Esta será una de las colecciones principales.
542: 
543: publications
544:    └── {publicationId}
545: 
546: Ejemplo:
547: 
548: {
549:   "publicationId": "PUB001",
550: 
551:   "ownerUid": "UID_CONTRATANTE",
552: 
553:   "status": "ACTIVE",
554: 
555:   "visibility": "PUBLIC",
556: 
557:   "type": "JOB_OFFER",
558: 
559:   "title": "Se requiere pintor profesional",
560: 
561:   "description": "Se requiere pintor con experiencia...",
562: 
563:   "category": "PINTURA",
564: 
565:   "subcategory": "PINTURA_INTERIOR",
566: 
567:   "skillsRequired": [
568:     "Pintura interior",
569:     "Pintura exterior"
570:   ],
571: 
572:   "payment": {
573:     "amount": 100,
574:     "currency": "PEN",
575:     "period": "DAY",
576:     "negotiable": false
577:   },
578: 
579:   "schedule": {
580:     "startDate": "Timestamp",
581:     "endDate": "Timestamp",
582:     "startTime": "08:00",
583:     "endTime": "17:00"
584:   },
585: 
586:   "workersNeeded": 2,
587: 
588:   "workersHired": 0,
589: 
590:   "location": {
591:     "district": "Carmen Alto",
592:     "province": "Huamanga",
593:     "department": "Ayacucho",
594: 
595:     "latitude": -13.16,
596:     "longitude": -74.22,
597: 
598:     "exactAddress": "..."
599:   },
600: 
601:   "workplaceId": "WORKPLACE_ID",
602: 
603:   "images": [
604:     {
605:       "url": "https://...",
606:       "storagePath": "publications/PUB001/image1.jpg"
607:     }
608:   ],
609: 
610:   "publisher": {
611:     "uid": "UID_CONTRATANTE",
612:     "name": "Constructora Andina",
613:     "username": "constructora_andina",
614:     "photoUrl": "https://...",
615:     "verified": true,
616:     "employerType": "EMPRESA",
617:     "sector": "CONSTRUCCION"
618:   },
619: 
620:   "statistics": {
621:     "views": 0,
622:     "likes": 0,
623:     "comments": 0,
624:     "shares": 0,
625:     "saves": 0,
626:     "applications": 0
627:   },
628: 
629:   "featured": false,
630: 
631:   "createdAt": "Timestamp",
632: 
633:   "updatedAt": "Timestamp",
634: 
635:   "expiresAt": "Timestamp"
636: }
637: Un detalle muy importante
638: 
639: Aunque publisher provenga de users, sí conviene copiar esos datos en la publicación.
640: 
641: ¿Por qué?
642: 
643: Si mañana la empresa cambia:
644: 
645: Constructora Andina
646: 
647: por:
648: 
649: Constructora Ayacucho
650: 
651: no quieres que todas las publicaciones antiguas cambien automáticamente.
652: 
653: Por eso guardas un snapshot del publicador.
654: 
655: 13. Publicación solamente con texto o con fotografías
656: 
657: Tu idea puede funcionar así:
658: 
659: Opción A
660: Título
661: Descripción
662: Opción B
663: Título
664: Descripción
665: Foto
666: Opción C
667: Título
668: Descripción
669: Foto 1
670: Foto 2
671: Foto 3
672: 
673: Por ejemplo:
674: 
675: "images": []
676: 
677: si no hay fotografías.
678: 
679: O:
680: 
681: "images": [
682:   {
683:     "url": "https://...",
684:     "storagePath": "publications/PUB001/1.jpg"
685:   },
686:   {
687:     "url": "https://...",
688:     "storagePath": "publications/PUB001/2.jpg"
689:   }
690: ]
691: 14. applications — POSTULACIONES
692: 
693: Cuando María pulsa:
694: 
695: Postular
696: 
697: creas:
698: 
699: applications
700:    └── {applicationId}
701: {
702:   "applicationId": "APP001",
703: 
704:   "publicationId": "PUB001",
705: 
706:   "workerUid": "UID_TRABAJADOR",
707: 
708:   "employerUid": "UID_CONTRATANTE",
709: 
710:   "status": "PENDING",
711: 
712:   "worker": {
713:     "name": "Maria Sanchez",
714:     "username": "maria_sanchez_ayacucho",
715:     "photoUrl": "https://...",
716:     "experienceYears": 4,
717:     "ratingAverage": 4.9
718:   },
719: 
720:   "message": "Estoy interesada en el trabajo.",
721: 
722:   "createdAt": "Timestamp",
723: 
724:   "updatedAt": "Timestamp"
725: }
726: 
727: Estados:
728: 
729: PENDING
730: ACCEPTED
731: REJECTED
732: CANCELLED
733: WITHDRAWN
734: COMPLETED
735: 
736: Esto alimenta:
737: 
738: Mis postulaciones
739: 
740: 15. jobs — TRABAJOS REALIZADOS
741: 
742: Cuando una postulación es aceptada, puedes crear un trabajo:
743: 
744: jobs
745:    └── {jobId}
746: {
747:   "jobId": "JOB001",
748: 
749:   "publicationId": "PUB001",
750: 
751:   "workerUid": "UID_TRABAJADOR",
752: 
753:   "employerUid": "UID_CONTRATANTE",
754: 
755:   "status": "IN_PROGRESS",
756: 
757:   "startedAt": "Timestamp",
758: 
759:   "completedAt": null,
760: 
761:   "agreedPayment": {
762:     "amount": 100,
763:     "currency": "PEN"
764:   },
765: 
766:   "createdAt": "Timestamp",
767: 
768:   "updatedAt": "Timestamp"
769: }
770: 
771: Cuando termina:
772: 
773: COMPLETED
774: 
775: y entonces se habilita la calificación.
776: 
777: 16. ratings — CALIFICACIONES
778: 
779: No pondría todas las calificaciones dentro de users.
780: 
781: Crear:
782: 
783: ratings
784:    └── {ratingId}
785: {
786:   "ratingId": "RATING001",
787: 
788:   "jobId": "JOB001",
789: 
790:   "publicationId": "PUB001",
791: 
792:   "fromUid": "UID_EVALUADOR",
793: 
794:   "toUid": "UID_EVALUADO",
795: 
796:   "rating": 5,
797: 
798:   "comment": "Muy responsable y puntual.",
799: 
800:   "type": "USER",
801: 
802:   "createdAt": "Timestamp"
803: }
804: 
805: Puedes tener:
806: 
807: Trabajador → Contratante
808: Contratante → Trabajador
809: 
810: Esto está alineado con tu sistema planteado de que ambas personas se califican después del trabajo.
811: 
812: 17. comments — COMENTARIOS
813: 
814: Para comentarios de publicaciones:
815: 
816: comments
817:    └── {commentId}
818: {
819:   "commentId": "COMMENT001",
820: 
821:   "publicationId": "PUB001",
822: 
823:   "authorUid": "UID",
824: 
825:   "authorName": "Maria Sanchez",
826: 
827:   "authorUsername": "maria_sanchez_ayacucho",
828: 
829:   "authorPhotoUrl": "https://...",
830: 
831:   "text": "¿El trabajo sigue disponible?",
832: 
833:   "status": "VISIBLE",
834: 
835:   "createdAt": "Timestamp",
836: 
837:   "updatedAt": "Timestamp"
838: }
839: 18. publication_likes
840: 
841: No hagas:
842: 
843: "likedBy": [
844:   "uid1",
845:   "uid2",
846:   "uid3",
847:   ...
848: ]
849: 
850: porque puede crecer demasiado.
851: 
852: Mejor:
853: 
854: publication_likes
855:    └── {publicationId_uid}
856: 
857: Ejemplo:
858: 
859: {
860:   "publicationId": "PUB001",
861: 
862:   "userUid": "UID001",
863: 
864:   "createdAt": "Timestamp"
865: }
866: 
867: Y en publications solamente:
868: 
869: "statistics": {
870:   "likes": 132
871: }
872: 19. publication_saves
873: 
874: Para:
875: 
876: 🔖 Guardar
877: 
878: publication_saves
879:    └── {publicationId_uid}
880: {
881:   "publicationId": "PUB001",
882: 
883:   "userUid": "UID001",
884: 
885:   "createdAt": "Timestamp"
886: }
887: 
888: Así puedes tener:
889: 
890: Mis publicaciones guardadas
891: 
892: 20. hidden_publications
893: 
894: Para:
895: 
896: No me interesa
897: 
898: hidden_publications
899:    └── {userUid_publicationId}
900: {
901:   "userUid": "UID001",
902: 
903:   "publicationId": "PUB001",
904: 
905:   "createdAt": "Timestamp",
906: 
907:   "reason": "NOT_INTERESTED"
908: }
909: 
910: Entonces esa publicación deja de aparecerle a ese usuario.
911: 
912: 21. publication_reports
913: 
914: Para el menú que aparece en tu segunda imagen:
915: 
916: 🚨 Denunciar publicación
917: 
918: publication_reports
919:    └── {reportId}
920: {
921:   "reportId": "REPORT001",
922: 
923:   "publicationId": "PUB001",
924: 
925:   "reporterUid": "UID001",
926: 
927:   "reason": "FRAUD",
928: 
929:   "description": "La oferta parece falsa.",
930: 
931:   "status": "PENDING",
932: 
933:   "createdAt": "Timestamp"
934: }
935: 
936: Categorías:
937: 
938: FRAUD
939: SCAM
940: INAPPROPRIATE
941: FALSE_INFORMATION
942: SPAM
943: DANGEROUS_JOB
944: OTHER
945: 22. user_reports
946: 
947: También debes permitir:
948: 
949: Denunciar usuario
950: 
951: {
952:   "reportId": "REPORT_USER001",
953: 
954:   "reportedUid": "UID_PROBLEMATICO",
955: 
956:   "reporterUid": "UID001",
957: 
958:   "reason": "SCAM",
959: 
960:   "description": "...",
961: 
962:   "status": "PENDING",
963: 
964:   "createdAt": "Timestamp"
965: }
966: 
967: Esto es diferente de denunciar una publicación.
968: 
969: 23. user_blocks
970: 
971: Para:
972: 
973: Bloquear usuario
974: 
975: user_blocks
976:    └── {blockerUid_blockedUid}
977: {
978:   "blockerUid": "UID001",
979: 
980:   "blockedUid": "UID002",
981: 
982:   "createdAt": "Timestamp"
983: }
984: 24. conversations
985: 
986: Para el chat:
987: 
988: conversations
989:    └── {conversationId}
990: {
991:   "conversationId": "CONV001",
992: 
993:   "participants": [
994:     "UID001",
995:     "UID002"
996:   ],
997: 
998:   "publicationId": "PUB001",
999: 
1000:   "lastMessage": "Hola, ¿sigue disponible?",
1001: 
1002:   "lastMessageAt": "Timestamp",
1003: 
1004:   "createdAt": "Timestamp"
1005: }
1006: 25. messages
1007: messages
1008:    └── {messageId}
1009: {
1010:   "conversationId": "CONV001",
1011: 
1012:   "senderUid": "UID001",
1013: 
1014:   "receiverUid": "UID002",
1015: 
1016:   "type": "TEXT",
1017: 
1018:   "text": "Hola, ¿sigue disponible?",
1019: 
1020:   "read": false,
1021: 
1022:   "createdAt": "Timestamp"
1023: }
1024: 
1025: Después puedes añadir:
1026: 
1027: TEXT
1028: IMAGE
1029: LOCATION
1030: SYSTEM
1031: 
1032: Esto coincide con tu idea de que puedan conversar, compartir ubicación, negociar precio y acordar horario.
1033: 
1034: 26. notifications
1035: notifications
1036:    └── {notificationId}
1037: {
1038:   "notificationId": "NOTIF001",
1039: 
1040:   "recipientUid": "UID001",
1041: 
1042:   "type": "NEW_APPLICATION",
1043: 
1044:   "title": "Nueva postulación",
1045: 
1046:   "message": "Un trabajador ha postulado a tu oferta.",
1047: 
1048:   "publicationId": "PUB001",
1049: 
1050:   "senderUid": "UID002",
1051: 
1052:   "read": false,
1053: 
1054:   "createdAt": "Timestamp"
1055: }
1056: 
1057: Otros tipos:
1058: 
1059: NEW_APPLICATION
1060: APPLICATION_ACCEPTED
1061: APPLICATION_REJECTED
1062: NEW_MESSAGE
1063: NEW_RATING
1064: NEW_COMMENT
1065: NEW_LIKE
1066: PUBLICATION_EXPIRING
1067: NEW_NEARBY_PUBLICATION
1068: 
1069: Tu documento también contempla notificaciones de nuevas ofertas cercanas.
1070: 
1071: 27. categories
1072: 
1073: En lugar de escribir las categorías manualmente por todas partes:
1074: 
1075: categories
1076: {
1077:   "id": "PINTURA",
1078: 
1079:   "name": "Pintura",
1080: 
1081:   "icon": "format_paint",
1082: 
1083:   "active": true,
1084: 
1085:   "order": 3
1086: }
1087: 
1088: Y podrías tener:
1089: 
1090: ALBANILERIA
1091: CONSTRUCCION
1092: PINTURA
1093: CARPINTERIA
1094: ELECTRICIDAD
1095: GASFITERIA
1096: JARDINERIA
1097: LIMPIEZA
1098: MUDANZAS
1099: DELIVERY
1100: COCINA
1101: AYUDANTE_GENERAL
1102: CUIDADO_NINOS
1103: CUIDADO_ADULTOS
1104: AGRICULTURA
1105: GANADERIA
1106: COSTURA
1107: VENTAS
1108: OTROS
1109: 
1110: Son las categorías planteadas en tu documento.
1111: 
1112: 28. Estructura final completa
1113: 
1114: En resumen, yo dejaría tu Firestore así:
1115: 
1116: 📁 users
1117:    └── {uid}
1118: 
1119: 📁 email_verifications
1120:    └── {verificationId}
1121: 
1122: 📁 workplaces
1123:    └── {workplaceId}
1124: 
1125: 📁 publications
1126:    └── {publicationId}
1127: 
1128: 📁 applications
1129:    └── {applicationId}
1130: 
1131: 📁 jobs
1132:    └── {jobId}
1133: 
1134: 📁 ratings
1135:    └── {ratingId}
1136: 
1137: 📁 comments
1138:    └── {commentId}
1139: 
1140: 📁 publication_likes
1141:    └── {publicationId_uid}
1142: 
1143: 📁 publication_saves
1144:    └── {publicationId_uid}
1145: 
1146: 📁 hidden_publications
1147:    └── {userUid_publicationId}
1148: 
1149: 📁 publication_reports
1150:    └── {reportId}
1151: 
1152: 📁 user_reports
1153:    └── {reportId}
1154: 
1155: 📁 user_blocks
1156:    └── {blockerUid_blockedUid}
1157: 
1158: 📁 conversations
1159:    └── {conversationId}
1160: 
1161: 📁 messages
1162:    └── {messageId}
1163: 
1164: 📁 notifications
1165:    └── {notificationId}
1166: 
1167: 📁 categories
1168:    └── {categoryId}
1169: 29. Firebase Storage
1170: 
1171: Las imágenes:
1172: 
1173: Perfil trabajador
1174: users/{uid}/profile/profile.jpg
1175: Perfil contratante
1176: users/{uid}/profile/profile.jpg
1177: Lugar/establecimiento
1178: users/{uid}/workplace/workplace.jpg
1179: Publicaciones
1180: publications/{publicationId}/image_1.jpg
1181: publications/{publicationId}/image_2.jpg
1182: publications/{publicationId}/image_3.jpg
1183: 
1184: Y en Firestore:
1185: 
1186: {
1187:   "url": "https://...",
1188:   "storagePath": "publications/PUB001/image_1.jpg"
1189: }
1190: 
1191: No guardes el archivo/base64 dentro de Firestore. Firestore está pensado para datos; las imágenes deben estar en Storage. Además, el documento de Firestore tiene un máximo de 1 MiB.
1192: 
1193: 30. Algo que cambiaría de tu Firebase actual
1194: 
1195: Actualmente tienes:
1196: 
1197: users
1198:  └── UID
1199:       ├── email
1200:       ├── emailVerified
1201:       ├── otpVerified
1202:       ├── registrationStatus
1203:       ├── role
1204:       ├── uid
1205:       └── verifiedAt
1206: 
1207: No necesitas borrarlo.
1208: 
1209: Puedes evolucionarlo hacia:
1210: 
1211: users
1212:  └── UID
1213:       │
1214:       ├── uid
1215:       ├── accountStatus
1216:       ├── registrationStatus
1217:       ├── roles[]
1218:       ├── activeRole
1219:       │
1220:       ├── auth{}
1221:       │
1222:       ├── identity{}
1223:       │
1224:       ├── profile{}
1225:       │
1226:       ├── location{}
1227:       │
1228:       ├── worker{}
1229:       │
1230:       ├── employer{}
1231:       │
1232:       ├── privacy{}
1233:       │
1234:       ├── statistics{}
1235:       │
1236:       ├── createdAt
1237:       ├── updatedAt
1238:       └── lastLoginAt
1239: 
1240: Y luego las demás colecciones se relacionan mediante:
1241: 
1242: uid
1243: publicationId
1244: applicationId
1245: jobId
1246: conversationId
1247: 31. El flujo completo de tu usuario quedaría así
1248: REGISTRO
1249:    │
1250:    ├── FASE 1
1251:    │     DNI / RUC
1252:    │     ↓
1253:    │     Validación de identidad
1254:    │
1255:    ├── FASE 2
1256:    │     Google
1257:    │       o
1258:    │     Correo + contraseña
1259:    │
1260:    ├── FASE 3
1261:    │     OTP
1262:    │     ↓
1263:    │     correo verificado
1264:    │
1265:    └── FASE 4
1266:          Crear users/{uid}
1267:               │
1268:               ↓
1269:         ENTRAR A CHAMBAYA
1270:               │
1271:        ┌──────┴───────┐
1272:        ↓              ↓
1273:    TRABAJADOR     CONTRATANTE
1274:        │              │
1275:        ↓              ↓
1276:    Completar       Completar
1277:     perfil          perfil
1278:        │              │
1279:        ↓              ↓
1280:  Buscar ofertas    Publicar
1281:        │              │
1282:     Postular       Postulantes
1283:        │              │
1284:        └──────┬───────┘
1285:               ↓
1286:             CHAT
1287:               ↓
1288:            TRABAJO
1289:               ↓
1290:          FINALIZADO
1291:               ↓
1292:        ⭐ CALIFICACIONES
1293:               ↓
1294:           REPUTACIÓN
1295: 
1296: Y eso encaja con el objetivo original de ChambAYA: conectar a quienes necesitan trabajadores con quienes buscan trabajos temporales, incorporando perfiles, ubicación, postulaciones, reputación, chat y seguridad.
1297: 
1298: Mi recomendación concreta para tu implementación ahora
1299: 
1300: No intentes crear las 17 colecciones manualmente de golpe en Firebase Console. Empieza solamente con:
1301: 
1302: 1. users
1303: 2. email_verifications
1304: 3. categories
1305: 4. publications
1306: 5. applications
1307: 6. workplaces
1308: 
1309: y deja preparadas conceptualmente:
1310: 
1311: ratings
1312: comments
1313: likes
1314: saves
1315: reports
1316: blocks
1317: conversations
1318: messages
1319: notifications
1320: jobs
1321: 
1322: Firestore crea las colecciones automáticamente cuando tu aplicación escribe el primer documento. Así puedes desarrollar fase por fase sin tener una consola llena de documentos vacíos.
1323: 
1324: Y para tu fase actual, la colección users debería ser la prioridad: ahí tienes que dejar bien diseñada desde ahora la compatibilidad con TRABAJADOR + CONTRATANTE, aunque inicialmente el usuario solo se registre como trabajador.

(End of file - total 1324 lines)
</content>
```

---

## Assistant (Build · Muse Spark 1.3 Free · 4.6s)

_Thinking:_



Ya tengo el plan maestro y el estado de Fase 1. Ahora reviso el código real.

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app</path>
<type>directory</type>
<entries>
.gitignore
api_oficios.json
build.gradle.kts
build/
google-services.json
proguard-rules.pro
src/

(7 entries)
</entries>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\build.gradle.kts"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\build.gradle.kts</path>
<type>file</type>
<content>
1: // Top-level build file where you can add configuration options common to all sub-projects/modules.
2: plugins {
3:     alias(libs.plugins.android.application) apply false
4:     alias(libs.plugins.kotlin.compose) apply false
5:     id("com.google.gms.google-services") version "4.4.2" apply false
6: }

(End of file - total 6 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\settings.gradle.kts"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\settings.gradle.kts</path>
<type>file</type>
<content>
1: pluginManagement {
2:     repositories {
3:         google {
4:             content {
5:                 includeGroupByRegex("com\\.android.*")
6:                 includeGroupByRegex("com\\.google.*")
7:                 includeGroupByRegex("androidx.*")
8:             }
9:         }
10:         mavenCentral()
11:         gradlePluginPortal()
12:     }
13: }
14: plugins {
15:     id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
16: }
17: dependencyResolutionManagement {
18:     repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
19:     repositories {
20:         google()
21:         mavenCentral()
22:     }
23: }
24: 
25: rootProject.name = "ChambAYA"
26: include(":app")
27:  

(End of file - total 27 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\firestore.rules"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\firestore.rules</path>
<type>file</type>
<content>
1: rules_version = '2';
2: service cloud.firestore {
3:   match /databases/{database}/documents {
4: 
5:     // =============================================================
6:     //  Helpers FASE 1
7:     // =============================================================
8: 
9:     function isOwner(uid) {
10:       return request.auth != null && request.auth.uid == uid;
11:     }
12: 
13:     function isValidRole(role) {
14:       return role is string && role in ['TRABAJADOR', 'CONTRATANTE'];
15:     }
16: 
17:     function isValidDocumentType(documentType) {
18:       return documentType is string && documentType in ['DNI', 'RUC'];
19:     }
20: 
21:     // DNI = 8 dígitos, RUC = 11 dígitos
22:     function isValidDocumentNumber(documentType, documentNumber) {
23:       return documentNumber is string
24:         && ((documentType == 'DNI' && documentNumber.matches('^[0-9]{8}$'))
25:           || (documentType == 'RUC' && documentNumber.matches('^[0-9]{11}$')));
26:     }
27: 
28:     // Bloque `identity` de `users/{uid}` (FASE 1)
29:     function hasValidIdentity() {
30:       let identity = request.resource.data.identity;
31:       return identity is map
32:         && identity.keys().hasAll([
33:              'documentType',
34:              'documentNumber',
35:              'documentNumberMasked',
36:              'identityVerified',
37:              'verifiedWith',
38:              'identityName',
39:              'verifiedAt'
40:            ])
41:         && isValidDocumentType(identity.documentType)
42:         && isValidDocumentNumber(identity.documentType, identity.documentNumber)
43:         && identity.documentNumberMasked is string
44:         && identity.documentNumberMasked.size() > 0
45:         && identity.identityVerified == true
46:         && identity.verifiedWith in ['RENIEC', 'SUNAT']
47:         && identity.identityName is string
48:         && identity.identityName.size() > 2
49:         && identity.verifiedAt != null;
50:     }
51: 
52:     // Bloque `auth` de `users/{uid}` (FASE 1). Nunca se guarda contraseña.
53:     function hasValidAuthBlock() {
54:       let authData = request.resource.data.auth;
55:       return authData is map
56:         && authData.keys().hasAll(['provider', 'email', 'emailVerified'])
57:         && authData.provider in ['EMAIL', 'GOOGLE']
58:         && authData.email is string
59:         && authData.email.size() > 3
60:         && authData.emailVerified == true;
61:     }
62: 
63:     // `roles` / `activeRole` deben ser coherentes entre sí.
64:     function hasValidRoles() {
65:       let roles = request.resource.data.roles;
66:       return roles is list
67:         && roles.size() >= 1
68:         && roles.size() <= 2
69:         && roles.hasOnly(['TRABAJADOR', 'CONTRATANTE'])
70:         && isValidRole(request.resource.data.activeRole)
71:         && request.resource.data.activeRole in roles;
72:     }
73: 
74:     function isActiveAndVerified() {
75:       return request.resource.data.registrationStatus == 'VERIFIED'
76:         && request.resource.data.accountStatus == 'ACTIVE';
77:     }
78: 
79:     function isCompleteRegistration() {
80:       return request.resource.data.uid != null
81:         && isActiveAndVerified()
82:         && hasValidRoles()
83:         && hasValidAuthBlock()
84:         && hasValidIdentity();
85:     }
86: 
87:     // El documento de identidad solo se puede rellenar una vez.
88:     // Si ya existía, el número de documento no puede cambiar.
89:     function keepsDocumentNumber() {
90:       let oldIdentity = resource.data.identity;
91:       return !(oldIdentity is map)
92:         || oldIdentity.documentNumber == request.resource.data.identity.documentNumber;
93:     }
94: 
95:     // Campos que el usuario siempre puede tocar en su propio documento.
96:     function onlyAuditFieldsChanged() {
97:       return request.resource.data.diff(resource.data)
98:         .affectedKeys()
99:         .hasOnly(['updatedAt', 'lastLoginAt', 'lastSeenAt']);
100:     }
101: 
102:     // =============================================================
103:     //  Helpers FASE 2 — Mi Perfil
104:     // =============================================================
105:     //
106:     // El usuario puede escribir SOLO en:
107:     //   profile   -> datos que completan su perfil público
108:     //   worker    -> su parte editable (experiencia, especialidades, habilidades)
109:     //   privacy   -> qué muestra de su contacto
110:     //   updatedAt -> auditoría
111:     //
112:     // No puede tocar NUNCA: uid, auth, identity, roles, activeRole,
113:     // registrationStatus, accountStatus, emailVerified, otpVerified,
114:     // ni la reputación de worker (workCount / ratingAverage / ratingCount)
115:     // ni los contadores de statistics.
116: 
117:     // @usuario: 3-30 caracteres en minúsculas, sin espacios.
118:     function isValidUsername(value) {
119:       return value is string
120:         && value.size() >= 3
121:         && value.size() <= 30
122:         && value.matches('^[a-z0-9._]+$');
123:     }
124: 
125:     // Teléfono: opcional, pero si se escribe tiene que ser un teléfono.
126:     //
127:     // No puede exigirse siempre: el registro de la FASE 1 no pide teléfono, así
128:     // que toda cuenta nace sin él. Si la regla rechazara el vacío, el usuario no
129:     // podría escribir NADA de la FASE 2 hasta ponerse un teléfono, y
130:     // `ensureProfileInitialized` (que escribe `phone: ""`) fallaría siempre.
131:     // El teléfono sube el porcentaje de completitud, pero no es obligatorio.
132:     function isValidPhone(value) {
133:       return value is string
134:         && (value.size() == 0
135:             || (value.size() >= 9
136:                 && value.size() <= 20
137:                 && value.matches('^[0-9+ ()-]+$')));
138:     }
139: 
140:     function isValidGender(value) {
141:       return value is string
142:         && (value == '' || value in ['MASCULINO', 'FEMENINO', 'OTRO']);
143:     }
144: 
145:     // Lista de textos cortos: tamaño acotado y longitud total acotada.
146:     // `join` exige elementos de texto, así que un mapa o un número metido en
147:     // la lista hace fallar la función y se deniega la escritura.
148:     function isValidStringList(value, maxItems, maxChars) {
149:       return value is list
150:         && value.size() <= maxItems
151:         && value.join(' ').size() <= maxChars;
152:     }
153: 
154:     // --- Foto de perfil -
155:     // Una foto nueva solo puede venir del módulo de avatares del propio usuario.
156:     // La app sube con `folder = chambaya/fotos-perfil/{uid}/profile` y Cloudinary
157:     // genera un nombre aleatorio dentro, así que la URL real es:
158:     //   https://res.cloudinary.com/<cloud>/image/upload/v1790543492
159:     //            /chambaya/fotos-perfil/{uid}/profile/ncr35rzw109sh9ykgagf.jpg
160:     // Si la foto no cambió se acepta la que haya (p. ej. la de Google que
161:     // dejó la FASE 1).
162:     //
163:     // OJO con el segmento `v1790543492`: es la versión del asset y Cloudinary lo
164:     // pone SIEMPRE en las URLs de entrega, así que tiene que admitirse. Y ojo con
165:     // el nivel extra tras el uid (`profile/<aleatorio>`): los dos se olvidaron
166:     // alguna vez y el síntoma era el mismo — la subida funcionaba, la imagen se
167:     // veía, y al pulsar "Finalizar" salía un PERMISSION_DENIED sin explicación.
168:     //
169:     // El patrón se ancla en `chambaya/fotos-perfil/{uid}/` y deja dos niveles
170:     // libres después, que es lo que permite renombrar la subcarpeta sin volver a
171:     // romper el guardado. Lo que NO se admite es salirse de la carpeta del propio
172:     // usuario.
173:     //
174:     // La carpeta debe ser la MISMA que devuelve `CloudinaryUploader.carpetaDePerfil`,
175:     // que es lo que la app escribe en `profile.profilePhotoPath`. Si se cambia una,
176:     // hay que cambiar las dos o el guardado se deniega sin motivo visible.
177:     function isOwnCloudinaryPhoto(uid) {
178:       let profile = request.resource.data.get('profile', '');
179:       return profile is map
180:         && profile.get('profilePhotoUrl', '') is string
181:         && profile.get('profilePhotoUrl', '').size() > 0
182:         && profile.get('profilePhotoUrl', '').matches(
183:              '^https://res\\.cloudinary\\.com/[^/]+/image/upload/(v[0-9]+/)?chambaya/fotos-perfil/' + uid + '/[^/]+/[^/]+$');
184:     }
185: 
186:     function photoUnchanged() {
187:       let before = resource.data.get('profile', '');
188:       let after = request.resource.data.get('profile', '');
189:       return before is map
190:         && after is map
191:         && before.get('profilePhotoUrl', '') == after.get('profilePhotoUrl', '')
192:         && before.get('profilePhotoPublicId', '') == after.get('profilePhotoPublicId', '');
193:     }
194: 
195:     // Detecta si esta escritura toca el bloque `profile`.
196:     //
197:     // OJO: no se usa `affectedData()`. En Rules `request.resource.data` SIEMPRE es
198:     // el documento completo después de la escritura, también cuando el cliente hizo
199:     // `update({'profile.photo': ...})`, así que `diff().affectedKeys()` ya devuelve
200:     // las claves de PRIMER nivel ('profile', 'worker', ...), no rutas con punto.
201:     // Por tanto `affectedKeys()` basta, y además es lo único fiable: el compilador
202:     // marca `affectedData()` como "Invalid function name", y una función que no
203:     // existe hace que la regla LANCE al evaluarse, lo que se traduce en un
204:     // `PERMISSION_DENIED` invisible en cualquier escritura de perfil.
205:     function photoBelongsToOwner(uid) {
206:       let affected = request.resource.data.diff(resource.data).affectedKeys();
207:       let tocaFoto = affected.hasAny(['profile']);
208:       return !tocaFoto || photoUnchanged() || isOwnCloudinaryPhoto(uid);
209:     }
210: 
211:     // --- Validación de lo que el usuario escribe -
212:     //
213:     // `worker`, `privacy` y `statistics` solo se validan si existen en el
214:     // documento resultante. Antes se exigían siempre, y eso rompía un caso
215:     // real: una cuenta que nunca pasó por "Mi Perfil" no tiene `statistics`, así
216:     // que guardar desde "Editar perfil" (que no lo escribe) se denegaba sin
217:     // motivo. `profile` sí o sí existe: lo crea el registro de la FASE 1.
218:     //
219:     // `profile.usernameNormalized` debe coincidir con `profile.username`:
220:     // es la misma clave que se reserva en `usernames/`.
221:     function isValidProfileUpdate() {
222:       let profile = request.resource.data.get('profile', '');
223:       return profile is map
224:         && isValidUsername(profile.get('username', ''))
225:         && profile.get('usernameNormalized', '') == profile.get('username', '')
226:         && isValidPhone(profile.get('phone', ''))
227:         && profile.get('bio', '') is string
228:         && profile.get('bio', '').size() <= 500
229:         && isValidGender(profile.get('gender', ''));
230:     }
231: 
232:     // El `&&` va antes de los `get()` a propósito: si `worker` no fuera un mapa,
233:     // Rules lanzaría un error de evaluación y la escritura se denegaría.
234:     function isValidWorkerUpdate() {
235:       let worker = request.resource.data.get('worker', '');
236:       return !(worker is map)
237:         || (worker.get('enabled', true) is bool
238:           && worker.get('experienceYears', 0) is number
239:           && worker.get('experienceYears', 0) >= 0
240:           && worker.get('experienceYears', 0) <= 70
241:           // Distingue "escribí 0 años" de "nunca toqué el campo". Sin esto, un
242:           // trabajador que empieza y no tiene experiencia no podía llegar nunca
243:           // al 100 %: el perfil se quedaba en 92 para siempre.
244:           && worker.get('experienceDeclared', false) is bool
245:           && isValidStringList(worker.get('specialties', []), 3, 180)
246:           && isValidStringList(worker.get('skills', []), 10, 600)
247:           && worker.get('profileCompleted', 0) is number
248:           && worker.get('profileCompleted', 0) >= 0
249:           && worker.get('profileCompleted', 0) <= 100);
250:     }
251: 
252:     function isValidPrivacyUpdate() {
253:       let privacy = request.resource.data.get('privacy', '');
254:       return !(privacy is map)
255:         || (privacy.get('showPhone', false) is bool
256:           && privacy.get('showEmail', false) is bool
257:           && privacy.get('showExactAddress', false) is bool);
258:     }
259: 
260: 
261:     // =============================================================
262:     //  FASE 3 — Perfil CONTRATANTE
263:     // =============================================================
264: 
265:     function isValidEmployerDocument(documentType, documentNumber) {
266:       return isValidDocumentNumber(documentType, documentNumber);
267:     }
268: 
269:     function isValidEmployerBlock() {
270:       let employer = request.resource.data.get('employer', '');
271:       return employer is map
272:         && employer.get('enabled', false) == true
273:         && employer.get('employerType', '') in ['PERSONA', 'EMPRESA', 'NEGOCIO', 'INDEPENDIENTE']
274:         && isValidDocumentType(employer.get('documentType', ''))
275:         && isValidEmployerDocument(
276:              employer.get('documentType', ''),
277:              employer.get('documentNumber', ''))
278:         && employer.get('businessName', '') is string
279:         && employer.get('businessName', '').size() >= 3
280:         && employer.get('businessName', '').size() <= 120
281:         && employer.get('commercialName', '') is string
282:         && employer.get('commercialName', '').size() <= 120
283:         && employer.get('sector', '') is string
284:         && employer.get('sector', '').size() <= 120
285:         && employer.get('documentNumber', '') is string
286:         && (
287:           (employer.get('documentType', '') == 'RUC'
288:             && employer.get('ruc', '') == employer.get('documentNumber', ''))
289:           || (employer.get('documentType', '') == 'DNI'
290:             && (employer.get('ruc', null) == null || employer.get('ruc', '') == ''))
291:         )
292:         && employer.get('identityName', '') is string
293:         && employer.get('identityName', '').size() >= 3
294:         // FASE 4 — enlace al lugar principal. Nulo = aún sin lugar.
295:         && (employer.get('workplaceId', null) == null
296:           || (employer.get('workplaceId', '') is string
297:             && employer.get('workplaceId', '').size() <= 80))
298:         && employer.get('publishedCount', 0) is number
299:         && employer.get('hiredCount', 0) is number
300:         && employer.get('ratingAverage', 0.0) is number
301:         && employer.get('ratingCount', 0) is number;
302:     }
303: 
304:     function keepsEmployerReputation() {
305:       let before = resource.data.get('employer', '');
306:       let after = request.resource.data.get('employer', '');
307:       return !(after is map)
308:         || (!(before is map)
309:           && after.get('publishedCount', 0) == 0
310:           && after.get('hiredCount', 0) == 0
311:           && after.get('ratingAverage', 0.0) == 0.0
312:           && after.get('ratingCount', 0) == 0)
313:         || (before is map
314:           && after.get('publishedCount', 0) == before.get('publishedCount', 0)
315:           && after.get('hiredCount', 0) == before.get('hiredCount', 0)
316:           && after.get('ratingAverage', 0.0) == before.get('ratingAverage', 0.0)
317:           && after.get('ratingCount', 0) == before.get('ratingCount', 0));
318:     }
319: 
320:     function validRoleListWithEmployer() {
321:       let roles = request.resource.data.get('roles', []);
322:       return roles is list
323:         && roles.size() == 2
324:         && roles.hasOnly(['TRABAJADOR', 'CONTRATANTE'])
325:         && request.resource.data.get('activeRole', '') == 'CONTRATANTE'
326:         && isValidEmployerBlock()
327:         && keepsEmployerReputation();
328:     }
329: 
330:     // Requisitos previos para activar contratante (FASE 3):
331:     // identidad verificada, correo verificado, perfil básico completo
332:     function hasContractorPrerequisites() {
333:       let identity = resource.data.get('identity', '');
334:       let auth = resource.data.get('auth', '');
335:       let profile = resource.data.get('profile', '');
336:       return identity is map
337:         && identity.get('identityVerified', false) == true
338:         && auth is map
339:         && auth.get('emailVerified', false) == true
340:         && profile is map
341:         && profile.get('fullName', '') is string
342:         && profile.get('fullName', '').size() > 0
343:         && profile.get('username', '') is string
344:         && profile.get('username', '').size() > 0
345:         && profile.get('phone', '') is string
346:         && profile.get('phone', '').size() > 0;
347:     }
348: 
349:     function isAllowedContractorActivation(uid) {
350:       return request.resource.data.diff(resource.data).affectedKeys()
351:           .hasOnly(['roles', 'activeRole', 'employer', 'updatedAt'])
352:         && resource.data.roles is list
353:         && resource.data.roles.hasOnly(['TRABAJADOR'])
354:         && validRoleListWithEmployer()
355:         && hasContractorPrerequisites();
356:     }
357: 
358:     // Permitir actualizar employer cuando el usuario ya tiene ambos roles
359:     // (aunque activeRole sea TRABAJADOR): no es una activación nueva, solo
360:     // una edición de datos de contratante ya existente.
361:     function isAllowedEmployerUpdate(uid) {
362:       return request.resource.data.diff(resource.data).affectedKeys()
363:           .hasOnly(['employer', 'updatedAt'])
364:         && resource.data.roles is list
365:         && resource.data.roles.hasOnly(['TRABAJADOR', 'CONTRATANTE'])
366:         && isValidEmployerBlock()
367:         && keepsEmployerReputation();
368:     }
369: 
370:     // Primera activación de TRABAJADOR en una cuenta que nació contratante.
371:     // Espejo de la activación de contratante: suma el rol y entra en ese modo.
372:     // El bloque `worker` nace en cero (o se habilita el existente) y la
373:     // reputación queda intacta; el % se recalcula solo con los checks de
374:     // trabajador.
375:     function isAllowedWorkerActivation() {
376:       return request.resource.data.diff(resource.data).affectedKeys()
377:           .hasOnly(['roles', 'activeRole', 'worker', 'updatedAt'])
378:         && resource.data.roles is list
379:         && resource.data.roles.hasOnly(['CONTRATANTE'])
380:         && request.resource.data.roles.hasOnly(['TRABAJADOR', 'CONTRATANTE'])
381:         && request.resource.data.activeRole == 'TRABAJADOR'
382:         && request.resource.data.get('worker', '').get('enabled', false) == true
383:         && isValidWorkerUpdate()
384:         && keepsWorkerReputation();
385:     }
386: 
387:     function isAllowedActiveRoleSwitch() {
388:       return request.resource.data.diff(resource.data).affectedKeys()
389:           .hasOnly(['activeRole', 'updatedAt'])
390:         && request.resource.data.activeRole in request.resource.data.roles
391:         && request.resource.data.roles == resource.data.roles
392:         && (
393:           request.resource.data.activeRole == 'TRABAJADOR'
394:           || (
395:             request.resource.data.activeRole == 'CONTRATANTE'
396:             && request.resource.data.get('employer', '').get('enabled', false) == true
397:           )
398:         );
399:     }
400: 
401:     // =============================================================
402:     //  FASE 4 — Lugar / establecimiento (workplaces)
403:     // =============================================================
404:     //
405:     // Un contratante tiene UN lugar principal enlazado en
406:     // `users/{uid}.employer.workplaceId` (la Fase 5 lo usa como sede).
407:     // La foto es opcional; cuando hay, debe venir del módulo
408:     // `chambaya/fotos-lugares/{ownerUid}/` del propio dueño, igual que el
409:     // avatar exige `chambaya/fotos-perfil/{uid}/`.
410: 
411:     function isValidWorkplaceType(value) {
412:       return value is string && value in [
413:         'VIVIENDA', 'LOCAL_COMERCIAL', 'EMPRESA', 'TALLER',
414:         'RESTAURANTE', 'OBRA', 'CAMPO', 'OTRO'
415:       ];
416:     }
417: 
418:     function isValidWorkplaceLocation(loc) {
419:       return loc is map
420:         && (loc.get('latitude', null) == null || loc.get('latitude', 0) is number)
421:         && (loc.get('longitude', null) == null || loc.get('longitude', 0) is number)
422:         && (loc.get('latitude', null) == null || loc.get('longitude', null) == null
423:           || (loc.get('latitude', 0) >= -90 && loc.get('latitude', 0) <= 90
424:             && loc.get('longitude', 0) >= -180 && loc.get('longitude', 0) <= 180));
425:     }
426: 
427:     // Foto del lugar: vacía (lugar sin foto, válido) o URL de Cloudinary del
428:     // propio dueño con su publicId. El segmento `v123/` de versión se admite,
429:     // igual que en `isOwnCloudinaryPhoto`.
430:     function isOwnLugarPhoto(ownerUid) {
431:       return request.resource.data.get('photoUrl', '') is string
432:         && request.resource.data.get('photoUrl', '').size() > 0
433:         && request.resource.data.get('photoPublicId', '') is string
434:         && request.resource.data.get('photoPublicId', '').size() > 0
435:         && request.resource.data.get('photoUrl', '').matches(
436:              '^https://res\\.cloudinary\\.com/[^/]+/image/upload/(v[0-9]+/)?chambaya/fotos-lugares/' + ownerUid + '/[^/]+/[^/]+$');
437:     }
438: 
439:     function lugarPhotoOk(ownerUid) {
440:       let url = request.resource.data.get('photoUrl', '');
441:       let pid = request.resource.data.get('photoPublicId', '');
442:       return (url == '' && pid == '')
443:         || isOwnLugarPhoto(ownerUid);
444:     }
445: 
446:     // Campos editables comunes a crear y actualizar.
447:     function hasValidWorkplaceFields(ownerUid) {
448:       let d = request.resource.data;
449:       return d.get('name', '') is string
450:         && d.get('name', '').size() >= 3
451:         && d.get('name', '').size() <= 120
452:         && isValidWorkplaceType(d.get('type', ''))
453:         && d.get('sector', '') is string
454:         && d.get('sector', '').size() <= 120
455:         && d.get('description', '') is string
456:         && d.get('description', '').size() <= 1000
457:         && d.get('address', '') is string
458:         && d.get('address', '').size() <= 200
459:         && d.get('district', '') is string
460:         && d.get('district', '').size() <= 80
461:         && d.get('province', '') is string
462:         && d.get('province', '').size() <= 80
463:         && d.get('department', '') is string
464:         && d.get('department', '').size() <= 80
465:         && isValidWorkplaceLocation(d.get('location', {}))
466:         && d.get('photoPath', '') is string
467:         && lugarPhotoOk(ownerUid);
468:     }
469: 
470:     function isValidWorkplaceCreate(workplaceId) {
471:       let d = request.resource.data;
472:       return d.get('workplaceId', '') == workplaceId
473:         && d.get('ownerUid', '') == request.auth.uid
474:         && hasValidWorkplaceFields(d.get('ownerUid', ''))
475:         // Nadie se autoverifica: nace en false y solo la plataforma lo cambia.
476:         && d.get('verified', true) == false
477:         && d.get('createdAt', null) != null
478:         && d.get('updatedAt', null) != null;
479:     }
480: 
481:     function isValidWorkplaceUpdate() {
482:       let d = request.resource.data;
483:       return d.get('workplaceId', '') == resource.data.get('workplaceId', '')
484:         && d.get('ownerUid', '') == resource.data.get('ownerUid', '')
485:         && hasValidWorkplaceFields(resource.data.get('ownerUid', ''))
486:         && d.get('verified', false) == resource.data.get('verified', false)
487:         && d.get('createdAt', null) == resource.data.get('createdAt', null);
488:     }
489: 
490:     // --- Reputación: intocable para el usuario -
491:     // `workCount`, `ratingAverage` y `ratingCount` los calcula la plataforma.
492:     // La primera vez que se crea el bloque tienen que valer cero; después
493:     // deben quedar exactamente como estaban.
494:     //
495:     // Si el bloque no existe en el documento resultante, la escritura no lo
496:     // tocó y no hay nada que proteger.
497:     function reputationIsZero(after) {
498:       return after is map
499:         && after.get('workCount', 0) == 0
500:         && after.get('ratingAverage', 0.0) == 0.0
501:         && after.get('ratingCount', 0) == 0;
502:     }
503: 
504:     function sameReputation(before, after) {
505:       return after is map
506:         && after.get('workCount', 0) == before.get('workCount', 0)
507:         && after.get('ratingAverage', 0.0) == before.get('ratingAverage', 0.0)
508:         && after.get('ratingCount', 0) == before.get('ratingCount', 0);
509:     }
510: 
511:     function keepsWorkerReputation() {
512:       let before = resource.data.get('worker', '');
513:       let after = request.resource.data.get('worker', '');
514:       return !(after is map)
515:         || (before is map && sameReputation(before, after))
516:         || (!(before is map) && reputationIsZero(after));
517:     }
518: 
519:     // Los contadores de `statistics` también los incrementa la plataforma:
520:     // en la FASE 2 únicamente se crean en cero.
521:     function statisticsAreZero(after) {
522:       return after is map
523:         && after.get('applicationsCount', 0) == 0
524:         && after.get('publicationsCount', 0) == 0
525:         && after.get('completedJobsCount', 0) == 0
526:         && after.get('savedPublicationsCount', 0) == 0
527:         && after.get('receivedRatingsCount', 0) == 0;
528:     }
529: 
530:     function sameStatistics(before, after) {
531:       return after is map
532:         && after.get('applicationsCount', 0) == before.get('applicationsCount', 0)
533:         && after.get('publicationsCount', 0) == before.get('publicationsCount', 0)
534:         && after.get('completedJobsCount', 0) == before.get('completedJobsCount', 0)
535:         && after.get('savedPublicationsCount', 0) == before.get('savedPublicationsCount', 0)
536:         && after.get('receivedRatingsCount', 0) == before.get('receivedRatingsCount', 0);
537:     }
538: 
539:     function createsZeroStatistics() {
540:       let before = resource.data.get('statistics', '');
541:       let after = request.resource.data.get('statistics', '');
542:       return !(after is map)
543:         || (before is map && sameStatistics(before, after))
544:         || (!(before is map) && statisticsAreZero(after));
545:     }
546: 
547:     // =============================================================
548:     //  FASE 5 — Contadores de publicaciones (denormalizados)
549:     // =============================================================
550:     //
551:     // Al crear/eliminar una publicación la app suma/resta 1 a
552:     // `employer.publishedCount` y `statistics.publicationsCount` (Mi Perfil los
553:     // muestra). Las ramas existentes lo impedirían: `keepsEmployerReputation`
554:     // y `createsZeroStatistics` exigen esos contadores intactos. Esta rama lo
555:     // permite SOLO cuando el único cambio es un paso de ±1 en esos dos
556:     // contadores: nadie puede inventarse reputación ni tocar nada más.
557:     function isAllowedPublicationCounterUpdate() {
558:       let touched = request.resource.data.diff(resource.data).affectedKeys();
559:       let beforeEmp = resource.data.get('employer', '');
560:       let afterEmp = request.resource.data.get('employer', '');
561:       let beforeStat = resource.data.get('statistics', '');
562:       let afterStat = request.resource.data.get('statistics', '');
563:       return touched.hasOnly(['employer', 'statistics', 'updatedAt'])
564:         && beforeEmp is map && afterEmp is map
565:         && beforeStat is map && afterStat is map
566:         && afterEmp.diff(beforeEmp).affectedKeys().hasOnly(['publishedCount'])
567:         && (afterEmp.get('publishedCount', 0) == beforeEmp.get('publishedCount', 0) + 1
568:           || afterEmp.get('publishedCount', 0) == beforeEmp.get('publishedCount', 0) - 1)
569:         && afterStat.diff(beforeStat).affectedKeys().hasOnly(['publicationsCount'])
570:         && (afterStat.get('publicationsCount', 0) == beforeStat.get('publicationsCount', 0) + 1
571:           || afterStat.get('publicationsCount', 0) == beforeStat.get('publicationsCount', 0) - 1);
572:     }
573: 
574:     // Rama FASE 2 de la actualización de `users/{uid}`.
575:     // `affectedKeys()` devuelve las claves de primer nivel, así que
576:     // `hasOnly(...)` ya impide tocar `identity`, `auth`, `roles`,
577:     // `registrationStatus`, `accountStatus`, `emailVerified` u `otpVerified`.
578:     function isAllowedProfileUpdate(uid) {
579:       return request.resource.data.diff(resource.data)
580:           .affectedKeys()
581:           .hasOnly(['profile', 'worker', 'privacy', 'statistics', 'employer', 'updatedAt'])
582:         && isValidProfileUpdate()
583:         && isValidWorkerUpdate()
584:         && isValidPrivacyUpdate()
585:         && photoBelongsToOwner(uid)
586:         && keepsWorkerReputation()
587:         && createsZeroStatistics()
588:         && (
589:           !(request.resource.data.diff(resource.data).affectedKeys().hasAny(['employer']))
590:           || (
591:             request.resource.data.activeRole == 'CONTRATANTE'
592:             && isValidEmployerBlock()
593:             && keepsEmployerReputation()
594:           )
595:         );
596:     }
597: 
598:     // =============================================================
599:     //  Verificación de correo OTP
600:     //  (compatible con plan Spark: la app escribe el hash directamente)
601:     // =============================================================
602:     function isValidOtpSession(ownerUid, verifiedState) {
603:       let data = request.resource.data;
604:       return data.keys().hasAll(['email', 'otpHash', 'verified'])
605:         && data.uid == ownerUid
606:         && data.email is string
607:         && data.email.size() > 3
608:         && data.otpHash is string
609:         && data.otpHash.size() > 0
610:         && data.verified == verifiedState;
611:     }
612: 
613:     match /email_verifications/{uid} {
614:       allow read: if isOwner(uid);
615: 
616:       // Crear o refrescar la sesión OTP (reenvío). Una sesión ya verificada
617:       // no se puede reutilizar ni sobrescribir.
618:       allow create, update: if isOwner(uid) && isValidOtpSession(uid, false);
619: 
620:       // Marcar el OTP como usado: es la única transición permitida sobre `verified`.
621:       allow update: if isOwner(uid)
622:         && resource.data.verified == false
623:         && request.resource.data.verified == true
624:         && request.resource.data.diff(resource.data)
625:              .affectedKeys()
626:              .hasOnly(['verified', 'otpHash', 'verifiedAt']);
627: 
628:       allow delete: if isOwner(uid);
629:     }
630: 
631:     // =============================================================
632:     //  users/{uid} — FASE 1 + FASE 2
633:     // =============================================================
634:     match /users/{uid} {
635:       // Sin listados: el DNI/RUC nunca debe exponerse en consultas masivas.
636:       allow list: if false;
637: 
638:       // El dueño puede leer su propio documento (login, perfil, resumen).
639:       allow get: if isOwner(uid);
640: 
641:       // Alta: solo el propio usuario, con identidad validada por padrón oficial.
642:       allow create: if isOwner(uid)
643:         && request.resource.data.uid == uid
644:         && isCompleteRegistration();
645: 
646:       // Actualización:
647:       //  1) Solo campos de auditoría (último ingreso), o
648:       //  2) completar el registro cuando aún no estaba VERIFIED, o
649:       //  3) migración única de cuentas creadas antes de la FASE 1, o
650:       //  4) FASE 2: completar/editar el perfil (Mi Perfil), o
651:       //  5) FASE 5: pasos ±1 en contadores de publicaciones, o
652:       //  6) FASE 9: agregado de reputación (lo escribe QUIEN CALIFICA,
653:       //     no el dueño: por eso va fuera del isOwner).
654:       allow update: if (isOwner(uid)
655:         && (onlyAuditFieldsChanged()
656:           || (!(resource.data.identity is map) && isCompleteRegistration())
657:           || (resource.data.registrationStatus != 'VERIFIED'
658:             && isCompleteRegistration()
659:             && keepsDocumentNumber())
660:           || isAllowedContractorActivation(uid)
661:           || isAllowedWorkerActivation()
662:           || isAllowedEmployerUpdate(uid)
663:           || isAllowedActiveRoleSwitch()
664:           || isAllowedPublicationCounterUpdate()
665:           || isAllowedProfileUpdate(uid)))
666:         || (request.auth != null && isAllowedRatingUpdate());
667: 
668:       allow delete: if false;
669:     }
670: 
671:     // =============================================================
672:     //  usernames/{username} — unicidad del @usuario (FASE 2)
673:     // =============================================================
674:     //
675:     // Un documento por @usuario normalizado. La transacción de
676:     // ProfileRepository es la que garantiza que dos personas no puedan tomar
677:     // el mismo nombre a la vez.
678:     match /usernames/{username} {
679:       // Consulta puntual de disponibilidad: cualquier usuario autenticado.
680:       allow get: if request.auth != null;
681: 
682:       // Sin listados: permitiría enumerar todos los @usuario de la plataforma.
683:       allow list: if false;
684: 
685:       // Solo se puede tomar un @usuario a nombre propio, con formato válido
686:       // (FASE 16: antes se podía ocupar cualquier cadena) y sin campos extra.
687:       allow create: if request.auth != null
688:         && isValidUsername(username)
689:         && request.resource.data.uid == request.auth.uid
690:         && request.resource.data.keys().hasOnly(['uid', 'username', 'updatedAt']);
691: 
692:       // No se puede robar ni reasignar el @usuario de otro.
693:       allow update: if request.auth != null
694:         && isValidUsername(username)
695:         && resource.data.uid == request.auth.uid
696:         && request.resource.data.uid == request.auth.uid
697:         && request.resource.data.keys().hasOnly(['uid', 'username', 'updatedAt']);
698: 
699:       // Al cambiar o borrar el perfil, el @usuario viejo queda libre.
700:       allow delete: if request.auth != null
701:         && resource.data.uid == request.auth.uid;
702:     }
703: 
704:     // =============================================================
705:     //  workplaces/{workplaceId} — FASE 4 Lugar / establecimiento
706:     // =============================================================
707:     //
708:     // Lectura para autenticados (la Fase 5-6 muestra la sede en publicaciones
709:     // y búsquedas cercanas); escritura solo del dueño con campos validados.
710:     match /workplaces/{workplaceId} {
711:       allow get: if request.auth != null;
712: 
713:       // Sin listado masivo: la Fase 6 filtra por dueño o cercanía con
714:       // consultas acotadas, no barriendo la colección.
715:       allow list: if request.auth != null;
716: 
717:       allow create: if request.auth != null
718:         && isValidWorkplaceCreate(workplaceId);
719: 
720:       allow update: if request.auth != null
721:         && resource.data.get('ownerUid', '') == request.auth.uid
722:         && isValidWorkplaceUpdate();
723: 
724:       allow delete: if request.auth != null
725:         && resource.data.get('ownerUid', '') == request.auth.uid;
726:     }
727: 
728:     // =============================================================
729:     //  FASE 5 — Publicaciones publications/{publicationId}
730:     // =============================================================
731:     //
732:     // Lectura pública de ACTIVE+PUBLIC para el feed (Fase 6); el dueño puede
733:     // leer las suyas en cualquier estado. Escritura solo del dueño contratante
734:     // con campos validados. La reputación (statistics, workersHired) y el
735:     // publicador verificado los calcula la plataforma: la app no los inventa.
736: 
737:     function isValidPublicationStatus(v) {
738:       return v is string && v in ['ACTIVE', 'PAUSED', 'FINISHED', 'ARCHIVED'];
739:     }
740: 
741:     function isValidPaymentBlock(p) {
742:       return p is map
743:         && p.get('amount', 0) is number
744:         && p.get('amount', 0) > 0
745:         && p.get('amount', 0) <= 100000
746:         && p.get('currency', '') == 'PEN'
747:         && p.get('period', '') in ['HOUR', 'DAY', 'WEEK', 'MONTH', 'JOB']
748:         && p.get('negotiable', false) is bool;
749:     }
750: 
751:     function isValidPublicationImages(imgs, ownerUid, pubId) {
752:       return imgs is list
753:         && imgs.size() <= 3;
754:     }
755: 
756:     function hasValidPublicationFields(pubId) {
757:       let d = request.resource.data;
758:       return d.get('publicationId', '') == pubId
759:         && d.get('ownerUid', '') == request.auth.uid
760:         && isValidPublicationStatus(d.get('status', ''))
761:         && d.get('visibility', '') in ['PUBLIC', 'HIDDEN']
762:         && d.get('type', '') == 'JOB_OFFER'
763:         && d.get('title', '') is string
764:         && d.get('title', '').size() >= 8
765:         && d.get('title', '').size() <= 100
766:         && d.get('description', '') is string
767:         && d.get('description', '').size() >= 20
768:         && d.get('description', '').size() <= 2000
769:         && d.get('category', '') is string
770:         && d.get('category', '').size() >= 2
771:         && d.get('category', '').size() <= 60
772:         && isValidPaymentBlock(d.get('payment', {}))
773:         && d.get('workersNeeded', 0) is number
774:         && d.get('workersNeeded', 0) >= 1
775:         && d.get('workersNeeded', 0) <= 50
776:         && d.get('location', {}).get('district', '') is string
777:         && d.get('location', {}).get('district', '').size() >= 2
778:         && isValidPublicationImages(d.get('images', []), request.auth.uid, pubId);
779:     }
780: 
781:     function keepsPublicationCounters() {
782:       let before = resource.data.get('statistics', {});
783:       let after = request.resource.data.get('statistics', {});
784:       let touched = request.resource.data.diff(resource.data).affectedKeys();
785:       return !touched.hasAny(['statistics'])
786:         || (after.get('views', 0) == before.get('views', 0)
787:           && after.get('likes', 0) == before.get('likes', 0)
788:           && after.get('comments', 0) == before.get('comments', 0)
789:           && after.get('shares', 0) == before.get('shares', 0)
790:           && after.get('saves', 0) == before.get('saves', 0)
791:           && after.get('applications', 0) == before.get('applications', 0));
792:     }
793: 
794:     match /publications/{publicationId} {
795:       // Feed: cualquiera autenticado puede listar ACTIVE+PUBLIC.
796:       // El detalle de una pausada/finalizada solo lo ve su dueño.
797:       allow get: if request.auth != null
798:         && (resource.data.get('status', '') == 'ACTIVE'
799:           || resource.data.get('ownerUid', '') == request.auth.uid);
800:       allow list: if request.auth != null;
801: 
802:       allow create: if request.auth != null
803:         && hasValidPublicationFields(publicationId)
804:         && request.resource.data.get('workersHired', 0) == 0
805:         && request.resource.data.get('createdAt', null) != null
806:         && request.resource.data.get('updatedAt', null) != null;
807: 
808:       // El dueño edita contenido; el estado cambia por su propio campo.
809:       // statistics solo se mueve en pasos de ±1 (vistas, likes, guardados,
810:       // comentarios, postulaciones: Fases 6-10) vía isAllowedStatisticsStep;
811:       // workersHired/publisher/ownerUid/createdAt los mueve el dueño o la
812:       // plataforma, nunca un tercero.
813:       allow update: if request.auth != null
814:         && ((resource.data.get('ownerUid', '') == request.auth.uid
815:           && request.resource.data.get('ownerUid', '') == resource.data.get('ownerUid', '')
816:           && request.resource.data.get('publicationId', '') == resource.data.get('publicationId', '')
817:           && request.resource.data.get('createdAt', null) == resource.data.get('createdAt', null)
818:           && keepsPublicationCounters())
819:           || isAllowedStatisticsStep());
820: 
821:       allow delete: if request.auth != null
822:         && resource.data.get('ownerUid', '') == request.auth.uid;
823:     }
824: 
825:     // =============================================================
826:     //  FASE 6 — Interacciones del feed
827:     // =============================================================
828:     // Documento id = {publicationId}_{uid}: solo su dueño lo escribe.
829: 
830:     function isOwnInteraction(docId) {
831:       return request.auth != null
832:         && docId.matches('^.+_' + request.auth.uid + '$');
833:     }
834: 
835:     match /publication_likes/{docId} {
836:       allow get, list: if request.auth != null;
837:       allow create: if isOwnInteraction(docId)
838:         && request.resource.data.get('userUid', '') == request.auth.uid
839:         && request.resource.data.get('publicationId', '') is string;
840:       allow delete: if isOwnInteraction(docId)
841:         && resource.data.get('userUid', '') == request.auth.uid;
842:       allow update: if false;
843:     }
844: 
845:     match /publication_saves/{docId} {
846:       allow get, list: if request.auth != null;
847:       allow create: if isOwnInteraction(docId)
848:         && request.resource.data.get('userUid', '') == request.auth.uid
849:         && request.resource.data.get('publicationId', '') is string;
850:       allow delete: if isOwnInteraction(docId)
851:         && resource.data.get('userUid', '') == request.auth.uid;
852:       allow update: if false;
853:     }
854: 
855:     match /hidden_publications/{docId} {
856:       allow get, list: if request.auth != null
857:         && resource.data.get('userUid', '') == request.auth.uid;
858:       allow create: if isOwnInteraction(docId)
859:         && request.resource.data.get('userUid', '') == request.auth.uid;
860:       allow update, delete: if request.auth != null
861:         && resource.data.get('userUid', '') == request.auth.uid;
862:     }
863: 
864:     match /publication_reports/{reportId} {
865:       allow get, list: if request.auth != null
866:         && resource.data.get('reporterUid', '') == request.auth.uid;
867:       allow create: if request.auth != null
868:         && request.resource.data.get('reporterUid', '') == request.auth.uid
869:         && request.resource.data.get('publicationId', '') is string
870:         && request.resource.data.get('reason', '') is string
871:         && request.resource.data.get('status', '') == 'PENDING';
872:       // El estado de moderación no lo toca el usuario normal.
873:       allow update, delete: if false;
874:     }
875: 
876:     // =============================================================
877:     //  FASES 6-10 — Pasos ±1 en statistics de publications
878:     // =============================================================
879:     //
880:     // Vistas, likes, guardados, comentarios y postulaciones los registran
881:     // usuarios DISTINTOS del dueño (un like lo escribe quien lo da). Sin esta
882:     // rama esos incrementos se denegarían siempre. Solo se admite mover esos
883:     // contadores de uno en uno y no tocar nada más.
884: 
885:     function statStepOk(before, after, key) {
886:       return after.get(key, 0) == before.get(key, 0)
887:         || after.get(key, 0) == before.get(key, 0) + 1
888:         || after.get(key, 0) == before.get(key, 0) - 1;
889:     }
890: 
891:     function isAllowedStatisticsStep() {
892:       let beforeS = resource.data.get('statistics', '');
893:       let afterS = request.resource.data.get('statistics', '');
894:       return request.auth != null
895:         && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['statistics', 'updatedAt'])
896:         && beforeS is map && afterS is map
897:         && afterS.diff(beforeS).affectedKeys()
898:           .hasOnly(['views', 'likes', 'comments', 'shares', 'saves', 'applications'])
899:         && statStepOk(beforeS, afterS, 'views')
900:         && statStepOk(beforeS, afterS, 'likes')
901:         && statStepOk(beforeS, afterS, 'comments')
902:         && statStepOk(beforeS, afterS, 'shares')
903:         && statStepOk(beforeS, afterS, 'saves')
904:         && statStepOk(beforeS, afterS, 'applications');
905:     }
906: 
907:     // =============================================================
908:     //  FASE 7 — Postulaciones applications/{applicationId}
909:     // =============================================================
910: 
911:     function isValidApplicationCreate() {
912:       let d = request.resource.data;
913:       return d.get('applicationId', '') is string
914:         && d.get('applicationId', '').size() > 0
915:         && d.get('publicationId', '') is string
916:         && d.get('publicationId', '').size() > 0
917:         && d.get('workerUid', '') == request.auth.uid
918:         && d.get('employerUid', '') is string
919:         && d.get('employerUid', '').size() > 0
920:         && d.get('employerUid', '') != request.auth.uid
921:         && d.get('status', '') == 'PENDING'
922:         && d.get('message', '') is string
923:         && d.get('message', '').size() <= 500
924:         && d.get('createdAt', null) != null
925:         && d.get('updatedAt', null) != null;
926:     }
927: 
928:     // PENDING → WITHDRAWN (trabajador) o → ACCEPTED/REJECTED (contratante).
929:     // Solo cambia status (+updatedAt); el resto es inmutable.
930:     function isValidApplicationTransition() {
931:       let from = resource.data.get('status', '');
932:       let to = request.resource.data.get('status', '');
933:       let byWorker = resource.data.get('workerUid', '') == request.auth.uid;
934:       let byEmployer = resource.data.get('employerUid', '') == request.auth.uid;
935:       return request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status', 'updatedAt'])
936:         && ((from == 'PENDING' && to == 'WITHDRAWN' && byWorker)
937:           || (from == 'PENDING' && (to == 'ACCEPTED' || to == 'REJECTED') && byEmployer));
938:     }
939: 
940:     match /applications/{applicationId} {
941:       allow get: if request.auth != null
942:         && (resource.data.get('workerUid', '') == request.auth.uid
943:           || resource.data.get('employerUid', '') == request.auth.uid);
944:       // Sin listado masivo: la app consulta por workerUid/employerUid.
945:       allow list: if request.auth != null;
946:       allow create: if request.auth != null && isValidApplicationCreate();
947:       allow update: if request.auth != null && isValidApplicationTransition();
948:       allow delete: if false;
949:     }
950: 
951:     // =============================================================
952:     //  FASE 8 — Trabajos jobs/{jobId}
953:     // =============================================================
954: 
955:     function isValidJobCreate() {
956:       let d = request.resource.data;
957:       return d.get('jobId', '') is string
958:         && d.get('jobId', '').size() > 0
959:         && d.get('employerUid', '') == request.auth.uid
960:         && d.get('workerUid', '') is string
961:         && d.get('workerUid', '') != request.auth.uid
962:         && d.get('applicationId', '') is string
963:         && d.get('publicationId', '') is string
964:         && d.get('status', '') == 'ACCEPTED'
965:         && d.get('agreedPayment', {}).get('amount', 0) is number
966:         && d.get('agreedPayment', {}).get('amount', 0) > 0
967:         && d.get('createdAt', null) != null
968:         && d.get('updatedAt', null) != null;
969:     }
970: 
971:     function isValidJobTransition() {
972:       let from = resource.data.get('status', '');
973:       let to = request.resource.data.get('status', '');
974:       let party = resource.data.get('workerUid', '') == request.auth.uid
975:         || resource.data.get('employerUid', '') == request.auth.uid;
976:       let okMove = (from == 'ACCEPTED' && (to == 'IN_PROGRESS' || to == 'CANCELLED'))
977:         || (from == 'IN_PROGRESS' && (to == 'COMPLETED' || to == 'CANCELLED'));
978:       return party && okMove
979:         && request.resource.data.diff(resource.data).affectedKeys()
980:           .hasOnly(['status', 'startedAt', 'completedAt', 'updatedAt'])
981:         && request.resource.data.get('workerUid', '') == resource.data.get('workerUid', '')
982:         && request.resource.data.get('employerUid', '') == resource.data.get('employerUid', '')
983:         && request.resource.data.get('applicationId', '') == resource.data.get('applicationId', '')
984:         && request.resource.data.get('publicationId', '') == resource.data.get('publicationId', '');
985:     }
986: 
987:     match /jobs/{jobId} {
988:       allow get: if request.auth != null
989:         && (resource.data.get('workerUid', '') == request.auth.uid
990:           || resource.data.get('employerUid', '') == request.auth.uid);
991:       // La app consulta por applicationId/publicationId+workerUid.
992:       allow list: if request.auth != null;
993:       allow create: if request.auth != null && isValidJobCreate();
994:       allow update: if request.auth != null && isValidJobTransition();
995:       allow delete: if false;
996:     }
997: 
998:     // =============================================================
999:     //  FASE 9 — Calificaciones ratings/{ratingId}
1000:     // =============================================================
1001: 
1002:     function isAllowedRatingUpdate() {
1003:       let touched = request.resource.data.diff(resource.data).affectedKeys();
1004:       let beforeW = resource.data.get('worker', '');
1005:       let afterW = request.resource.data.get('worker', '');
1006:       let beforeE = resource.data.get('employer', '');
1007:       let afterE = request.resource.data.get('employer', '');
1008:       let beforeS = resource.data.get('statistics', '');
1009:       let afterS = request.resource.data.get('statistics', '');
1010:       let workerStep = touched.hasOnly(['worker', 'statistics', 'updatedAt'])
1011:         && beforeW is map && afterW is map
1012:         && afterW.diff(beforeW).affectedKeys().hasOnly(['ratingAverage', 'ratingCount'])
1013:         && afterW.get('ratingCount', 0) == beforeW.get('ratingCount', 0) + 1
1014:         && afterW.get('ratingAverage', 0.0) is number
1015:         && afterW.get('ratingAverage', 0.0) >= 0.0
1016:         && afterW.get('ratingAverage', 0.0) <= 5.0;
1017:       let employerStep = touched.hasOnly(['employer', 'statistics', 'updatedAt'])
1018:         && beforeE is map && afterE is map
1019:         && afterE.diff(beforeE).affectedKeys().hasOnly(['ratingAverage', 'ratingCount'])
1020:         && afterE.get('ratingCount', 0) == beforeE.get('ratingCount', 0) + 1
1021:         && afterE.get('ratingAverage', 0.0) is number
1022:         && afterE.get('ratingAverage', 0.0) >= 0.0
1023:         && afterE.get('ratingAverage', 0.0) <= 5.0;
1024:       return request.auth != null
1025:         && beforeS is map && afterS is map
1026:         && afterS.diff(beforeS).affectedKeys().hasOnly(['receivedRatingsCount'])
1027:         && afterS.get('receivedRatingsCount', 0) == beforeS.get('receivedRatingsCount', 0) + 1
1028:         && (workerStep || employerStep);
1029:     }
1030: 
1031:     match /ratings/{ratingId} {
1032:       allow get, list: if request.auth != null;
1033:       allow create: if request.auth != null
1034:         && request.resource.data.get('fromUid', '') == request.auth.uid
1035:         && request.resource.data.get('toUid', '') is string
1036:         && request.resource.data.get('toUid', '').size() > 0
1037:         && request.resource.data.get('toUid', '') != request.auth.uid
1038:         && request.resource.data.get('jobId', '') is string
1039:         && request.resource.data.get('jobId', '').size() > 0
1040:         && request.resource.data.get('rating', 0) is number
1041:         && request.resource.data.get('rating', 0) >= 1
1042:         && request.resource.data.get('rating', 0) <= 5
1043:         && request.resource.data.get('comment', '') is string
1044:         && request.resource.data.get('comment', '').size() <= 500
1045:         && request.resource.data.get('createdAt', null) != null;
1046:       allow update, delete: if false;
1047:     }
1048: 
1049:     // =============================================================
1050:     //  FASE 10 — Comentarios comments/{commentId}
1051:     // =============================================================
1052: 
1053:     match /comments/{commentId} {
1054:       allow get: if request.auth != null
1055:         && (resource.data.get('status', '') == 'VISIBLE'
1056:           || resource.data.get('authorUid', '') == request.auth.uid);
1057:       allow list: if request.auth != null;
1058:       allow create: if request.auth != null
1059:         && request.resource.data.get('authorUid', '') == request.auth.uid
1060:         && request.resource.data.get('publicationId', '') is string
1061:         && request.resource.data.get('publicationId', '').size() > 0
1062:         && request.resource.data.get('text', '') is string
1063:         && request.resource.data.get('text', '').size() >= 1
1064:         && request.resource.data.get('text', '').size() <= 500
1065:         && request.resource.data.get('status', '') == 'VISIBLE'
1066:         && request.resource.data.get('createdAt', null) != null;
1067:       allow update: if request.auth != null
1068:         && resource.data.get('authorUid', '') == request.auth.uid
1069:         && request.resource.data.get('authorUid', '') == request.auth.uid
1070:         && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['text', 'updatedAt']);
1071:       allow delete: if request.auth != null
1072:         && resource.data.get('authorUid', '') == request.auth.uid;
1073:     }
1074: 
1075:     // =============================================================
1076:     //  FASE 6/7/17 — Perfiles públicos public_profiles/{uid}
1077:     // =============================================================
1078:     //
1079:     // Solo datos visibles para todos (nombre, @usuario, foto, bio, distrito,
1080:     // verificación y bloques worker/employer sin reputación). La app lo
1081:     // mantiene con syncPublicProfile al guardar perfil, activar roles y
1082:     // enlazar lugar. Los agregados (publicaciones, trabajos, promedios) se
1083:     // calculan en vivo, nunca se duplican aquí.
1084: 
1085:     function isValidPublicProfile(uid) {
1086:       let d = request.resource.data;
1087:       let w = d.get('worker', '');
1088:       let e = d.get('employer', '');
1089:       return d.get('uid', '') == uid
1090:         && d.get('fullName', '') is string
1091:         && d.get('fullName', '').size() <= 120
1092:         && isValidUsername(d.get('username', ''))
1093:         && d.get('photoUrl', '') is string
1094:         && d.get('photoUrl', '').size() <= 500
1095:         && d.get('bio', '') is string
1096:         && d.get('bio', '').size() <= 500
1097:         && d.get('district', '') is string
1098:         && d.get('district', '').size() <= 80
1099:         && d.get('province', '') is string
1100:         && d.get('province', '').size() <= 80
1101:         && d.get('identityVerified', false) is bool
1102:         && w is map

(Output capped at 50 KB. Showing lines 1-1102. Use offset=1103 to continue.)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\functions"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\functions</path>
<type>directory</type>
<entries>
.env
.env.example
index.js
package.json

(4 entries)
</entries>
```

---

## Assistant (Build · Muse Spark 1.3 Free · 4.7s)

_Thinking:_



**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src</path>
<type>directory</type>
<entries>
androidTest/
main/
test/

(3 entries)
</entries>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\build.gradle.kts"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\build.gradle.kts</path>
<type>file</type>
<content>
1: plugins {
2:     alias(libs.plugins.android.application)
3:     alias(libs.plugins.kotlin.compose)
4:     id("com.google.gms.google-services")
5: }
6: 
7: android {
8:     namespace = "com.proyecto.chambaya"
9:     compileSdk = 36
10: 
11:     defaultConfig {
12:         applicationId = "com.proyecto.chambaya"
13:         minSdk = 24
14:         targetSdk = 36
15:         versionCode = 1
16:         versionName = "1.0"
17: 
18:         // ── FASE 2 · Cloudinary (subida de fotos) ────────────────────────────
19:         // Credenciales PÚBLICAS: cloud name + preset sin firmar.
20:         // El "API Secret" se queda fuera de la app a propósito; vive solo en el
21:         // dashboard de Cloudinary.
22:         buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"vtmk2tgh\"")
23:         buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"chambaya_preset\"")
24:         // Raíz de los módulos de imagen. Cada tipo de imagen va a su propio módulo
25:         // para que el dashboard de Cloudinary no mezcle avatares con mapas o con
26:         // anuncios:
27:         //
28:         //   chambaya/
29:         //     oficios/            <- imágenes de las categorías de oficios
30:         //                              (las sube quien mantiene api_oficios.json)
31:         //     fotos-perfil/       <- avatares, una carpeta por uid
32:         //     fotos-lugares/      <- fotos de los lugares del mapa
33:         //     fotos-publicaciones/<- fotos de los anuncios y publicaciones
34:         //
35:         // Sin espacios ni acentos a propósito: estas rutas acaban dentro de una
36:         // expresión regular de `firestore.rules` y en la URL pública, y un espacio
37:         // obligaría a escribir `%20` en los dos sitios.
38:         buildConfigField("String", "CLOUDINARY_FOLDER_ROOT", "\"chambaya\"")
39:         buildConfigField("String", "CLOUDINARY_FOLDER_PERFILES", "\"fotos-perfil\"")
40:         buildConfigField("String", "CLOUDINARY_FOLDER_LUGARES", "\"fotos-lugares\"")
41:         buildConfigField("String", "CLOUDINARY_FOLDER_PUBLICACIONES", "\"fotos-publicaciones\"")
42: 
43:         testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
44:     }
45: 
46:     buildTypes {
47:         release {
48:             isMinifyEnabled = false
49:             proguardFiles(
50:                 getDefaultProguardFile("proguard-android-optimize.txt"),
51:                 "proguard-rules.pro"
52:             )
53:         }
54:     }
55:     compileOptions {
56:         sourceCompatibility = JavaVersion.VERSION_11
57:         targetCompatibility = JavaVersion.VERSION_11
58:     }
59:     buildFeatures {
60:         compose = true
61:         // FASE 2: `BuildConfig` para las credenciales públicas de Cloudinary.
62:         // Solo van el Cloud Name y el nombre del preset unsigned: el API Secret
63:         // NUNCA debe entrar en la app (permitiría subir/borrar desde el móvil).
64:         buildConfig = true
65:     }
66: }
67: 
68: dependencies {
69:     implementation(libs.androidx.activity.ktx)
70:     implementation(libs.androidx.appcompat)
71:     implementation(libs.androidx.constraintlayout)
72:     implementation(libs.androidx.cardview)
73:     implementation(libs.androidx.coordinatorlayout)
74:     implementation(libs.androidx.core.ktx)
75:     implementation(libs.material)
76: 
77:     // Firebase & Auth
78:     implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
79:     implementation("com.google.firebase:firebase-auth")
80:     implementation("com.google.firebase:firebase-firestore")
81:     implementation("com.google.firebase:firebase-functions")
82:     implementation("com.google.android.gms:play-services-auth:21.3.0")
83: 
84:     // Compose
85:     implementation(libs.androidx.lifecycle.runtime.ktx)
86:     implementation(libs.androidx.activity.compose)
87:     implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
88:     implementation(platform(libs.androidx.compose.bom))
89:     implementation(libs.androidx.ui)
90:     implementation(libs.androidx.ui.graphics)
91:     implementation(libs.androidx.ui.tooling.preview)
92:     implementation(libs.androidx.material3)
93:     debugImplementation(libs.androidx.ui.tooling)
94: 
95:     // Splash screen
96:     implementation(libs.androidx.core.splashscreen)
97: 
98:     // MapLibre Maps SDK (Native Mapbox Vector Rendering Engine) & Location Services
99:     implementation("org.maplibre.gl:android-sdk:11.8.3")
100:     implementation("com.google.android.gms:play-services-location:21.3.0")
101: 
102:     // Glide for image loading
103:     implementation("com.github.bumptech.glide:glide:4.16.0")
104: 
105:     // Coil for SVG support (mejor que Glide para SVG)
106:     implementation("io.coil-kt:coil:2.5.0")
107:     implementation("io.coil-kt:coil-svg:2.5.0")
108: 
109:     // Gson for JSON parsing
110:     implementation("com.google.code.gson:gson:2.10.1")
111: 
112:     // Carrusel de fotos del detalle (ViewPager2 + dots propios)
113:     implementation("androidx.viewpager2:viewpager2:1.1.0")
114: 
115:     testImplementation(libs.junit)
116:     androidTestImplementation(libs.androidx.espresso.core)
117:     androidTestImplementation(libs.androidx.junit)
118: }

(End of file - total 118 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\firestore.rules",
  "offset": 1103
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\firestore.rules</path>
<type>file</type>
<content>
1103:         && (w.get('specialties', []) is list && w.get('specialties', []).size() <= 3)
1104:         && (w.get('skills', []) is list && w.get('skills', []).size() <= 10)
1105:         && e is map
1106:         && e.get('businessName', '') is string
1107:         && e.get('businessName', '').size() <= 120
1108:         && (e.get('workplaceId', null) == null
1109:           || (e.get('workplaceId', '') is string && e.get('workplaceId', '').size() <= 80));
1110:     }
1111: 
1112:     match /public_profiles/{uid} {
1113:       allow get, list: if request.auth != null;
1114:       allow create, update: if isOwner(uid) && isValidPublicProfile(uid);
1115:       allow delete: if false;
1116:     }
1117: 
1118:     // =============================================================
1119:     //  FASE 12 — Bloqueos user_blocks/{blockerUid_blockedUid}
1120:     // =============================================================
1121:     //
1122:     // Id determinístico para que las reglas del chat puedan comprobar el
1123:     // bloqueo en ambas direcciones sin consultas adicionales.
1124: 
1125:     match /user_blocks/{docId} {
1126:       allow get: if request.auth != null
1127:         && resource.data.get('blockerUid', '') == request.auth.uid;
1128:       // La app consulta mis bloqueos (blockerUid == yo).
1129:       allow list: if request.auth != null;
1130:       allow create: if request.auth != null
1131:         && request.resource.data.get('blockerUid', '') == request.auth.uid
1132:         && request.resource.data.get('blockedUid', '') is string
1133:         && request.resource.data.get('blockedUid', '').size() > 0
1134:         && request.resource.data.get('blockedUid', '') != request.auth.uid
1135:         && docId == request.resource.data.get('blockerUid', '') + '_' + request.resource.data.get('blockedUid', '')
1136:         && request.resource.data.get('createdAt', null) != null;
1137:       allow update: if false;
1138:       allow delete: if request.auth != null
1139:         && resource.data.get('blockerUid', '') == request.auth.uid;
1140:     }
1141: 
1142:     // =============================================================
1143:     //  FASE 12 — Denuncias de usuarios user_reports/{reportId}
1144:     // =============================================================
1145: 
1146:     match /user_reports/{reportId} {
1147:       allow get, list: if request.auth != null
1148:         && resource.data.get('reporterUid', '') == request.auth.uid;
1149:       allow create: if request.auth != null
1150:         && request.resource.data.get('reporterUid', '') == request.auth.uid
1151:         && request.resource.data.get('reportedUid', '') is string
1152:         && request.resource.data.get('reportedUid', '').size() > 0
1153:         && request.resource.data.get('reportedUid', '') != request.auth.uid
1154:         && request.resource.data.get('reason', '') is string
1155:         && request.resource.data.get('reason', '').size() > 0
1156:         && request.resource.data.get('description', '') is string
1157:         && request.resource.data.get('description', '').size() <= 500
1158:         && request.resource.data.get('status', '') == 'PENDING'
1159:         && request.resource.data.get('createdAt', null) != null;
1160:       allow update, delete: if false;
1161:     }
1162: 
1163:     // =============================================================
1164:     //  FASE 13 — Chat conversations/{conversationId} + messages
1165:     // =============================================================
1166: 
1167:     function isParticipant(convData) {
1168:       return convData.get('participants', []).hasAny([request.auth.uid]);
1169:     }
1170: 
1171:     function otherParticipant() {
1172:       let p = request.resource.data.get('participants', []);
1173:       return p[0] == request.auth.uid ? p[1] : p[0];
1174:     }
1175: 
1176:     function notBlocked() {
1177:       return !exists(/databases/$(database)/documents/user_blocks/$(request.auth.uid + '_' + otherParticipant()))
1178:         && !exists(/databases/$(database)/documents/user_blocks/$(otherParticipant() + '_' + request.auth.uid));
1179:     }
1180: 
1181:     function isValidConversationCreate() {
1182:       let d = request.resource.data;
1183:       let p = d.get('participants', []);
1184:       return d.get('conversationId', '') is string
1185:         && d.get('conversationId', '').size() > 0
1186:         && p is list && p.size() == 2
1187:         && p.hasOnly([request.auth.uid, otherParticipant()])
1188:         && otherParticipant() != request.auth.uid
1189:         && d.get('publicationId', '') is string
1190:         && d.get('lastMessage', '') is string
1191:         && d.get('lastMessage', '').size() <= 160
1192:         && d.get('lastMessageAt', null) != null
1193:         && d.get('createdAt', null) != null
1194:         && notBlocked();
1195:     }
1196: 
1197:     match /conversations/{conversationId} {
1198:       // El exists() cubre el get() de un chat aún no creado (recurso nulo:
1199:       // resource.data reventaría). La app primero consulta y solo crea si falta.
1200:       allow get: if request.auth != null
1201:         && (!exists(/databases/$(database)/documents/conversations/$(conversationId))
1202:           || isParticipant(resource.data));
1203:       // La app consulta whereArrayContains(participants, yo).
1204:       allow list: if request.auth != null;
1205:       allow create: if request.auth != null && isValidConversationCreate();
1206:       // Solo el avance del último mensaje (el texto vive en messages/).
1207:       allow update: if request.auth != null && isParticipant(resource.data)
1208:         && request.resource.data.diff(resource.data).affectedKeys()
1209:           .hasOnly(['lastMessage', 'lastMessageAt'])
1210:         && request.resource.data.get('lastMessage', '') is string
1211:         && request.resource.data.get('lastMessage', '').size() <= 160;
1212:       allow delete: if false;
1213: 
1214:       // Mensajes: solo participantes (se verifica contra el documento padre).
1215:       match /messages/{messageId} {
1216:         allow get, list: if request.auth != null
1217:           && isParticipant(get(/databases/$(database)/documents/conversations/$(conversationId)).data);
1218:         allow create: if request.auth != null
1219:           && request.resource.data.get('senderUid', '') == request.auth.uid
1220:           && isParticipant(get(/databases/$(database)/documents/conversations/$(conversationId)).data)
1221:           && request.resource.data.get('type', '') == 'TEXT'
1222:           && request.resource.data.get('text', '') is string
1223:           && request.resource.data.get('text', '').size() >= 1
1224:           && request.resource.data.get('text', '').size() <= 1000
1225:           && request.resource.data.get('read', true) == false
1226:           && request.resource.data.get('createdAt', null) != null;
1227:         // Solo marcar como leído.
1228:         allow update: if request.auth != null
1229:           && resource.data.get('senderUid', '') != request.auth.uid
1230:           && isParticipant(get(/databases/$(database)/documents/conversations/$(conversationId)).data)
1231:           && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['read'])
1232:           && request.resource.data.get('read', false) == true;
1233:         allow delete: if false;
1234:       }
1235:     }
1236: 
1237:     // =============================================================
1238:     //  FASE 15 — Categorías categories/{categoryId}
1239:     // =============================================================
1240:     //
1241:     // Lectura abierta; la escritura es administrativa (seed con credenciales
1242:     // de proyecto), nunca desde la app.
1243: 
1244:     match /categories/{categoryId} {
1245:       allow get, list: if request.auth != null;
1246:       allow create, update, delete: if false;
1247:     }
1248: 
1249:     // FASE 7/14 — Notificaciones (la bandeja completa con push es FASE 14).
1250:     match /notifications/{notificationId} {
1251:       allow get: if request.auth != null
1252:         && resource.data.get('recipientUid', '') == request.auth.uid;
1253:       // La app consulta por recipientUid.
1254:       allow list: if request.auth != null;
1255:       allow create: if request.auth != null
1256:         && request.resource.data.get('senderUid', '') == request.auth.uid
1257:         && request.resource.data.get('recipientUid', '') is string
1258:         && request.resource.data.get('recipientUid', '').size() > 0
1259:         && request.resource.data.get('type', '') is string
1260:         && request.resource.data.get('title', '') is string
1261:         && request.resource.data.get('read', true) == false
1262:         && request.resource.data.get('createdAt', null) != null;
1263:       // El destinatario solo puede marcar como leída.
1264:       allow update: if request.auth != null
1265:         && resource.data.get('recipientUid', '') == request.auth.uid
1266:         && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['read'])
1267:         && request.resource.data.get('read', null) is bool;
1268:       allow delete: if request.auth != null
1269:         && resource.data.get('recipientUid', '') == request.auth.uid;
1270:     }
1271:   }
1272: }

(End of file - total 1272 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\functions\\index.js"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\functions\index.js</path>
<type>file</type>
<content>
1: /**
2:  * Backend Cloud Functions para ChambAYA
3:  * FASE 3: Verificación de Correo mediante OTP de 6 dígitos
4:  * 
5:  * Tecnologías:
6:  * - Firebase Functions (v1 / v2 compatible)
7:  * - Firebase Admin SDK (Firestore & Auth)
8:  * - Node.js crypto (Generación y hash seguro SHA-256)
9:  * - Resend API / Nodemailer (Envío de correo transaccional)
10:  */
11: 
12: const functions = require("firebase-functions");
13: const admin = require("firebase-admin");
14: const crypto = require("crypto");
15: 
16: // Inicializar Admin SDK una sola vez
17: if (!admin.apps.length) {
18:     admin.initializeApp();
19: }
20: 
21: const db = admin.firestore();
22: 
23: // Constantes de seguridad
24: const OTP_LENGTH = 6;
25: const OTP_EXPIRATION_MS = 5 * 60 * 1000; // 5 minutos
26: const RESEND_COOLDOWN_MS = 60 * 1000;     // 60 segundos
27: const MAX_ATTEMPTS = 5;                   // Máximo 5 intentos por código
28: 
29: // =============================================================
30: //  Constantes FASE 1 - Esquema de `users/{uid}`
31: // =============================================================
32: const VALID_ROLES = ["TRABAJADOR", "CONTRATANTE"];
33: const VALID_DOCUMENT_TYPES = ["DNI", "RUC"];
34: const VALID_IDENTITY_SOURCES = ["RENIEC", "SUNAT"];
35: const DEFAULT_COUNTRY = "Peru";
36: 
37: /**
38:  * Versión segura del número de documento para vistas públicas.
39:  */
40: function maskDocumentNumber(documentType, documentNumber) {
41:     if (documentType === "RUC") {
42:         return `***${documentNumber.slice(-4)}`;
43:     }
44:     return `****${documentNumber.slice(-4)}`;
45: }
46: 
47: /**
48:  * Valida y normaliza el bloque `identity` que envía el cliente.
49:  * Devuelve `null` si el documento no tiene formato válido (DNI/RUC).
50:  */
51: function normalizeIdentity(rawIdentity) {
52:     if (!rawIdentity || typeof rawIdentity !== "object") {
53:         return null;
54:     }
55: 
56:     const documentType = String(rawIdentity.documentType || "").toUpperCase();
57:     const documentNumber = String(rawIdentity.documentNumber || "").trim();
58:     const identityName = String(rawIdentity.identityName || "").trim();
59: 
60:     if (VALID_DOCUMENT_TYPES.indexOf(documentType) === -1) {
61:         return null;
62:     }
63: 
64:     const expectedLength = documentType === "DNI" ? 8 : 11;
65:     if (!/^[0-9]+$/.test(documentNumber) || documentNumber.length !== expectedLength) {
66:         return null;
67:     }
68: 
69:     if (identityName.length <= 2) {
70:         return null;
71:     }
72: 
73:     const verifiedWith = VALID_IDENTITY_SOURCES.indexOf(rawIdentity.verifiedWith) !== -1
74:         ? rawIdentity.verifiedWith
75:         : (documentType === "RUC" ? "SUNAT" : "RENIEC");
76: 
77:     return {
78:         documentType: documentType,
79:         documentNumber: documentNumber,
80:         documentNumberMasked: String(rawIdentity.documentNumberMasked || "").trim()
81:             || maskDocumentNumber(documentType, documentNumber),
82:         identityVerified: true,
83:         verifiedWith: verifiedWith,
84:         identityName: identityName,
85:         identityStatus: String(rawIdentity.identityStatus || "").trim(),
86:         location: String(rawIdentity.location || "").trim(),
87:         firstName: String(rawIdentity.firstName || "").trim(),
88:         lastName: String(rawIdentity.lastName || "").trim()
89:     };
90: }
91: 
92: /**
93:  * Arma el documento de `users/{uid}` de la FASE 1: nombre oficial del padrón,
94:  * DNI/RUC, roles y verificaciones. Nunca incluye la contraseña.
95:  * Incluye el espejo de campos raíz que la app ya leía (`email`, `role`, etc.).
96:  */
97: function buildUserDocument(options) {
98:     const uid = options.uid;
99:     const identity = options.identity || null;
100:     const profile = options.profile || {};
101:     const now = admin.firestore.FieldValue.serverTimestamp();
102:     const safeRole = VALID_ROLES.indexOf(options.role) !== -1 ? options.role : "TRABAJADOR";
103:     const email = options.email || "";
104:     const otpVerified = options.otpVerified === true;
105:     const photoUrl = String(profile.profilePhotoUrl || "").trim();
106:     const fullName = identity && identity.identityName
107:         ? identity.identityName
108:         : String(profile.fullName || "").trim();
109: 
110:     const userDoc = {
111:         uid: uid,
112:         accountStatus: "ACTIVE",
113:         registrationStatus: "VERIFIED",
114:         roles: [safeRole],
115:         activeRole: safeRole,
116:         auth: {
117:             provider: options.provider === "GOOGLE" ? "GOOGLE" : "EMAIL",
118:             email: email,
119:             emailVerified: true,
120:             otpVerified: otpVerified,
121:             verificationMethod: otpVerified ? "CHAMBAYA_OTP" : "FIREBASE_EMAIL_LINK"
122:         },
123:         profile: {
124:             firstName: identity ? identity.firstName : "",
125:             lastName: identity ? identity.lastName : "",
126:             fullName: fullName,
127:             profilePhotoUrl: photoUrl,
128:             profilePhotoPublicId: "",
129:             profilePhotoSource: photoUrl ? "GOOGLE" : "DEFAULT",
130:             country: DEFAULT_COUNTRY
131:         },
132:         createdAt: options.existingCreatedAt || admin.firestore.FieldValue.serverTimestamp(),
133:         updatedAt: now,
134:         lastLoginAt: now,
135:         // Espejo de compatibilidad con las versiones previas de la app
136:         email: email,
137:         role: safeRole,
138:         emailVerified: true,
139:         otpVerified: otpVerified,
140:         authMethod: options.authMethod === "GOOGLE" ? "GOOGLE" : "EMAIL_PASSWORD",
141:         verifiedAt: now
142:     };
143: 
144:     if (identity) {
145:         userDoc.identity = Object.assign({}, identity, { verifiedAt: now });
146:     }
147: 
148:     return userDoc;
149: }
150: 
151: /**
152:  * Obtener la sal secreta del entorno
153:  */
154: function getOtpSalt() {
155:     return process.env.OTP_SALT || "chambaya_secure_otp_default_salt_2026";
156: }
157: 
158: /**
159:  * Generar un OTP criptográficamente seguro de 6 dígitos (puede iniciar con 0)
160:  */
161: function generateSecureOtp() {
162:     const min = 0;
163:     const max = 1000000;
164:     const num = crypto.randomInt(min, max);
165:     return num.toString().padStart(OTP_LENGTH, "0");
166: }
167: 
168: /**
169:  * Generar el hash SHA-256 del OTP combinado con sal y UID
170:  */
171: function hashOtp(otp, uid) {
172:     const salt = getOtpSalt();
173:     return crypto.createHash("sha256").update(`${otp}:${salt}:${uid}`).digest("hex");
174: }
175: 
176: /**
177:  * Enviar correo con el código OTP utilizando Resend o Nodemailer
178:  */
179: async function sendOtpEmail(email, otp) {
180:     const resendApiKey = process.env.RESEND_API_KEY;
181:     const fromEmail = process.env.RESEND_FROM_EMAIL || "ChambAYA <onboarding@resend.dev>";
182: 
183:     const htmlContent = `
184:     <!DOCTYPE html>
185:     <html lang="es">
186:     <head>
187:       <meta charset="utf-8">
188:       <title>Código de verificación ChambAYA</title>
189:       <style>
190:         body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #F8FAFC; margin: 0; padding: 24px; color: #1E293B; }
191:         .container { max-width: 500px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; padding: 32px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05); border: 1px solid #E2E8F0; }
192:         .header { text-align: center; margin-bottom: 24px; }
193:         .logo { font-size: 28px; font-weight: 800; color: #0284C7; letter-spacing: -0.5px; }
194:         .title { font-size: 20px; font-weight: 700; color: #0F172A; margin-top: 16px; margin-bottom: 8px; }
195:         .subtitle { font-size: 14px; color: #64748B; margin: 0; }
196:         .code-box { background: #F0F9FF; border: 2px dashed #0284C7; border-radius: 12px; padding: 20px; text-align: center; margin: 28px 0; }
197:         .otp-code { font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #0369A1; font-family: monospace; }
198:         .footer { font-size: 12px; color: #94A3B8; text-align: center; margin-top: 32px; border-top: 1px solid #F1F5F9; padding-top: 16px; line-height: 1.5; }
199:         .badge { display: inline-block; background: #E0F2FE; color: #0369A1; font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 20px; margin-top: 8px; }
200:       </style>
201:     </head>
202:     <body>
203:       <div class="container">
204:         <div class="header">
205:           <div class="logo">Chamb<span style="color:#0EA5E9;">AYA</span></div>
206:           <div class="title">Verifica tu correo electrónico</div>
207:           <p class="subtitle">Usa el siguiente código de 6 dígitos para continuar tu registro en ChambAYA.</p>
208:         </div>
209:         
210:         <div class="code-box">
211:           <div class="otp-code">${otp}</div>
212:           <div class="badge">Válido durante 5 minutos</div>
213:         </div>
214:         
215:         <p style="font-size: 13px; color: #475569; line-height: 1.5;">
216:           Por seguridad, no compartas este código con nadie. El equipo de ChambAYA nunca te pedirá tu código por teléfono ni mensaje.
217:         </p>
218:         
219:         <div class="footer">
220:           Si no solicitaste este código, puedes ignorar este mensaje de forma segura.<br>
221:           © ${new Date().getFullYear()} ChambAYA. Todos los derechos reservados.
222:         </div>
223:       </div>
224:     </body>
225:     </html>
226:     `;
227: 
228:     const textContent = `Hola,\n\nTu código de verificación de ChambAYA es: ${otp}\n\nEste código es válido durante 5 minutos.\nSi no solicitaste este código, puedes ignorar este mensaje.\n\nEquipo ChambAYA`;
229: 
230:     if (resendApiKey) {
231:         try {
232:             const { Resend } = require("resend");
233:             const resend = new Resend(resendApiKey);
234:             await resend.emails.send({
235:                 from: fromEmail,
236:                 to: email,
237:                 subject: "Tu código de verificación de ChambAYA",
238:                 html: htmlContent,
239:                 text: textContent
240:             });
241:             console.log(`[Resend] OTP enviado exitosamente a ${email}`);
242:             return true;
243:         } catch (error) {
244:             console.error("[Resend Error]", error);
245:             throw new Error(`Error enviando correo: ${error.message}`);
246:         }
247:     } else {
248:         // En entorno de desarrollo o previo a configuración de la API Key,
249:         // registramos el evento en el logger del backend para no romper el flujo.
250:         console.warn(`[DEV/TEST] RESEND_API_KEY no configurada. Código generado para ${email}: [${otp}]`);
251:         return true;
252:     }
253: }
254: 
255: /**
256:  * Cloud Function Callable: requestEmailOtp
257:  * Genera y envía un código OTP de 6 dígitos al correo del usuario autenticado.
258:  */
259: exports.requestEmailOtp = functions.https.onCall(async (data, context) => {
260:     // 1. Validar autenticación
261:     if (!context.auth || !context.auth.uid) {
262:         throw new functions.https.HttpsError(
263:             "unauthenticated",
264:             "Debes estar autenticado para solicitar un código de verificación."
265:         );
266:     }
267: 
268:     const uid = context.auth.uid;
269:     const userRecord = await admin.auth().getUser(uid);
270:     const email = (data && data.email) ? data.email.trim().toLowerCase() : (userRecord.email || "").toLowerCase();
271: 
272:     if (!email) {
273:         throw new functions.https.HttpsError(
274:             "invalid-argument",
275:             "No se encontró una dirección de correo asociada a la cuenta."
276:         );
277:     }
278: 
279:     const now = Date.now();
280:     const verificationRef = db.collection("email_verifications").doc(uid);
281:     const existingDoc = await verificationRef.get();
282: 
283:     // 2. Control de Reenvíos (Cooldown de 60 segundos)
284:     if (existingDoc.exists) {
285:         const existingData = existingDoc.data();
286:         if (existingData.resendAvailableAt) {
287:             const resendAvailableAtMs = existingData.resendAvailableAt.toMillis();
288:             if (now < resendAvailableAtMs) {
289:                 const waitSeconds = Math.ceil((resendAvailableAtMs - now) / 1000);
290:                 throw new functions.https.HttpsError(
291:                     "resource-exhausted",
292:                     `Debes esperar ${waitSeconds} segundos antes de solicitar otro código.`
293:                 );
294:             }
295:         }
296:     }
297: 
298:     // 3. Generación criptográfica segura de 6 dígitos
299:     const otp = generateSecureOtp();
300:     const hashedOtp = hashOtp(otp, uid);
301: 
302:     const expiresAt = admin.firestore.Timestamp.fromMillis(now + OTP_EXPIRATION_MS);
303:     const resendAvailableAt = admin.firestore.Timestamp.fromMillis(now + RESEND_COOLDOWN_MS);
304: 
305:     // 4. Guardar información temporal en Firestore (el OTP nunca se guarda en texto plano)
306:     await verificationRef.set({
307:         uid: uid,
308:         email: email,
309:         otpHash: hashedOtp,
310:         expiresAt: expiresAt,
311:         resendAvailableAt: resendAvailableAt,
312:         attempts: 0,
313:         maxAttempts: MAX_ATTEMPTS,
314:         verified: false,
315:         updatedAt: admin.firestore.FieldValue.serverTimestamp(),
316:         createdAt: existingDoc.exists ? (existingDoc.data().createdAt || admin.firestore.FieldValue.serverTimestamp()) : admin.firestore.FieldValue.serverTimestamp()
317:     }, { merge: true });
318: 
319:     // 5. Enviar el correo electrónico
320:     try {
321:         await sendOtpEmail(email, otp);
322:     } catch (mailErr) {
323:         console.error("Error al enviar el correo:", mailErr);
324:         throw new functions.https.HttpsError(
325:             "internal",
326:             "No pudimos enviar el correo de verificación. Intenta nuevamente."
327:         );
328:     }
329: 
330:     return {
331:         success: true,
332:         message: "Código de verificación enviado correctamente.",
333:         expiresAtMillis: now + OTP_EXPIRATION_MS,
334:         resendCooldownSeconds: 60
335:     };
336: });
337: 
338: /**
339:  * Cloud Function Callable: verifyEmailOtp
340:  * Valida el código de 6 dígitos introducido por el usuario.
341:  */
342: exports.verifyEmailOtp = functions.https.onCall(async (data, context) => {
343:     // 1. Validar autenticación
344:     if (!context.auth || !context.auth.uid) {
345:         throw new functions.https.HttpsError(
346:             "unauthenticated",
347:             "Debes estar autenticado para verificar tu correo."
348:         );
349:     }
350: 
351:     const uid = context.auth.uid;
352:     const inputOtp = (data && data.otp) ? String(data.otp).trim() : "";
353: 
354:     if (!inputOtp || inputOtp.length !== OTP_LENGTH || !/^\d{6}$/.test(inputOtp)) {
355:         throw new functions.https.HttpsError(
356:             "invalid-argument",
357:             "El código debe contener exactamente 6 dígitos numéricos."
358:         );
359:     }
360: 
361:     const verificationRef = db.collection("email_verifications").doc(uid);
362:     const verificationDoc = await verificationRef.get();
363: 
364:     if (!verificationDoc.exists) {
365:         throw new functions.https.HttpsError(
366:             "not-found",
367:             "No se encontró una solicitud de verificación activa. Solicita un nuevo código."
368:         );
369:     }
370: 
371:     const verificationData = verificationDoc.data();
372: 
373:     // Ya verificado
374:     if (verificationData.verified === true) {
375:         return {
376:             success: true,
377:             verified: true,
378:             message: "Tu correo electrónico ya ha sido verificado."
379:         };
380:     }
381: 
382:     const now = Date.now();
383: 
384:     // 2. Validar límite de intentos
385:     const currentAttempts = verificationData.attempts || 0;
386:     if (currentAttempts >= MAX_ATTEMPTS) {
387:         // Invalidar OTP actual
388:         await verificationRef.update({
389:             otpHash: null,
390:             attempts: currentAttempts + 1,
391:             invalidatedAt: admin.firestore.FieldValue.serverTimestamp()
392:         });
393:         throw new functions.https.HttpsError(
394:             "failed-precondition",
395:             "Has superado el número de intentos. Solicita un nuevo código para continuar."
396:         );
397:     }
398: 
399:     // 3. Validar expiración (5 minutos con reloj de servidor)
400:     if (!verificationData.expiresAt || now > verificationData.expiresAt.toMillis()) {
401:         throw new functions.https.HttpsError(
402:             "deadline-exceeded",
403:             "El código ha expirado. Solicita un nuevo código para continuar."
404:         );
405:     }
406: 
407:     // 4. Validar coincidencia de código mediante comparación segura
408:     const expectedHash = verificationData.otpHash;
409:     const computedHash = hashOtp(inputOtp, uid);
410: 
411:     if (!expectedHash || expectedHash !== computedHash) {
412:         const nextAttempts = currentAttempts + 1;
413:         const remaining = Math.max(0, MAX_ATTEMPTS - nextAttempts);
414: 
415:         await verificationRef.update({
416:             attempts: nextAttempts,
417:             lastFailedAttempt: admin.firestore.FieldValue.serverTimestamp()
418:         });
419: 
420:         if (remaining === 0) {
421:             throw new functions.https.HttpsError(
422:                 "failed-precondition",
423:                 "Has superado el número de intentos. Solicita un nuevo código para continuar."
424:             );
425:         }
426: 
427:         throw new functions.https.HttpsError(
428:             "invalid-argument",
429:             `Código incorrecto. Verifica el código e inténtalo nuevamente. Te quedan ${remaining} intento(s).`
430:         );
431:     }
432: 
433:     // FASE 1: preparar `users/{uid}` con el nombre oficial y el DNI/RUC
434:     const userDocRef = db.collection("users").doc(uid);
435:     const userRole = (data && data.role) ? data.role : "TRABAJADOR";
436:     const userEmail = (data && data.email) ? String(data.email).trim().toLowerCase() : (verificationData.email || "");
437:     const identity = normalizeIdentity(data ? data.identity : null);
438: 
439:     if ((data && data.identity) && !identity) {
440:         // El cliente envió una identidad con formato inválido: no se marca el OTP
441:         // como usado para que el cliente pueda reintentar la verificación.
442:         throw new functions.https.HttpsError(
443:             "invalid-argument",
444:             "Los datos de identidad enviados no son válidos. Vuelve a verificar tu DNI o RUC."
445:         );
446:     }
447: 
448:     // Preservar `createdAt` si el documento ya existía
449:     let existingCreatedAt = null;
450:     try {
451:         const existingUserDoc = await userDocRef.get();
452:         if (existingUserDoc.exists && existingUserDoc.data().createdAt) {
453:             existingCreatedAt = existingUserDoc.data().createdAt;
454:         }
455:     } catch (readErr) {
456:         console.warn("[verifyEmailOtp] No se pudo leer el documento previo:", readErr.message);
457:     }
458: 
459:     // 5. Código correcto: Actualización atómica en backend
460:     const batch = db.batch();
461: 
462:     // Invalidar OTP para que sea de un solo uso y marcar como verificado
463:     batch.update(verificationRef, {
464:         verified: true,
465:         otpHash: null, // Destruir el hash para que sea estrictamente de un solo uso
466:         verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
467:         attempts: currentAttempts
468:     });
469: 
470: 
471:     batch.set(userDocRef, buildUserDocument({
472:         uid: uid,
473:         role: userRole,
474:         email: userEmail,
475:         provider: data && data.provider,
476:         authMethod: data && data.authMethod,
477:         identity: identity,
478:         profile: data ? data.profile : null,
479:         existingCreatedAt: existingCreatedAt,
480:         otpVerified: true
481:     }), { merge: true });
482: 
483:     await batch.commit();
484: 
485:     console.log(`[OTP Success] Usuario ${uid} (${userEmail}) verificado correctamente con rol ${userRole}`);
486: 
487:     return {
488:         success: true,
489:         verified: true,
490:         registrationStatus: "VERIFIED",
491:         identitySaved: identity !== null,
492:         message: "Código verificado exitosamente."
493:     };
494: });

(End of file - total 494 lines)
</content>
```

---

## Assistant (Build · Muse Spark 1.3 Free · 4.8s)

_Thinking:_



**Tool: glob**

**Input:**
```json
{
  "path": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA",
  "pattern": "app/src/main/java/**/*.kt"
}
```

**Output:**
```
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\RegistroActivity.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\MainActivity.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\LoginActivity.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\IdiomaManager.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\EditarPerfilActivity.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\workplace\MapaUbicacionSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\workplace\LugarIcons.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\workplace\EditarLugarActivity.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\UbicacionCercana.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\RateSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\PublicProfileSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\PublicationOptionsSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\PublicationFeedItem.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\PublicationAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\NotificationsSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\LocationSearchSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\JobDetailSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\JobCardAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\JobCard.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\ImagenCategoriaChamba.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\FragmentoChambas.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\FilterBottomSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\DetailPhotoAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\CommentsSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\CommentAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\CategoriasChamba.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\CategoriaAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\Categoria.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\ApplySheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\chat\FragmentoMensajes.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\chat\ChatConversacion.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\chat\AdaptadorMensajes.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\chat\AdaptadorConversaciones.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\chat\ActividadChatDetalle.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\SplashActivity.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\foto\VistaRecorte.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\foto\RecortarFotoActivity.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\trabajos\FragmentoMisTrabajos.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\trabajos\TimelineAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\CommentRepository.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\ChatRepository.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\CategoryRepository.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\BlockRepository.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\ApplicationRepository.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\WorkplaceRepository.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\RegistrationRepository.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\RatingRepository.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\PublicationRepository.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\PublicationInteractionRepository.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\ProfileRepository.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\PadronRepository.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\NotificationRepository.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\JobRepository.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\FirestoreErrors.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\BienvenidaActivity.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\BarraEstadoUtils.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\SavedSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\RatingsAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\ProfileCache.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\OficioIcons.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\JobsSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\JobHistoryAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\InicialesDrawable.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\FragmentoMiPerfil.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\FragmentoAjustesPerfil.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\EspecialidadSelectorAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\publish\CrearPublicacionActivity.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\publish\ContenedorDeslizable.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\publish\ApplicantsAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\publish\MisPublicacionesAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\publish\FragmentoPublicar.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\publish\MyApplicationsAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\remote\IdentityValidationService.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\remote\GeoSearchService.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\remote\CloudinaryUploader.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\map\MapaEstilo.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\map\GeoPlaceAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\map\FragmentoMapas.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\map\MapData.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\map\MapPlaceAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\IdentityNameParser.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\CommentModels.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\ChatModels.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\ApplicationModels.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\NotificationModels.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\JobModels.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\OficioCatalog.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\PeruLocations.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\local\PendingRegistrationStore.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\PublicationModels.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\ProfileModels.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\PublicProfileModels.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\RatingModels.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\RegistrationModels.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\WorkplaceModels.kt
```

**Tool: glob**

**Input:**
```json
{
  "path": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA",
  "pattern": "app/src/main/res/layout/*.xml"
}
```

**Output:**
```
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\popup_seleccion_idioma.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_timeline_job.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_rating.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_publicar_vacio.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_photo_grid.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_oficio_selector.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_notification.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_my_publication.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_my_application.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_map_place.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_map_category.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_job_history.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_job_card.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_geo_place.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_especialidad_perfil.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_detail_photo.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_comment.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_chat_conversacion.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_chat_bubble_sent.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_chat_bubble_received.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_categoria.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\item_applicant.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\fragmento_publicar.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\fragmento_mi_perfil.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\fragmento_mis_trabajos.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\fragmento_mensajes.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\fragmento_mapas.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\fragmento_chambas.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\fragmento_ajustes_perfil.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\dialog_registration_success.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\dialog_profile_success.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\dialog_notifications.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\dialog_logout_confirmacion.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\dialog_job_options.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\dialog_editar_perfil.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\dialog_account_info.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_saved.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_rate.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_public_profile.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_notifications.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_mapa_ubicacion.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_lugar_guardado.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_location_search.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_job_detail.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_jobs.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_identity_verified.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_filters.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_comments.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_apply.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_adjuntos.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\activity_recortar_foto.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\activity_editar_lugar.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\activity_crear_publicacion.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\actividad_splash.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\actividad_registro.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\actividad_principal.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\actividad_login.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\actividad_chat_detalle.xml
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\actividad_bienvenida.xml
```

---

## Assistant (Build · Muse Spark 1.3 Free · 7.7s)

_Thinking:_



Veo que el proyecto ya está mucho más avanzado que Fase 1. Reviso el historial y pantallas principales para completar el análisis.

**Tool: bash**

**Input:**
```json
{
  "command": "git log --oneline -20; echo ---; git status --short | head -n 50; echo ---; git branch -a",
  "workdir": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA"
}
```

**Output:**
```
0a29652 categorias mas
79168b0 comentarios diseño
9ec402a notificaicones diseños
89a057c notificacion restablecer
f67d3f1 notifcaicones eliminar
6c48585 eliminar chat mensaje
b6d53dd letra visible
fd3ef5f configuraicon funcion
15e8f4b boton enviar visual
a00e489 Delete kotlin-compiler-6060907329138716805.salive
22dd995 editar perfil rediseño
f574fe9 carrucel imagen
29d3d5c mejoras v2
936520a mejoras diseño
039db74 chambas mapas
27567f9 chats
3004da4 carrusel en trabajo detalle
1a37734 logotipo
11f7964 mejoras diseño
9775150 mejoras
---
head : El t�rmino 'head' no se reconoce como nombre de un cmdlet, funci�n, archivo de script o programa ejecutable. 
---
Compruebe si escribi� correctamente el nombre o, si incluy� una ruta de acceso, compruebe que dicha ruta es correcta e 
int�ntelo de nuevo.
En l�nea: 1 Car�cter: 55
+ git log --oneline -20; echo ---; git status --short | head -n 50; ech ...
+                                                       ~~~~
    + CategoryInfo          : ObjectNotFound: (head:String) [], CommandNotFoundException
    + FullyQualifiedErrorId : CommandNotFoundException
 
* main
  remotes/origin/HEAD -> origin/main
  remotes/origin/main

```

---

## Assistant (Build · Muse Spark 1.3 Free · 5.9s)

**Tool: bash**

**Input:**
```json
{
  "command": "git log --oneline -40",
  "workdir": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA"
}
```

**Output:**
```
0a29652 categorias mas
79168b0 comentarios diseño
9ec402a notificaicones diseños
89a057c notificacion restablecer
f67d3f1 notifcaicones eliminar
6c48585 eliminar chat mensaje
b6d53dd letra visible
fd3ef5f configuraicon funcion
15e8f4b boton enviar visual
a00e489 Delete kotlin-compiler-6060907329138716805.salive
22dd995 editar perfil rediseño
f574fe9 carrucel imagen
29d3d5c mejoras v2
936520a mejoras diseño
039db74 chambas mapas
27567f9 chats
3004da4 carrusel en trabajo detalle
1a37734 logotipo
11f7964 mejoras diseño
9775150 mejoras
b382c76 mapas
571c61e mejora splash
496617c logo splash screen
f8a0328 quiatr cambio de rol mejora proyecto
f5f3828 CHAT
4e6fc59 CHATS
a4add49 noticacaciones y posulaciones
8b234b5 publicaciones
6c2af55 publicarcaiones
fa6e096 indicator
1adf875 pantalla de bienvenida
f5297bb pantala de bievnedida
6c232b6 pantalla de bievenida
163b6bd CAMBIO DE ROL
5a88f1e MEJORAR LOGICA
b01efb8 cambios en editar perol
fb734ad editar frontend de editar lugar
451efa9 publicar
fead1f4 fragmento publicar
60cebc2 crear publicacion

```

**Tool: glob**

**Input:**
```json
{
  "path": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA",
  "pattern": "app/src/main/java/com/proyecto/chambaya/ui/**/*.kt"
}
```

**Output:**
```
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\map\MapPlaceAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\map\MapData.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\map\MapaEstilo.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\map\GeoPlaceAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\map\FragmentoMapas.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\chat\FragmentoMensajes.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\chat\ChatConversacion.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\chat\AdaptadorMensajes.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\chat\AdaptadorConversaciones.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\chat\ActividadChatDetalle.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\publish\MyApplicationsAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\publish\MisPublicacionesAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\publish\FragmentoPublicar.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\publish\CrearPublicacionActivity.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\publish\ContenedorDeslizable.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\publish\ApplicantsAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\SavedSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\RatingsAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\ProfileCache.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\OficioIcons.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\JobsSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\JobHistoryAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\InicialesDrawable.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\FragmentoMiPerfil.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\FragmentoAjustesPerfil.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\EspecialidadSelectorAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\UbicacionCercana.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\RateSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\trabajos\TimelineAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\PublicProfileSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\PublicationOptionsSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\trabajos\FragmentoMisTrabajos.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\foto\RecortarFotoActivity.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\foto\VistaRecorte.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\PublicationFeedItem.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\PublicationAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\workplace\MapaUbicacionSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\NotificationsSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\workplace\LugarIcons.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\workplace\EditarLugarActivity.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\LocationSearchSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\JobDetailSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\JobCardAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\JobCard.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\ImagenCategoriaChamba.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\FragmentoChambas.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\FilterBottomSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\DetailPhotoAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\CommentsSheet.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\CommentAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\CategoriasChamba.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\CategoriaAdapter.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\Categoria.kt
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\ApplySheet.kt
```

---

## Assistant (Build · Muse Spark 1.3 Free · 12.3s)

_Thinking:_



# Análisis completo — APP-ChambaAYA

## 1. Qué es
**ChambAYA**: app Android nativa (Kotlin) para conectar **Trabajadores** con **Contratantes** en Ayacucho/Perú. Ofertas de trabajo temporal, postulaciones, chat, calificaciones, mapa.

Stack real verificado en código:
- `app/build.gradle.kts`: `compileSdk 36`, `minSdk 24`, Firebase BOM `33.9.0` (Auth + Firestore + Functions), Play Auth `21.3.0`, MapLibre `11.8.3`, Glide + Coil-SVG, Gson, ViewPager2, Compose + Views mixto.
- Backend: `functions/index.js` (OTP email 6 dígitos, SHA-256 + sal), Firestore como BD única.
- Imágenes: **Cloudinary** (no Storage — correcto porque Spark no da Storage). Preset `chambaya_preset`, carpetas `chambaya/fotos-perfil|fotos-lugares|fotos-publicaciones`.
- Mapa: MapLibre + `play-services-location`.

## 2. Arquitectura
```
Firebase Auth (uid)
   → Firestore users/{uid}
        ├── auth / identity / profile / worker / employer / privacy / statistics
   → colecciones independientes (bien): workplaces, publications,
      applications, jobs, ratings, comments, publication_likes/saves,
      hidden_publications, publication_reports, user_reports, user_blocks,
      conversations/messages, notifications, categories, usernames, public_profiles
   → Cloudinary (solo url + publicId en Firestore)
```

Patrón: Activities + Fragments + `data/` limpio:
- `data/model/`: `RegistrationModels, ProfileModels, PublicationModels, ApplicationModels, JobModels, RatingModels, ChatModels, CommentModels, WorkplaceModels...`
- `data/repository/`: 14 repos (`Registration, Profile, Publication, PublicationInteraction, Application, Job, Rating, Comment, Workplace, Chat, Notification, Block, Category, Padron...`)
- `data/remote/`: `IdentityValidationService (RENIEC/SUNAT), CloudinaryUploader, GeoSearchService`
- `data/local/`: `PendingRegistrationStore`
- UI: `ui/jobs (feed, filtros, detalle, comentarios), ui/publish, ui/chat, ui/map, ui/profile, ui/workplace, ui/trabajos`

~110 archivos Kotlin + ~60 layouts. App real, no prototipo.

## 3. Estado real vs documentos
Tus `.md` dicen "FASE 1 terminada", pero el código y `firestore.rules` (1272 líneas) ya cubren **Fases 1–15**:

| Fase plan | Estado en código |
|---|---|
| 1 Registro DNI/RUC + OTP + `users/{uid}` | ✅ Sólida + reanudación (`FASE-1-REANUDACION-REGISTRO.md`) |
| 2 Mi Perfil + `usernames` + foto | ✅ Implementado (`ProfileRepository`, `EditarPerfilActivity`, `FragmentoMiPerfil`) |
| 3 Rol Contratante + `activeRole` | ✅ (`employer`, cambio de rol) |
| 4 Workplaces | ✅ (`WorkplaceRepository`, `EditarLugarActivity`, mapa) |
| 5-6 Publicaciones + Feed/filtros | ✅ (`PublicationRepository`, `FragmentoChambas`, `CrearPublicacionActivity`, carrusel 3 fotos) |
| 7-8 Postulaciones + Jobs | ✅ (`ApplicationRepository`, `JobRepository`, `ApplicantsAdapter`) |
| 9 Ratings | ✅ (`RatingRepository`, `RateSheet`, rules `isAllowedRatingUpdate`) |
| 10-11 Comments / Likes / Saves / Ocultar | ✅ (`CommentsSheet`, `PublicationInteractionRepository`) |
| 12 Reportes / Bloqueos + `public_profiles` | ✅ Rules completas |
| 13 Chat | ✅ (`ChatRepository`, `ActividadChatDetalle`, conversations/messages con anti-bloqueo) |
| 14 Notificaciones | ⚠️ Parcial (`NotificationRepository`, `NotificationsSheet`, bandeja en Firestore; push FCM no verificado) |
| 15 Categorías | ✅ (`CategoryRepository`, `api_oficios.json`) |
| 16 Security Rules | ✅ Muy avanzadas, mejor que el plan |
| 17-20 Perfil público / % completado / historial / E2E | ⚠️ Parcial (`PublicProfileSheet`, `JobsSheet`, `FragmentoMisTrabajos` existen, falta test E2E documentado) |

El `git log` confirma el orden: registro → bienvenida → publicar → chats → mapas → diseño/notificaciones/comentarios.

## 4. Puntos fuertes
1. **Rules ejemplares**: validan DNI 8 / RUC 11, `roles/activeRole` coherentes, reputación inmutable por cliente, contadores solo ±1, fotos ancladas a `chambaya/.../{uid}/`, transiciones `PENDING→ACCEPTED/REJECTED/WITHDRAWN`, `jobs` solo por partes, comentarios solo autor.
2. **Registro resiliente**: `PendingRegistrationStore` + `fetchRegistrationState(MISSING/INCOMPLETE/COMPLETE/UNKNOWN)` resuelve el bug de "cuenta huérfana".
3. **Sin listas gigantes**: likes/saves/reports en colecciones propias, respeta límite 1 MiB.
4. **Snapshot `publisher/worker`** en publicaciones/postulaciones (no se rompe si cambia el perfil).
5. **Privacidad**: `documentNumberMasked`, `showPhone/showEmail/showExactAddress`, `list:false` en users.

## 5. Riesgos y deuda (importante)
1. **Secretos filtrados en repo**: `CHAMBAYA_IMPLEMENTACION_FASES.md:118-119` tiene `API Key + API Secret + Cloud Name` de Cloudinary. Rota el secret y bórralo del git.
2. **Token RENIEC/SUNAT en el APK** (`IdentityValidationService` + `RegistroActivity`) y **sal OTP en cliente** — cualquiera puede falsificar identidad. Mover a Cloud Function (lo reconoce tu propio doc Fase 1 §7).
3. **Sin unicidad global DNI/RUC**: dos cuentas pueden usar mismo DNI. Falta `identity_reservations` transaccional.
4. `functions/.env` existe en repo — verifica que no tenga `RESEND_API_KEY` / `OTP_SALT` reales.
5. `README.md` vacío (1 línea), docs de fases desactualizados vs código.
6. `google-services.json` commiteado (normal en Android, pero confirma repo privado).
7. `compileSdk/target 36` + `minSdk 24`: revisa compatibilidad MapLibre 11.8.3 en API 24.

## 6. Qué haría ahora
1. Rotar Cloudinary secret, limpiar `.md` y `.env`, revisar `.gitignore`.
2. Mover validación RENIEC/SUNAT + OTP a Functions (plan Blaze) y cerrar `email_verifications` a solo-backend.
3. Crear `identity_reservations/{tipo_numero}` para unicidad DNI/RUC.
4. Test E2E Fase 20 con 2 usuarios (Trabajador A + Contratante B) y documentar resultado.
5. Actualizar `README.md` + consolidar las 3 docs de Fase 1 en una sola.


---

## User

ok quiero que soluciones esto:
14 Notificaciones                       │⚠️ Parcial (NotificationRepository, NotificationsSheet, │                                            
     │                                        │bandeja en Firestore; push FCM no verificado)
17-20 Perfil público / % completado /   │⚠️ Parcial (PublicProfileSheet, JobsSheet,              │    LSP                                     
     │historial / E2E                         │FragmentoMisTrabajos existen, falta test E2E            │    LSPs are disabled                       
     │                                        │documentado)  
Sin unicidad global DNI/RUC: dos cuentas pueden usar mismo DNI. Falta identity_reservations                                                 
        transaccional. 
las clavez api y tokens dejalo haci ya terminado el proyecto lo cambiare por ahora no tocar las calvez expuestas 

---

## Assistant (Build · Muse Spark 1.3 Free · 14.2s)

_Thinking:_



Entendido — completo notificaciones, perfil público/historial y unicidad DNI/RUC sin tocar las claves.

**Tool: todowrite**

**Input:**
```json
{
  "todos": [
    {
      "content": "Explorar notificaciones, perfil público, historial y registro actual",
      "status": "in_progress",
      "priority": "high"
    },
    {
      "content": "Completar Fase 14 Notificaciones (bandeja + triggers)",
      "status": "pending",
      "priority": "high"
    },
    {
      "content": "Completar Fases 17-19 Perfil público / % completado / historial",
      "status": "pending",
      "priority": "high"
    },
    {
      "content": "Implementar unicidad DNI/RUC con identity_reservations",
      "status": "pending",
      "priority": "high"
    },
    {
      "content": "Verificar compilación y documentar test E2E",
      "status": "pending",
      "priority": "medium"
    }
  ]
}
```

**Output:**
```
[
  {
    "content": "Explorar notificaciones, perfil público, historial y registro actual",
    "status": "in_progress",
    "priority": "high"
  },
  {
    "content": "Completar Fase 14 Notificaciones (bandeja + triggers)",
    "status": "pending",
    "priority": "high"
  },
  {
    "content": "Completar Fases 17-19 Perfil público / % completado / historial",
    "status": "pending",
    "priority": "high"
  },
  {
    "content": "Implementar unicidad DNI/RUC con identity_reservations",
    "status": "pending",
    "priority": "high"
  },
  {
    "content": "Verificar compilación y documentar test E2E",
    "status": "pending",
    "priority": "medium"
  }
]
```

---

## Assistant (Build · Muse Spark 1.3 Free · 4.0s)

_Thinking:_



**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\NotificationRepository.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\NotificationRepository.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.data.repository
2: 
3: import com.google.android.gms.tasks.Tasks
4: import com.google.firebase.firestore.FieldValue
5: import com.google.firebase.firestore.FirebaseFirestore
6: import com.google.firebase.firestore.Query
7: import com.proyecto.chambaya.data.model.AppNotification
8: import com.proyecto.chambaya.data.model.toAppNotification
9: import kotlinx.coroutines.Dispatchers
10: import kotlinx.coroutines.withContext
11: 
12: /**
13:  * Notificaciones básicas (FASE 7: se crean al postular/aceptar/rechazar;
14:  * la bandeja completa con push es FASE 14).
15:  */
16: class NotificationRepository(
17:     private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
18: ) {
19: 
20:     suspend fun push(
21:         recipientUid: String,
22:         type: String,
23:         title: String,
24:         message: String,
25:         senderUid: String,
26:         publicationId: String = ""
27:     ): Result<Unit> = withContext(Dispatchers.IO) {
28:         runCatching {
29:             if (recipientUid.isBlank() || recipientUid == senderUid) return@runCatching
30:             Tasks.await(
31:                 firestore.collection(COLLECTION).add(
32:                     mapOf(
33:                         "recipientUid" to recipientUid,
34:                         "type" to type,
35:                         "title" to title.trim().take(120),
36:                         "message" to message.trim().take(300),
37:                         "senderUid" to senderUid,
38:                         "publicationId" to publicationId,
39:                         "read" to false,
40:                         "createdAt" to FieldValue.serverTimestamp()
41:                     )
42:                 )
43:             )
44:             Unit
45:         }
46:     }
47: 
48:     suspend fun unreadCount(uid: String, limit: Long = 100): Int =
49:         withContext(Dispatchers.IO) {
50:             runCatching {
51:                 Tasks.await(
52:                     firestore.collection(COLLECTION)
53:                         .whereEqualTo("recipientUid", uid)
54:                         .whereEqualTo("read", false)
55:                         .limit(limit)
56:                         .get()
57:                 ).size()
58:             }.getOrDefault(0)
59:         }
60: 
61:     fun listenMine(
62:         uid: String,
63:         limit: Long = 30,
64:         onUpdate: (List<AppNotification>) -> Unit,
65:         onError: (Exception) -> Unit
66:     ): com.google.firebase.firestore.ListenerRegistration {
67:         return firestore.collection(COLLECTION)
68:             .whereEqualTo("recipientUid", uid)
69:             .orderBy("createdAt", Query.Direction.DESCENDING)
70:             .limit(limit)
71:             .addSnapshotListener { snap, e ->
72:                 if (e != null) { onError(e); return@addSnapshotListener }
73:                 if (snap == null) return@addSnapshotListener
74:                 runCatching { snap.documents.map { it.toAppNotification() } }
75:                     .onSuccess(onUpdate)
76:                     .onFailure { onError(it as? Exception ?: Exception(it)) }
77:             }
78:     }
79: 
80:     suspend fun markAllRead(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
81:         runCatching {
82:             val snap = Tasks.await(
83:                 firestore.collection(COLLECTION)
84:                     .whereEqualTo("recipientUid", uid)
85:                     .whereEqualTo("read", false)
86:                     .limit(100)
87:                     .get()
88:             )
89:             val batch = firestore.batch()
90:             snap.documents.forEach { batch.update(it.reference, "read", true) }
91:             Tasks.await(batch.commit())
92:             Unit
93:         }
94:     }
95: 
96:     /** Marca una o varias como leídas (true) o no leídas (false). */
97:     suspend fun setRead(uid: String, ids: List<String>, read: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
98:         runCatching {
99:             require(uid.isNotBlank() && ids.isNotEmpty()) { "Avisos no válidos." }
100:             val batch = firestore.batch()
101:             ids.chunked(400).forEach { lote ->
102:                 lote.forEach { batch.update(firestore.collection(COLLECTION).document(it), "read", read) }
103:             }
104:             Tasks.await(batch.commit())
105:             Unit
106:         }
107:     }
108: 
109:     /** Elimina una notificación propia. */
110:     suspend fun deleteOne(uid: String, notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
111:         runCatching {
112:             require(uid.isNotBlank() && notificationId.isNotBlank()) { "Aviso no válido." }
113:             val ref = firestore.collection(COLLECTION).document(notificationId)
114:             val actual = Tasks.await(ref.get())
115:             require(actual.exists()) { "El aviso ya no existe." }
116:             require(actual.getString("recipientUid") == uid) { "Ese aviso no te pertenece." }
117:             Tasks.await(ref.delete())
118:             Unit
119:         }
120:     }
121: 
122:     /** Elimina todas mis notificaciones (lotes de 100). */
123:     suspend fun deleteAll(uid: String): Result<Int> = withContext(Dispatchers.IO) {
124:         runCatching {
125:             require(uid.isNotBlank()) { "Sesión no válida." }
126:             val snap = Tasks.await(
127:                 firestore.collection(COLLECTION)
128:                     .whereEqualTo("recipientUid", uid)
129:                     .limit(100)
130:                     .get()
131:             )
132:             if (snap.isEmpty) return@runCatching 0
133:             val batch = firestore.batch()
134:             snap.documents.forEach { batch.delete(it.reference) }
135:             Tasks.await(batch.commit())
136:             snap.size()
137:         }
138:     }
139: 
140:     /** Restaura un aviso borrado (botón Deshacer): conserva contenido y hora. */
141:     suspend fun restore(uid: String, n: AppNotification): Result<Unit> = withContext(Dispatchers.IO) {
142:         runCatching {
143:             require(uid.isNotBlank() && n.recipientUid == uid) { "Aviso no válido." }
144:             Tasks.await(
145:                 firestore.collection(COLLECTION).add(
146:                     mapOf(
147:                         "recipientUid" to uid,
148:                         "type" to n.type,
149:                         "title" to n.title,
150:                         "message" to n.message,
151:                         "senderUid" to n.senderUid,
152:                         "publicationId" to n.publicationId,
153:                         "read" to n.read,
154:                         "createdAt" to (n.createdAt ?: FieldValue.serverTimestamp())
155:                     )
156:                 )
157:             )
158:             Unit
159:         }
160:     }
161: 
162:     companion object {
163:         const val COLLECTION = "notifications"
164:     }
165: }

(End of file - total 165 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\model\\NotificationModels.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\NotificationModels.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.data.model
2: 
3: import com.google.firebase.Timestamp
4: import com.google.firebase.firestore.DocumentSnapshot
5: 
6: /**
7:  * Notificaciones básicas (FASE 7 las crea; la bandeja completa es FASE 14).
8:  * Colección: notifications/{notificationId}
9:  */
10: object NotificationType {
11:     const val NEW_APPLICATION = "NEW_APPLICATION"
12:     const val APPLICATION_ACCEPTED = "APPLICATION_ACCEPTED"
13:     const val APPLICATION_REJECTED = "APPLICATION_REJECTED"
14:     const val JOB_COMPLETED = "JOB_COMPLETED"
15:     const val NEW_MESSAGE = "NEW_MESSAGE"
16:     const val NEW_RATING = "NEW_RATING"
17:     const val NEW_COMMENT = "NEW_COMMENT"
18:     const val NEW_LIKE = "NEW_LIKE"
19: }
20: 
21: data class AppNotification(
22:     val notificationId: String = "",
23:     val recipientUid: String = "",
24:     val type: String = "",
25:     val title: String = "",
26:     val message: String = "",
27:     val senderUid: String = "",
28:     val publicationId: String = "",
29:     val read: Boolean = false,
30:     val createdAt: Timestamp? = null
31: )
32: 
33: fun DocumentSnapshot.toAppNotification(): AppNotification {
34:     return AppNotification(
35:         notificationId = getString("notificationId") ?: id,
36:         recipientUid = getString("recipientUid").orEmpty(),
37:         type = getString("type").orEmpty(),
38:         title = getString("title").orEmpty(),
39:         message = getString("message").orEmpty(),
40:         senderUid = getString("senderUid").orEmpty(),
41:         publicationId = getString("publicationId").orEmpty(),
42:         read = getBoolean("read") ?: false,
43:         createdAt = getTimestamp("createdAt")
44:     )
45: }

(End of file - total 45 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\ui\\jobs\\NotificationsSheet.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\NotificationsSheet.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.ui.jobs
2: 
3: import android.app.Dialog
4: import android.graphics.Color
5: import android.graphics.Canvas
6: import android.graphics.drawable.ColorDrawable
7: import android.os.Bundle
8: import android.view.LayoutInflater
9: import android.view.Gravity
10: import android.view.View
11: import android.view.ViewGroup
12: import android.widget.ProgressBar
13: import android.widget.TextView
14: import android.widget.Toast
15: import androidx.appcompat.app.AlertDialog
16: import androidx.core.content.ContextCompat
17: import androidx.core.os.bundleOf
18: import androidx.lifecycle.lifecycleScope
19: import androidx.recyclerview.widget.DiffUtil
20: import androidx.recyclerview.widget.ItemTouchHelper
21: import androidx.recyclerview.widget.LinearLayoutManager
22: import androidx.recyclerview.widget.ListAdapter
23: import androidx.recyclerview.widget.RecyclerView
24: import androidx.fragment.app.DialogFragment
25: import com.google.android.material.snackbar.Snackbar
26: import com.google.firebase.auth.FirebaseAuth
27: import com.proyecto.chambaya.R
28: import com.proyecto.chambaya.data.model.AppNotification
29: import com.proyecto.chambaya.data.model.NotificationType
30: import com.proyecto.chambaya.data.model.publicationTimeAgo
31: import com.proyecto.chambaya.data.repository.NotificationRepository
32: import kotlinx.coroutines.launch
33: 
34: /**
35:  * Bandeja de notificaciones: abrir con tap, deslizar a la izquierda para
36:  * eliminar una (con Deshacer) y papelera para borrar todas (con confirmación).
37:  */
38: class NotificationsSheet : DialogFragment() {
39: 
40:     private val repo = NotificationRepository()
41:     private var adapter: Adapter? = null
42:     private var registration: com.google.firebase.firestore.ListenerRegistration? = null
43:     private var backCallback: androidx.activity.OnBackPressedCallback? = null
44: 
45:     override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
46:         return inflater.inflate(R.layout.dialog_notifications, container, false)
47:     }
48: 
49:     override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = Dialog(requireContext())
50: 
51:     override fun onStart() {
52:         super.onStart()
53:         dialog?.window?.let { window ->
54:             val density = resources.displayMetrics.density
55:             val metrics = resources.displayMetrics
56:             val width = (metrics.widthPixels - 36f * density).toInt()
57:             val height = minOf((metrics.heightPixels * 0.72f).toInt(), (620f * density).toInt())
58:             window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
59:             window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
60:             window.setDimAmount(0.42f)
61:             window.setGravity(Gravity.CENTER)
62:             window.setLayout(width, height)
63:             window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
64:         }
65:     }
66: 
67:     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
68:         super.onViewCreated(view, savedInstanceState)
69:         val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
70:         if (uid.isBlank()) { dismiss(); return }
71: 
72:         val rv = view.findViewById<RecyclerView>(R.id.rvNotifications)
73:         rv.layoutManager = LinearLayoutManager(requireContext())
74:         adapter = Adapter(
75:             onOpen = { n ->
76:                 if (adapter?.modoSeleccion == true) {
77:                     alternarSeleccion(n)
78:                 } else if (n.publicationId.isNotBlank()) {
79:                     parentFragmentManager.setFragmentResult(
80:                         REQUEST_OPEN_PUB, bundleOf(EXTRA_PUB to n.publicationId)
81:                     )
82:                     dismiss()
83:                 }
84:             },
85:             onLongPress = { n, anchor -> mostrarMenuPulsacion(uid, n, anchor) }
86:         )
87:         rv.adapter = adapter
88:         setupSwipeToDelete(rv, uid)
89:         setupBarraSeleccion(uid)
90:         val progress = view.findViewById<ProgressBar>(R.id.progressNotif)
91:         val empty = view.findViewById<View>(R.id.layoutNotifEmpty)
92:         progress.visibility = View.VISIBLE
93: 
94:         registration = repo.listenMine(
95:             uid, 30,
96:             onUpdate = { list ->
97:                 if (!isAdded) return@listenMine
98:                 progress.visibility = View.GONE
99:                 empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
100:                 rv.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
101:                 adapter?.submitList(list)
102:             },
103:             onError = {
104:                 if (!isAdded) return@listenMine
105:                 progress.visibility = View.GONE
106:                 empty.visibility = View.VISIBLE
107:                 rv.visibility = View.GONE
108:             }
109:         )
110:         view.findViewById<View>(R.id.btnNotifReadAll).setOnClickListener { v ->
111:             v.isEnabled = false
112:             viewLifecycleOwner.lifecycleScope.launch {
113:                 val result = repo.markAllRead(uid)
114:                 if (!isAdded) return@launch
115:                 v.isEnabled = true
116:                 if (result.isFailure) {
117:                     Toast.makeText(requireContext(), "No se pudieron marcar como leídas.", Toast.LENGTH_SHORT).show()
118:                 }
119:             }
120:         }
121:         view.findViewById<View>(R.id.btnNotifDeleteAll).setOnClickListener {
122:             confirmarBorrarTodas(uid)
123:         }
124:         view.findViewById<View>(R.id.btnNotifClose).setOnClickListener { dismiss() }
125:     }
126: 
127:     /** Menú de pulsación larga: leído/no leído, eliminar y seleccionar. */
128:     private fun mostrarMenuPulsacion(uid: String, n: AppNotification, anchor: View) {
129:         val menu = androidx.appcompat.widget.PopupMenu(requireContext(), anchor)
130:         menu.menu.add(0, 1, 0, if (n.read) "Marcar como no leído" else "Marcar como leído")
131:         menu.menu.add(0, 2, 0, "Eliminar")
132:         menu.menu.add(0, 3, 0, "Seleccionar")
133:         menu.setOnMenuItemClickListener { item ->
134:             when (item.itemId) {
135:                 1 -> {
136:                     viewLifecycleOwner.lifecycleScope.launch {
137:                         val result = repo.setRead(uid, listOf(n.notificationId), !n.read)
138:                         if (!isAdded) return@launch
139:                         Toast.makeText(
140:                             requireContext(),
141:                             when {
142:                                 result.isFailure -> "No se pudo actualizar la notificación."
143:                                 n.read -> "Notificación marcada como no leída."
144:                                 else -> "Notificación marcada como leída."
145:                             },
146:                             Toast.LENGTH_SHORT
147:                         ).show()
148:                     }
149:                     true
150:                 }
151:                 2 -> {
152:                     eliminarUno(uid, n)
153:                     true
154:                 }
155:                 3 -> {
156:                     entrarSeleccion(n)
157:                     true
158:                 }
159:                 else -> false
160:             }
161:         }
162:         menu.show()
163:     }
164: 
165:     /** Borra un aviso con Deshacer (la lista en vivo se repinta sola). */
166:     private fun eliminarUno(uid: String, n: AppNotification, pos: Int? = null) {
167:         viewLifecycleOwner.lifecycleScope.launch {
168:             val r = repo.deleteOne(uid, n.notificationId)
169:             if (!isAdded) return@launch
170:             if (r.isSuccess) {
171:                 Snackbar.make(requireView(), "Notificación eliminada", Snackbar.LENGTH_LONG)
172:                     .setAction("Deshacer") {
173:                         viewLifecycleOwner.lifecycleScope.launch {
174:                             repo.restore(uid, n)
175:                         }
176:                     }
177:                     .show()
178:             } else {
179:                 if (pos != null) adapter?.notifyItemChanged(pos)
180:                 Toast.makeText(requireContext(), "No se pudo eliminar.", Toast.LENGTH_SHORT).show()
181:             }
182:         }
183:     }
184: 
185:     // ── Selección múltiple ───────────────────────────────────
186: 
187:     private fun entrarSeleccion(n: AppNotification) {
188:         adapter?.entrarSeleccion(n)
189:         mostrarBarraSeleccion()
190:     }
191: 
192:     private fun alternarSeleccion(n: AppNotification) {
193:         val sigue = adapter?.alternar(n) == true
194:         if (sigue) actualizarBarraSeleccion() else ocultarBarraSeleccion()
195:     }
196: 
197:     private fun mostrarBarraSeleccion() {
198:         view?.findViewById<View>(R.id.layoutNotifHeader)?.visibility = View.GONE
199:         view?.findViewById<View>(R.id.layoutNotifActions)?.visibility = View.GONE
200:         view?.findViewById<View>(R.id.layoutNotifSelection)?.visibility = View.VISIBLE
201:         actualizarBarraSeleccion()
202:         backCallback?.isEnabled = true
203:     }
204: 
205:     private fun actualizarBarraSeleccion() {
206:         val n = adapter?.seleccionados?.size ?: 0
207:         view?.findViewById<TextView>(R.id.tvSelCount)?.text =
208:             if (n == 1) "1 seleccionada" else "$n seleccionadas"
209:         view?.findViewById<TextView>(R.id.btnSelAll)?.text =
210:             if (n > 0 && n == adapter?.itemCount) "Ninguna" else "Todo"
211:     }
212: 
213:     private fun salirSeleccion() {
214:         adapter?.limpiarSeleccion()
215:         ocultarBarraSeleccion()
216:     }
217: 
218:     private fun ocultarBarraSeleccion() {
219:         view?.findViewById<View>(R.id.layoutNotifSelection)?.visibility = View.GONE
220:         view?.findViewById<View>(R.id.layoutNotifHeader)?.visibility = View.VISIBLE
221:         view?.findViewById<View>(R.id.layoutNotifActions)?.visibility = View.VISIBLE
222:         backCallback?.isEnabled = false
223:     }
224: 
225:     private fun setupBarraSeleccion(uid: String) {
226:         backCallback = object : androidx.activity.OnBackPressedCallback(false) {
227:             override fun handleOnBackPressed() = salirSeleccion()
228:         }
229:         requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback!!)
230:         view?.findViewById<View>(R.id.btnSelBack)?.setOnClickListener { salirSeleccion() }
231:         view?.findViewById<View>(R.id.btnSelAll)?.setOnClickListener {
232:             val a = adapter ?: return@setOnClickListener
233:             if (a.seleccionados.size == a.itemCount && a.itemCount > 0) salirSeleccion()
234:             else {
235:                 a.seleccionarTodo()
236:                 actualizarBarraSeleccion()
237:             }
238:         }
239:         view?.findViewById<View>(R.id.btnSelRead)?.setOnClickListener {
240:             val ids = adapter?.seleccionados?.toList().orEmpty()
241:             if (ids.isEmpty()) return@setOnClickListener
242:             viewLifecycleOwner.lifecycleScope.launch {
243:                 val r = repo.setRead(uid, ids, true)
244:                 if (!isAdded) return@launch
245:                 Toast.makeText(
246:                     requireContext(),
247:                     if (r.isSuccess) "Marcadas como leídas." else "No se pudo actualizar.",
248:                     Toast.LENGTH_SHORT
249:                 ).show()
250:             }
251:         }
252:         view?.findViewById<View>(R.id.btnSelDelete)?.setOnClickListener {
253:             val lista = adapter?.avisosSeleccionados().orEmpty()
254:             if (lista.isEmpty()) return@setOnClickListener
255:             salirSeleccion()
256:             viewLifecycleOwner.lifecycleScope.launch {
257:                 val resultados = lista.map { it to repo.deleteOne(uid, it.notificationId) }
258:                 if (!isAdded) return@launch
259:                 val eliminadas = resultados.filter { it.second.isSuccess }.map { it.first }
260:                 val fallidas = resultados.size - eliminadas.size
261:                 if (fallidas > 0) {
262:                     Toast.makeText(
263:                         requireContext(),
264:                         "$fallidas notificación(es) no se pudieron eliminar.",
265:                         Toast.LENGTH_SHORT
266:                     ).show()
267:                 }
268:                 if (eliminadas.isEmpty()) return@launch
269:                 Snackbar.make(
270:                     requireView(),
271:                     if (lista.size == 1) "1 notificación eliminada"
272:                     else "${lista.size} notificaciones eliminadas",
273:                     Snackbar.LENGTH_LONG
274:                 )
275:                     .setAction("Deshacer") {
276:                         viewLifecycleOwner.lifecycleScope.launch {
277:                             eliminadas.forEach { repo.restore(uid, it) }
278:                         }
279:                     }
280:                     .show()
281:             }
282:         }
283:     }
284: 
285:     /** Papelera superior: borra todo con confirmación previa. */
286:     private fun confirmarBorrarTodas(uid: String) {
287:         if (adapter?.itemCount == 0) {
288:             Toast.makeText(requireContext(), "No hay notificaciones.", Toast.LENGTH_SHORT).show()
289:             return
290:         }
291:         AlertDialog.Builder(requireContext())
292:             .setTitle("Eliminar notificaciones")
293:             .setMessage("Se borrarán todas tus notificaciones. Esta acción no se puede deshacer.")
294:             .setPositiveButton("Eliminar todo") { _, _ ->
295:                 viewLifecycleOwner.lifecycleScope.launch {
296:                     val r = repo.deleteAll(uid)
297:                     if (!isAdded) return@launch
298:                     Toast.makeText(
299:                         requireContext(),
300:                         if (r.isSuccess) "Notificaciones eliminadas." else "No se pudo eliminar.",
301:                         Toast.LENGTH_SHORT
302:                     ).show()
303:                 }
304:             }
305:             .setNegativeButton("Cancelar", null)
306:             .show()
307:     }
308: 
309:     /** Deslizar a la izquierda = eliminar con fondo rojo + Deshacer. */
310:     private fun setupSwipeToDelete(rv: RecyclerView, uid: String) {
311:         val fondo = ColorDrawable(0)
312:         val icono = ContextCompat.getDrawable(requireContext(), R.drawable.ic_notif_delete)
313:         val rojo = requireContext().getColor(R.color.notif_swipe_delete_bg)
314:         val blanco = requireContext().getColor(R.color.white)
315:         val density = resources.displayMetrics.density
316: 
317:         val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
318:             override fun onMove(
319:                 rv: RecyclerView,
320:                 vh: RecyclerView.ViewHolder,
321:                 target: RecyclerView.ViewHolder
322:             ): Boolean = false
323: 
324:             override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
325:                 val pos = vh.bindingAdapterPosition
326:                 if (pos == RecyclerView.NO_POSITION) return
327:                 val aviso = adapter?.currentList?.getOrNull(pos) ?: return
328:                 if (adapter?.modoSeleccion == true) {
329:                     // En selección no se elimina por swipe: se restaura la fila.
330:                     adapter?.notifyItemChanged(pos)
331:                     return
332:                 }
333:                 eliminarUno(uid, aviso, pos)
334:             }
335: 
336:             override fun onChildDraw(
337:                 c: Canvas,
338:                 rv: RecyclerView,
339:                 vh: RecyclerView.ViewHolder,
340:                 dX: Float,
341:                 dY: Float,
342:                 actionState: Int,
343:                 isCurrentlyActive: Boolean
344:             ) {
345:                 if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
346:                     val item = vh.itemView
347:                     fondo.color = rojo
348:                     fondo.setBounds(
349:                         (item.right + dX).toInt(),
350:                         (item.top + 8 * density).toInt(),
351:                         item.right,
352:                         (item.bottom - 8 * density).toInt()
353:                     )
354:                     fondo.draw(c)
355:                     icono?.let {
356:                         val lado = (24 * density).toInt()
357:                         val margen = (20 * density).toInt()
358:                         val arriba = item.top + (item.height - lado) / 2
359:                         it.setBounds(
360:                             item.right - margen - lado,
361:                             arriba,
362:                             item.right - margen,
363:                             arriba + lado
364:                         )
365:                         it.setTint(blanco)
366:                         it.draw(c)
367:                     }
368:                 }
369:                 super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
370:             }
371:         }
372:         ItemTouchHelper(callback).attachToRecyclerView(rv)
373:     }
374: 
375:     override fun onDestroyView() {
376:         registration?.remove()
377:         registration = null
378:         adapter = null
379:         backCallback = null
380:         super.onDestroyView()
381:     }
382: 
383:     class Adapter(
384:         private val onOpen: (AppNotification) -> Unit,
385:         private val onLongPress: (AppNotification, View) -> Unit
386:     ) : ListAdapter<AppNotification, Adapter.VH>(Diff()) {
387:         /** Ids en selección múltiple (pulsación larga). */
388:         val seleccionados = mutableSetOf<String>()
389:         var modoSeleccion = false
390:             private set
391: 
392:         override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
393:             val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
394:             return VH(v, onOpen, onLongPress, ::estaSeleccionado)
395:         }
396:         override fun onBindViewHolder(h: VH, p: Int) = h.bind(getItem(p))
397: 
398:         fun estaSeleccionado(id: String): Boolean = id in seleccionados
399: 
400:         /** Entra en modo selección con este aviso marcado. */
401:         fun entrarSeleccion(n: AppNotification) {
402:             modoSeleccion = true
403:             seleccionados.add(n.notificationId)
404:             notifyDataSetChanged()
405:         }
406: 
407:         /** Marca/desmarca en modo selección. Devuelve true si sigue en modo. */
408:         fun alternar(n: AppNotification): Boolean {
409:             val id = n.notificationId
410:             if (id in seleccionados) seleccionados.remove(id) else seleccionados.add(id)
411:             if (seleccionados.isEmpty()) {
412:                 modoSeleccion = false
413:             }
414:             notifyDataSetChanged()
415:             return modoSeleccion
416:         }
417: 
418:         fun seleccionarTodo() {
419:             seleccionados.clear()
420:             currentList.forEach { seleccionados.add(it.notificationId) }
421:             notifyDataSetChanged()
422:         }
423: 
424:         fun limpiarSeleccion() {
425:             seleccionados.clear()
426:             modoSeleccion = false
427:             notifyDataSetChanged()
428:         }
429: 
430:         fun avisosSeleccionados(): List<AppNotification> {
431:             val ids = seleccionados.toSet()
432:             return currentList.filter { it.notificationId in ids }
433:         }
434: 
435:         class VH(
436:             view: View,
437:             private val onOpen: (AppNotification) -> Unit,
438:             private val onLongPress: (AppNotification, View) -> Unit,
439:             private val esSeleccionado: (String) -> Boolean
440:         ) : RecyclerView.ViewHolder(view) {
441:             private val dot: View = view.findViewById(R.id.dotUnread)
442:             private val tvTitle: TextView = view.findViewById(R.id.tvNotifTitle)
443:             private val tvMsg: TextView = view.findViewById(R.id.tvNotifMessage)
444:             private val tvTime: TextView = view.findViewById(R.id.tvNotifTime)
445:             fun bind(n: AppNotification) {
446:                 tvTitle.text = n.title.ifBlank {
447:                     when (n.type) {
448:                         NotificationType.NEW_APPLICATION -> "Nueva postulación"
449:                         NotificationType.APPLICATION_ACCEPTED -> "¡Fuiste seleccionado!"
450:                         NotificationType.APPLICATION_REJECTED -> "Postulación decidida"
451:                         else -> "Aviso"
452:                     }
453:                 }
454:                 tvMsg.text = n.message
455:                 tvTime.text = publicationTimeAgo(n.createdAt)
456:                 dot.visibility = if (n.read) View.INVISIBLE else View.VISIBLE
457:                 itemView.setBackgroundColor(
458:                     if (esSeleccionado(n.notificationId)) itemView.context.getColor(R.color.notif_selected_bg)
459:                     else android.graphics.Color.TRANSPARENT
460:                 )
461:                 itemView.setOnClickListener { onOpen(n) }
462:                 itemView.setOnLongClickListener {
463:                     onLongPress(n, it)
464:                     true
465:                 }
466:             }
467:         }
468:         class Diff : DiffUtil.ItemCallback<AppNotification>() {
469:             override fun areItemsTheSame(a: AppNotification, b: AppNotification): Boolean =
470:                 a.notificationId == b.notificationId
471:             override fun areContentsTheSame(a: AppNotification, b: AppNotification): Boolean = a == b
472:         }
473:     }
474: 
475:     companion object {
476:         const val REQUEST_OPEN_PUB = "notif_open_pub"
477:         const val EXTRA_PUB = "publicationId"
478:     }
479: }

(End of file - total 479 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\RegistrationRepository.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\RegistrationRepository.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.data.repository
2: 
3: import com.google.android.gms.tasks.Tasks
4: import com.google.firebase.auth.FirebaseUser
5: import com.google.firebase.firestore.FieldValue
6: import com.google.firebase.firestore.DocumentSnapshot
7: import com.google.firebase.firestore.FirebaseFirestore
8: import com.google.firebase.firestore.SetOptions
9: import com.proyecto.chambaya.data.model.AccountStatuses
10: import com.proyecto.chambaya.data.model.AuthMethods
11: import com.proyecto.chambaya.data.model.AuthProviders
12: import com.proyecto.chambaya.data.model.BirthDates
13: import com.proyecto.chambaya.data.model.EmailVerificationMethods
14: import com.proyecto.chambaya.data.model.EmployerTypes
15: import com.proyecto.chambaya.data.model.Genders
16: import com.proyecto.chambaya.data.model.IdentityDocumentTypes
17: import com.proyecto.chambaya.data.model.IdentityNameParser
18: import com.proyecto.chambaya.data.model.PendingRegistration
19: import com.proyecto.chambaya.data.model.PeruLocations
20: import com.proyecto.chambaya.data.model.ProfilePhotoSources
21: import com.proyecto.chambaya.data.model.RegistrationStatuses
22: import com.proyecto.chambaya.data.model.UserRoles
23: import kotlinx.coroutines.Dispatchers
24: import kotlinx.coroutines.withContext
25: 
26: /**
27:  * Estado real del registro de una cuenta, para distinguir
28:  * "cuenta ya registrada" de "registro a medias" (bug de reanudación de FASE 1).
29:  */
30: enum class UserRegistrationState {
31:     /** No existe documento `users/{uid}`: la cuenta de Auth quedó huérfana. */
32:     MISSING,
33: 
34:     /** Existe `users/{uid}` pero el registro todavía no se completó. */
35:     INCOMPLETE,
36: 
37:     /** Registro completo: la cuenta ya está dada de alta en ChambAYA. */
38:     COMPLETE,
39: 
40:     /** No se pudo consultar (sin red o error de permisos): estado indeterminado. */
41:     UNKNOWN
42: }
43: 
44: /**
45:  * FASE 1 — Repositorio de `users/{uid}`.
46:  *
47:  * Única fuente de verdad para crear el documento de usuario al terminar el registro.
48:  * Antes el documento se armaba "a mano" en tres lugares distintos
49:  * (`RegistroActivity`, fallback de OTP y Cloud Function) y solo guardaba el correo.
50:  *
51:  * Documento resultante (FASE 1 del plan maestro):
52:  *
53:  * ```
54:  * users/{uid}
55:  *   uid                    -> uid de Firebase Authentication
56:  *   accountStatus          -> "ACTIVE"
57:  *   registrationStatus     -> "VERIFIED"
58:  *   roles[]                -> ["TRABAJADOR"] | ["CONTRATANTE"]
59:  *   activeRole             -> rol principal
60:  *   auth.provider          -> "EMAIL" | "GOOGLE"
61:  *   auth.email             -> correo de la cuenta
62:  *   auth.emailVerified     -> true
63:  *   auth.otpVerified       -> true cuando el correo se confirmó con OTP de ChambAYA
64:  *   auth.verificationMethod-> "CHAMBAYA_OTP" | "FIREBASE_EMAIL_LINK"
65:  *   identity.documentType  -> "DNI" | "RUC"
66:  *   identity.documentNumber-> número validado (solo lectura del propietario)
67:  *   identity.documentNumberMasked -> versión segura para vistas públicas
68:  *   identity.identityVerified     -> true
69:  *   identity.verifiedWith -> "RENIEC" | "SUNAT"
70:  *   identity.identityName -> nombre oficial (persona o razón social)
71:  *   identity.identityStatus / location -> datos oficiales del padrón
72:  *   identity.verifiedAt   -> Timestamp
73:  *   profile.firstName / lastName / fullName -> nombre del usuario (REGISTRO)
74:  *   profile.profilePhotoUrl / profilePhotoPublicId / profilePhotoSource
75:  *   profile.country
76:  *   createdAt / updatedAt / lastLoginAt -> Timestamp
77:  * ```
78:  *
79:  * NO se guarda: contraseñas, imágenes en Base64 ni datos de FASE 2 en adelante.
80:  */
81: class RegistrationRepository(
82:     private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
83: ) {
84: 
85:     /**
86:      * Crea `users/{uid}` con los datos mínimos de la FASE 1.
87:      *
88:      * Es idempotente: si el documento ya tiene la identidad y el nombre
89:      * guardados, solo actualiza `updatedAt` / `lastLoginAt` (esto respeta las
90:      * Firestore Security Rules). Si quedó incompleto, completa los campos.
91:      */
92:     suspend fun finalizeRegistration(
93:         pending: PendingRegistration,
94:         firebaseUser: FirebaseUser? = null
95:     ): Result<Unit> = withContext(Dispatchers.IO) {
96:         runCatching {
97:             val userRef = firestore.collection(COLLECTION_USERS).document(pending.uid)
98:             val existing = Tasks.await(userRef.get())
99: 
100:             val payload = if (!existing.exists() || !existing.hasFase1Data()) {
101:                 buildRegistrationDocument(pending, firebaseUser)
102:             } else {
103:                 buildRefreshDocument()
104:             }
105: 
106:             Tasks.await(userRef.set(payload, SetOptions.merge()))
107:         }.map { }
108:     }
109: 
110:     /**
111:      * Consulta el estado real del registro de `users/{uid}`.
112:      *
113:      * Se usa antes de mostrar "esta cuenta ya está registrada": si el documento
114:      * no existe o está incompleto, el registro debe **reanudarse** en lugar de
115:      * bloquear al usuario.
116:      */
117:     suspend fun fetchRegistrationState(uid: String): UserRegistrationState =
118:         withContext(Dispatchers.IO) {
119:             runCatching {
120:                 val snapshot = Tasks.await(
121:                     firestore.collection(COLLECTION_USERS).document(uid).get()
122:                 )
123:                 when {
124:                     !snapshot.exists() -> UserRegistrationState.MISSING
125:                     isRegistered(snapshot) -> UserRegistrationState.COMPLETE
126:                     else -> UserRegistrationState.INCOMPLETE
127:                 }
128:             }.getOrDefault(UserRegistrationState.UNKNOWN)
129:         }
130: 
131:     /**
132:      * Mismo criterio que usa `LoginActivity` para dar acceso a la app.
133:      * Garantiza el invariante: *si el usuario puede entrar por login,
134:      * no se le debe ofrecer un registro nuevo*.
135:      */
136:     fun isRegistered(snapshot: DocumentSnapshot): Boolean {
137:         val registrationStatus = snapshot.getString("registrationStatus").orEmpty()
138:         val accountStatus = snapshot.getString("accountStatus").orEmpty()
139:         return registrationStatus in RegistrationStatuses.REGISTERED ||
140:             accountStatus == AccountStatuses.ACTIVE
141:     }
142: 
143:     /**
144:      * `true` cuando el documento ya guarda la identidad oficial y el nombre
145:      * del usuario (bloques `identity` y `profile` de la FASE 1).
146:      */
147:     private fun DocumentSnapshot.hasFase1Data(): Boolean {
148:         val identity = get("identity") as? Map<*, *> ?: return false
149:         val profile = get("profile") as? Map<*, *> ?: return false
150:         val documentNumber = identity["documentNumber"] as? String ?: return false
151:         val identityName = identity["identityName"] as? String ?: return false
152:         val fullName = profile["fullName"] as? String ?: return false
153:         return documentNumber.isNotBlank() && identityName.isNotBlank() && fullName.isNotBlank()
154:     }
155: 
156:     /** Registra el último ingreso sin tocar datos de identidad ni verificaciones. */
157:     suspend fun touchLastLogin(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
158:         runCatching {
159:             val userRef = firestore.collection(COLLECTION_USERS).document(uid)
160:             if (!Tasks.await(userRef.get()).exists()) return@runCatching
161:             Tasks.await(userRef.set(buildRefreshDocument(), SetOptions.merge()))
162:         }.map { }
163:     }
164: 
165:     /** Payload completo de alta (documento nuevo). */
166:     fun buildRegistrationDocument(
167:         pending: PendingRegistration,
168:         firebaseUser: FirebaseUser? = null
169:     ): Map<String, Any?> {
170:         val now = FieldValue.serverTimestamp()
171:         val identity = pending.identity
172:         val email = pending.email.ifBlank { firebaseUser?.email.orEmpty() }.trim().lowercase()
173:         val provider = pending.provider.ifBlank { resolveProvider(firebaseUser) }
174:         val authMethod = pending.authMethod.ifBlank { resolveAuthMethod(provider) }
175:         val verificationMethod = pending.verificationMethod.ifBlank {
176:             if (pending.otpVerified) {
177:                 EmailVerificationMethods.CHAMBAYA_OTP
178:             } else {
179:                 EmailVerificationMethods.FIREBASE_EMAIL_LINK
180:             }
181:         }
182: 
183:         val (firstName, lastName) = resolvePersonalNames(pending)
184:         val photoUrl = pending.accountPhotoUrl.trim()
185: 
186:         return linkedMapOf(
187:             // --- Núcleo de la cuenta ---
188:             "uid" to pending.uid,
189:             "accountStatus" to AccountStatuses.ACTIVE,
190:             "registrationStatus" to RegistrationStatuses.VERIFIED,
191:             "roles" to listOf(pending.role),
192:             "activeRole" to pending.role,
193: 
194:             // --- Credenciales (sin contraseña: vive en Firebase Auth) ---
195:             "auth" to linkedMapOf(
196:                 "provider" to provider,
197:                 "email" to email,
198:                 "emailVerified" to true,
199:                 "otpVerified" to pending.otpVerified,
200:                 "verificationMethod" to verificationMethod
201:             ),
202: 
203:             // --- Identidad validada por padrón oficial ---
204:             "identity" to linkedMapOf(
205:                 "documentType" to identity.documentType,
206:                 "documentNumber" to identity.documentNumber,
207:                 "documentNumberMasked" to identity.maskedDocumentNumber,
208:                 "identityVerified" to true,
209:                 "verifiedWith" to identity.source,
210:                 "identityName" to identity.displayName,
211:                 "identityStatus" to (identity.statusLabel ?: ""),
212:                 "location" to (identity.locationLabel ?: ""),
213:                 "verifiedAt" to now
214:             ),
215: 
216:             // --- Nombre del usuario (lo que faltaba guardar) ---
217:             "profile" to linkedMapOf(
218:                 "firstName" to firstName,
219:                 "lastName" to lastName,
220:                 "fullName" to identity.displayName.ifBlank { pending.accountDisplayName },
221:                 "profilePhotoUrl" to photoUrl,
222:                 "profilePhotoPublicId" to "",
223:                 "profilePhotoSource" to if (photoUrl.isNotEmpty()) {
224:                     ProfilePhotoSources.GOOGLE
225:                 } else {
226:                     ProfilePhotoSources.DEFAULT
227:                 },
228:                 "country" to COUNTRY,
229:                 // Datos que la API de padron ya traia y se descartaban: RENIEC
230:                 // devuelve el cumpleanos y el sexo, SUNAT la direccion descompuesta.
231:                 // Se guardan desde el alta para que "Editar perfil" (FASE 2) abra
232:                 // con ellos puestos en vez de en blanco. Si el padron no los
233:                 // entrega quedan vacios y el campo sigue pendiente, que es
234:                 // justamente lo que mide el porcentaje de completitud.
235:                 "birthDate" to BirthDates.soloSiValida(identity.birthDate),
236:                 "gender" to identity.gender.takeIf { it in Genders.ALL }.orEmpty(),
237:                 "department" to PeruLocations.nombreCanonico(identity.department),
238:                 "province" to PeruLocations.provinciaCanonica(
239:                     identity.department,
240:                     identity.province
241:                 ),
242:                 "district" to identity.district.trim()
243:             ),
244: 
245:             // --- Contratante: nace con su bloque employer (rol fijo) ---
246:             // El tipo se eligió en el Paso 1 (Persona/Empresa/Negocio/
247:             // Independiente). Sin él, se deduce del documento (RUC→EMPRESA).
248:             // Las reglas de creación no exigen este bloque, así que es seguro.
249:             *employerEntries(pending),
250: 
251:             // --- Auditoría ---
252:             "createdAt" to now,
253:             "updatedAt" to now,
254:             "lastLoginAt" to now,
255: 
256:             // --- Espejo de compatibilidad ---
257:             // Campos raíz que ya leía `LoginActivity`; se conservan para no
258:             // romper datos existentes ni otras pantallas de la app.
259:             "email" to email,
260:             "role" to pending.role,
261:             "emailVerified" to true,
262:             "otpVerified" to pending.otpVerified,
263:             "authMethod" to authMethod,
264:             "verifiedAt" to now
265:         )
266:     }
267: 
268:     /**
269:      * Bloque `employer` inicial para cuentas que nacen CONTRATANTE.
270:      *
271:      * Se devuelve como arreglo (vacío para trabajador) para poder expandirlo
272:      * con `*` dentro del `linkedMapOf` del documento de alta.
273:      */
274:     private fun employerEntries(pending: PendingRegistration): Array<Pair<String, Any?>> {
275:         if (pending.role != UserRoles.CONTRATANTE) return emptyArray()
276:         val tipo = pending.employerType.takeIf { EmployerTypes.isValid(it) }
277:             ?: if (pending.identity.documentType == IdentityDocumentTypes.RUC) {
278:                 EmployerTypes.EMPRESA
279:             } else {
280:                 EmployerTypes.PERSONA
281:             }
282:         val docNum = pending.identity.documentNumber
283:         return arrayOf(
284:             "employer" to linkedMapOf(
285:                 "enabled" to true,
286:                 "employerType" to tipo,
287:                 "businessName" to pending.identity.displayName,
288:                 "commercialName" to "",
289:                 "sector" to "",
290:                 "documentType" to pending.identity.documentType,
291:                 "documentNumber" to docNum,
292:                 "documentNumberMasked" to pending.identity.maskedDocumentNumber,
293:                 "ruc" to docNum.takeIf { pending.identity.documentType == IdentityDocumentTypes.RUC },
294:                 "identityName" to pending.identity.displayName,
295:                 "workplaceId" to null,
296:                 "publishedCount" to 0,
297:                 "hiredCount" to 0,
298:                 "ratingAverage" to 0.0,
299:                 "ratingCount" to 0
300:             )
301:         )
302:     }
303: 
304:     /**
305:      * Payload mínimo para un documento ya creado.
306:      * Solo toca los campos que el usuario puede modificar por Rules.
307:      */
308:     fun buildRefreshDocument(): Map<String, Any?> = linkedMapOf(
309:         "updatedAt" to FieldValue.serverTimestamp(),
310:         "lastLoginAt" to FieldValue.serverTimestamp()
311:     )
312: 
313:     /** Nombres para `profile`: en RUC se usa la razón social como nombre completo. */
314:     private fun resolvePersonalNames(pending: PendingRegistration): Pair<String, String> {
315:         val identity = pending.identity
316:         if (identity.isCompany) return "" to ""
317:         if (identity.firstName.isNotBlank() || identity.lastName.isNotBlank()) {
318:             return identity.firstName to identity.lastName
319:         }
320:         // Respaldo: nombre entregado por Google cuando el padrón no lo detalla.
321:         val fromGoogle = IdentityNameParser.parseFirstAndLastName(pending.accountDisplayName)
322:         return fromGoogle.firstName to fromGoogle.lastName
323:     }
324: 
325:     private fun resolveProvider(firebaseUser: FirebaseUser?): String =
326:         if (firebaseUser?.providerData?.any { it.providerId == AuthProviders.GOOGLE } == true) {
327:             AuthProviders.GOOGLE
328:         } else {
329:             AuthProviders.EMAIL
330:         }
331: 
332:     private fun resolveAuthMethod(provider: String): String =
333:         if (provider == AuthProviders.GOOGLE) AuthMethods.GOOGLE else AuthMethods.EMAIL_PASSWORD
334: 
335:     companion object {
336:         const val COLLECTION_USERS = "users"
337:         private const val COUNTRY = "Peru"
338:     }
339: }

(End of file - total 339 lines)
</content>
```

---

## Assistant (Build · Muse Spark 1.3 Free · 5.7s)

_Thinking:_



**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\ui\\jobs\\PublicProfileSheet.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\PublicProfileSheet.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.ui.jobs
2: 
3: import android.os.Bundle
4: import android.view.LayoutInflater
5: import android.view.View
6: import android.view.ViewGroup
7: import android.widget.ImageView
8: import android.widget.ProgressBar
9: import android.widget.TextView
10: import android.widget.Toast
11: import androidx.core.os.bundleOf
12: import androidx.lifecycle.lifecycleScope
13: import coil.load
14: import com.google.android.material.bottomsheet.BottomSheetDialogFragment
15: import com.google.firebase.auth.FirebaseAuth
16: import com.proyecto.chambaya.R
17: import com.proyecto.chambaya.data.model.JobStatus
18: import com.proyecto.chambaya.data.repository.BlockRepository
19: import com.proyecto.chambaya.data.repository.ChatRepository
20: import com.proyecto.chambaya.data.repository.JobRepository
21: import com.proyecto.chambaya.data.repository.ProfileRepository
22: import com.proyecto.chambaya.data.repository.PublicationRepository
23: import com.proyecto.chambaya.data.repository.RatingRepository
24: import com.proyecto.chambaya.data.repository.WorkplaceRepository
25: import kotlinx.coroutines.async
26: import kotlinx.coroutines.launch
27: 
28: /**
29:  * Perfil público (FASE 6/7, base de FASE 17): contratante Y trabajador.
30:  *
31:  * Lee `public_profiles/{uid}` (visible para todos) y calcula los agregados
32:  * en vivo (publicaciones, trabajos, reputación): nunca muestra DNI, correo,
33:  * teléfono ni dirección exacta.
34:  */
35: class PublicProfileSheet : BottomSheetDialogFragment() {
36: 
37:     private val profileRepo = ProfileRepository()
38:     private val placeRepo = WorkplaceRepository()
39:     private val pubRepo = PublicationRepository()
40:     private val jobRepo = JobRepository()
41:     private val ratingRepo = RatingRepository()
42:     private val chatRepo = ChatRepository()
43:     private val blockRepo = BlockRepository()
44:     private var loadedProfile: com.proyecto.chambaya.data.model.PublicProfile? = null
45: 
46:     override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
47:         return inflater.inflate(R.layout.bottom_sheet_public_profile, container, false)
48:     }
49: 
50:     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
51:         super.onViewCreated(view, savedInstanceState)
52:         val uid = requireArguments().getString(ARG_UID).orEmpty()
53:         if (uid.isBlank()) { dismiss(); return }
54:         val progress = view.findViewById<ProgressBar>(R.id.progressPublic)
55:         val error = view.findViewById<TextView>(R.id.tvPublicError)
56:         progress.visibility = View.VISIBLE
57: 
58:         viewLifecycleOwner.lifecycleScope.launch {
59:             val perfil = profileRepo.loadPublicProfile(uid).getOrNull()
60:             if (perfil == null || !isAdded) {
61:                 progress.visibility = View.GONE
62:                 if (perfil == null) {
63:                     error.visibility = View.VISIBLE
64:                     error.text = "No se pudo cargar el perfil."
65:                 }
66:                 return@launch
67:             }
68:             loadedProfile = perfil
69:             val esEmpresa = perfil.employer.enabled
70: 
71:             // ── Identidad ──
72:             view.findViewById<TextView>(R.id.tvPublicName).text = perfil.displayName()
73:             val rol = if (esEmpresa) {
74:                 perfil.employer.commercialName.ifBlank {
75:                     perfil.employer.businessName.ifBlank {
76:                         perfil.employer.employerType.ifBlank { "Contratante" }
77:                     }
78:                 }
79:             } else "Trabajador"
80:             view.findViewById<TextView>(R.id.tvPublicUsername).text =
81:                 "@${perfil.username.ifBlank { "chambaya" }} · $rol"
82:             view.findViewById<ImageView>(R.id.ivPublicVerified).visibility =
83:                 if (perfil.identityVerified) View.VISIBLE else View.GONE
84:             val avatar = view.findViewById<ImageView>(R.id.ivPublicAvatar)
85:             if (perfil.photoUrl.isNotBlank()) {
86:                 avatar.load(perfil.photoUrl) {
87:                     crossfade(true); placeholder(R.drawable.ic_user_circle); error(R.drawable.ic_user_circle)
88:                 }
89:             }
90:             val bio = perfil.bio.trim()
91:             val tvBio = view.findViewById<TextView>(R.id.tvPublicBio)
92:             if (bio.isNotBlank()) { tvBio.visibility = View.VISIBLE; tvBio.text = bio }
93: 
94:             // ── Lugar (solo empresa con lugar) ──
95:             val workplaceId = perfil.employer.workplaceId.orEmpty()
96:             if (esEmpresa && workplaceId.isNotBlank()) {
97:                 val lugar = placeRepo.loadById(workplaceId).getOrNull()
98:                 if (lugar != null && isAdded) {
99:                     view.findViewById<View>(R.id.rowPublicPlace).visibility = View.VISIBLE
100:                     view.findViewById<TextView>(R.id.tvPublicPlaceName).text = lugar.name
101:                     view.findViewById<TextView>(R.id.tvPublicPlaceDistrict).text =
102:                         listOf(lugar.district, lugar.province).filter { it.isNotBlank() }.joinToString(", ")
103:                 }
104:             }
105: 
106:             // ── Agregados en vivo ──
107:             val pubsD = async { pubRepo.byOwner(uid, 100).getOrNull().orEmpty() }
108:             val jobsEmpD = async { if (esEmpresa) jobRepo.listByEmployer(uid, 100).getOrNull().orEmpty() else emptyList() }
109:             val jobsWorkD = async { if (!esEmpresa) jobRepo.listByWorker(uid, 100).getOrNull().orEmpty() else emptyList() }
110:             val ratingsD = async { ratingRepo.receivedBy(uid, 100).getOrNull().orEmpty() }
111:             val pubs = pubsD.await()
112:             val jobsEmp = jobsEmpD.await()
113:             val jobsWork = jobsWorkD.await()
114:             val ratings = ratingsD.await()
115:             if (!isAdded) return@launch
116:             progress.visibility = View.GONE
117: 
118:             if (esEmpresa) {
119:                 view.findViewById<TextView>(R.id.tvStatLabel1).text = "Publicadas"
120:                 view.findViewById<TextView>(R.id.tvStatLabel2).text = "Contrataciones"
121:                 view.findViewById<TextView>(R.id.tvPublicPublished).text = pubs.size.toString()
122:                 view.findViewById<TextView>(R.id.tvPublicHired).text =
123:                     jobsEmp.count { it.status != JobStatus.CANCELLED }.toString()
124:             } else {
125:                 view.findViewById<TextView>(R.id.tvStatLabel1).text = "Trabajos"
126:                 view.findViewById<TextView>(R.id.tvStatLabel2).text = "Experiencia"
127:                 view.findViewById<TextView>(R.id.tvPublicPublished).text =
128:                     jobsWork.count { it.status == JobStatus.COMPLETED }.toString()
129:                 view.findViewById<TextView>(R.id.tvPublicHired).text =
130:                     if (perfil.worker.experienceYears > 0) "${perfil.worker.experienceYears} años" else "—"
131:             }
132:             view.findViewById<TextView>(R.id.tvPublicRating).text =
133:                 if (ratings.isNotEmpty()) {
134:                     String.format("%.1f", ratings.map { it.rating }.average())
135:                 } else "—"
136: 
137:             configurarAcciones(view, uid)
138:         }
139:     }
140: 
141:     /** Mensaje + bloquear/denunciar (FASE 12/13). Se oculta viéndose a sí mismo. */
142:     private fun configurarAcciones(view: View, uid: String) {
143:         val me = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
144:         val btnMsg = view.findViewById<View>(R.id.btnPublicMessage)
145:         val btnMore = view.findViewById<View>(R.id.btnPublicMore)
146:         if (me.isBlank() || me == uid) {
147:             btnMsg.visibility = View.GONE
148:             btnMore.visibility = View.GONE
149:             return
150:         }
151:         val publicationId = requireArguments().getString(ARG_PUB).orEmpty()
152:         val publicationTitle = requireArguments().getString(ARG_PUB_TITLE).orEmpty()
153:         btnMsg.setOnClickListener {
154:             it.isEnabled = false
155:             viewLifecycleOwner.lifecycleScope.launch {
156:                 val r = chatRepo.ensureConversation(me, uid, publicationId, publicationTitle)
157:                 if (!isAdded) return@launch
158:                 it.isEnabled = true
159:                 if (r.isSuccess) {
160:                     val conv = r.getOrThrow()
161:                     val intent = android.content.Intent(
162:                         requireContext(),
163:                         com.proyecto.chambaya.ui.chat.ActividadChatDetalle::class.java
164:                     ).apply {
165:                         putExtra(
166:                             com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_CONV_ID,
167:                             conv.conversationId
168:                         )
169:                         putExtra(
170:                             com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_OTHER_UID,
171:                             uid
172:                         )
173:                         putExtra(
174:                             com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_NOMBRE,
175:                             loadedProfile?.displayName()
176:                         )
177:                         putExtra(
178:                             com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_FOTO,
179:                             loadedProfile?.photoUrl.orEmpty()
180:                         )
181:                         putExtra(
182:                             com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_PUB_TITULO,
183:                             publicationTitle
184:                         )
185:                     }
186:                     // La foto viaja por el perfil público en la apertura.
187:                     dismiss()
188:                     startActivity(intent)
189:                 } else {
190:                     android.widget.Toast.makeText(
191:                         requireContext(),
192:                         r.exceptionOrNull()?.message ?: "No se pudo abrir el chat.",
193:                         Toast.LENGTH_LONG
194:                     ).show()
195:                 }
196:             }
197:         }
198:         btnMore.setOnClickListener { anchor ->
199:             val menu = androidx.appcompat.widget.PopupMenu(requireContext(), anchor)
200:             menu.menu.add(0, 1, 0, "Bloquear")
201:             menu.menu.add(0, 2, 0, "Denunciar")
202:             menu.setOnMenuItemClickListener { item ->
203:                 when (item.itemId) {
204:                     1 -> confirmarBloqueo(uid)
205:                     2 -> denunciar(uid)
206:                 }
207:                 true
208:             }
209:             menu.show()
210:         }
211:     }
212: 
213:     private fun confirmarBloqueo(uid: String) {
214:         val me = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
215:         androidx.appcompat.app.AlertDialog.Builder(requireContext())
216:             .setTitle("Bloquear usuario")
217:             .setMessage("No verás sus chambas ni podrán escribirse.")
218:             .setPositiveButton("Bloquear") { _, _ ->
219:                 viewLifecycleOwner.lifecycleScope.launch {
220:                     val ya = blockRepo.isBlocked(me, uid)
221:                     val r = if (ya) blockRepo.unblock(me, uid) else blockRepo.block(me, uid)
222:                     if (!isAdded) return@launch
223:                     android.widget.Toast.makeText(
224:                         requireContext(),
225:                         when {
226:                             r.isFailure -> "No se pudo completar."
227:                             ya -> "Usuario desbloqueado."
228:                             else -> "Usuario bloqueado."
229:                         },
230:                         Toast.LENGTH_SHORT
231:                     ).show()
232:                     if (r.isSuccess && !ya) dismiss()
233:                 }
234:             }
235:             .setNegativeButton("Cancelar", null)
236:             .show()
237:     }
238: 
239:     private fun denunciar(uid: String) {
240:         val me = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
241:         if (me.isBlank()) return
242:         val motivos = arrayOf("Spam", "Acoso", "Fraude o estafa", "Contenido inapropiado", "Otro")
243:         androidx.appcompat.app.AlertDialog.Builder(requireContext())
244:             .setTitle("Denunciar usuario")
245:             .setItems(motivos) { _, cual ->
246:                 viewLifecycleOwner.lifecycleScope.launch {
247:                     val r = blockRepo.reportUser(me, uid, motivos[cual])
248:                     if (isAdded) {
249:                         android.widget.Toast.makeText(
250:                             requireContext(),
251:                             if (r.isSuccess) "Denuncia enviada. La revisaremos." else "No se pudo enviar.",
252:                             Toast.LENGTH_SHORT
253:                         ).show()
254:                     }
255:                 }
256:             }
257:             .setNegativeButton("Cancelar", null)
258:             .show()
259:     }
260: 
261:     companion object {
262:         private const val ARG_UID = "uid"
263:         private const val ARG_PUB = "publicationId"
264:         private const val ARG_PUB_TITLE = "publicationTitle"
265:         fun newInstance(uid: String, publicationId: String = "", publicationTitle: String = "") =
266:             PublicProfileSheet().apply {
267:                 arguments = androidx.core.os.bundleOf(
268:                     ARG_UID to uid,
269:                     ARG_PUB to publicationId,
270:                     ARG_PUB_TITLE to publicationTitle
271:                 )
272:             }
273:     }
274: }

(End of file - total 274 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\ProfileRepository.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\ProfileRepository.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.data.repository
2: 
3: import com.google.android.gms.tasks.Tasks
4: import com.google.firebase.firestore.FieldValue
5: import com.google.firebase.firestore.FirebaseFirestore
6: import com.google.firebase.firestore.SetOptions
7: import com.proyecto.chambaya.data.model.Genders
8: import com.proyecto.chambaya.data.model.OficioCatalog
9: import com.proyecto.chambaya.data.model.PeruLocations
10: import com.proyecto.chambaya.data.model.ProfileCompletion
11: import com.proyecto.chambaya.data.model.ProfileDraft
12: import com.proyecto.chambaya.data.model.EmployerDraft
13: import com.proyecto.chambaya.data.model.IdentityDocumentTypes
14: import com.proyecto.chambaya.data.model.UserRoles
15: import com.proyecto.chambaya.data.model.ProfileLimits
16: import com.proyecto.chambaya.data.model.ProfilePhotoSources
17: import com.proyecto.chambaya.data.model.UserProfile
18: import com.proyecto.chambaya.data.model.ValidatedIdentity
19: import com.proyecto.chambaya.data.model.normalizarUsername
20: import com.proyecto.chambaya.data.model.toPublicProfile
21: import com.proyecto.chambaya.data.model.PublicProfile
22: import com.proyecto.chambaya.data.model.toUserProfile
23: import com.proyecto.chambaya.data.remote.CloudinaryUploader
24: import com.proyecto.chambaya.data.remote.IdentityValidationResult
25: import com.proyecto.chambaya.data.remote.IdentityValidationService
26: import kotlinx.coroutines.Dispatchers
27: import kotlinx.coroutines.withContext
28: 
29: /**
30:  * FASE 2 — Repositorio del perfil del usuario (`users/{uid}`).
31:  *
32:  * Complementa a [RegistrationRepository] (FASE 1) sin tocarlo: aquel crea el
33:  * documento y es el dueño de `uid`, `auth`, `identity` y `registrationStatus`;
34:  * este solo completa y lee los bloques de la FASE 2.
35:  *
36:  * ```
37:  * users/{uid}
38:  *   profile  -> + username, usernameNormalized, phone, bio, district,
39:  *               province, department, birthDate, gender, profilePhotoPath
40:  *   worker   -> enabled, experienceYears, specialties, skills, profileCompleted
41:  *   privacy  -> showPhone, showExactAddress, showEmail
42:  *   statistics -> contadores de la plataforma
43:  * ```
44:  *
45:  * Guaranteías:
46:  *  - **Actualización parcial**: solo se escriben los campos del borrador. Si el
47:  *    usuario no tocó un campo, el valor que ya estaba en Firestore se conserva.
48:  *  - **Nada de la FASE 1 se puede tocar**: `uid`, `auth`, `identity`,
49:  *    `registrationStatus` y `emailVerified`/`otpVerified` no aparecen jamás en
50:  *    un `update()` de esta clase.
51:  *  - **Reputación protegida**: `workCount`, `ratingAverage` y `ratingCount` se
52:  *    copian tal cual; los calcula la plataforma, no el usuario.
53:  *  - **`profileCompleted` es derivado**: se recalcula con [computeCompletion]
54:  *    antes de guardar, así que nunca puede contradecir los datos guardados.
55:  *  - **Username único y normalizado**: se reserva un documento en
56:  *    `usernames/{usernameNormalized}` mediante una transacción, de modo que dos
57:  *    usuarios no puedan tomar el mismo `@usuario` a la vez.
58:  */
59: class ProfileRepository(
60:     private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
61:     private val identityService: IdentityValidationService = IdentityValidationService()
62: ) {
63: 
64:     // ═══════════════════════════════════════════════════════════════
65:     //  LECTURA
66:     // ═══════════════════════════════════════════════════════════════
67: 
68:     /** Lee `users/{uid}` y devuelve el perfil completo de la FASE 2. */
69:     suspend fun loadProfile(uid: String): Result<UserProfile> = withContext(Dispatchers.IO) {
70:         runCatching {
71:             Tasks.await(firestore.collection(COLLECTION_USERS).document(uid).get()).toUserProfile()
72:         }
73:     }
74: 
75:     /**
76:      * Lee el PERFIL PÚBLICO de cualquier usuario (`public_profiles/{uid}`).
77:      *
78:      * A diferencia de [loadProfile] (solo el dueño por reglas), este sí puede
79:      * usarse para mostrar contratantes y trabajadores ajenos: el documento
80:      * solo lleva datos públicos.
81:      */
82:     suspend fun loadPublicProfile(uid: String): Result<PublicProfile> =
83:         withContext(Dispatchers.IO) {
84:             runCatching {
85:                 if (uid.isBlank()) throw IllegalArgumentException("Usuario no válido.")
86:                 Tasks.await(firestore.collection(COLLECTION_PUBLIC).document(uid).get())
87:                     .takeIf { it.exists() }?.toPublicProfile()
88:                     ?: throw NoSuchElementException("Perfil no disponible.")
89:             }
90:         }
91: 
92:     /**
93:      * Replica los datos públicos de `users/{uid}` a `public_profiles/{uid}`.
94:      *
95:      * Se llama (mejor esfuerzo) al guardar perfil, activar roles y enlazar
96:      * lugar. Si el documento público no existe, lo crea.
97:      */
98:     suspend fun syncPublicProfile(uid: String): Result<Unit> =
99:         withContext(Dispatchers.IO) {
100:             runCatching {
101:                 if (uid.isBlank()) return@runCatching
102:                 val perfil = Tasks.await(
103:                     firestore.collection(COLLECTION_USERS).document(uid).get()
104:                 ).toUserProfile()
105:                 Tasks.await(
106:                     firestore.collection(COLLECTION_PUBLIC).document(uid)
107:                         .set(perfil.toPublicDoc(), SetOptions.merge())
108:                 )
109:                 Unit
110:             }
111:         }
112: 
113:     /** Documento público derivado del perfil privado (sin datos sensibles). */
114:     private fun UserProfile.toPublicDoc(): Map<String, Any?> = mapOf(
115:         "uid" to uid,
116:         "fullName" to profile.fullName,
117:         "username" to profile.username,
118:         "photoUrl" to profile.profilePhotoUrl,
119:         "bio" to profile.bio,
120:         "district" to profile.district,
121:         "province" to profile.province,
122:         "identityVerified" to identity.identityVerified,
123:         "worker" to mapOf(
124:             "enabled" to worker.enabled,
125:             "experienceYears" to worker.experienceYears,
126:             "specialties" to worker.specialties,
127:             "skills" to worker.skills
128:         ),
129:         "employer" to mapOf(
130:             "enabled" to employer.enabled,
131:             "employerType" to employer.employerType,
132:             "businessName" to employer.businessName,
133:             "commercialName" to employer.commercialName,
134:             "sector" to employer.sector,
135:             "workplaceId" to employer.workplaceId
136:         ),
137:         "updatedAt" to FieldValue.serverTimestamp()
138:     )
139: 
140:     /**
141:      * Crea los bloques de la FASE 2 que todavía no existen.
142:      *
143:      * Las cuentas creadas en la FASE 1 no tienen `worker`, `privacy` ni
144:      * `statistics`, y el `username` puede estar vacío. Esta función los completa
145:      * una sola vez, con valores neutros y un `@usuario` derivado del nombre.
146:      *
147:      * Es idempotente y de escritura mínima: si el documento ya está completo
148:      * no escribe nada, y si le falta algo solo manda ese algo. Por eso entrar
149:      * al perfil no necesita permiso de escritura en cada visita.
150:      *
151:      * Y es "a mejor hacer": [UserProfile] se construye tolerando bloques
152:      * ausentes, así que la pantalla puede pintar el perfil aunque esta escritura
153:      * sea rechazada (reglas sin desplegar, red, ...). Quien la llama debe
154:      * tratar el fallo como informativo, nunca como error fatal.
155:      */
156:     suspend fun ensureProfileInitialized(uid: String): Result<UserProfile> =
157:         withContext(Dispatchers.IO) {
158:             runCatching {
159:                 val ref = firestore.collection(COLLECTION_USERS).document(uid)
160:                 val actual = Tasks.await(ref.get())
161:                 val perfil = actual.toUserProfile()
162: 
163:                 val employerNecesario = perfil.activeRole == UserRoles.CONTRATANTE && actual.get("employer") !is Map<*, *>
164:                 if (perfil.tieneBloquesFase2 && !employerNecesario) return@runCatching perfil
165: 
166:                 // `username` se guarda SIEMPRE normalizado, para que coincida
167:                 // con `usernameNormalized` y con la clave de `usernames/`.
168:                 val username = normalizarUsername(perfil.profile.username)
169:                     .takeIf { esUsernameValido(it) }
170:                     ?: generarUsername(uid, perfil.profile.fullName)
171: 
172:                 val cambios = mutableMapOf<String, Any?>()
173: 
174:                 // `profile` se completa campo a campo para no pisar el nombre ni
175:                 // la foto que puso el registro, y solo se envía lo que cambia.
176:                 val actuales = actual.get("profile") as? Map<*, *> ?: emptyMap<Any, Any>()
177:                 mapOf(
178:                     "username" to username,
179:                     "usernameNormalized" to normalizarUsername(username),
180:                     "phone" to perfil.profile.phone,
181:                     "bio" to perfil.profile.bio,
182:                     "district" to perfil.profile.district,
183:                     "province" to perfil.profile.province,
184:                     "department" to perfil.profile.department,
185:                     "birthDate" to perfil.profile.birthDate,
186:                     "gender" to perfil.profile.gender,
187:                     "country" to perfil.profile.country
188:                 ).forEach { (campo, valor) ->
189:                     if (actuales[campo] != valor) cambios["profile.$campo"] = valor
190:                 }
191: 
192:                 if (actual.get("worker") !is Map<*, *>) {
193:                     cambios["worker"] = mapOf(
194:                         "enabled" to (perfil.roles.contains(UserRoles.TRABAJADOR)),
195:                         "experienceYears" to 0,
196:                         "specialties" to emptyList<String>(),
197:                         "skills" to emptyList<String>(),
198:                         "workCount" to 0,
199:                         "ratingAverage" to 0.0,
200:                         "ratingCount" to 0,
201:                         "profileCompleted" to 0
202:                     )
203:                 }
204: 
205:                 if (actual.get("privacy") !is Map<*, *>) {
206:                     cambios["privacy"] = mapOf(
207:                         "showPhone" to false,
208:                         "showExactAddress" to false,
209:                         "showEmail" to false
210:                     )
211:                 }
212: 
213:                 if (actual.get("statistics") !is Map<*, *>) {
214:                     cambios["statistics"] = mapOf(
215:                         "applicationsCount" to 0,
216:                         "publicationsCount" to 0,
217:                         "completedJobsCount" to 0,
218:                         "savedPublicationsCount" to 0,
219:                         "receivedRatingsCount" to 0
220:                     )
221:                 }
222: 
223:                 // Las cuentas que se registraron originalmente como CONTRATANTE
224:                 // llegan con `roles=["CONTRATANTE"]` y no necesitan una segunda
225:                 // cuenta: sembramos su bloque employer desde la identidad ya
226:                 // verificada en FASE 1.
227:                 if (perfil.activeRole == UserRoles.CONTRATANTE && actual.get("employer") !is Map<*, *>) {
228:                     val identidad = perfil.identity
229:                     val tipo = if (identidad.documentType == IdentityDocumentTypes.RUC) "EMPRESA" else "PERSONA"
230:                     cambios["employer"] = mapOf(
231:                         "enabled" to true,
232:                         "employerType" to tipo,
233:                         "businessName" to identidad.identityName.ifBlank { perfil.profile.fullName },
234:                         "commercialName" to "",
235:                         "sector" to "",
236:                         "documentType" to identidad.documentType,
237:                         "documentNumber" to identidad.documentNumber,
238:                         "ruc" to identidad.documentNumber.takeIf {
239:                             identidad.documentType == IdentityDocumentTypes.RUC
240:                         },
241:                         "identityName" to identidad.identityName.ifBlank { perfil.profile.fullName },
242:                         "workplaceId" to null,
243:                         "publishedCount" to 0,
244:                         "hiredCount" to 0,
245:                         "ratingAverage" to 0.0,
246:                         "ratingCount" to 0
247:                     )
248:                 }
249: 
250:                 cambios["updatedAt"] = FieldValue.serverTimestamp()
251: 
252:                 Tasks.await(ref.update(cambios))
253:                 runCatching { syncPublicProfile(uid) }
254:                 Tasks.await(ref.get()).toUserProfile()
255:             }
256:         }
257: 
258:     // ═══════════════════════════════════════════════════════════════
259:     //  FASE 3 — CONTRATANTE / CAMBIO DE MODO
260:     // ═══════════════════════════════════════════════════════════════
261: 
262:     /**
263:      * Activa CONTRATANTE sin crear otra cuenta. La información de `worker` no
264:      * se toca. Si el usuario ya tenía un perfil employer, se actualizan solo
265:      * sus datos de contratante y se conserva el mismo uid.
266:      *
267:      * Requisitos previos (FASE 3):
268:      *  - Identidad verificada (DNI/RUC con RENIEC/SUNAT)
269:      *  - Correo verificado
270:      *  - Perfil básico completo (nombre, @usuario, teléfono)
271:      *
272:      * Validación inteligente:
273:      *  - Si el documento del employer coincide con el de la identidad verificada,
274:      *    no se hace llamada HTTP a RENIEC/SUNAT (ya está validado).
275:      *  - Si es diferente, se valida externamente antes de guardar.
276:      */
277:     suspend fun activateContractor(
278:         uid: String,
279:         draft: EmployerDraft
280:     ): Result<UserProfile> = withContext(Dispatchers.IO) {
281:         runCatching {
282:             require(draft.documentType == IdentityDocumentTypes.DNI ||
283:                 draft.documentType == IdentityDocumentTypes.RUC) {
284:                 "Selecciona DNI o RUC."
285:             }
286:             val cleanDocument = draft.documentNumber.filter(Char::isDigit)
287:             require(
288:                 (draft.documentType == IdentityDocumentTypes.DNI && cleanDocument.length == 8) ||
289:                     (draft.documentType == IdentityDocumentTypes.RUC && cleanDocument.length == 11)
290:             ) {
291:                 if (draft.documentType == IdentityDocumentTypes.DNI)
292:                     "El DNI debe tener 8 dígitos."
293:                 else
294:                     "El RUC debe tener 11 dígitos."
295:             }
296: 
297:             val ref = firestore.collection(COLLECTION_USERS).document(uid)
298:             val actual = Tasks.await(ref.get()).toUserProfile()
299:             require(actual.uid == uid) { "La cuenta no es válida." }
300: 
301:             // ═══════════════════════════════════════════════════════════════
302:             //  REQUISITOS PREVIOS (FASE 3)
303:             // ═══════════════════════════════════════════════════════════════
304:             require(actual.identity.identityVerified) {
305:                 "Tu identidad debe estar verificada para activar el modo contratante."
306:             }
307:             require(actual.auth.emailVerified) {
308:                 "Tu correo debe estar verificado para activar el modo contratante."
309:             }
310:             require(actual.profile.fullName.isNotBlank() && actual.profile.username.isNotBlank()) {
311:                 "Completa tu perfil básico antes de activar el modo contratante."
312:             }
313:             require(actual.profile.phone.isNotBlank()) {
314:                 "Agrega un teléfono a tu perfil antes de activar el modo contratante."
315:             }
316: 
317:             // ═══════════════════════════════════════════════════════════════
318:             //  VALIDACIÓN INTELIGENTE
319:             // ═══════════════════════════════════════════════════════════════
320:             // Si el documento del employer coincide con el de la identidad
321:             // verificada, ya está validado: no hace falta llamada HTTP.
322:             val documentoCoincide = actual.identity.documentType == draft.documentType &&
323:                 actual.identity.documentNumber == cleanDocument
324: 
325:             val identityName = if (documentoCoincide) {
326:                 actual.identity.identityName
327:             } else {
328:                 // Documento diferente: validar externamente
329:                 val identidad = when (draft.documentType) {
330:                     IdentityDocumentTypes.RUC -> identityService.validateRuc(cleanDocument)
331:                     else -> identityService.validateDni(cleanDocument)
332:                 }
333:                 when (identidad) {
334:                     is IdentityValidationResult.Success -> identidad.identity.fullName
335:                     is IdentityValidationResult.Rejected ->
336:                         throw IllegalArgumentException(identidad.message)
337:                     is IdentityValidationResult.ServiceError ->
338:                         throw IllegalArgumentException(
339:                             "El servicio de identidad no está disponible (HTTP ${identidad.httpCode})."
340:                         )
341:                     is IdentityValidationResult.NetworkError ->
342:                         throw IllegalArgumentException("No hay conexión para verificar el documento.")
343:                 }
344:             }
345: 
346:             val roles = linkedSetOf<String>()
347:             roles += UserRoles.TRABAJADOR
348:             roles += UserRoles.CONTRATANTE
349: 
350:             val employer = mapOf(
351:                 "enabled" to true,
352:                 "employerType" to draft.employerType,
353:                 "businessName" to draft.businessName.trim(),
354:                 "commercialName" to draft.commercialName.trim(),
355:                 "sector" to draft.sector.trim(),
356:                 "documentType" to draft.documentType,
357:                 "documentNumber" to cleanDocument,
358:                 "documentNumberMasked" to ValidatedIdentity.maskDocumentNumber(cleanDocument),
359:                 "ruc" to draft.ruc?.filter(Char::isDigit)?.takeIf { it.isNotBlank() },
360:                 "identityName" to identityName,
361:                 "workplaceId" to actual.employer.workplaceId,
362:                 "publishedCount" to actual.employer.publishedCount,
363:                 "hiredCount" to actual.employer.hiredCount,
364:                 "ratingAverage" to actual.employer.ratingAverage,
365:                 "ratingCount" to actual.employer.ratingCount
366:             )
367: 
368:             Tasks.await(
369:                 ref.update(
370:                     mapOf(
371:                         "roles" to roles.toList(),
372:                         "activeRole" to UserRoles.CONTRATANTE,
373:                         "employer" to employer,
374:                         "updatedAt" to FieldValue.serverTimestamp()
375:                     )
376:                 )
377:             )
378:             runCatching { syncPublicProfile(uid) }
379:             Tasks.await(ref.get()).toUserProfile()
380:         }
381:     }
382: 
383:     /** Cambia el modo activo sin borrar ningún perfil. */
384:     suspend fun switchActiveRole(uid: String, role: String): Result<UserProfile> =
385:         withContext(Dispatchers.IO) {
386:             runCatching {
387:                 require(UserRoles.isValid(role)) { "Rol no válido." }
388:                 val ref = firestore.collection(COLLECTION_USERS).document(uid)
389:                 val snapshot = Tasks.await(ref.get())
390:                 val roles = snapshot.get("roles") as? List<*>
391:                 require(roles?.contains(role) == true) {
392:                     "Ese modo todavía no está activado."
393:                 }
394:                 if (role == UserRoles.CONTRATANTE) {
395:                     val employer = snapshot.get("employer") as? Map<*, *>
396:                     require(employer?.get("enabled") == true) {
397:                         "Completa el perfil de contratante antes de cambiar de modo."
398:                     }
399:                 }
400:                 Tasks.await(
401:                     ref.update(
402:                         mapOf(
403:                             "activeRole" to role,
404:                             "updatedAt" to FieldValue.serverTimestamp()
405:                         )
406:                     )
407:                 )
408:                 Tasks.await(ref.get()).toUserProfile()
409:             }
410:         }
411: 
412:     /**
413:      * Activa TRABAJADOR por primera vez en una cuenta que nació contratante.
414:      *
415:      * Espejo de [activateContractor] pero sin validación de identidad: el
416:      * trabajador no declara documentos, solo completa su perfil en el wizard
417:      * (paso 3 = experiencia). Si el rol ya existe, solo cambia el modo.
418:      *
419:      * Al cambiar de rol el porcentaje se recalcula solo: los checks de
420:      * trabajador y de contratante son distintos ([ProfileCompletion]), así
421:      * que el primer cambio casi siempre baja el % hasta completar lo nuevo.
422:      */
423:     suspend fun activateWorker(uid: String): Result<UserProfile> =
424:         withContext(Dispatchers.IO) {
425:             runCatching {
426:                 val ref = firestore.collection(COLLECTION_USERS).document(uid)
427:                 val snapshot = Tasks.await(ref.get())
428:                 val actual = snapshot.toUserProfile()
429:                 require(actual.uid == uid) { "La cuenta no es válida." }
430: 
431:                 if (actual.roles.contains(UserRoles.TRABAJADOR)) {
432:                     return@runCatching switchActiveRole(uid, UserRoles.TRABAJADOR).getOrThrow()
433:                 }
434:                 require(actual.roles.contains(UserRoles.CONTRATANTE)) {
435:                     "Rol actual no válido."
436:                 }
437: 
438:                 val roles = linkedSetOf(UserRoles.TRABAJADOR, UserRoles.CONTRATANTE)
439:                 val cambios = mutableMapOf<String, Any?>(
440:                     "roles" to roles.toList(),
441:                     "activeRole" to UserRoles.TRABAJADOR,
442:                     "updatedAt" to FieldValue.serverTimestamp()
443:                 )
444:                 // Si el documento no trae bloque `worker` se crea en cero
445:                 // (reputación intacta); si lo trae, solo se habilita.
446:                 if (snapshot.get("worker") !is Map<*, *>) {
447:                     cambios["worker"] = mapOf(
448:                         "enabled" to true,
449:                         "experienceYears" to 0,
450:                         "experienceDeclared" to false,
451:                         "specialties" to emptyList<String>(),
452:                         "skills" to emptyList<String>(),
453:                         "workCount" to 0,
454:                         "ratingAverage" to 0.0,
455:                         "ratingCount" to 0,
456:                         "profileCompleted" to 0
457:                     )
458:                 } else {
459:                     cambios["worker.enabled"] = true
460:                 }
461:                 Tasks.await(ref.update(cambios))
462:                 runCatching { syncPublicProfile(uid) }
463:                 Tasks.await(ref.get()).toUserProfile()
464:             }
465:         }
466: 
467:     // ═══════════════════════════════════════════════════════════════
468:     //  UNICIDAD DEL USERNAME
469:     // ═══════════════════════════════════════════════════════════════
470: 
471:     /**
472:      * ¿El `@usuario` está libre?
473:      *
474:      * Siempre disponible para el propio usuario: si el documento de
475:      * `usernames/{normalizado}` le pertenece, puede volver a usarlo.
476:      */
477:     suspend fun isUsernameAvailable(username: String, uid: String): Boolean =
478:         withContext(Dispatchers.IO) {
479:             val normalizado = normalizarUsername(username)
480:             if (normalizado.length < ProfileLimits.USERNAME_MIN) return@withContext false
481:             runCatching {
482:                 val ref = firestore.collection(COLLECTION_USERNAMES).document(normalizado)
483:                 val snapshot = Tasks.await(ref.get())
484:                 !snapshot.exists() || snapshot.getString("uid") == uid
485:             }.getOrDefault(false)
486:         }
487: 
488:     /**
489:      * Reserva `usernames/{normalizado}` para [uid].
490:      *
491:      * La transacción es la que garantiza la unicidad: dos personas que elijan el
492:      * mismo nombre al mismo tiempo, solo una consigue el `create`.
493:      */
494:     private suspend fun reservarUsername(normalizado: String, uid: String, visible: String) {
495:         val ref = firestore.collection(COLLECTION_USERNAMES).document(normalizado)
496:         Tasks.await(
497:             firestore.runTransaction { transaction ->
498:                 val snapshot = transaction.get(ref)
499:                 if (snapshot.exists() && snapshot.getString("uid") != uid) {
500:                     throw UsernameYaTomado(normalizado)
501:                 }
502:                 transaction.set(
503:                     ref,
504:                     mapOf(
505:                         "uid" to uid,
506:                         "username" to visible,
507:                         "updatedAt" to FieldValue.serverTimestamp()
508:                     )
509:                 )
510:                 null
511:             }
512:         )
513:     }
514: 
515:     /** Libera el `@usuario` anterior cuando el usuario lo cambia. */
516:     private suspend fun liberarUsername(normalizado: String, uid: String) {
517:         if (normalizado.isBlank()) return
518:         val ref = firestore.collection(COLLECTION_USERNAMES).document(normalizado)
519:         runCatching {
520:             val snapshot = Tasks.await(ref.get())
521:             if (snapshot.exists() && snapshot.getString("uid") == uid) {
522:                 Tasks.await(ref.delete())
523:             }
524:         }
525:     }
526: 
527:     /**
528:      * Propone un `@usuario` a partir del nombre: "Maria Sanchez" -> "maria_sanchez".
529:      *
530:      * Si ya está tomado se le añade un sufijo corto del `uid` para que siempre
531:      * quede algo disponible sin preguntar nada.
532:      */
533:     private suspend fun generarUsername(uid: String, nombreCompleto: String): String {
534:         val base = normalizarUsername(nombreCompleto).take(ProfileLimits.USERNAME_MAX - 5)
535:         val candidatos = listOf(
536:             base.ifBlank { "chambaya" },
537:             "${base.ifBlank { "chambaya" }}_${uid.take(4).lowercase()}",
538:             "usuario_${uid.take(6).lowercase()}"
539:         )
540:         return candidatos.firstOrNull { esUsernameValido(it) && isUsernameAvailable(it, uid) }
541:             ?: "usuario_${uid.take(8).lowercase()}"
542:     }
543: 
544:     /**
545:      * ¿El `@usuario` ya normalizado cumple lo que piden las Rules?
546:      *
547:      * El rango de caracteres es `a-z 0-9 _ .`, el mismo `^[a-z0-9._]+$` de
548:      * `isValidUsername` en `firestore.rules`. Si la app fuera más laxa que las
549:      * Rules, el guardado se denegaría en el servidor en vez de fallar aquí.
550:      */
551:     private fun esUsernameValido(value: String) =
552:         value.length in ProfileLimits.USERNAME_MIN..ProfileLimits.USERNAME_MAX &&
553:             value.all { it in 'a'..'z' || it in '0'..'9' || it == '_' || it == '.' }
554: 
555:     // ═══════════════════════════════════════════════════════════════
556:     //  ESCRITURA
557:     // ═══════════════════════════════════════════════════════════════
558: 
559:     /**
560:      * Guarda el borrador del wizard de edición.
561:      *
562:      * Orden de las operaciones (por si algo falla a medias):
563:      *  1. se validan los datos,
564:      *  2. se reserva el `@usuario` nuevo (transacción),
565:      *  3. se escribe el documento con `update()` parcial,
566:      *  4. se libera el `@usuario` viejo.
567:      *
568:      * Devuelve el perfil ya guardado, con el `profileCompleted` recalculado.
569:      */
570:     suspend fun saveProfile(
571:         uid: String,
572:         draft: ProfileDraft,
573:         photoUrl: String? = null,
574:         photoPublicId: String? = null
575:     ): Result<UserProfile> = withContext(Dispatchers.IO) {
576:         runCatching {
577:             val ref = firestore.collection(COLLECTION_USERS).document(uid)
578:             val anterior = Tasks.await(ref.get())
579:             val previo = anterior.toUserProfile()
580: 
581:             val username = normalizarUsername(draft.username)
582:             require(esUsernameValido(username)) { "El nombre de usuario no es válido." }
583: 
584:             val anteriorNormalized = previo.profile.usernameNormalized
585:             val cambiaUsername = username != anteriorNormalized
586: 
587:             // 1) Unicidad: si el @usuario cambió, hay que tomar el documento nuevo
588:             //    ANTES de escribir el perfil, para no dejar el perfil apuntando a
589:             //    un nombre que otro usuario ya tomó.
590:             if (cambiaUsername) {
591:                 reservarUsername(username, uid, draft.username.trim())
592:             }
593: 
594:             // 2) Actualización parcial: se escriben rutas con punto, así
595:             //    Firestore solo toca esas hojas y conserva el resto.
596:             val cambios = mutableMapOf<String, Any?>(
597:                 "profile.fullName" to draft.fullName.trim(),
598:                 "profile.username" to username,
599:                 "profile.usernameNormalized" to username,
600:                 "profile.phone" to draft.phone.trim(),
601:                 "profile.bio" to draft.bio.trim(),
602:                 "profile.birthDate" to draft.birthDate.trim(),
603:                 "profile.gender" to draft.gender,
604:                 "profile.district" to draft.district.trim(),
605:                 "profile.province" to draft.province.trim(),
606:                 "profile.department" to draft.department.trim(),
607: 
608:                 "worker.enabled" to draft.workerEnabled,
609:                 "worker.experienceYears" to draft.experienceYears,
610:                 // Distingue "escribí 0 años" de "nunca toqué el campo": sin esto,
611:                 // el que empieza de cero no podía llegar nunca al 100 %.
612:                 "worker.experienceDeclared" to draft.experienceDeclared,
613:                 "worker.specialties" to draft.specialties,
614:                 "worker.skills" to draft.skills,
615: 
616:                 // El paso 4 del wizard (qué contacto se muestra) también se
617:                 // guarda: sin esto los interruptores de privacidad se perdían.
618:                 "privacy.showPhone" to draft.showPhone,
619:                 "privacy.showEmail" to draft.showEmail,
620:                 "privacy.showExactAddress" to draft.showExactAddress
621:             )
622: 
623:             // La foto solo se escribe si se subió una nueva: si no, se conserva.
624:             if (!photoUrl.isNullOrBlank() && !photoPublicId.isNullOrBlank()) {
625:                 cambios["profile.profilePhotoUrl"] = photoUrl
626:                 cambios["profile.profilePhotoPublicId"] = photoPublicId
627:                 cambios["profile.profilePhotoSource"] = ProfilePhotoSources.CUSTOM
628:                 // `profilePhotoPath` documenta la carpeta de Cloudinary. Se pide la
629:                 // misma función que usa la subida en vez de escribir la ruta aquí:
630:                 // esa carpeta es la que valida `isOwnCloudinaryPhoto` en las Rules,
631:                 // así que si las dos se desincronizan el guardado se deniega.
632:                 cambios["profile.profilePhotoPath"] = CloudinaryUploader.carpetaDePerfil(uid)
633:             }
634: 
635:             // 3) `profileCompleted` se deriva del perfil resultante, nunca se
636:             //    envía lo que "_dice_" el formulario.
637:             cambios["worker.profileCompleted"] = calcularCompletitud(cambios, previo)
638: 
639:             cambios["updatedAt"] = FieldValue.serverTimestamp()
640:             Tasks.await(ref.update(cambios))
641: 
642:             // 4) El @usuario viejo ya no lo necesita nadie.
643:             if (cambiaUsername) {
644:                 liberarUsername(anteriorNormalized, uid)
645:             }
646: 
647:             // 5) Réplica pública (mejor esfuerzo, no bloquea el guardado).
648:             runCatching { syncPublicProfile(uid) }
649: 
650:             Tasks.await(ref.get()).toUserProfile()
651:         }
652:     }
653: 
654:     /**
655:      * Calcula el `profileCompleted` que corresponde a [cambios] aplicado sobre
656:      * [previo].
657:      *
658:      * Se construye un perfil en memoria con los valores nuevos y se usa el
659:      * mismo [ProfileCompletion] que luego pintarán las pantallas, de modo que el
660:      * número guardado y el número mostrado salen del mismo cálculo.
661:      */
662:     private fun calcularCompletitud(
663:         cambios: Map<String, Any?>,
664:         previo: UserProfile
665:     ): Int {
666:         val perfil = previo.copy(
667:             profile = previo.profile.copy(
668:                 fullName = (cambios["profile.fullName"] as? String) ?: previo.profile.fullName,
669:                 username = (cambios["profile.username"] as? String) ?: previo.profile.username,
670:                 phone = (cambios["profile.phone"] as? String) ?: previo.profile.phone,
671:                 bio = (cambios["profile.bio"] as? String) ?: previo.profile.bio,
672:                 birthDate = (cambios["profile.birthDate"] as? String) ?: previo.profile.birthDate,
673:                 gender = (cambios["profile.gender"] as? String) ?: previo.profile.gender,
674:                 district = (cambios["profile.district"] as? String) ?: previo.profile.district,
675:                 province = (cambios["profile.province"] as? String) ?: previo.profile.province,
676:                 department = (cambios["profile.department"] as? String) ?: previo.profile.department,
677:                 profilePhotoUrl = (cambios["profile.profilePhotoUrl"] as? String)
678:                     ?: previo.profile.profilePhotoUrl
679:             ),
680:             worker = previo.worker.copy(
681:                 enabled = cambios["worker.enabled"] as? Boolean ?: previo.worker.enabled,
682:                 experienceYears = (cambios["worker.experienceYears"] as? Int)
683:                     ?: previo.worker.experienceYears,
684:                 experienceDeclared = cambios["worker.experienceDeclared"] as? Boolean
685:                     ?: previo.worker.experienceDeclared,
686:                 specialties = cambios.listaDeTextos("worker.specialties")
687:                     ?: previo.worker.specialties,
688:                 skills = cambios.listaDeTextos("worker.skills") ?: previo.worker.skills
689:             ),
690:             privacy = previo.privacy.copy(
691:                 showPhone = cambios["privacy.showPhone"] as? Boolean ?: previo.privacy.showPhone,
692:                 showEmail = cambios["privacy.showEmail"] as? Boolean ?: previo.privacy.showEmail,
693:                 showExactAddress = cambios["privacy.showExactAddress"] as? Boolean
694:                     ?: previo.privacy.showExactAddress
695:             )
696:         )
697:         return perfil.completion().percent
698:     }
699: 
700:     /**
701:      * Lee una lista de textos de los cambios pendientes.
702:      *
703:      * El mapa de cambios es `Map<String, Any?>` porque se arma para Firestore, así
704:      * que la lista llega como `List<*>`: se filtra elemento a elemento en vez de
705:      * hacer un cast directo, que sería unchecked.
706:      */
707:     private fun Map<String, Any?>.listaDeTextos(key: String): List<String>? =
708:         (this[key] as? List<*>)?.mapNotNull { it?.toString() }
709: 
710:     /** Calcula la completitud sin escribir nada (para el resumen del paso 4). */
711:     fun computeCompletion(perfil: UserProfile): ProfileCompletion = perfil.completion()
712: 
713:     /**
714:      * Propone un `@usuario` para un perfil que todavía no tiene uno.
715:      *
716:      * Las cuentas de la FASE 1 nacen sin `username` y las Rules exigen uno válido
717:      * para cualquier escritura de la FASE 2. El asistente de edición lo necesita
718:      * ya en memoria (para pintar el campo y no bloquear el guardado con un error
719:      * de validación), pero sin tocar Firestore: esta función no consulta
720:      * `usernames/` ni reserva nada, solo normaliza lo que hay.
721:      *
722:      * [ensureProfileInitialized] sigue siendo quien decide el `@usuario` definitivo
723:      * comprobando la disponibilidad real; esta es la versión optimista para la
724:      * pantalla, y si más tarde el otro dice que no, el usuario escribe otro.
725:      */
726:     fun sugerirUsername(perfil: UserProfile): String {
727:         val guardado = normalizarUsername(perfil.profile.username)
728:         if (guardado.length >= ProfileLimits.USERNAME_MIN) return guardado
729: 
730:         val nombre = perfil.profile.fullName.ifBlank { perfil.auth.email.substringBefore("@") }
731:         val base = normalizarUsername(nombre).take(ProfileLimits.USERNAME_MAX - 5)
732:         val candidatos = listOf(
733:             base.ifBlank { "chambaya" },
734:             "${base.ifBlank { "chambaya" }}_${perfil.uid.take(4).lowercase()}",
735:             "usuario_${perfil.uid.take(6).lowercase()}"
736:         )
737:         return candidatos.firstOrNull { esUsernameValido(it) }
738:             ?: "usuario_${perfil.uid.take(8).lowercase()}"
739:     }
740: 
741:     // ═══════════════════════════════════════════════════════════════
742:     //  VALIDACIÓN (la misma que aplican las Firestore Security Rules)
743:     // ═══════════════════════════════════════════════════════════════
744: 
745:     /** Errores de validación de un borrador. Vacío = válido. */
746:     fun validate(draft: ProfileDraft): List<String> {
747:         val errores = mutableListOf<String>()
748: 
749:         if (draft.fullName.trim().length < 3) {
750:             errores += "Ingresa tu nombre completo (mínimo 3 caracteres)."
751:         }
752: 
753:         val username = normalizarUsername(draft.username)
754:         if (username.length !in ProfileLimits.USERNAME_MIN..ProfileLimits.USERNAME_MAX) {
755:             errores += "El @usuario debe tener entre ${ProfileLimits.USERNAME_MIN} y " +
756:                 "${ProfileLimits.USERNAME_MAX} caracteres."
757:         }
758: 
759:         // El teléfono es opcional (el registro no lo pide): si se escribe, tiene que
760:         // ser válido. Es la misma regla que `isValidPhone` en las Rules.
761:         val digitos = draft.phone.filter { it.isDigit() }
762:         if (digitos.isNotEmpty() && digitos.length !in ProfileLimits.PHONE_MIN..ProfileLimits.PHONE_MAX) {
763:             errores += "El teléfono debe tener entre ${ProfileLimits.PHONE_MIN} y " +
764:                 "${ProfileLimits.PHONE_MAX} dígitos."
765:         }
766: 
767:         if (draft.bio.trim().length > ProfileLimits.BIO_MAX) {
768:             errores += "La descripción no puede pasar de ${ProfileLimits.BIO_MAX} caracteres."
769:         }
770: 
771:         if (draft.gender.isNotBlank() && draft.gender !in Genders.ALL) {
772:             errores += "El género seleccionado no es válido."
773:         }
774: 
775:         if (draft.experienceYears !in 0..ProfileLimits.EXPERIENCE_MAX) {
776:             errores += "Los años de experiencia no son válidos."
777:         }
778: 
779:         if (draft.specialties.size > ProfileLimits.SPECIALTY_MAX) {
780:             errores += "Solo puedes elegir ${ProfileLimits.SPECIALTY_MAX} especialidades."
781:         }
782: 
783:         if (draft.skills.size > ProfileLimits.SKILL_MAX_COUNT) {
784:             errores += "Máximo ${ProfileLimits.SKILL_MAX_COUNT} habilidades."
785:         }
786: 
787:         if (draft.department.isNotBlank() && !PeruLocations.esDepartamentoValido(draft.department)) {
788:             errores += "El departamento seleccionado no existe."
789:         }
790:         if (draft.province.isNotBlank() &&
791:             !PeruLocations.esProvinciaValida(draft.department, draft.province)
792:         ) {
793:             errores += "La provincia no pertenece al departamento elegido."
794:         }
795:         if (draft.district.isNotBlank() &&
796:             !PeruLocations.esDistritoValido(draft.department, draft.province, draft.district)
797:         ) {
798:             errores += "El distrito no es válido."
799:         }
800: 
801:         return errores
802:     }
803: 
804:     /**
805:      * Convierte texto libre en lista de habilidades.
806:      *
807:      * Acepta comas o saltos de línea, quita vacíos y duplicados y corta cada
808:      * habilidad a un largo razonable.
809:      */
810:     fun parseSkills(texto: String): List<String> = texto
811:         .split(',', '\n')
812:         .map { it.trim() }
813:         .filter { it.isNotEmpty() }
814:         .map { it.take(ProfileLimits.SKILL_MAX_LENGTH) }
815:         .distinctBy { it.lowercase() }
816:         .take(ProfileLimits.SKILL_MAX_COUNT)
817: 
818:     /**
819:      * Filtra las especialidades contra el catálogo de oficios.
820:      *
821:      * Así el perfil no puede guardar oficios inventados y se mantiene coherente
822:      * con las categorías de las publicaciones.
823:      */
824:     fun filtrarEspecialidades(context: android.content.Context, candidatas: List<String>): List<String> {
825:         val validas = OficioCatalog.load(context).map { it.categoria }
826:         return candidatas
827:             .map { it.trim() }
828:             .filter { texto ->
829:                 validas.any { OficioCatalog.normalizar(it) == OficioCatalog.normalizar(texto) }
830:             }
831:             .distinctBy { OficioCatalog.normalizar(it) }
832:             .take(ProfileLimits.SPECIALTY_MAX)
833:     }
834: 
835:     companion object {
836:         const val COLLECTION_USERS = "users"
837: 
838:         /** Reserva de unicidad de `@usuario`: `usernames/{usernameNormalized}`. */
839:         const val COLLECTION_USERNAMES = "usernames"
840: 
841:         /** Perfil público visible para todos: `public_profiles/{uid}`. */
842:         const val COLLECTION_PUBLIC = "public_profiles"
843:     }
844: }
845: 
846: /** El `@usuario` ya pertenece a otra cuenta. */
847: class UsernameYaTomado(val username: String) :
848:     Exception("El @usuario $username ya está en uso.")

(End of file - total 848 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\ApplicationRepository.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\ApplicationRepository.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.data.repository
2: 
3: import com.google.android.gms.tasks.Tasks
4: import com.google.firebase.firestore.FieldValue
5: import com.google.firebase.firestore.FirebaseFirestore
6: import com.google.firebase.firestore.Query
7: import com.proyecto.chambaya.data.model.ApplicationStatus
8: import com.proyecto.chambaya.data.model.Job
9: import com.proyecto.chambaya.data.model.JobApplication
10: import com.proyecto.chambaya.data.model.NotificationType
11: import com.proyecto.chambaya.data.model.Publication
12: import com.proyecto.chambaya.data.model.PublicationStatus
13: import com.proyecto.chambaya.data.model.UserProfile
14: import com.proyecto.chambaya.data.model.toJobApplication
15: import com.proyecto.chambaya.data.model.validateApplicationMessage
16: import kotlinx.coroutines.Dispatchers
17: import kotlinx.coroutines.withContext
18: 
19: /**
20:  * FASE 7 — Postulaciones.
21:  *
22:  * El trabajador postula/retira; el contratante acepta/rechaza. Al aceptar
23:  * nace el job (FASE 8), se suma el contratado y se notifica a ambas partes.
24:  */
25: data class DecideResult(
26:     val application: JobApplication,
27:     val job: Job?,
28:     /** true si con esta aceptación se cubrieron todas las vacantes. */
29:     val publicationFilled: Boolean
30: )
31: 
32: class ApplicationRepository(
33:     private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
34:     private val jobs: JobRepository = JobRepository(firestore),
35:     private val publications: PublicationRepository = PublicationRepository(firestore),
36:     private val notifications: NotificationRepository = NotificationRepository(firestore)
37: ) {
38: 
39:     fun newApplicationId(): String =
40:         firestore.collection(COLLECTION).document().id
41: 
42:     /** PENDING existente del trabajador en esa publicación (anti-duplicado). */
43:     suspend fun pendingFor(publicationId: String, workerUid: String): Result<JobApplication?> =
44:         withContext(Dispatchers.IO) {
45:             runCatching {
46:                 if (publicationId.isBlank() || workerUid.isBlank()) return@runCatching null
47:                 Tasks.await(
48:                     firestore.collection(COLLECTION)
49:                         .whereEqualTo("publicationId", publicationId)
50:                         .whereEqualTo("workerUid", workerUid)
51:                         .whereEqualTo("status", ApplicationStatus.PENDING)
52:                         .limit(1)
53:                         .get()
54:                 ).documents.firstOrNull()?.toJobApplication()
55:             }
56:         }
57: 
58:     /** Última postulación del trabajador en esa publicación (cualquier estado). */
59:     suspend fun lastFor(publicationId: String, workerUid: String): Result<JobApplication?> =
60:         withContext(Dispatchers.IO) {
61:             runCatching {
62:                 if (publicationId.isBlank() || workerUid.isBlank()) return@runCatching null
63:                 Tasks.await(
64:                     firestore.collection(COLLECTION)
65:                         .whereEqualTo("publicationId", publicationId)
66:                         .whereEqualTo("workerUid", workerUid)
67:                         .orderBy("createdAt", Query.Direction.DESCENDING)
68:                         .limit(1)
69:                         .get()
70:                 ).documents.firstOrNull()?.toJobApplication()
71:             }
72:         }
73: 
74:     suspend fun apply(
75:         workerUid: String,
76:         publication: Publication,
77:         perfil: UserProfile,
78:         message: String
79:     ): Result<JobApplication> = withContext(Dispatchers.IO) {
80:         runCatching {
81:             val errores = validateApplicationMessage(message)
82:             require(errores.isEmpty()) { errores.first() }
83:             require(workerUid.isNotBlank()) { "Sesión no válida." }
84:             require(workerUid != publication.ownerUid) { "No puedes postularte a tu propia chamba." }
85:             require(publication.status == PublicationStatus.ACTIVE) {
86:                 "Esta chamba ya no acepta postulaciones."
87:             }
88:             require(pendingFor(publication.publicationId, workerUid).getOrThrow() == null) {
89:                 "Ya te postulaste a esta chamba."
90:             }
91:             val ref = firestore.collection(COLLECTION).document()
92:             val now = FieldValue.serverTimestamp()
93:             Tasks.await(
94:                 ref.set(
95:                     mapOf(
96:                         "applicationId" to ref.id,
97:                         "publicationId" to publication.publicationId,
98:                         "publicationTitle" to publication.title,
99:                         "workerUid" to workerUid,
100:                         "employerUid" to publication.ownerUid,
101:                         "status" to ApplicationStatus.PENDING,
102:                         "worker" to mapOf(
103:                             "name" to perfil.profile.fullName,
104:                             "username" to perfil.profile.username,
105:                             "photoUrl" to perfil.profile.profilePhotoUrl,
106:                             "experienceYears" to perfil.worker.experienceYears,
107:                             "ratingAverage" to perfil.worker.ratingAverage,
108:                             "ratingCount" to perfil.worker.ratingCount
109:                         ),
110:                         "message" to message.trim().take(500),
111:                         "createdAt" to now,
112:                         "updatedAt" to now
113:                     )
114:                 )
115:             )
116:             // Contador + aviso al contratante (mejor esfuerzo).
117:             runCatching {
118:                 Tasks.await(
119:                     firestore.collection(PublicationRepository.COLLECTION)
120:                         .document(publication.publicationId)
121:                         .update("statistics.applications", FieldValue.increment(1))
122:                 )
123:             }
124:             notifications.push(
125:                 recipientUid = publication.ownerUid,
126:                 type = NotificationType.NEW_APPLICATION,
127:                 title = "Nueva postulación",
128:                 message = "${perfil.profile.fullName.ifBlank { "Un trabajador" }} se postuló a “${publication.title.take(60)}”.",
129:                 senderUid = workerUid,
130:                 publicationId = publication.publicationId
131:             )
132:             Tasks.await(ref.get()).toJobApplication()
133:         }
134:     }
135: 
136:     suspend fun withdraw(workerUid: String, applicationId: String): Result<JobApplication> =
137:         withContext(Dispatchers.IO) {
138:             runCatching {
139:                 val ref = firestore.collection(COLLECTION).document(applicationId)
140:                 val snap = Tasks.await(ref.get())
141:                 require(snap.exists()) { "La postulación ya no existe." }
142:                 val app = snap.toJobApplication()
143:                 require(app.workerUid == workerUid) { "Esa postulación no es tuya." }
144:                 require(app.status == ApplicationStatus.PENDING) {
145:                     "Solo puedes retirar una postulación pendiente."
146:                 }
147:                 Tasks.await(
148:                     ref.update(
149:                         mapOf(
150:                             "status" to ApplicationStatus.WITHDRAWN,
151:                             "updatedAt" to FieldValue.serverTimestamp()
152:                         )
153:                     )
154:                 )
155:                 Tasks.await(ref.get()).toJobApplication()
156:             }
157:         }
158: 
159:     /**
160:      * Acepta o rechaza. Al aceptar: crea (o reutiliza) el job, suma el
161:      * contratado y finaliza la publicación si se cubrieron las vacantes.
162:      */
163:     suspend fun decide(employerUid: String, applicationId: String, accept: Boolean): Result<DecideResult> =
164:         withContext(Dispatchers.IO) {
165:             runCatching {
166:                 val ref = firestore.collection(COLLECTION).document(applicationId)
167:                 val snap = Tasks.await(ref.get())
168:                 require(snap.exists()) { "La postulación ya no existe." }
169:                 var app = snap.toJobApplication()
170:                 require(app.employerUid == employerUid) { "Esa solicitud no es para ti." }
171:                 require(app.status == ApplicationStatus.PENDING) { "Esta solicitud ya fue decidida." }
172: 
173:                 if (!accept) {
174:                     Tasks.await(
175:                         ref.update(
176:                             mapOf(
177:                                 "status" to ApplicationStatus.REJECTED,
178:                                 "updatedAt" to FieldValue.serverTimestamp()
179:                             )
180:                         )
181:                     )
182:                     app = Tasks.await(ref.get()).toJobApplication()
183:                     notifications.push(
184:                         recipientUid = app.workerUid,
185:                         type = NotificationType.APPLICATION_REJECTED,
186:                         title = "Postulación no seleccionada",
187:                         message = "El contratante eligió a otro especialista para “${app.publicationTitle.take(60)}”.",
188:                         senderUid = employerUid,
189:                         publicationId = app.publicationId
190:                     )
191:                     return@runCatching DecideResult(app, null, false)
192:                 }
193: 
194:                 val pub = publications.getById(app.publicationId).getOrNull()
195:                 require(pub != null) { "La publicación ya no existe." }
196:                 require(pub.ownerUid == employerUid) { "Esa chamba no te pertenece." }
197: 
198:                 // Idempotente: si el job ya existe (reintento), se reutiliza.
199:                 val job = jobs.findByApplication(app.applicationId).getOrNull()
200:                     ?: jobs.create(
201:                         employerUid = employerUid,
202:                         application = app,
203:                         agreedAmount = pub.payment.amount,
204:                         publicationTitle = pub.title
205:                     ).getOrThrow()
206: 
207:                 Tasks.await(
208:                     ref.update(
209:                         mapOf(
210:                             "status" to ApplicationStatus.ACCEPTED,
211:                             "updatedAt" to FieldValue.serverTimestamp()
212:                         )
213:                     )
214:                 )
215:                 app = Tasks.await(ref.get()).toJobApplication()
216: 
217:                 val hired = pub.workersHired + 1
218:                 runCatching {
219:                     Tasks.await(
220:                         firestore.collection(PublicationRepository.COLLECTION)
221:                             .document(pub.publicationId)
222:                             .update("workersHired", hired)
223:                     )
224:                 }
225:                 var filled = false
226:                 if (hired >= pub.workersNeeded) {
227:                     filled = publications.changeStatus(employerUid, pub.publicationId, PublicationStatus.FINISHED)
228:                         .isSuccess
229:                 }
230:                 notifications.push(
231:                     recipientUid = app.workerUid,
232:                     type = NotificationType.APPLICATION_ACCEPTED,
233:                     title = "¡Fuiste seleccionado!",
234:                     message = "Te eligieron para “${app.publicationTitle.take(60)}”. Ponte en contacto para coordinar.",
235:                     senderUid = employerUid,
236:                     publicationId = app.publicationId
237:                 )
238:                 DecideResult(app, job, filled)
239:             }
240:         }
241: 
242:     suspend fun listByEmployer(employerUid: String, limit: Long = 50): Result<List<JobApplication>> =
243:         withContext(Dispatchers.IO) {
244:             runCatching {
245:                 Tasks.await(
246:                     firestore.collection(COLLECTION)
247:                         .whereEqualTo("employerUid", employerUid)
248:                         .orderBy("createdAt", Query.Direction.DESCENDING)
249:                         .limit(limit)
250:                         .get()
251:                 ).documents.map { it.toJobApplication() }
252:             }
253:         }
254: 
255:     suspend fun listByWorker(workerUid: String, limit: Long = 50): Result<List<JobApplication>> =
256:         withContext(Dispatchers.IO) {
257:             runCatching {
258:                 Tasks.await(
259:                     firestore.collection(COLLECTION)
260:                         .whereEqualTo("workerUid", workerUid)
261:                         .orderBy("createdAt", Query.Direction.DESCENDING)
262:                         .limit(limit)
263:                         .get()
264:                 ).documents.map { it.toJobApplication() }
265:             }
266:         }
267: 
268:     companion object {
269:         const val COLLECTION = "applications"
270:     }
271: }

(End of file - total 271 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\JobRepository.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\JobRepository.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.data.repository
2: 
3: import com.google.android.gms.tasks.Tasks
4: import com.google.firebase.firestore.FieldValue
5: import com.google.firebase.firestore.FirebaseFirestore
6: import com.proyecto.chambaya.data.model.AgreedPayment
7: import com.proyecto.chambaya.data.model.Job
8: import com.proyecto.chambaya.data.model.JobApplication
9: import com.proyecto.chambaya.data.model.JobStatus
10: import com.proyecto.chambaya.data.model.toJob
11: import kotlinx.coroutines.Dispatchers
12: import kotlinx.coroutines.withContext
13: 
14: /**
15:  * FASE 8 — Trabajos que nacen al aceptar una postulación.
16:  */
17: class JobRepository(
18:     private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
19: ) {
20: 
21:     suspend fun create(
22:         employerUid: String,
23:         application: JobApplication,
24:         agreedAmount: Double,
25:         publicationTitle: String
26:     ): Result<Job> = withContext(Dispatchers.IO) {
27:         runCatching {
28:             require(employerUid.isNotBlank()) { "Sesión no válida." }
29:             require(employerUid == application.employerUid) { "Solo el contratante crea el trabajo." }
30:             require(agreedAmount > 0) { "Monto no válido." }
31:             val ref = firestore.collection(COLLECTION).document()
32:             val now = FieldValue.serverTimestamp()
33:             Tasks.await(
34:                 ref.set(
35:                     mapOf(
36:                         "jobId" to ref.id,
37:                         "applicationId" to application.applicationId,
38:                         "publicationId" to application.publicationId,
39:                         "publicationTitle" to publicationTitle,
40:                         "workerUid" to application.workerUid,
41:                         "employerUid" to application.employerUid,
42:                         "status" to JobStatus.ACCEPTED,
43:                         "startedAt" to null,
44:                         "completedAt" to null,
45:                         "agreedPayment" to mapOf("amount" to agreedAmount, "currency" to "PEN"),
46:                         "createdAt" to now,
47:                         "updatedAt" to now
48:                     )
49:                 )
50:             )
51:             Tasks.await(ref.get()).toJob()
52:         }
53:     }
54: 
55:     suspend fun findByApplication(applicationId: String): Result<Job?> =
56:         withContext(Dispatchers.IO) {
57:             runCatching {
58:                 if (applicationId.isBlank()) return@runCatching null
59:                 Tasks.await(
60:                     firestore.collection(COLLECTION)
61:                         .whereEqualTo("applicationId", applicationId)
62:                         .limit(1)
63:                         .get()
64:                 ).documents.firstOrNull()?.toJob()
65:             }
66:         }
67: 
68:     suspend fun findByPublicationAndWorker(publicationId: String, workerUid: String): Result<Job?> =
69:         withContext(Dispatchers.IO) {
70:             runCatching {
71:                 if (publicationId.isBlank() || workerUid.isBlank()) return@runCatching null
72:                 Tasks.await(
73:                     firestore.collection(COLLECTION)
74:                         .whereEqualTo("publicationId", publicationId)
75:                         .whereEqualTo("workerUid", workerUid)
76:                         .limit(1)
77:                         .get()
78:                 ).documents.firstOrNull()?.toJob()
79:             }
80:         }
81: 
82:     suspend fun getById(jobId: String): Result<Job?> = withContext(Dispatchers.IO) {
83:         runCatching {
84:             if (jobId.isBlank()) return@runCatching null
85:             Tasks.await(firestore.collection(COLLECTION).document(jobId).get())
86:                 .takeIf { it.exists() }?.toJob()
87:         }
88:     }
89: 
90:     /** Jobs de varias postulaciones de una sola pasada (vista del contratante). */
91:     suspend fun findByApplicationIds(ids: List<String>): Result<Map<String, Job>> =
92:         withContext(Dispatchers.IO) {
93:             runCatching {
94:                 val out = mutableMapOf<String, Job>()
95:                 ids.distinct().filter { it.isNotBlank() }.chunked(30).forEach { chunk ->
96:                     Tasks.await(
97:                         firestore.collection(COLLECTION)
98:                             .whereIn("applicationId", chunk)
99:                             .get()
100:                     ).documents.map { it.toJob() }.forEach { out[it.applicationId] = it }
101:                 }
102:                 out
103:             }
104:         }
105: 
106:     /** Jobs donde el usuario es contratante (recientes primero). */
107:     suspend fun listByEmployer(employerUid: String, limit: Long = 100): Result<List<Job>> =
108:         withContext(Dispatchers.IO) {
109:             runCatching {
110:                 Tasks.await(
111:                     firestore.collection(COLLECTION)
112:                         .whereEqualTo("employerUid", employerUid)
113:                         .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
114:                         .limit(limit)
115:                         .get()
116:                 ).documents.map { it.toJob() }
117:             }
118:         }
119: 
120:     /** Jobs donde el usuario es trabajador (recientes primero). */
121:     suspend fun listByWorker(workerUid: String, limit: Long = 100): Result<List<Job>> =
122:         withContext(Dispatchers.IO) {
123:             runCatching {
124:                 Tasks.await(
125:                     firestore.collection(COLLECTION)
126:                         .whereEqualTo("workerUid", workerUid)
127:                         .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
128:                         .limit(limit)
129:                         .get()
130:                 ).documents.map { it.toJob() }
131:             }
132:         }
133: 
134:     /**
135:      * Avanza el estado respetando [JobStatus.nextFrom].
136:      * Solo participantes; al completar se sella completedAt.
137:      */
138:     suspend fun transition(uid: String, jobId: String, to: String): Result<Job> =
139:         withContext(Dispatchers.IO) {
140:             runCatching {
141:                 val ref = firestore.collection(COLLECTION).document(jobId)
142:                 val snap = Tasks.await(ref.get())
143:                 require(snap.exists()) { "El trabajo ya no existe." }
144:                 val job = snap.toJob()
145:                 require(uid == job.workerUid || uid == job.employerUid) {
146:                     "Ese trabajo no te involucra."
147:                 }
148:                 require(to in JobStatus.nextFrom(job.status)) {
149:                     "Ese cambio de estado no es válido."
150:                 }
151:                 val cambios = mutableMapOf<String, Any?>(
152:                     "status" to to,
153:                     "updatedAt" to FieldValue.serverTimestamp()
154:                 )
155:                 if (to == JobStatus.IN_PROGRESS && job.startedAt == null) {
156:                     cambios["startedAt"] = FieldValue.serverTimestamp()
157:                 }
158:                 if (to == JobStatus.COMPLETED) {
159:                     cambios["completedAt"] = FieldValue.serverTimestamp()
160:                 }
161:                 Tasks.await(ref.update(cambios))
162:                 Tasks.await(ref.get()).toJob()
163:             }
164:         }
165: 
166:     companion object {
167:         const val COLLECTION = "jobs"
168:     }
169: }

(End of file - total 169 lines)
</content>
```

---

## Assistant (Build · Muse Spark 1.3 Free · 4.3s)

_Thinking:_



Ya vi notificaciones y perfil. Ahora reviso historial y completitud para cerrar el diagnóstico.

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\model\\ProfileModels.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\ProfileModels.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.data.model
2: 
3: import com.google.firebase.firestore.DocumentSnapshot
4: import java.util.Locale
5: 
6: /**
7:  * FASE 2 — PERFIL DEL USUARIO.
8:  *
9:  * Modelos de dominio de `users/{uid}` para la fase en la que el usuario completa
10:  * los datos que NO eran necesarios durante el registro.
11:  *
12:  * Reglas del plan maestro que se respetan aquí:
13:  *  - `RegistrationRepository` (FASE 1) es el dueño de `uid`, `auth`, `identity`
14:  *    y del nombre oficial. Esta capa solo LEE esos bloques, nunca los escribe.
15:  *  - El usuario únicamente puede modificar `profile`, `worker` (su parte de
16:  *    trabajador), `privacy` y `updatedAt`.
17:  *  - `worker.workCount`, `worker.ratingAverage` y `worker.ratingCount` son
18:  *    reputación: los calcula la plataforma, no el usuario.
19:  *  - `worker.profileCompleted` NUNCA se envía como dato del formulario: se
20:  *    deriva aquí (ver [ProfileCompletion]) para que no pueda quedar inconsistente.
21:  */
22: 
23: /** Valores admitidos en `profile.gender`. */
24: object Genders {
25:     const val MASCULINO = "MASCULINO"
26:     const val FEMENINO = "FEMENINO"
27:     const val OTRO = "OTRO"
28: 
29:     val ALL = listOf(MASCULINO, FEMENINO, OTRO)
30: 
31:     fun label(value: String?): String = when (value) {
32:         MASCULINO -> "Masculino"
33:         FEMENINO -> "Femenino"
34:         OTRO -> "Prefiero no decirlo"
35:         else -> ""
36:     }
37: 
38:     /** Acepta lo que venga guardado (incluidas etiquetas antiguas en español). */
39:     fun fromStored(value: String?): String = when (value?.trim()?.lowercase(Locale.ROOT)) {
40:         "masculino", MASCULINO.lowercase(Locale.ROOT) -> MASCULINO
41:         "femenino", FEMENINO.lowercase(Locale.ROOT) -> FEMENINO
42:         "otro", "prefiero no decirlo", MASCULINO -> OTRO
43:         else -> ""
44:     }
45: }
46: 
47: /** Límites de los campos editables. La app y las Rules validan lo mismo. */
48: object ProfileLimits {
49:     const val USERNAME_MIN = 3
50:     const val USERNAME_MAX = 30
51:     const val PHONE_MIN = 9
52:     const val PHONE_MAX = 20
53:     const val BIO_MAX = 500
54:     const val SKILL_MAX_LENGTH = 60
55:     const val SKILL_MAX_COUNT = 10
56:     const val SPECIALTY_MAX = 3
57:     const val EXPERIENCE_MAX = 70
58: }
59: 
60: /**
61:  * `profile.birthDate` es texto en `dd/MM/aaaa`, no un `Timestamp`.
62:  *
63:  * Se guardó así desde la FASE 2 (el `DatePicker` devuelve día, mes y año sueltos y
64:  * un `Timestamp` obligaría a elegir zona horaria para una fecha que no la tiene) y
65:  * se mantiene: migrar el documento entero sería un cambio de modelo que no aporta
66:  * nada ahora. Lo que sí hace falta es un único sitio donde decidir si un texto es
67:  * una fecha válida, porque la escribe el usuario, el `DatePicker` y ahora también
68:  * la API de RENIEC.
69:  */
70: object BirthDates {
71: 
72:     private val PATRON = Regex("^(\\d{2})/(\\d{2})/(\\d{4})$")
73: 
74:     /** `true` si [value] es una fecha real en `dd/MM/aaaa` y está dentro de lo posible. */
75:     fun esValida(value: String?): Boolean {
76:         val partes = PATRON.matchEntire(value?.trim().orEmpty()) ?: return false
77:         val dia = partes.groupValues[1].toInt()
78:         val mes = partes.groupValues[2].toInt()
79:         val anio = partes.groupValues[3].toInt()
80:         return mes in 1..12 && dia in 1..diasDelMes(mes, anio) &&
81:             anio in ANIO_MIN..ANIO_MAX
82:     }
83: 
84:     /**
85:      * Devuelve [value] si es una fecha válida, o cadena vacía si no lo es.
86:      *
87:      * Es el filtro que se aplica a lo que llega de fuera (API de RENIEC): un dato
88:      * que no se entiende se descarta y el campo queda pendiente para el usuario, en
89:      * vez de guardar basura que luego no se puede editar.
90:      */
91:     fun soloSiValida(value: String?): String =
92:         if (esValida(value)) value!!.trim() else ""
93: 
94:     private fun diasDelMes(mes: Int, anio: Int): Int = when (mes) {
95:         1, 3, 5, 7, 8, 10, 12 -> 31
96:         4, 6, 9, 11 -> 30
97:         2 -> if ((anio % 4 == 0 && anio % 100 != 0) || anio % 400 == 0) 29 else 28
98:         else -> 0
99:     }
100: 
101:     /** Nadie que use la app nació antes de 1900 ni dentro de un mes. */
102:     private const val ANIO_MIN = 1900
103: 
104:     /** Nadie que use la app nació antes de 1900; el techo deja margen de sobra. */
105:     private const val ANIO_MAX = 2100
106: }
107: 
108: /**
109:  * `profile` de `users/{uid}`.
110:  *
111:  * Los campos de FASE 1 (`firstName`, `lastName`, `fullName`, `country`,
112:  * `profilePhotoSource`) se conservan; los de FASE 2 se completan aquí.
113:  */
114: data class ProfileBlock(
115:     val firstName: String = "",
116:     val lastName: String = "",
117:     val fullName: String = "",
118:     val username: String = "",
119:     val usernameNormalized: String = "",
120:     val phone: String = "",
121:     val profilePhotoUrl: String = "",
122:     val profilePhotoPublicId: String = "",
123:     val profilePhotoSource: String = ProfilePhotoSources.DEFAULT,
124:     val bio: String = "",
125:     val district: String = "",
126:     val province: String = "",
127:     val department: String = "",
128:     val birthDate: String = "",
129:     val gender: String = "",
130:     val country: String = "Peru"
131: ) {
132:     /** "Carmen Alto, Ayacucho" o el nombre tal cual si no hay distrito. */
133:     val locationLabel: String
134:         get() = listOf(district, department).filter { it.isNotBlank() }.joinToString(", ")
135: 
136:     /** Nunca se guarda una foto ajena: el `publicId` siempre vive en nuestra carpeta. */
137:     val hasCustomPhoto: Boolean
138:         get() = profilePhotoSource == ProfilePhotoSources.CUSTOM &&
139:             profilePhotoUrl.isNotBlank() &&
140:             profilePhotoPublicId.isNotBlank()
141: }
142: 
143: /**
144:  * `worker` de `users/{uid}`: lo que el usuario ofrece como trabajador.
145:  *
146:  * `workCount` / `ratingAverage` / `ratingCount` son de la plataforma y por eso
147:  * se separan de lo que el usuario puede escribir.
148:  */
149: data class WorkerBlock(
150:     val enabled: Boolean = true,
151:     val experienceYears: Int = 0,
152:     /**
153:      * `true` cuando el usuario respondió el campo "Años de experiencia", incluso
154:      * si la respuesta fue `0`.
155:      *
156:      * Hace falta porque [experienceYears] es un `Int` que vale `0` sin responder:
157:      * un trabajador sin experiencia es un caso legítimo —de hecho, es el caso
158:      * mayoritario en ChambAYA— y antes era imposible de representar, así que
159:      * escribir `0` dejaba el perfil clavado en 92 % para siempre. Con esta
160:      * bandera, `0` es una respuesta válida y guardable, y "sin responder" sigue
161:      * siendo un estado distinguible. La admiten las Rules sin cambios de esquema
162:      * (`isValidWorkerUpdate` valida claves concretas, no un esquema cerrado).
163:      */
164:     val experienceDeclared: Boolean = false,
165:     val specialties: List<String> = emptyList(),
166:     val skills: List<String> = emptyList(),
167:     val workCount: Int = 0,
168:     val ratingAverage: Double = 0.0,
169:     val ratingCount: Int = 0,
170:     val profileCompleted: Int = 0
171: ) {
172:     val isWorker: Boolean get() = enabled
173: }
174: 
175: /**
176:  * Datos persistentes del modo CONTRATANTE.
177:  *
178:  * Se mantiene separado de `worker` para que cambiar de modo nunca obligue a
179:  * repetir los datos del trabajador. El documento de identidad del registro
180:  * original no se reemplaza; este bloque guarda la identidad que el usuario
181:  * decidió usar como contratante.
182:  */
183: data class EmployerBlock(
184:     val enabled: Boolean = false,
185:     val employerType: String = "",
186:     val businessName: String = "",
187:     val commercialName: String = "",
188:     val sector: String = "",
189:     val documentType: String = "",
190:     val documentNumber: String = "",
191:     val documentNumberMasked: String = "",
192:     val ruc: String? = null,
193:     val identityName: String = "",
194:     val workplaceId: String? = null,
195:     val publishedCount: Int = 0,
196:     val hiredCount: Int = 0,
197:     val ratingAverage: Double = 0.0,
198:     val ratingCount: Int = 0
199: ) {
200:     val isConfigured: Boolean get() = enabled && employerType.isNotBlank()
201:     val documentLabel: String
202:         get() = listOf(documentType, documentNumberMasked.ifBlank { documentNumber })
203:             .filter { it.isNotBlank() }.joinToString(": ")
204: }
205: 
206: /** `privacy` de `users/{uid}`. Por defecto nada se muestra públicamente. */
207: data class PrivacyBlock(
208:     val showPhone: Boolean = false,
209:     val showExactAddress: Boolean = false,
210:     val showEmail: Boolean = false
211: )
212: 
213: /** `statistics` de `users/{uid}`. Solo las incrementa la plataforma. */
214: data class StatisticsBlock(
215:     val applicationsCount: Int = 0,
216:     val publicationsCount: Int = 0,
217:     val completedJobsCount: Int = 0,
218:     val savedPublicationsCount: Int = 0,
219:     val receivedRatingsCount: Int = 0
220: )
221: 
222: /**
223:  * `identity` de `users/{uid}` (FASE 1) en modo SOLO LECTURA.
224:  *
225:  * Se modela aquí únicamente para pintar en el perfil qué documento está
226:  * verificado y con qué padrón. Nunca se envía de vuelta a Firestore.
227:  */
228: data class IdentityBlock(
229:     val documentType: String = "",
230:     val documentNumber: String = "",
231:     val documentNumberMasked: String = "",
232:     val identityVerified: Boolean = false,
233:     val verifiedWith: String = "",
234:     val identityName: String = ""
235: ) {
236:     /** "DNI: 72345678 • Verificado con RENIEC" */
237:     val verifiedLabel: String
238:         get() = buildString {
239:             if (documentType.isNotBlank()) append("$documentType: ")
240:             append(documentNumberMasked.ifBlank { ValidatedIdentity.maskDocumentNumber(documentNumber) })
241:             if (verifiedWith.isNotBlank()) append(" • Verificado con $verifiedWith")
242:         }
243: }
244: 
245: /** `auth` de `users/{uid}` (FASE 1) en modo SOLO LECTURA. */
246: data class AuthBlock(
247:     val email: String = "",
248:     val emailVerified: Boolean = false,
249:     val otpVerified: Boolean = false
250: )
251: 
252: /**
253:  * `users/{uid}` completo tal como lo ve la pantalla de perfil.
254:  *
255:  * `identity` y `auth` se cargan para poder mostrarlos, pero la escritura de la
256:  * FASE 2 solo toca `profile`, `worker` y `privacy`.
257:  */
258: data class UserProfile(
259:     val uid: String = "",
260:     val roles: List<String> = listOf(UserRoles.TRABAJADOR),
261:     val activeRole: String = UserRoles.TRABAJADOR,
262:     val registrationStatus: String = "",
263:     val accountStatus: String = "",
264:     val profile: ProfileBlock = ProfileBlock(),
265:     val worker: WorkerBlock = WorkerBlock(),
266:     val employer: EmployerBlock = EmployerBlock(),
267:     val privacy: PrivacyBlock = PrivacyBlock(),
268:     val statistics: StatisticsBlock = StatisticsBlock(),
269:     val identity: IdentityBlock = IdentityBlock(),
270:     val auth: AuthBlock = AuthBlock(),
271:     /**
272:      * `true` si el documento ya tiene los bloques que aporta la FASE 2
273:      * (`worker`, `privacy`, `statistics`) y un `@usuario` reservado.
274:      *
275:      * Las cuentas de la FASE 1 nacen sin ellos. La pantalla de perfil solo los
276:      * siembra una vez, y lo hace "a mejor hacer": si esa escritura se rechaza,
277:      * el perfil se sigue mostrando con los valores por defecto de los modelos.
278:      */
279:     val tieneBloquesFase2: Boolean = false
280: ) {
281:     /**
282:      * Calcula el porcentaje de completitud a partir del estado real del
283:      * documento. Se usa para el badge, el banner y el resumen del wizard.
284:      */
285:     fun completion(): ProfileCompletion = ProfileCompletion.from(this)
286: 
287:     /** `true` cuando el usuario guardó una foto propia en Cloudinary. */
288:     val hasPhoto: Boolean
289:         get() = profile.profilePhotoUrl.isNotBlank()
290: }
291: 
292: /**
293:  * Porcentaje de completitud y qué falta para llegar al 100 %.
294:  *
295:  * Se DERIVA del documento, nunca se envía como dato: así es imposible que el
296:  * porcentaje guardado y los datos guardados se contradigan.
297:  */
298: data class ProfileCompletion(
299:     val percent: Int,
300:     val missing: List<String>
301: ) {
302:     val isComplete: Boolean get() = percent >= 100
303: 
304:     companion object {
305:         private val COMMON_CHECKS: List<Pair<String, (UserProfile) -> Boolean>> = listOf(
306:             "Foto de perfil" to { it.hasPhoto },
307:             "Nombre completo" to { it.profile.fullName.isNotBlank() },
308:             "Nombre de usuario" to { it.profile.username.isNotBlank() },
309:             "Teléfono" to { it.profile.phone.filter(Char::isDigit).length >= ProfileLimits.PHONE_MIN },
310:             "Descripción" to { it.profile.bio.isNotBlank() },
311:             "Distrito" to { it.profile.district.isNotBlank() },
312:             "Provincia" to { it.profile.province.isNotBlank() },
313:             "Departamento" to { it.profile.department.isNotBlank() },
314:             "Privacidad" to { true }
315:         )
316: 
317:         private val WORKER_CHECKS: List<Pair<String, (UserProfile) -> Boolean>> = listOf(
318:             "Fecha de nacimiento" to { it.profile.birthDate.isNotBlank() },
319:             "Género" to { it.profile.gender.isNotBlank() },
320:             // `0` cuenta como respuesta: quien empieza en ChambAYA legítimamente no
321:             // tiene años de experiencia. `experienceDeclared` es lo que distingue
322:             // "escribí 0" de "nunca lo toqué".
323:             "Años de experiencia" to {
324:                 it.worker.experienceYears > 0 || it.worker.experienceDeclared
325:             },
326:             "Especialidades" to { it.worker.specialties.isNotEmpty() },
327:             "Habilidades" to { it.worker.skills.isNotEmpty() }
328:         )
329: 
330:         private val EMPLOYER_CHECKS: List<Pair<String, (UserProfile) -> Boolean>> = listOf(
331:             "Tipo de contratante" to { it.employer.employerType.isNotBlank() },
332:             "Identidad del contratante" to { it.employer.documentType.isNotBlank() && it.employer.documentNumber.isNotBlank() },
333:             "Nombre comercial o negocio" to { it.employer.businessName.isNotBlank() },
334:             // FASE 4 — el establecimiento es obligatorio para contratar/publicar:
335:             // sin lugar no hay sede para las chambas.
336:             "Lugar o establecimiento" to { !it.employer.workplaceId.isNullOrBlank() }
337:         )
338: 
339:         fun checksFor(profile: UserProfile): List<Pair<String, (UserProfile) -> Boolean>> =
340:             COMMON_CHECKS + if (profile.activeRole == UserRoles.CONTRATANTE) EMPLOYER_CHECKS else WORKER_CHECKS
341: 
342:         fun from(profile: UserProfile): ProfileCompletion {
343:             val checks = checksFor(profile)
344:             val faltan = checks.filterNot { it.second(profile) }.map { it.first }
345:             val completados = checks.size - faltan.size
346:             return ProfileCompletion(
347:                 percent = (completados * 100) / checks.size,
348:                 missing = faltan
349:             )
350:         }
351: 
352:         fun empty(): ProfileCompletion = ProfileCompletion(0, COMMON_CHECKS.map { it.first } + WORKER_CHECKS.map { it.first })
353:     }
354: }
355: 
356: /**
357:  * Compuerta de contratación y publicación.
358:  *
359:  * El trabajador publica/postula con su perfil tal cual. Quien NO está en rol
360:  * trabajador (contratante, empresa, negocio, independiente…) solo puede
361:  * contratar o publicar si su perfil supera el [UMBRAL_CONTRATAR_PUBLICAR]:
362:  * sin datos mínimos no hay ofertas confiables.
363:  */
364: const val UMBRAL_CONTRATAR_PUBLICAR = 50
365: 
366: fun UserProfile.puedeContratarOPublicar(): Boolean =
367:     if (activeRole == UserRoles.TRABAJADOR) true
368:     else completion().percent > UMBRAL_CONTRATAR_PUBLICAR
369: 
370: /**
371:  * Datos que envía `EditarPerfilActivity` al finalizar los 4 pasos.
372:  *
373:  * Solo contiene campos editables: si algo falta en el borrador, el
374:  * repositorio conserva el valor que ya estaba en Firestore.
375:  */
376: data class EmployerDraft(
377:     val employerType: String,
378:     val businessName: String,
379:     val commercialName: String,
380:     val sector: String,
381:     val documentType: String,
382:     val documentNumber: String,
383:     val identityName: String,
384:     val ruc: String? = null
385: )
386: 
387: data class ProfileDraft(
388:     val fullName: String = "",
389:     val username: String = "",
390:     val phone: String = "",
391:     val bio: String = "",
392:     val birthDate: String = "",
393:     val gender: String = "",
394:     val department: String = "",
395:     val province: String = "",
396:     val district: String = "",
397:     val experienceYears: Int = 0,
398:     val experienceDeclared: Boolean = false,
399:     val specialties: List<String> = emptyList(),
400:     val skills: List<String> = emptyList(),
401:     val showPhone: Boolean = false,
402:     val showEmail: Boolean = false,
403:     val showExactAddress: Boolean = false,
404:     val workerEnabled: Boolean = true
405: ) {
406:     /** `null` cuando el usuario no eligió foto: se conserva la que ya tenía. */
407:     val photoPublicId: String? = null
408:     val photoUrl: String? = null
409: 
410:     companion object {
411:         /** Construye el borrador inicial a partir de lo que hay en Firestore. */
412:         fun from(profile: UserProfile): ProfileDraft = ProfileDraft(
413:             fullName = profile.profile.fullName,
414:             username = profile.profile.username,
415:             phone = profile.profile.phone,
416:             bio = profile.profile.bio,
417:             birthDate = profile.profile.birthDate,
418:             gender = profile.profile.gender,
419:             department = profile.profile.department,
420:             province = profile.profile.province,
421:             district = profile.profile.district,
422:             experienceYears = profile.worker.experienceYears,
423:             experienceDeclared = profile.worker.experienceDeclared,
424:             specialties = profile.worker.specialties,
425:             skills = profile.worker.skills,
426:             showPhone = profile.privacy.showPhone,
427:             showEmail = profile.privacy.showEmail,
428:             showExactAddress = profile.privacy.showExactAddress,
429:             workerEnabled = profile.worker.enabled
430:         )
431:     }
432: }
433: 
434: // ═══════════════════════════════════════════════════════════════════
435: //  Lectura de `users/{uid}`
436: // ═══════════════════════════════════════════════════════════════════
437: 
438: /** Lee los bloques de la FASE 2 tolerando documentos que aún no los tienen. */
439: internal fun DocumentSnapshot.profileBlock(): ProfileBlock {
440:     val data = get("profile") as? Map<*, *> ?: emptyMap<Any, Any>()
441:     return ProfileBlock(
442:         firstName = data.str("firstName"),
443:         lastName = data.str("lastName"),
444:         fullName = data.str("fullName"),
445:         username = data.str("username"),
446:         usernameNormalized = data.str("usernameNormalized"),
447:         phone = data.str("phone"),
448:         profilePhotoUrl = data.str("profilePhotoUrl"),
449:         profilePhotoPublicId = data.str("profilePhotoPublicId"),
450:         profilePhotoSource = data.str("profilePhotoSource")
451:             .ifBlank { ProfilePhotoSources.DEFAULT },
452:         bio = data.str("bio"),
453:         district = data.str("district"),
454:         province = data.str("province"),
455:         department = data.str("department"),
456:         birthDate = data.str("birthDate"),
457:         gender = Genders.fromStored(data.str("gender")),
458:         country = data.str("country").ifBlank { "Peru" }
459:     )
460: }
461: 
462: internal fun DocumentSnapshot.employerBlock(): EmployerBlock {
463:     val data = get("employer") as? Map<*, *> ?: emptyMap<Any, Any>()
464:     return EmployerBlock(
465:         enabled = data["enabled"] as? Boolean ?: false,
466:         employerType = data.str("employerType"),
467:         businessName = data.str("businessName"),
468:         commercialName = data.str("commercialName"),
469:         sector = data.str("sector"),
470:         documentType = data.str("documentType"),
471:         documentNumber = data.str("documentNumber"),
472:         documentNumberMasked = data.str("documentNumberMasked"),
473:         ruc = data.str("ruc").ifBlank { null },
474:         identityName = data.str("identityName"),
475:         workplaceId = data.str("workplaceId").ifBlank { null },
476:         publishedCount = (data["publishedCount"] as? Number)?.toInt() ?: 0,
477:         hiredCount = (data["hiredCount"] as? Number)?.toInt() ?: 0,
478:         ratingAverage = (data["ratingAverage"] as? Number)?.toDouble() ?: 0.0,
479:         ratingCount = (data["ratingCount"] as? Number)?.toInt() ?: 0
480:     )
481: }
482: 
483: internal fun DocumentSnapshot.workerBlock(): WorkerBlock {
484:     val data = get("worker") as? Map<*, *> ?: emptyMap<Any, Any>()
485:     return WorkerBlock(
486:         enabled = data["enabled"] as? Boolean ?: true,
487:         experienceYears = (data["experienceYears"] as? Number)?.toInt() ?: 0,
488:         experienceDeclared = data["experienceDeclared"] as? Boolean ?: false,
489:         specialties = data.strList("specialties"),
490:         skills = data.strList("skills"),
491:         workCount = (data["workCount"] as? Number)?.toInt() ?: 0,
492:         ratingAverage = (data["ratingAverage"] as? Number)?.toDouble() ?: 0.0,
493:         ratingCount = (data["ratingCount"] as? Number)?.toInt() ?: 0,
494:         profileCompleted = (data["profileCompleted"] as? Number)?.toInt() ?: 0
495:     )
496: }
497: 
498: internal fun DocumentSnapshot.privacyBlock(): PrivacyBlock {
499:     val data = get("privacy") as? Map<*, *> ?: emptyMap<Any, Any>()
500:     return PrivacyBlock(
501:         showPhone = data["showPhone"] as? Boolean ?: false,
502:         showExactAddress = data["showExactAddress"] as? Boolean ?: false,
503:         showEmail = data["showEmail"] as? Boolean ?: false
504:     )
505: }
506: 
507: internal fun DocumentSnapshot.statisticsBlock(): StatisticsBlock {
508:     val data = get("statistics") as? Map<*, *> ?: emptyMap<Any, Any>()
509:     return StatisticsBlock(
510:         applicationsCount = (data["applicationsCount"] as? Number)?.toInt() ?: 0,
511:         publicationsCount = (data["publicationsCount"] as? Number)?.toInt() ?: 0,
512:         completedJobsCount = (data["completedJobsCount"] as? Number)?.toInt() ?: 0,
513:         savedPublicationsCount = (data["savedPublicationsCount"] as? Number)?.toInt() ?: 0,
514:         receivedRatingsCount = (data["receivedRatingsCount"] as? Number)?.toInt() ?: 0
515:     )
516: }
517: 
518: /** Bloque `identity` de la FASE 1, en solo lectura. */
519: internal fun DocumentSnapshot.identityBlock(): IdentityBlock {
520:     val data = get("identity") as? Map<*, *> ?: emptyMap<Any, Any>()
521:     return IdentityBlock(
522:         documentType = data.str("documentType"),
523:         documentNumber = data.str("documentNumber"),
524:         documentNumberMasked = data.str("documentNumberMasked"),
525:         identityVerified = data["identityVerified"] as? Boolean ?: false,
526:         verifiedWith = data.str("verifiedWith"),
527:         identityName = data.str("identityName")
528:     )
529: }
530: 
531: /** Bloque `auth` de la FASE 1, en solo lectura. */
532: internal fun DocumentSnapshot.authBlock(): AuthBlock {
533:     val data = get("auth") as? Map<*, *> ?: emptyMap<Any, Any>()
534:     return AuthBlock(
535:         email = data.str("email").ifBlank { getString("email").orEmpty() },
536:         emailVerified = data["emailVerified"] as? Boolean ?: (getBoolean("emailVerified") ?: false),
537:         otpVerified = data["otpVerified"] as? Boolean ?: (getBoolean("otpVerified") ?: false)
538:     )
539: }
540: 
541: /** Construye el [UserProfile] completo desde el documento de Firestore. */
542: internal fun DocumentSnapshot.toUserProfile(): UserProfile {
543:     val rolesParsed = (get("roles") as? List<*>)?.mapNotNull { it?.toString() }
544:         ?.filter(UserRoles::isValid)
545:         ?.ifEmpty { listOf(UserRoles.TRABAJADOR) }
546:         ?: listOf(getString("activeRole") ?: getString("role") ?: UserRoles.TRABAJADOR)
547:     val workerParsed = workerBlock()
548:     return UserProfile(
549:     uid = id,
550:     roles = rolesParsed,
551:     activeRole = getString("activeRole") ?: getString("role") ?: UserRoles.TRABAJADOR,
552:     registrationStatus = getString("registrationStatus").orEmpty(),
553:     accountStatus = getString("accountStatus").orEmpty(),
554:     profile = profileBlock(),
555:     worker = workerParsed.copy(enabled = rolesParsed.contains(UserRoles.TRABAJADOR) && workerParsed.enabled),
556:     employer = employerBlock(),
557:     privacy = privacyBlock(),
558:     statistics = statisticsBlock(),
559:     identity = identityBlock(),
560:     auth = authBlock(),
561:     // Los bloques de la FASE 2 se siembran con una escritura aparte, y solo si
562:     // faltan: la pantalla de perfil no puede depender de que esa escritura se
563:     // autorice, así que se limitan a leer el documento.
564:     tieneBloquesFase2 = get("worker") is Map<*, *> &&
565:         get("privacy") is Map<*, *> &&
566:         get("statistics") is Map<*, *> &&
567:         !getString("profile.username").isNullOrBlank()
568:     )
569: }
570: 
571: // ── helpers de lectura tolerante ──────────────────────────────────
572: 
573: private fun Map<*, *>.str(key: String): String = this[key]?.toString()?.trim().orEmpty()
574: 
575: private fun Map<*, *>.strList(key: String): List<String> =
576:     (this[key] as? List<*>)?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
577:         .orEmpty()

(End of file - total 577 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\ui\\trabajos\\FragmentoMisTrabajos.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\trabajos\FragmentoMisTrabajos.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.ui.trabajos
2: 
3: import android.animation.ArgbEvaluator
4: import android.animation.ValueAnimator
5: import android.content.Intent
6: import android.content.res.ColorStateList
7: import android.os.Bundle
8: import android.view.LayoutInflater
9: import android.view.View
10: import android.view.ViewGroup
11: import android.view.animation.DecelerateInterpolator
12: import android.view.animation.OvershootInterpolator
13: import android.widget.ImageView
14: import android.widget.ProgressBar
15: import android.widget.TextView
16: import android.widget.Toast
17: import androidx.appcompat.app.AlertDialog
18: import androidx.core.view.ViewCompat
19: import androidx.core.view.WindowInsetsCompat
20: import androidx.core.view.updatePadding
21: import androidx.fragment.app.Fragment
22: import androidx.lifecycle.lifecycleScope
23: import androidx.recyclerview.widget.RecyclerView
24: import com.google.android.material.button.MaterialButton
25: import com.google.firebase.auth.FirebaseAuth
26: import com.proyecto.chambaya.BarraEstadoUtils
27: import com.proyecto.chambaya.R
28: import com.proyecto.chambaya.data.model.ApplicationStatus
29: import com.proyecto.chambaya.data.model.Job
30: import com.proyecto.chambaya.data.model.JobApplication
31: import com.proyecto.chambaya.data.model.JobStatus
32: import com.proyecto.chambaya.data.repository.ApplicationRepository
33: import com.proyecto.chambaya.data.repository.ChatRepository
34: import com.proyecto.chambaya.data.repository.JobRepository
35: import com.proyecto.chambaya.data.repository.ProfileRepository
36: import com.proyecto.chambaya.data.repository.RatingRepository
37: import com.proyecto.chambaya.ui.jobs.JobDetailSheet
38: import com.proyecto.chambaya.ui.jobs.RateSheet
39: import com.proyecto.chambaya.ui.publish.MyApplicationsAdapter
40: import com.proyecto.chambaya.ui.publish.MyAppRow
41: import kotlinx.coroutines.launch
42: 
43: /**
44:  * Solo rol Trabajador: Solicitudes (mis postulaciones) + Historial
45:  * (línea de tiempo de trabajos con fechas de contratación, inicio y fin).
46:  *
47:  * Replica el lenguaje visual de Publicar (barra de pestañas con indicador
48:  * deslizante y cambio por toque o deslizamiento).
49:  */
50: class FragmentoMisTrabajos : Fragment() {
51: 
52:     private var seccionActual = SECCION_SOLICITUDES
53: 
54:     private lateinit var tabs: List<View>
55:     private lateinit var iconos: List<ImageView>
56:     private lateinit var paneles: List<View>
57:     private lateinit var barraTabs: View
58:     private lateinit var indicador: View
59: 
60:     private var colorActivo = 0
61:     private var colorInactivo = 0
62:     private var anchoBarraPrevio = -1
63: 
64:     private val appRepository = ApplicationRepository()
65:     private val jobRepository = JobRepository()
66:     private val ratingRepository = RatingRepository()
67:     private val profileRepository = ProfileRepository()
68: 
69:     // Solicitudes
70:     private var rvSolicitudes: RecyclerView? = null
71:     private var progressSol: ProgressBar? = null
72:     private var emptySol: View? = null
73:     private var solAdapter: MyApplicationsAdapter? = null
74:     private var solFilter = 0
75:     private var workerApps: List<JobApplication> = emptyList()
76:     private var jobsCache: Map<String, Job> = emptyMap()
77:     private var ratedCache: Set<String> = emptySet()
78:     private var solLoading = false
79: 
80:     // Historial
81:     private var rvHistorial: RecyclerView? = null
82:     private var progressHist: ProgressBar? = null
83:     private var emptyHist: View? = null
84:     private var histAdapter: TimelineAdapter? = null
85:     private var histFilter = 0 // 0 todos · 1 en curso · 2 completados
86:     private var historyJobs: List<Job> = emptyList()
87:     private var entityCache: Map<String, String> = emptyMap()
88:     private var histLoading = false
89: 
90:     override fun onCreateView(
91:         inflater: LayoutInflater,
92:         container: ViewGroup?,
93:         savedInstanceState: Bundle?
94:     ): View? {
95:         return inflater.inflate(R.layout.fragmento_mis_trabajos, container, false)
96:     }
97: 
98:     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
99:         super.onViewCreated(view, savedInstanceState)
100: 
101:         seccionActual = savedInstanceState?.getInt(KEY_SECCION, SECCION_SOLICITUDES) ?: SECCION_SOLICITUDES
102:         colorActivo = requireContext().getColor(R.color.profile_tab_active)
103:         colorInactivo = requireContext().getColor(R.color.profile_tab_inactive)
104: 
105:         barraTabs = view.findViewById(R.id.barraTabs)
106:         indicador = view.findViewById(R.id.indicadorTab)
107:         ViewCompat.setOnApplyWindowInsetsListener(barraTabs) { v, insets ->
108:             val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
109:             v.updatePadding(top = statusBar.top)
110:             insets
111:         }
112:         ViewCompat.requestApplyInsets(barraTabs)
113:         tabs = listOf(view.findViewById(R.id.tabSolicitudes), view.findViewById(R.id.tabHistorial))
114:         iconos = listOf(view.findViewById(R.id.iconoSolicitudes), view.findViewById(R.id.iconoHistorial))
115:         paneles = listOf(view.findViewById(R.id.panelSolicitudes), view.findViewById(R.id.panelHistorial))
116: 
117:         configurarSolicitudes(view)
118:         configurarHistorial(view)
119: 
120:         tabs.forEachIndexed { indice, tab ->
121:             tab.setOnClickListener { seleccionar(indice, animar = true) }
122:         }
123:         view.findViewById<com.proyecto.chambaya.ui.publish.ContenedorDeslizable>(R.id.contenedorSecciones).alDeslizar = { dir ->
124:             seleccionar(seccionActual + dir, animar = true)
125:         }
126: 
127:         aplicarEstado(animar = false)
128:         barraTabs.addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
129:             val ancho = right - left
130:             if (ancho != anchoBarraPrevio) {
131:                 anchoBarraPrevio = ancho
132:                 moverIndicador(animar = false)
133:             }
134:         }
135: 
136:         parentFragmentManager.setFragmentResultListener(RateSheet.REQUEST_RATED, viewLifecycleOwner) { _, _ ->
137:             cargarSolicitudes()
138:             cargarHistorial()
139:         }
140:     }
141: 
142:     override fun onSaveInstanceState(outState: Bundle) {
143:         super.onSaveInstanceState(outState)
144:         outState.putInt(KEY_SECCION, seccionActual)
145:     }
146: 
147:     override fun onResume() {
148:         super.onResume()
149:         BarraEstadoUtils.aplicarColor(requireActivity(), requireContext().getColor(R.color.white))
150:         if (::barraTabs.isInitialized) {
151:             cargarSolicitudes()
152:             cargarHistorial()
153:         }
154:     }
155: 
156:     override fun onDestroyView() {
157:         rvSolicitudes = null
158:         progressSol = null
159:         emptySol = null
160:         solAdapter = null
161:         rvHistorial = null
162:         progressHist = null
163:         emptyHist = null
164:         histAdapter = null
165:         super.onDestroyView()
166:     }
167: 
168:     // ── Solicitudes ───────────────────────────────────────────────
169: 
170:     private fun configurarSolicitudes(view: View) {
171:         rvSolicitudes = view.findViewById(R.id.rvSolicitudes)
172:         progressSol = view.findViewById(R.id.progressSolicitudes)
173:         emptySol = view.findViewById(R.id.emptySolicitudes)
174:         solAdapter = MyApplicationsAdapter(
175:             onPrimary = { row -> accionMiPostulacion(row) },
176:             onContact = { row -> abrirChat(row.app.employerUid, row.app.publicationId, row.app.publicationTitle) },
177:             onOpenDetail = { row ->
178:                 JobDetailSheet.newInstance(row.app.publicationId).show(parentFragmentManager, "detail")
179:             }
180:         )
181:         rvSolicitudes?.adapter = solAdapter
182:         rvSolicitudes?.visibility = View.GONE
183: 
184:         val chips = listOf(
185:             view.findViewById<TextView>(R.id.chipSolAll),
186:             view.findViewById<TextView>(R.id.chipSolPending),
187:             view.findViewById<TextView>(R.id.chipSolDecided)
188:         )
189:         chips.forEachIndexed { i, chip ->
190:             chip.setOnClickListener {
191:                 solFilter = i
192:                 chips.forEachIndexed { j, c -> pintarChip(c, j == i) }
193:                 pintarSolicitudes()
194:             }
195:         }
196:         emptySol?.findViewById<MaterialButton>(R.id.btnVacioAccion)?.apply {
197:             text = "Explorar chambas"
198:             setOnClickListener {
199:                 (activity as? com.proyecto.chambaya.MainActivity)
200:                     ?.navigateToTab(R.id.nav_jobs)
201:             }
202:         }
203:     }
204: 
205:     private fun pintarChip(chip: TextView, on: Boolean) {
206:         chip.setBackgroundResource(if (on) R.drawable.bg_chip_active else R.drawable.bg_chip_inactive)
207:         chip.setTextColor(requireContext().getColor(if (on) R.color.white else R.color.text_primary))
208:     }
209: 
210:     private fun cargarSolicitudes() {
211:         val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
212:         if (solLoading || !isAdded) return
213:         solLoading = true
214:         progressSol?.visibility = View.VISIBLE
215:         rvSolicitudes?.visibility = View.GONE
216:         emptySol?.visibility = View.GONE
217:         viewLifecycleOwner.lifecycleScope.launch {
218:             workerApps = appRepository.listByWorker(uid).getOrNull().orEmpty()
219:             val acceptedIds = workerApps
220:                 .filter { it.status == ApplicationStatus.ACCEPTED }
221:                 .map { it.applicationId }
222:             jobsCache = if (acceptedIds.isEmpty()) emptyMap()
223:             else jobRepository.findByApplicationIds(acceptedIds).getOrNull().orEmpty()
224:             val completedIds = jobsCache.values
225:                 .filter { it.status == JobStatus.COMPLETED }
226:                 .map { it.jobId }
227:             ratedCache = if (completedIds.isEmpty()) emptySet()
228:             else ratingRepository.ratedJobIds(completedIds, uid).getOrNull().orEmpty()
229:             if (!isAdded) {
230:                 solLoading = false
231:                 return@launch
232:             }
233:             solLoading = false
234:             pintarSolicitudes()
235:         }
236:     }
237: 
238:     private fun pintarSolicitudes() {
239:         if (!isAdded) return
240:         progressSol?.visibility = View.GONE
241:         val filtradas = when (solFilter) {
242:             1 -> workerApps.filter { it.status == ApplicationStatus.PENDING }
243:             2 -> workerApps.filter { it.status != ApplicationStatus.PENDING }
244:             else -> workerApps
245:         }
246:         if (filtradas.isEmpty()) {
247:             rvSolicitudes?.visibility = View.GONE
248:             emptySol?.visibility = View.VISIBLE
249:             emptySol?.findViewById<TextView>(R.id.tvVacioTitulo)?.text = "Tus postulaciones"
250:             emptySol?.findViewById<TextView>(R.id.tvVacioSubtitulo)?.text =
251:                 "Cuando te postules a una chamba, seguirás aquí su estado."
252:         } else {
253:             emptySol?.visibility = View.GONE
254:             rvSolicitudes?.visibility = View.VISIBLE
255:             solAdapter?.submitList(
256:                 filtradas.map { app ->
257:                     val job = jobsCache[app.applicationId]
258:                     MyAppRow(app, job, job?.jobId in ratedCache)
259:                 }
260:             )
261:         }
262:     }
263: 
264:     private fun accionMiPostulacion(row: MyAppRow) {
265:         val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
266:         val app = row.app
267:         val job = row.job
268:         when {
269:             app.status == ApplicationStatus.PENDING -> {
270:                 AlertDialog.Builder(requireContext())
271:                     .setTitle("Retirar postulación")
272:                     .setMessage("¿Retirar tu postulación a “${app.publicationTitle.take(50)}”?")
273:                     .setPositiveButton("Retirar") { _, _ ->
274:                         viewLifecycleOwner.lifecycleScope.launch {
275:                             val r = appRepository.withdraw(uid, app.applicationId)
276:                             if (!isAdded) return@launch
277:                             Toast.makeText(
278:                                 requireContext(),
279:                                 if (r.isSuccess) "Postulación retirada." else "No se pudo retirar.",
280:                                 Toast.LENGTH_SHORT
281:                             ).show()
282:                             cargarSolicitudes()
283:                             cargarHistorial()
284:                         }
285:                     }
286:                     .setNegativeButton("Cancelar", null)
287:                     .show()
288:             }
289:             job?.status == JobStatus.COMPLETED && !row.ratedByMe -> {
290:                 RateSheet.newInstance(job.jobId).show(parentFragmentManager, "rate")
291:             }
292:             else -> {
293:                 JobDetailSheet.newInstance(app.publicationId).show(parentFragmentManager, "detail")
294:             }
295:         }
296:     }
297: 
298:     private fun abrirChat(otherUid: String, publicationId: String, publicationTitle: String) {
299:         val me = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
300:         if (me.isBlank() || otherUid.isBlank() || me == otherUid) return
301:         viewLifecycleOwner.lifecycleScope.launch {
302:             val conv = ChatRepository()
303:                 .ensureConversation(me, otherUid, publicationId, publicationTitle).getOrNull()
304:             val perfil = profileRepository.loadPublicProfile(otherUid).getOrNull()
305:             if (!isAdded) return@launch
306:             if (conv == null) {
307:                 Toast.makeText(requireContext(), "No se pudo abrir el chat.", Toast.LENGTH_LONG).show()
308:                 return@launch
309:             }
310:             val intent = Intent(
311:                 requireContext(),
312:                 com.proyecto.chambaya.ui.chat.ActividadChatDetalle::class.java
313:             ).apply {
314:                 putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_CONV_ID, conv.conversationId)
315:                 putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_OTHER_UID, otherUid)
316:                 putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_NOMBRE, perfil?.displayName() ?: "Chat")
317:                 putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_FOTO, perfil?.photoUrl.orEmpty())
318:                 putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_PUB_TITULO, publicationTitle)
319:             }
320:             startActivity(intent)
321:         }
322:     }
323: 
324:     // ── Historial ─────────────────────────────────────────────────
325: 
326:     private fun configurarHistorial(view: View) {
327:         rvHistorial = view.findViewById(R.id.rvHistorial)
328:         progressHist = view.findViewById(R.id.progressHistorial)
329:         emptyHist = view.findViewById(R.id.emptyHistorial)
330:         histAdapter = TimelineAdapter(
331:             onRate = { row ->
332:                 RateSheet.newInstance(row.job.jobId).show(parentFragmentManager, "rate")
333:             }
334:         )
335:         rvHistorial?.adapter = histAdapter
336:         rvHistorial?.visibility = View.GONE
337: 
338:         val chips = listOf(
339:             view.findViewById<TextView>(R.id.chipHistAll),
340:             view.findViewById<TextView>(R.id.chipHistActive),
341:             view.findViewById<TextView>(R.id.chipHistDone)
342:         )
343:         chips.forEachIndexed { i, chip ->
344:             chip.setOnClickListener {
345:                 histFilter = i
346:                 chips.forEachIndexed { j, c -> pintarChip(c, j == i) }
347:                 pintarHistorial()
348:             }
349:         }
350:         emptyHist?.findViewById<MaterialButton>(R.id.btnVacioAccion)?.apply {
351:             text = "Explorar chambas"
352:             setOnClickListener {
353:                 (activity as? com.proyecto.chambaya.MainActivity)
354:                     ?.navigateToTab(R.id.nav_jobs)
355:             }
356:         }
357:     }
358: 
359:     private fun cargarHistorial() {
360:         val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
361:         if (histLoading || !isAdded) return
362:         histLoading = true
363:         progressHist?.visibility = View.VISIBLE
364:         rvHistorial?.visibility = View.GONE
365:         emptyHist?.visibility = View.GONE
366:         viewLifecycleOwner.lifecycleScope.launch {
367:             val jobs = jobRepository.listByWorker(uid).getOrNull().orEmpty()
368:             // Entidades (contratantes) de cada trabajo.
369:             val entityIds = jobs.map { it.employerUid }.distinct().filter { it.isNotBlank() }
370:             val nombres = mutableMapOf<String, String>()
371:             entityIds.forEach { id ->
372:                 profileRepository.loadPublicProfile(id).getOrNull()?.let {
373:                     nombres[id] = it.displayName()
374:                 }
375:             }
376:             val completedIds = jobs.filter { it.status == JobStatus.COMPLETED }.map { it.jobId }
377:             val rated = if (completedIds.isEmpty()) emptySet()
378:             else ratingRepository.ratedJobIds(completedIds, uid).getOrNull().orEmpty()
379:             if (!isAdded) {
380:                 histLoading = false
381:                 return@launch
382:             }
383:             histLoading = false
384:             historyJobs = jobs
385:             entityCache = nombres
386:             // Reutiliza el caché de calificados si ya se cargó en solicitudes.
387:             if (rated.isNotEmpty()) ratedCache = ratedCache + rated
388:             pintarHistorial()
389:         }
390:     }
391: 
392:     private fun pintarHistorial() {
393:         if (!isAdded) return
394:         progressHist?.visibility = View.GONE
395:         val filtrados = when (histFilter) {
396:             1 -> historyJobs.filter { it.status == JobStatus.ACCEPTED || it.status == JobStatus.IN_PROGRESS }
397:             2 -> historyJobs.filter { it.status == JobStatus.COMPLETED }
398:             else -> historyJobs
399:         }
400:         if (filtrados.isEmpty()) {
401:             rvHistorial?.visibility = View.GONE
402:             emptyHist?.visibility = View.VISIBLE
403:             emptyHist?.findViewById<TextView>(R.id.tvVacioTitulo)?.text = "Tu recorrido"
404:             emptyHist?.findViewById<TextView>(R.id.tvVacioSubtitulo)?.text =
405:                 "Aquí verás en qué trabajos participaste: contratación, inicio y fin."
406:         } else {
407:             emptyHist?.visibility = View.GONE
408:             rvHistorial?.visibility = View.VISIBLE
409:             histAdapter?.submitList(
410:                 filtrados.map { job ->
411:                     TimelineRow(job, entityCache[job.employerUid].orEmpty(), job.jobId in ratedCache)
412:                 }
413:             )
414:         }
415:     }
416: 
417:     // ── Cambio de sección (misma mecánica que Publicar) ───────────
418: 
419:     private fun seleccionar(nueva: Int, animar: Boolean) {
420:         if (nueva !in paneles.indices || nueva == seccionActual) return
421:         val anterior = seccionActual
422:         seccionActual = nueva
423:         aplicarEstado(animar, anterior)
424:     }
425: 
426:     private fun aplicarEstado(animar: Boolean, anterior: Int = seccionActual) {
427:         actualizarIconos(animar)
428:         moverIndicador(animar)
429:         mostrarPanel(anterior, seccionActual, animar)
430:     }
431: 
432:     private fun actualizarIconos(animar: Boolean) {
433:         iconos.forEachIndexed { indice, icono ->
434:             val activo = indice == seccionActual
435:             val destino = if (activo) colorActivo else colorInactivo
436:             val origen = (icono.tag as? Int) ?: colorInactivo
437:             tabs[indice].isSelected = activo
438: 
439:             if (animar && origen != destino) {
440:                 ValueAnimator.ofObject(ArgbEvaluator(), origen, destino).apply {
441:                     duration = 200
442:                     addUpdateListener {
443:                         icono.imageTintList = ColorStateList.valueOf(it.animatedValue as Int)
444:                     }
445:                     start()
446:                 }
447:                 if (activo) {
448:                     icono.scaleX = 0.82f
449:                     icono.scaleY = 0.82f
450:                     icono.animate().scaleX(1f).scaleY(1f).setDuration(280)
451:                         .setInterpolator(OvershootInterpolator(2.2f)).start()
452:                 }
453:             } else {
454:                 icono.imageTintList = ColorStateList.valueOf(destino)
455:             }
456:             icono.tag = destino
457:         }
458:     }
459: 
460:     private fun moverIndicador(animar: Boolean) {
461:         val anchoTab = barraTabs.width / tabs.size
462:         if (anchoTab == 0) return
463:         val destino = seccionActual * anchoTab + (anchoTab - indicador.layoutParams.width) / 2f
464:         indicador.animate().cancel()
465:         if (animar) {
466:             indicador.animate().translationX(destino).setDuration(260)
467:                 .setInterpolator(DecelerateInterpolator(1.6f)).start()
468:         } else {
469:             indicador.translationX = destino
470:         }
471:     }
472: 
473:     private fun mostrarPanel(anterior: Int, nueva: Int, animar: Boolean) {
474:         val desplazamiento = resources.displayMetrics.density * 28f
475:         val direccion = if (nueva >= anterior) 1 else -1
476: 
477:         paneles.forEachIndexed { indice, panel ->
478:             panel.animate().cancel()
479:             if (!animar || (indice != anterior && indice != nueva)) {
480:                 panel.visibility = if (indice == nueva) View.VISIBLE else View.GONE
481:                 panel.alpha = 1f
482:                 panel.translationX = 0f
483:             }
484:         }
485:         if (!animar || anterior == nueva) return
486: 
487:         val sale = paneles[anterior]
488:         val entra = paneles[nueva]
489: 
490:         if (entra.visibility != View.VISIBLE) {
491:             entra.alpha = 0f
492:             entra.translationX = direccion * desplazamiento
493:             entra.visibility = View.VISIBLE
494:         }
495:         entra.animate().alpha(1f).translationX(0f).setDuration(260)
496:             .setInterpolator(DecelerateInterpolator()).start()
497: 
498:         sale.animate().alpha(0f).translationX(-direccion * desplazamiento).setDuration(160)
499:             .withEndAction {
500:                 sale.visibility = View.GONE
501:                 sale.alpha = 1f
502:                 sale.translationX = 0f
503:             }.start()
504:     }
505: 
506:     private companion object {
507:         const val KEY_SECCION = "mistrabajos_seccion"
508:         const val SECCION_SOLICITUDES = 0
509:         const val SECCION_HISTORIAL = 1
510:     }
511: }

(End of file - total 511 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\firestore.indexes.json"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\firestore.indexes.json</path>
<type>file</type>
<content>
1: {
2:   "indexes": [
3:     {
4:       "collectionGroup": "publications",
5:       "queryScope": "COLLECTION",
6:       "fields": [
7:         { "fieldPath": "status", "order": "ASCENDING" },
8:         { "fieldPath": "visibility", "order": "ASCENDING" },
9:         { "fieldPath": "createdAt", "order": "DESCENDING" }
10:       ]
11:     },
12:     {
13:       "collectionGroup": "publications",
14:       "queryScope": "COLLECTION",
15:       "fields": [
16:         { "fieldPath": "ownerUid", "order": "ASCENDING" },
17:         { "fieldPath": "createdAt", "order": "DESCENDING" }
18:       ]
19:     },
20:     {
21:       "collectionGroup": "applications",
22:       "queryScope": "COLLECTION",
23:       "fields": [
24:         { "fieldPath": "publicationId", "order": "ASCENDING" },
25:         { "fieldPath": "workerUid", "order": "ASCENDING" },
26:         { "fieldPath": "status", "order": "ASCENDING" }
27:       ]
28:     },
29:     {
30:       "collectionGroup": "applications",
31:       "queryScope": "COLLECTION",
32:       "fields": [
33:         { "fieldPath": "publicationId", "order": "ASCENDING" },
34:         { "fieldPath": "workerUid", "order": "ASCENDING" },
35:         { "fieldPath": "createdAt", "order": "DESCENDING" }
36:       ]
37:     },
38:     {
39:       "collectionGroup": "applications",
40:       "queryScope": "COLLECTION",
41:       "fields": [
42:         { "fieldPath": "employerUid", "order": "ASCENDING" },
43:         { "fieldPath": "createdAt", "order": "DESCENDING" }
44:       ]
45:     },
46:     {
47:       "collectionGroup": "applications",
48:       "queryScope": "COLLECTION",
49:       "fields": [
50:         { "fieldPath": "workerUid", "order": "ASCENDING" },
51:         { "fieldPath": "createdAt", "order": "DESCENDING" }
52:       ]
53:     },
54:     {
55:       "collectionGroup": "jobs",
56:       "queryScope": "COLLECTION",
57:       "fields": [
58:         { "fieldPath": "publicationId", "order": "ASCENDING" },
59:         { "fieldPath": "workerUid", "order": "ASCENDING" }
60:       ]
61:     },
62:     {
63:       "collectionGroup": "jobs",
64:       "queryScope": "COLLECTION",
65:       "fields": [
66:         { "fieldPath": "employerUid", "order": "ASCENDING" },
67:         { "fieldPath": "createdAt", "order": "DESCENDING" }
68:       ]
69:     },
70:     {
71:       "collectionGroup": "jobs",
72:       "queryScope": "COLLECTION",
73:       "fields": [
74:         { "fieldPath": "workerUid", "order": "ASCENDING" },
75:         { "fieldPath": "createdAt", "order": "DESCENDING" }
76:       ]
77:     },
78:     {
79:       "collectionGroup": "ratings",
80:       "queryScope": "COLLECTION",
81:       "fields": [
82:         { "fieldPath": "jobId", "order": "ASCENDING" },
83:         { "fieldPath": "fromUid", "order": "ASCENDING" }
84:       ]
85:     },
86:     {
87:       "collectionGroup": "ratings",
88:       "queryScope": "COLLECTION",
89:       "fields": [
90:         { "fieldPath": "toUid", "order": "ASCENDING" },
91:         { "fieldPath": "createdAt", "order": "DESCENDING" }
92:       ]
93:     },
94:     {
95:       "collectionGroup": "comments",
96:       "queryScope": "COLLECTION",
97:       "fields": [
98:         { "fieldPath": "publicationId", "order": "ASCENDING" },
99:         { "fieldPath": "createdAt", "order": "ASCENDING" }
100:       ]
101:     },
102:     {
103:       "collectionGroup": "notifications",
104:       "queryScope": "COLLECTION",
105:       "fields": [
106:         { "fieldPath": "recipientUid", "order": "ASCENDING" },
107:         { "fieldPath": "createdAt", "order": "DESCENDING" }
108:       ]
109:     },
110:     {
111:       "collectionGroup": "notifications",
112:       "queryScope": "COLLECTION",
113:       "fields": [
114:         { "fieldPath": "recipientUid", "order": "ASCENDING" },
115:         { "fieldPath": "read", "order": "ASCENDING" }
116:       ]
117:     },
118:     {
119:       "collectionGroup": "conversations",
120:       "queryScope": "COLLECTION",
121:       "fields": [
122:         { "fieldPath": "participants", "order": "ASCENDING" },
123:         { "fieldPath": "lastMessageAt", "order": "DESCENDING" }
124:       ]
125:     },
126:     {
127:       "collectionGroup": "categories",
128:       "queryScope": "COLLECTION",
129:       "fields": [
130:         { "fieldPath": "active", "order": "ASCENDING" },
131:         { "fieldPath": "order", "order": "ASCENDING" }
132:       ]
133:     }
134:   ],
135:   "fieldOverrides": []
136: }

(End of file - total 136 lines)
</content>
```

---

## Assistant (Build · Muse Spark 1.3 Free · 4.6s)

_Thinking:_



**Tool: grep**

**Input:**
```json
{
  "pattern": "notifications\\.push|NotificationType\\.",
  "path": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java"
}
```

**Output:**
```
Found 16 matches
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\ApplicationRepository.kt:
  Line 124:             notifications.push(

  Line 126:                 type = NotificationType.NEW_APPLICATION,

  Line 183:                     notifications.push(

  Line 185:                         type = NotificationType.APPLICATION_REJECTED,

  Line 230:                 notifications.push(

  Line 232:                     type = NotificationType.APPLICATION_ACCEPTED,


C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\ChatRepository.kt:
  Line 151:                     notifications.push(

  Line 153:                         type = NotificationType.NEW_MESSAGE,


C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\RatingRepository.kt:
  Line 142:             notifications.push(

  Line 144:                 type = com.proyecto.chambaya.data.model.NotificationType.NEW_RATING,


C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\publish\FragmentoPublicar.kt:
  Line 739:                                 type = com.proyecto.chambaya.data.model.NotificationType.JOB_COMPLETED,


C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\CommentsSheet.kt:
  Line 113:                             type = NotificationType.NEW_COMMENT,


C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\JobDetailSheet.kt:
  Line 409:                             type = com.proyecto.chambaya.data.model.NotificationType.NEW_COMMENT,


C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\jobs\NotificationsSheet.kt:
  Line 448:                         NotificationType.NEW_APPLICATION -> "Nueva postulación"

  Line 449:                         NotificationType.APPLICATION_ACCEPTED -> "¡Fuiste seleccionado!"

  Line 450:                         NotificationType.APPLICATION_REJECTED -> "Postulación decidida"

```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\RatingRepository.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\RatingRepository.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.data.repository
2: 
3: import com.google.android.gms.tasks.Tasks
4: import com.google.firebase.firestore.FieldValue
5: import com.google.firebase.firestore.FirebaseFirestore
6: import com.google.firebase.firestore.Query
7: import com.proyecto.chambaya.data.model.Job
8: import com.proyecto.chambaya.data.model.JobStatus
9: import com.proyecto.chambaya.data.model.Rating
10: import com.proyecto.chambaya.data.model.UserProfile
11: import com.proyecto.chambaya.data.model.toRating
12: import com.proyecto.chambaya.data.model.validateRating
13: import kotlinx.coroutines.Dispatchers
14: import kotlinx.coroutines.withContext
15: import kotlin.math.round
16: 
17: /**
18:  * FASE 9 — Calificaciones en ambas direcciones, solo con el job COMPLETED
19:  * y una sola vez por (jobId + fromUid). Actualiza el promedio del evaluado.
20:  */
21: class RatingRepository(
22:     private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
23: ) {
24:     private val notifications = NotificationRepository(firestore)
25: 
26:     suspend fun existingFor(jobId: String, fromUid: String): Result<Rating?> =
27:         withContext(Dispatchers.IO) {
28:             runCatching {
29:                 if (jobId.isBlank() || fromUid.isBlank()) return@runCatching null
30:                 Tasks.await(
31:                     firestore.collection(COLLECTION)
32:                         .whereEqualTo("jobId", jobId)
33:                         .whereEqualTo("fromUid", fromUid)
34:                         .limit(1)
35:                         .get()
36:                 ).documents.firstOrNull()?.toRating()
37:             }
38:         }
39: 
40:     suspend fun receivedBy(uid: String, limit: Long = 20): Result<List<Rating>> =
41:         withContext(Dispatchers.IO) {
42:             runCatching {
43:                 Tasks.await(
44:                     firestore.collection(COLLECTION)
45:                         .whereEqualTo("toUid", uid)
46:                         .orderBy("createdAt", Query.Direction.DESCENDING)
47:                         .limit(limit)
48:                         .get()
49:                 ).documents.map { it.toRating() }
50:             }
51:         }
52: 
53:     /** Jobs ya calificados por [fromUid] (para pintar "Calificar" solo si falta). */
54:     suspend fun ratedJobIds(jobIds: List<String>, fromUid: String): Result<Set<String>> =
55:         withContext(Dispatchers.IO) {
56:             runCatching {
57:                 val out = mutableSetOf<String>()
58:                 if (jobIds.isEmpty() || fromUid.isBlank()) return@runCatching out
59:                 jobIds.distinct().filter { it.isNotBlank() }.chunked(30).forEach { chunk ->
60:                     Tasks.await(
61:                         firestore.collection(COLLECTION)
62:                             .whereIn("jobId", chunk)
63:                             .whereEqualTo("fromUid", fromUid)
64:                             .get()
65:                     ).documents.map { it.toRating() }
66:                         .filter { it.fromUid == fromUid }
67:                         .forEach { out += it.jobId }
68:                 }
69:                 out
70:             }
71:         }
72: 
73:     suspend fun rate(
74:         rater: UserProfile,
75:         job: Job,
76:         stars: Int,
77:         comment: String
78:     ): Result<Rating> = withContext(Dispatchers.IO) {
79:         runCatching {
80:             val errores = validateRating(stars, comment)
81:             require(errores.isEmpty()) { errores.first() }
82:             require(job.status == JobStatus.COMPLETED) {
83:                 "Solo puedes calificar un trabajo completado."
84:             }
85:             val fromUid = rater.uid
86:             require(fromUid == job.workerUid || fromUid == job.employerUid) {
87:                 "Ese trabajo no te involucra."
88:             }
89:             require(existingFor(job.jobId, fromUid).getOrThrow() == null) {
90:                 "Ya calificaste este trabajo."
91:             }
92:             val toUid = if (fromUid == job.workerUid) job.employerUid else job.workerUid
93:             // El agregado lo escribe quien califica (rama isAllowedRatingUpdate):
94:             // el destino debe tener sus bloques listos.
95:             val targetSnap = Tasks.await(
96:                 firestore.collection(ProfileRepository.COLLECTION_USERS).document(toUid).get()
97:             )
98:             val toWorker = toUid == job.workerUid
99:             require((targetSnap.get(if (toWorker) "worker" else "employer") as? Map<*, *>) != null) {
100:                 "Ese perfil aún no tiene historial para calificar."
101:             }
102:             require((targetSnap.get("statistics") as? Map<*, *>) != null) {
103:                 "Ese perfil aún no está listo para recibir calificaciones."
104:             }
105:             val ref = firestore.collection(COLLECTION).document()
106:             Tasks.await(
107:                 ref.set(
108:                     mapOf(
109:                         "ratingId" to ref.id,
110:                         "jobId" to job.jobId,
111:                         "publicationId" to job.publicationId,
112:                         "publicationTitle" to job.publicationTitle,
113:                         "fromUid" to fromUid,
114:                         "toUid" to toUid,
115:                         "rating" to stars,
116:                         "comment" to comment.trim().take(500),
117:                         "createdAt" to FieldValue.serverTimestamp()
118:                     )
119:                 )
120:             )
121:             // Promedio del evaluado: al trabajador se le mueve worker.*,
122:             // al contratante employer.* (+1 en recibidas en ambos casos).
123:             // Se reutiliza targetSnap (leído arriba): evita una lectura y
124:             // garantiza que el promedio parte de lo validado.
125:             val userRef = firestore.collection(ProfileRepository.COLLECTION_USERS).document(toUid)
126:             val block = targetSnap.get(if (toWorker) "worker" else "employer") as? Map<*, *>
127:             val oldAvg = (block?.get("ratingAverage") as? Number)?.toDouble() ?: 0.0
128:             val oldCount = (block?.get("ratingCount") as? Number)?.toInt() ?: 0
129:             val newAvg = round(((oldAvg * oldCount + stars) / (oldCount + 1)) * 10) / 10.0
130:             val prefix = if (toWorker) "worker" else "employer"
131:             Tasks.await(
132:                 userRef.update(
133:                     mapOf(
134:                         "$prefix.ratingAverage" to newAvg,
135:                         "$prefix.ratingCount" to oldCount + 1,
136:                         "statistics.receivedRatingsCount" to FieldValue.increment(1),
137:                         "updatedAt" to FieldValue.serverTimestamp()
138:                     )
139:                 )
140:             )
141:             // Aviso al evaluado (FASE 14).
142:             notifications.push(
143:                 recipientUid = toUid,
144:                 type = com.proyecto.chambaya.data.model.NotificationType.NEW_RATING,
145:                 title = "Nueva calificación",
146:                 message = "Recibiste $stars ${if (stars == 1) "estrella" else "estrellas"} por “${job.publicationTitle.take(60)}”.",
147:                 senderUid = fromUid,
148:                 publicationId = job.publicationId
149:             )
150:             Tasks.await(ref.get()).toRating()
151:         }
152:     }
153: 
154:     companion object {
155:         const val COLLECTION = "ratings"
156:     }
157: }

(End of file - total 157 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\ChatRepository.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\ChatRepository.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.data.repository
2: 
3: import com.google.android.gms.tasks.Tasks
4: import com.google.firebase.firestore.FieldValue
5: import com.google.firebase.firestore.FirebaseFirestore
6: import com.google.firebase.firestore.ListenerRegistration
7: import com.google.firebase.firestore.Query
8: import com.proyecto.chambaya.data.model.ChatMessage
9: import com.proyecto.chambaya.data.model.ChatLimits
10: import com.proyecto.chambaya.data.model.Conversation
11: import com.proyecto.chambaya.data.model.MessageType
12: import com.proyecto.chambaya.data.model.NotificationType
13: import com.proyecto.chambaya.data.model.conversationIdFor
14: import com.proyecto.chambaya.data.model.toChatMessage
15: import com.proyecto.chambaya.data.model.toConversation
16: import kotlinx.coroutines.Dispatchers
17: import kotlinx.coroutines.withContext
18: 
19: /**
20:  * FASE 13 — Conversaciones y mensajes en tiempo real.
21:  */
22: class ChatRepository(
23:     private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
24:     private val blocks: BlockRepository = BlockRepository(firestore),
25:     private val notifications: NotificationRepository = NotificationRepository(firestore)
26: ) {
27: 
28:     /**
29:      * Abre (o crea) la conversación 1:1. Falla si hay bloqueo en
30:      * cualquier dirección.
31:      */
32:     suspend fun ensureConversation(
33:         myUid: String,
34:         otherUid: String,
35:         publicationId: String = "",
36:         publicationTitle: String = ""
37:     ): Result<Conversation> = withContext(Dispatchers.IO) {
38:         runCatching {
39:             require(myUid.isNotBlank() && otherUid.isNotBlank() && myUid != otherUid) {
40:                 "Conversación no válida."
41:             }
42:             if (blocks.isBlocked(myUid, otherUid)) {
43:                 throw IllegalStateException("Desbloquea a este usuario para chatear.")
44:             }
45:             if (blocks.isBlocked(otherUid, myUid)) {
46:                 throw IllegalStateException("No puedes iniciar este chat por ahora.")
47:             }
48:             val id = conversationIdFor(publicationId, myUid, otherUid)
49:             // OJO: no usar get() directo aquí — si el doc no existe, la regla
50:             // `isParticipant(resource.data)` deniega la lectura. La consulta
51:             // por campo (allow list abierto) devuelve vacío sin problema.
52:             val existing = Tasks.await(
53:                 firestore.collection(COLLECTION)
54:                     .whereEqualTo("conversationId", id)
55:                     .limit(1)
56:                     .get()
57:             ).documents.firstOrNull()?.toConversation()
58:             if (existing != null) return@runCatching existing
59:             val ref = firestore.collection(COLLECTION).document(id)
60:             val now = FieldValue.serverTimestamp()
61:             val participants = listOf(myUid, otherUid).sorted()
62:             Tasks.await(
63:                 ref.set(
64:                     mapOf(
65:                         "conversationId" to id,
66:                         "participants" to participants,
67:                         "publicationId" to publicationId,
68:                         "publicationTitle" to publicationTitle.take(120),
69:                         "lastMessage" to "",
70:                         "lastMessageAt" to now,
71:                         "createdAt" to now
72:                     )
73:                 )
74:             )
75:             Tasks.await(ref.get()).toConversation()
76:         }
77:     }
78: 
79:     fun listenMine(
80:         uid: String,
81:         onUpdate: (List<Conversation>) -> Unit,
82:         onError: (Exception) -> Unit
83:     ): ListenerRegistration {
84:         return firestore.collection(COLLECTION)
85:             .whereArrayContains("participants", uid)
86:             .orderBy("lastMessageAt", Query.Direction.DESCENDING)
87:             .limit(50)
88:             .addSnapshotListener { snap, e ->
89:                 if (e != null) { onError(e); return@addSnapshotListener }
90:                 if (snap == null) return@addSnapshotListener
91:                 runCatching { snap.documents.map { it.toConversation() } }
92:                     .onSuccess(onUpdate)
93:                     .onFailure { onError(it as? Exception ?: Exception(it)) }
94:             }
95:     }
96: 
97:     fun listenMessages(
98:         conversationId: String,
99:         onUpdate: (List<ChatMessage>) -> Unit,
100:         onError: (Exception) -> Unit
101:     ): ListenerRegistration {
102:         return firestore.collection(COLLECTION).document(conversationId)
103:             .collection(SUB_MESSAGES)
104:             .orderBy("createdAt", Query.Direction.ASCENDING)
105:             .limit(200)
106:             .addSnapshotListener { snap, e ->
107:                 if (e != null) { onError(e); return@addSnapshotListener }
108:                 if (snap == null) return@addSnapshotListener
109:                 runCatching { snap.documents.map { it.toChatMessage() } }
110:                     .onSuccess(onUpdate)
111:                     .onFailure { onError(it as? Exception ?: Exception(it)) }
112:             }
113:     }
114: 
115:     suspend fun sendMessage(senderUid: String, conversationId: String, text: String): Result<Unit> =
116:         withContext(Dispatchers.IO) {
117:             runCatching {
118:                 val clean = text.trim().take(ChatLimits.TEXT_MAX)
119:                 require(clean.isNotEmpty()) { "Escribe un mensaje." }
120:                 require(senderUid.isNotBlank() && conversationId.isNotBlank()) { "Sesión no válida." }
121:                 val convRef = firestore.collection(COLLECTION).document(conversationId)
122:                 val conv = Tasks.await(convRef.get()).takeIf { it.exists() }?.toConversation()
123:                     ?: throw IllegalArgumentException("La conversación ya no existe.")
124:                 require(conv.participants.contains(senderUid)) { "Esa conversación no es tuya." }
125:                 val msgRef = convRef.collection(SUB_MESSAGES).document()
126:                 val now = FieldValue.serverTimestamp()
127:                 Tasks.await(
128:                     msgRef.set(
129:                         mapOf(
130:                             "messageId" to msgRef.id,
131:                             "senderUid" to senderUid,
132:                             "type" to MessageType.TEXT,
133:                             "text" to clean,
134:                             "read" to false,
135:                             "createdAt" to now
136:                         )
137:                     )
138:                 )
139:                 runCatching {
140:                     Tasks.await(
141:                         convRef.update(
142:                             mapOf(
143:                                 "lastMessage" to clean.take(160),
144:                                 "lastMessageAt" to now
145:                             )
146:                         )
147:                     )
148:                 }
149:                 val other = conv.otherUid(senderUid)
150:                 if (other.isNotBlank()) {
151:                     notifications.push(
152:                         recipientUid = other,
153:                         type = NotificationType.NEW_MESSAGE,
154:                         title = "Nuevo mensaje",
155:                         message = clean.take(120),
156:                         senderUid = senderUid,
157:                         publicationId = conv.publicationId
158:                     )
159:                 }
160:                 Unit
161:             }
162:         }
163: 
164:     /** Marca como leídos los mensajes del otro (lote de hasta 50). */
165:     suspend fun markRead(uid: String, conversationId: String): Result<Unit> =
166:         withContext(Dispatchers.IO) {
167:             runCatching {
168:                 val snap = Tasks.await(
169:                     firestore.collection(COLLECTION).document(conversationId)
170:                         .collection(SUB_MESSAGES)
171:                         .whereEqualTo("read", false)
172:                         .limit(50)
173:                         .get()
174:                 )
175:                 val batch = firestore.batch()
176:                 var n = 0
177:                 snap.documents.map { it.toChatMessage() }
178:                     .filter { !it.read && it.senderUid != uid }
179:                     .forEach {
180:                         batch.update(
181:                             firestore.collection(COLLECTION).document(conversationId)
182:                                 .collection(SUB_MESSAGES).document(it.messageId),
183:                             "read", true
184:                         )
185:                         n++
186:                     }
187:                 if (n > 0) Tasks.await(batch.commit())
188:                 Unit
189:             }
190:         }
191: 
192:     /** No leídos de una conversación (para el badge de la lista). */
193:     suspend fun unreadIn(conversationId: String, uid: String): Int =
194:         withContext(Dispatchers.IO) {
195:             runCatching {
196:                 Tasks.await(
197:                     firestore.collection(COLLECTION).document(conversationId)
198:                         .collection(SUB_MESSAGES)
199:                         .whereEqualTo("read", false)
200:                         .limit(100)
201:                         .get()
202:                 ).documents.map { it.toChatMessage() }
203:                     .count { it.senderUid != uid }
204:             }.getOrDefault(0)
205:         }
206: 
207:     companion object {
208:         const val COLLECTION = "conversations"
209:         const val SUB_MESSAGES = "messages"
210:     }
211: }

(End of file - total 211 lines)
</content>
```

---

## Assistant (Build · Muse Spark 1.3 Free · 11.4s)

_Thinking:_



**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\PublicationInteractionRepository.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\PublicationInteractionRepository.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.data.repository
2: 
3: import com.google.android.gms.tasks.Tasks
4: import com.google.firebase.firestore.FieldValue
5: import com.google.firebase.firestore.FirebaseFirestore
6: import kotlinx.coroutines.Dispatchers
7: import kotlinx.coroutines.withContext
8: 
9: /**
10:  * FASE 6 (acciones del feed) — likes, guardados, "no me interesa" y denuncias.
11:  *
12:  * Cada interacción vive en su propia colección (nunca listas dentro del
13:  * documento) para no crear arreglos que crezcan sin límite:
14:  *  - publication_likes/{publicationId_uid}
15:  *  - publication_saves/{publicationId_uid}
16:  *  - hidden_publications/{publicationId_uid}
17:  *  - publication_reports (un doc por denuncia)
18:  *
19:  * Los contadores statistics.likes / statistics.saves se mantienen con
20:  * incrementos atómicos en publications/{id}.
21:  */
22: class PublicationInteractionRepository(
23:     private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
24: ) {
25: 
26:     private fun docId(publicationId: String, uid: String) = "${publicationId}_${uid}"
27: 
28:     // ── LIKES ────────────────────────────────────────────────
29: 
30:     suspend fun isLiked(publicationId: String, uid: String): Boolean =
31:         withContext(Dispatchers.IO) {
32:             runCatching {
33:                 Tasks.await(
34:                     firestore.collection(COL_LIKES).document(docId(publicationId, uid)).get()
35:                 ).exists()
36:             }.getOrDefault(false)
37:         }
38: 
39:     /** Alterna el like. Devuelve el estado final (true = con like). */
40:     suspend fun toggleLike(publicationId: String, uid: String): Result<Boolean> =
41:         withContext(Dispatchers.IO) {
42:             runCatching {
43:                 val ref = firestore.collection(COL_LIKES).document(docId(publicationId, uid))
44:                 val existe = Tasks.await(ref.get()).exists()
45:                 val pubRef = firestore.collection(PublicationRepository.COLLECTION).document(publicationId)
46:                 if (existe) {
47:                     Tasks.await(ref.delete())
48:                     runCatching { Tasks.await(pubRef.update("statistics.likes", FieldValue.increment(-1))) }
49:                     false
50:                 } else {
51:                     Tasks.await(
52:                         ref.set(
53:                             mapOf(
54:                                 "publicationId" to publicationId,
55:                                 "userUid" to uid,
56:                                 "createdAt" to FieldValue.serverTimestamp()
57:                             )
58:                         )
59:                     )
60:                     runCatching { Tasks.await(pubRef.update("statistics.likes", FieldValue.increment(1))) }
61:                     true
62:                 }
63:             }
64:         }
65: 
66:     // ── GUARDADOS ────────────────────────────────────────────
67: 
68:     suspend fun isSaved(publicationId: String, uid: String): Boolean =
69:         withContext(Dispatchers.IO) {
70:             runCatching {
71:                 Tasks.await(
72:                     firestore.collection(COL_SAVES).document(docId(publicationId, uid)).get()
73:                 ).exists()
74:             }.getOrDefault(false)
75:         }
76: 
77:     suspend fun savedIds(publicationIds: List<String>, uid: String): Set<String> =
78:         withContext(Dispatchers.IO) {
79:             if (publicationIds.isEmpty() || uid.isBlank()) return@withContext emptySet()
80:             val resultado = mutableSetOf<String>()
81:             // Lecturas puntuales en paralelo (lotes pequeños del feed).
82:             publicationIds.chunked(10).forEach { lote ->
83:                 lote.map { pid ->
84:                     runCatching {
85:                         Tasks.await(firestore.collection(COL_SAVES).document(docId(pid, uid)).get())
86:                             .takeIf { it.exists() }?.let { resultado += pid }
87:                     }
88:                 }
89:             }
90:             resultado
91:         }
92: 
93:     suspend fun likedIds(publicationIds: List<String>, uid: String): Set<String> =
94:         withContext(Dispatchers.IO) {
95:             if (publicationIds.isEmpty() || uid.isBlank()) return@withContext emptySet()
96:             val resultado = mutableSetOf<String>()
97:             publicationIds.chunked(10).forEach { lote ->
98:                 lote.forEach { pid ->
99:                     runCatching {
100:                         Tasks.await(firestore.collection(COL_LIKES).document(docId(pid, uid)).get())
101:                             .takeIf { it.exists() }?.let { resultado += pid }
102:                     }
103:                 }
104:             }
105:             resultado
106:         }
107: 
108:     /** Ids guardados por mí (para la bandeja de Guardados). */
109:     suspend fun mySaves(uid: String, limit: Long = 100): Result<Set<String>> =
110:         withContext(Dispatchers.IO) {
111:             runCatching {
112:                 if (uid.isBlank()) return@runCatching emptySet()
113:                 Tasks.await(
114:                     firestore.collection(COL_SAVES)
115:                         .whereEqualTo("userUid", uid)
116:                         .limit(limit)
117:                         .get()
118:                 ).documents.mapNotNull { it.getString("publicationId") }.toSet()
119:             }
120:         }
121: 
122:     /** Alterna el guardado. Devuelve el estado final (true = guardado). */
123:     suspend fun toggleSave(publicationId: String, uid: String): Result<Boolean> =
124:         withContext(Dispatchers.IO) {
125:             runCatching {
126:                 val ref = firestore.collection(COL_SAVES).document(docId(publicationId, uid))
127:                 val existe = Tasks.await(ref.get()).exists()
128:                 val pubRef = firestore.collection(PublicationRepository.COLLECTION).document(publicationId)
129:                 if (existe) {
130:                     Tasks.await(ref.delete())
131:                     runCatching { Tasks.await(pubRef.update("statistics.saves", FieldValue.increment(-1))) }
132:                     false
133:                 } else {
134:                     Tasks.await(
135:                         ref.set(
136:                             mapOf(
137:                                 "publicationId" to publicationId,
138:                                 "userUid" to uid,
139:                                 "createdAt" to FieldValue.serverTimestamp()
140:                             )
141:                         )
142:                     )
143:                     runCatching { Tasks.await(pubRef.update("statistics.saves", FieldValue.increment(1))) }
144:                     true
145:                 }
146:             }
147:         }
148: 
149:     // ── NO ME INTERESA ───────────────────────────────────────
150: 
151:     suspend fun hide(publicationId: String, uid: String, reason: String = "NOT_INTERESTED"): Result<Unit> =
152:         withContext(Dispatchers.IO) {
153:             runCatching {
154:                 Tasks.await(
155:                     firestore.collection(COL_HIDDEN).document(docId(publicationId, uid))
156:                         .set(
157:                             mapOf(
158:                                 "publicationId" to publicationId,
159:                                 "userUid" to uid,
160:                                 "reason" to reason,
161:                                 "createdAt" to FieldValue.serverTimestamp()
162:                             )
163:                         )
164:                 )
165:                 Unit
166:             }
167:         }
168: 
169:     suspend fun hiddenIds(publicationIds: List<String>, uid: String): Set<String> =
170:         withContext(Dispatchers.IO) {
171:             if (publicationIds.isEmpty() || uid.isBlank()) return@withContext emptySet()
172:             val resultado = mutableSetOf<String>()
173:             publicationIds.forEach { pid ->
174:                 runCatching {
175:                     Tasks.await(firestore.collection(COL_HIDDEN).document(docId(pid, uid)).get())
176:                         .takeIf { it.exists() }?.let { resultado += pid }
177:                 }
178:             }
179:             resultado
180:         }
181: 
182:     // ── DENUNCIAS ────────────────────────────────────────────
183: 
184:     suspend fun report(
185:         publicationId: String,
186:         reporterUid: String,
187:         reason: String,
188:         description: String = ""
189:     ): Result<Unit> = withContext(Dispatchers.IO) {
190:         runCatching {
191:             require(reason.isNotBlank()) { "Elige un motivo." }
192:             Tasks.await(
193:                 firestore.collection(COL_REPORTS).add(
194:                     mapOf(
195:                         "publicationId" to publicationId,
196:                         "reporterUid" to reporterUid,
197:                         "reason" to reason,
198:                         "description" to description.trim().take(500),
199:                         "status" to "PENDING",
200:                         "createdAt" to FieldValue.serverTimestamp()
201:                     )
202:                 )
203:             )
204:             Unit
205:         }
206:     }
207: 
208:     companion object {
209:         const val COL_LIKES = "publication_likes"
210:         const val COL_SAVES = "publication_saves"
211:         const val COL_HIDDEN = "hidden_publications"
212:         const val COL_REPORTS = "publication_reports"
213:     }
214: }

(End of file - total 214 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\ui\\profile\\JobsSheet.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\ui\profile\JobsSheet.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.ui.profile
2: 
3: import android.os.Bundle
4: import android.view.LayoutInflater
5: import android.view.View
6: import android.view.ViewGroup
7: import android.widget.ProgressBar
8: import android.widget.TextView
9: import androidx.core.os.bundleOf
10: import androidx.lifecycle.lifecycleScope
11: import androidx.recyclerview.widget.LinearLayoutManager
12: import androidx.recyclerview.widget.RecyclerView
13: import com.google.android.material.bottomsheet.BottomSheetDialogFragment
14: import com.google.firebase.auth.FirebaseAuth
15: import com.proyecto.chambaya.R
16: import com.proyecto.chambaya.data.model.JobStatus
17: import com.proyecto.chambaya.data.model.UserRoles
18: import com.proyecto.chambaya.data.repository.JobRepository
19: import com.proyecto.chambaya.data.repository.ProfileRepository
20: import com.proyecto.chambaya.data.repository.RatingRepository
21: import com.proyecto.chambaya.ui.jobs.RateSheet
22: import kotlinx.coroutines.launch
23: 
24: /**
25:  * FASE 19 — Mis trabajos (trabajador) o trabajos contratados (contratante),
26:  * con su estado, contraparte y calificación cuando corresponde.
27:  */
28: class JobsSheet : BottomSheetDialogFragment() {
29: 
30:     private val jobRepo = JobRepository()
31:     private val profileRepo = ProfileRepository()
32:     private val ratingRepo = RatingRepository()
33:     private var adapter: JobHistoryAdapter? = null
34: 
35:     override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
36:         return inflater.inflate(R.layout.bottom_sheet_jobs, container, false)
37:     }
38: 
39:     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
40:         super.onViewCreated(view, savedInstanceState)
41:         val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
42:         if (uid.isBlank()) { dismiss(); return }
43:         val comoEmpleador = requireArguments().getBoolean(ARG_EMPLOYER)
44:         view.findViewById<TextView>(R.id.tvJobsTitle).text =
45:             if (comoEmpleador) "Trabajos contratados" else "Mis trabajos"
46: 
47:         val rv = view.findViewById<RecyclerView>(R.id.rvJobs)
48:         rv.layoutManager = LinearLayoutManager(requireContext())
49:         adapter = JobHistoryAdapter(
50:             onRate = { row ->
51:                 RateSheet.newInstance(row.job.jobId).show(parentFragmentManager, "rate")
52:             }
53:         )
54:         rv.adapter = adapter
55:         val progress = view.findViewById<ProgressBar>(R.id.progressJobs)
56:         val empty = view.findViewById<TextView>(R.id.tvJobsEmpty)
57:         progress.visibility = View.VISIBLE
58: 
59:         parentFragmentManager.setFragmentResultListener(RateSheet.REQUEST_RATED, viewLifecycleOwner) { _, _ ->
60:             cargar(view, uid, comoEmpleador)
61:         }
62:         cargar(view, uid, comoEmpleador)
63:     }
64: 
65:     private fun cargar(view: View, uid: String, comoEmpleador: Boolean) {
66:         val progress = view.findViewById<ProgressBar>(R.id.progressJobs)
67:         val empty = view.findViewById<TextView>(R.id.tvJobsEmpty)
68:         viewLifecycleOwner.lifecycleScope.launch {
69:             val jobs = if (comoEmpleador) {
70:                 jobRepo.listByEmployer(uid).getOrNull().orEmpty()
71:             } else {
72:                 jobRepo.listByWorker(uid).getOrNull().orEmpty()
73:             }
74:             val others = jobs.map { if (comoEmpleador) it.workerUid else it.employerUid }.distinct()
75:             val perfiles = mutableMapOf<String, com.proyecto.chambaya.data.model.PublicProfile>()
76:             others.forEach { id ->
77:                 profileRepo.loadPublicProfile(id).getOrNull()?.let { perfiles[id] = it }
78:             }
79:             val completedIds = jobs.filter { it.status == JobStatus.COMPLETED }.map { it.jobId }
80:             val rated = if (completedIds.isEmpty()) emptySet()
81:             else ratingRepo.ratedJobIds(completedIds, uid).getOrNull().orEmpty()
82:             if (!isAdded) return@launch
83:             progress.visibility = View.GONE
84:             if (jobs.isEmpty()) {
85:                 empty.visibility = View.VISIBLE
86:             } else {
87:                 empty.visibility = View.GONE
88:                 adapter?.submitList(
89:                     jobs.map { job ->
90:                         val otherId = if (comoEmpleador) job.workerUid else job.employerUid
91:                         JobHistoryRow(job, perfiles[otherId], job.jobId in rated)
92:                     }
93:                 )
94:             }
95:         }
96:     }
97: 
98:     override fun onDestroyView() {
99:         adapter = null
100:         super.onDestroyView()
101:     }
102: 
103:     companion object {
104:         private const val ARG_EMPLOYER = "employer"
105:         fun newInstance(comoEmpleador: Boolean) = JobsSheet().apply {
106:             arguments = bundleOf(ARG_EMPLOYER to comoEmpleador)
107:         }
108:     }
109: }

(End of file - total 109 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\res\\layout\\bottom_sheet_public_profile.xml"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\res\layout\bottom_sheet_public_profile.xml</path>
<type>file</type>
<content>
1: <?xml version="1.0" encoding="utf-8"?>
2: <!-- FASE 6 — Perfil público del contratante (BottomSheet).
3:      Muestra foto, verificación, sector, reputación y lugar. Sin datos privados. -->
4: <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
5:     xmlns:app="http://schemas.android.com/apk/res-auto"
6:     xmlns:tools="http://schemas.android.com/tools"
7:     android:layout_width="match_parent"
8:     android:layout_height="wrap_content"
9:     android:orientation="vertical"
10:     android:background="@drawable/bg_bottom_sheet_rounded"
11:     android:paddingStart="20dp"
12:     android:paddingEnd="20dp"
13:     android:paddingBottom="28dp">
14: 
15:     <View
16:         android:layout_width="38dp"
17:         android:layout_height="4dp"
18:         android:layout_gravity="center_horizontal"
19:         android:layout_marginTop="10dp"
20:         android:layout_marginBottom="16dp"
21:         android:background="@drawable/bg_bottom_sheet_handle" />
22: 
23:     <LinearLayout
24:         android:layout_width="match_parent"
25:         android:layout_height="wrap_content"
26:         android:gravity="center_vertical"
27:         android:orientation="horizontal">
28: 
29:         <com.google.android.material.imageview.ShapeableImageView
30:             android:id="@+id/ivPublicAvatar"
31:             android:layout_width="64dp"
32:             android:layout_height="64dp"
33:             android:src="@drawable/ic_user_circle"
34:             app:shapeAppearanceOverlay="@style/CircularImageStyle" />
35: 
36:         <LinearLayout
37:             android:layout_width="0dp"
38:             android:layout_height="wrap_content"
39:             android:layout_weight="1"
40:             android:layout_marginStart="14dp"
41:             android:orientation="vertical">
42: 
43:             <LinearLayout
44:                 android:layout_width="wrap_content"
45:                 android:layout_height="wrap_content"
46:                 android:gravity="center_vertical"
47:                 android:orientation="horizontal">
48: 
49:                 <TextView
50:                     android:id="@+id/tvPublicName"
51:                     android:layout_width="wrap_content"
52:                     android:layout_height="wrap_content"
53:                     android:fontFamily="sans-serif-medium"
54:                     android:textColor="@color/text_primary"
55:                     android:textSize="17sp"
56:                     tools:text="Constructora Andina" />
57: 
58:                 <ImageView
59:                     android:id="@+id/ivPublicVerified"
60:                     android:layout_width="17dp"
61:                     android:layout_height="17dp"
62:                     android:layout_marginStart="6dp"
63:                     android:src="@drawable/ic_verified"
64:                     android:contentDescription="Verificado" />
65:             </LinearLayout>
66: 
67:             <TextView
68:                 android:id="@+id/tvPublicUsername"
69:                 android:layout_width="wrap_content"
70:                 android:layout_height="wrap_content"
71:                 android:layout_marginTop="2dp"
72:                 android:textColor="@color/text_secondary"
73:                 android:textSize="13sp"
74:                 tools:text="@constructora_andina · Empresa" />
75:         </LinearLayout>
76:     </LinearLayout>
77: 
78:     <LinearLayout
79:         android:layout_width="match_parent"
80:         android:layout_height="wrap_content"
81:         android:layout_marginTop="16dp"
82:         android:orientation="horizontal">
83: 
84:         <LinearLayout
85:             android:layout_width="0dp"
86:             android:layout_height="wrap_content"
87:             android:layout_weight="1"
88:             android:gravity="center"
89:             android:orientation="vertical">
90: 
91:             <TextView
92:                 android:id="@+id/tvPublicPublished"
93:                 android:layout_width="wrap_content"
94:                 android:layout_height="wrap_content"
95:                 android:fontFamily="sans-serif-black"
96:                 android:textColor="@color/text_primary"
97:                 android:textSize="17sp"
98:                 tools:text="12" />
99: 
100:             <TextView
101:                 android:id="@+id/tvStatLabel1"
102:                 android:layout_width="wrap_content"
103:                 android:layout_height="wrap_content"
104:                 android:text="Publicadas"
105:                 android:textColor="@color/text_secondary"
106:                 android:textSize="12sp" />
107:         </LinearLayout>
108: 
109:         <LinearLayout
110:             android:layout_width="0dp"
111:             android:layout_height="wrap_content"
112:             android:layout_weight="1"
113:             android:gravity="center"
114:             android:orientation="vertical">
115: 
116:             <TextView
117:                 android:id="@+id/tvPublicHired"
118:                 android:layout_width="wrap_content"
119:                 android:layout_height="wrap_content"
120:                 android:fontFamily="sans-serif-black"
121:                 android:textColor="@color/text_primary"
122:                 android:textSize="17sp"
123:                 tools:text="8" />
124: 
125:             <TextView
126:                 android:id="@+id/tvStatLabel2"
127:                 android:layout_width="wrap_content"
128:                 android:layout_height="wrap_content"
129:                 android:text="Contrataciones"
130:                 android:textColor="@color/text_secondary"
131:                 android:textSize="12sp" />
132:         </LinearLayout>
133: 
134:         <LinearLayout
135:             android:layout_width="0dp"
136:             android:layout_height="wrap_content"
137:             android:layout_weight="1"
138:             android:gravity="center"
139:             android:orientation="vertical">
140: 
141:             <TextView
142:                 android:id="@+id/tvPublicRating"
143:                 android:layout_width="wrap_content"
144:                 android:layout_height="wrap_content"
145:                 android:fontFamily="sans-serif-black"
146:                 android:textColor="@color/text_primary"
147:                 android:textSize="17sp"
148:                 tools:text="4.8" />
149: 
150:             <TextView
151:                 android:layout_width="wrap_content"
152:                 android:layout_height="wrap_content"
153:                 android:text="Calificación"
154:                 android:textColor="@color/text_secondary"
155:                 android:textSize="12sp" />
156:         </LinearLayout>
157:     </LinearLayout>
158: 
159:     <LinearLayout
160:         android:layout_width="match_parent"
161:         android:layout_height="wrap_content"
162:         android:layout_marginTop="16dp"
163:         android:orientation="horizontal">
164: 
165:         <com.google.android.material.button.MaterialButton
166:             android:id="@+id/btnPublicMessage"
167:             android:layout_width="0dp"
168:             android:layout_height="48dp"
169:             android:layout_weight="1"
170:             android:text="Enviar mensaje"
171:             android:textColor="@android:color/white"
172:             android:textSize="14sp"
173:             app:cornerRadius="14dp"
174:             app:backgroundTint="@color/brand_color" />
175: 
176:         <com.google.android.material.button.MaterialButton
177:             android:id="@+id/btnPublicMore"
178:             android:layout_width="52dp"
179:             android:layout_height="48dp"
180:             android:layout_marginStart="8dp"
181:             android:insetTop="0dp"
182:             android:insetBottom="0dp"
183:             app:cornerRadius="14dp"
184:             app:backgroundTint="@color/background_light"
185:             app:icon="@drawable/ic_more_vertical"
186:             app:iconTint="@color/text_primary"
187:             app:iconGravity="textStart"
188:             app:iconPadding="0dp"
189:             android:contentDescription="Más opciones" />
190:     </LinearLayout>
191: 
192:     <TextView
193:         android:id="@+id/tvPublicBio"
194:         android:layout_width="match_parent"
195:         android:layout_height="wrap_content"
196:         android:layout_marginTop="14dp"
197:         android:lineSpacingMultiplier="1.35"
198:         android:textColor="@color/text_primary"
199:         android:textSize="14sp"
200:         android:visibility="gone"
201:         tools:visibility="visible"
202:         tools:text="Empresa ayacuchana dedicada a la construcción y remodelación." />
203: 
204:     <LinearLayout
205:         android:id="@+id/rowPublicPlace"
206:         android:layout_width="match_parent"
207:         android:layout_height="wrap_content"
208:         android:layout_marginTop="12dp"
209:         android:background="@drawable/bg_chip_inactive"
210:         android:gravity="center_vertical"
211:         android:orientation="horizontal"
212:         android:padding="14dp"
213:         android:visibility="gone"
214:         tools:visibility="visible">
215: 
216:         <ImageView
217:             android:layout_width="22dp"
218:             android:layout_height="22dp"
219:             android:src="@drawable/ic_home_marker"
220:             android:contentDescription="@null" />
221: 
222:         <LinearLayout
223:             android:layout_width="match_parent"
224:             android:layout_height="wrap_content"
225:             android:layout_marginStart="12dp"
226:             android:orientation="vertical">
227: 
228:             <TextView
229:                 android:id="@+id/tvPublicPlaceName"
230:                 android:layout_width="wrap_content"
231:                 android:layout_height="wrap_content"
232:                 android:fontFamily="sans-serif-medium"
233:                 android:textColor="@color/text_primary"
234:                 android:textSize="14sp"
235:                 tools:text="Ferretería El Sol" />
236: 
237:             <TextView
238:                 android:id="@+id/tvPublicPlaceDistrict"
239:                 android:layout_width="wrap_content"
240:                 android:layout_height="wrap_content"
241:                 android:textColor="@color/text_secondary"
242:                 android:textSize="12sp"
243:                 tools:text="Carmen Alto, Huamanga" />
244:         </LinearLayout>
245:     </LinearLayout>
246: 
247:     <ProgressBar
248:         android:id="@+id/progressPublic"
249:         android:layout_width="wrap_content"
250:         android:layout_height="wrap_content"
251:         android:layout_gravity="center_horizontal"
252:         android:layout_marginTop="20dp"
253:         android:visibility="gone" />
254: 
255:     <TextView
256:         android:id="@+id/tvPublicError"
257:         android:layout_width="match_parent"
258:         android:layout_height="wrap_content"
259:         android:layout_marginTop="16dp"
260:         android:gravity="center"
261:         android:textColor="@color/text_secondary"
262:         android:textSize="13sp"
263:         android:visibility="gone" />
264: 
265: </LinearLayout>

(End of file - total 265 lines)
</content>
```

---

## Assistant (Build · Muse Spark 1.3 Free · 12.5s)

_Thinking:_



Diagnóstico completo. Ahora implemento los tres bloques.

**Tool: grep**

**Input:**
```json
{
  "pattern": "finalizeRegistration|reserveIdentity|identity_reservations",
  "path": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java"
}
```

**Output:**
```
Found 2 matches
C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\RegistroActivity.kt:
  Line 2320:             val result = registrationRepository.finalizeRegistration(pending, firebaseUser)


C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\repository\RegistrationRepository.kt:
  Line 92:     suspend fun finalizeRegistration(

```

---

## Assistant (Build · Muse Spark 1.3 Free · 7.2s)

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\RegistroActivity.kt",
  "offset": 2280,
  "limit": 80
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\RegistroActivity.kt</path>
<type>file</type>
<content>
2280:                 if (role == UserRoles.CONTRATANTE) {
2281:                     if (identity.documentType == IdentityDocumentTypes.RUC) EmployerTypes.EMPRESA
2282:                     else EmployerTypes.PERSONA
2283:                 } else ""
2284:             }
2285: 
2286:         return PendingRegistration(
2287:             uid = uid,
2288:             role = role,
2289:             email = email,
2290:             provider = if (isGoogle) AuthProviders.GOOGLE else AuthProviders.EMAIL,
2291:             authMethod = if (isGoogle) AuthMethods.GOOGLE else AuthMethods.EMAIL_PASSWORD,
2292:             verificationMethod = if (isOtpVerified) {
2293:                 EmailVerificationMethods.CHAMBAYA_OTP
2294:             } else {
2295:                 EmailVerificationMethods.FIREBASE_EMAIL_LINK
2296:             },
2297:             otpVerified = isOtpVerified,
2298:             identity = identity,
2299:             accountDisplayName = googleDisplayName.ifBlank { stored?.accountDisplayName.orEmpty() },
2300:             accountPhotoUrl = googlePhotoUrl.ifBlank { stored?.accountPhotoUrl.orEmpty() },
2301:             employerType = employerType
2302:         )
2303:     }
2304: 
2305:     /**
2306:      * Crea o actualiza `users/{uid}` con los datos mínimos de la FASE 1:
2307:      * nombre oficial, DNI/RUC, roles y verificaciones.
2308:      * Es idempotente: se puede volver a llamar desde el resumen final.
2309:      */
2310:     private fun finalizeUserAccount(firebaseUser: FirebaseUser?, onFinished: (Boolean) -> Unit) {
2311:         val pending = buildPendingRegistration(firebaseUser)
2312: 
2313:         if (pending == null) {
2314:             showToast("No pudimos recuperar los datos de tu identidad. Vuelve a verificarla.")
2315:             onFinished(false)
2316:             return
2317:         }
2318: 
2319:         lifecycleScope.launch {
2320:             val result = registrationRepository.finalizeRegistration(pending, firebaseUser)
2321:             if (result.isSuccess) {
2322:                 pendingRegistrationStore.savePending(pending)
2323:             }
2324:             onFinished(result.isSuccess)
2325:         }
2326:     }
2327: 
2328:     /** Proveedor de autenticación de la cuenta actual ("EMAIL" o "GOOGLE"). */
2329:     private fun currentProviderId(): String =
2330:         if (registeredAuthMethod == AuthMethods.GOOGLE) AuthProviders.GOOGLE else AuthProviders.EMAIL
2331: 
2332:     /** Datos de identidad que se envían a la Cloud Function (mismo esquema que Firestore). */
2333:     private fun identityPayloadForBackend(): Map<String, Any> {
2334:         val identity = validatedIdentity ?: return emptyMap()
2335:         return mapOf(
2336:             "documentType" to identity.documentType,
2337:             "documentNumber" to identity.documentNumber,
2338:             "documentNumberMasked" to identity.maskedDocumentNumber,
2339:             "identityVerified" to true,
2340:             "verifiedWith" to identity.source,
2341:             "identityName" to identity.displayName,
2342:             "identityStatus" to (identity.statusLabel ?: ""),
2343:             "location" to (identity.locationLabel ?: ""),
2344:             "firstName" to identity.firstName,
2345:             "lastName" to identity.lastName
2346:         )
2347:     }
2348: 
2349:     private fun profilePayloadForBackend(): Map<String, Any> {
2350:         val identity = validatedIdentity ?: return emptyMap()
2351:         return mapOf(
2352:             "fullName" to identity.displayName.ifBlank { googleDisplayName },
2353:             "profilePhotoUrl" to googlePhotoUrl
2354:         )
2355:     }
2356: 
2357:     /**
2358:      * Restaura el estado del registro si la Activity fue recreada
2359:      * (rotación o el sistema mató el proceso).

(Showing lines 2280-2359 of 2429. Use offset=2360 to continue.)
</content>
```

---

## Assistant (Build · Muse Spark 1.3 Free · 5.6s)

_Thinking:_



**Tool: todowrite**

**Input:**
```json
{
  "todos": [
    {
      "content": "Explorar notificaciones, perfil público, historial y registro actual",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Completar Fase 14 Notificaciones (bandeja + triggers)",
      "status": "in_progress",
      "priority": "high"
    },
    {
      "content": "Completar Fases 17-19 Perfil público / % completado / historial",
      "status": "pending",
      "priority": "high"
    },
    {
      "content": "Implementar unicidad DNI/RUC con identity_reservations",
      "status": "pending",
      "priority": "high"
    },
    {
      "content": "Verificar compilación y documentar test E2E",
      "status": "pending",
      "priority": "medium"
    }
  ]
}
```

**Output:**
```
[
  {
    "content": "Explorar notificaciones, perfil público, historial y registro actual",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Completar Fase 14 Notificaciones (bandeja + triggers)",
    "status": "in_progress",
    "priority": "high"
  },
  {
    "content": "Completar Fases 17-19 Perfil público / % completado / historial",
    "status": "pending",
    "priority": "high"
  },
  {
    "content": "Implementar unicidad DNI/RUC con identity_reservations",
    "status": "pending",
    "priority": "high"
  },
  {
    "content": "Verificar compilación y documentar test E2E",
    "status": "pending",
    "priority": "medium"
  }
]
```

---

## Assistant (Build · Muse Spark 1.3 Free · 7.7s)

_Thinking:_



Empiezo con Fase 14: tipos de notificación faltantes y triggers.

**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\model\\NotificationModels.kt",
  "oldString": "/**\n * Notificaciones básicas (FASE 7 las crea; la bandeja completa es FASE 14).\n * Colección: notifications/{notificationId}\n */\nobject NotificationType {\n    const val NEW_APPLICATION = \"NEW_APPLICATION\"\n    const val APPLICATION_ACCEPTED = \"APPLICATION_ACCEPTED\"\n    const val APPLICATION_REJECTED = \"APPLICATION_REJECTED\"\n    const val JOB_COMPLETED = \"JOB_COMPLETED\"\n    const val NEW_MESSAGE = \"NEW_MESSAGE\"\n    const val NEW_RATING = \"NEW_RATING\"\n    const val NEW_COMMENT = \"NEW_COMMENT\"\n    const val NEW_LIKE = \"NEW_LIKE\"\n}",
  "newString": "/**\n * Notificaciones FASE 14 (bandeja completa).\n * Colección: notifications/{notificationId}\n *\n * Las crean los repositorios (postulación, chat, calificación, comentario,\n * like, job) con [NotificationRepository.push]; la bandeja las muestra con\n * [com.proyecto.chambaya.ui.jobs.NotificationsSheet].\n */\nobject NotificationType {\n    const val NEW_APPLICATION = \"NEW_APPLICATION\"\n    const val APPLICATION_ACCEPTED = \"APPLICATION_ACCEPTED\"\n    const val APPLICATION_REJECTED = \"APPLICATION_REJECTED\"\n    const val APPLICATION_WITHDRAWN = \"APPLICATION_WITHDRAWN\"\n    const val JOB_IN_PROGRESS = \"JOB_IN_PROGRESS\"\n    const val JOB_COMPLETED = \"JOB_COMPLETED\"\n    const val JOB_CANCELLED = \"JOB_CANCELLED\"\n    const val NEW_MESSAGE = \"NEW_MESSAGE\"\n    const val NEW_RATING = \"NEW_RATING\"\n    const val NEW_COMMENT = \"NEW_COMMENT\"\n    const val NEW_LIKE = \"NEW_LIKE\"\n    const val PUBLICATION_EXPIRING = \"PUBLICATION_EXPIRING\"\n    const val NEW_NEARBY_PUBLICATION = \"NEW_NEARBY_PUBLICATION\"\n\n    /** Título por defecto cuando el documento no trae uno. */\n    fun defaultTitle(type: String): String = when (type) {\n        NEW_APPLICATION -> \"Nueva postulación\"\n        APPLICATION_ACCEPTED -> \"¡Fuiste seleccionado!\"\n        APPLICATION_REJECTED -> \"Postulación decidida\"\n        APPLICATION_WITHDRAWN -> \"Postulación retirada\"\n        JOB_IN_PROGRESS -> \"Trabajo en curso\"\n        JOB_COMPLETED -> \"Trabajo completado\"\n        JOB_CANCELLED -> \"Trabajo cancelado\"\n        NEW_MESSAGE -> \"Nuevo mensaje\"\n        NEW_RATING -> \"Nueva calificación\"\n        NEW_COMMENT -> \"Nuevo comentario\"\n        NEW_LIKE -> \"Nuevo me gusta\"\n        PUBLICATION_EXPIRING -> \"Tu chamba vence pronto\"\n        NEW_NEARBY_PUBLICATION -> \"Nueva chamba cerca de ti\"\n        else -> \"Aviso\"\n    }\n}"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 8.5s)

_Thinking:_



**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\NotificationRepository.kt",
  "oldString": "/**\n * Notificaciones básicas (FASE 7: se crean al postular/aceptar/rechazar;\n * la bandeja completa con push es FASE 14).\n */\nclass NotificationRepository(\n    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()\n) {\n\n    suspend fun push(\n        recipientUid: String,\n        type: String,\n        title: String,\n        message: String,\n        senderUid: String,\n        publicationId: String = \"\"\n    ): Result<Unit> = withContext(Dispatchers.IO) {\n        runCatching {\n            if (recipientUid.isBlank() || recipientUid == senderUid) return@runCatching\n            Tasks.await(\n                firestore.collection(COLLECTION).add(\n                    mapOf(\n                        \"recipientUid\" to recipientUid,\n                        \"type\" to type,\n                        \"title\" to title.trim().take(120),\n                        \"message\" to message.trim().take(300),\n                        \"senderUid\" to senderUid,\n                        \"publicationId\" to publicationId,\n                        \"read\" to false,\n                        \"createdAt\" to FieldValue.serverTimestamp()\n                    )\n                )\n            )\n            Unit\n        }\n    }",
  "newString": "/**\n * FASE 14 — Bandeja de notificaciones completa.\n *\n * Los repositorios crean avisos con [push] (postulación, chat, calificación,\n * comentario, like, job). La bandeja ([listenMine]) los muestra en vivo y el\n * badge de no leídos sale de [unreadCount] / [listenUnreadCount].\n *\n * Sin push nativo (FCM): la app avisa en primer plano por listeners. El push\n * remoto queda como mejora futura sin cambiar este contrato.\n */\nclass NotificationRepository(\n    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()\n) {\n\n    suspend fun push(\n        recipientUid: String,\n        type: String,\n        title: String,\n        message: String,\n        senderUid: String,\n        publicationId: String = \"\"\n    ): Result<Unit> = withContext(Dispatchers.IO) {\n        runCatching {\n            if (recipientUid.isBlank() || recipientUid == senderUid) return@runCatching\n            // `notificationId` se guarda explícito (antes solo vivía en el id del\n            // documento): así `toAppNotification()` no depende del fallback.\n            val ref = firestore.collection(COLLECTION).document()\n            Tasks.await(\n                ref.set(\n                    mapOf(\n                        \"notificationId\" to ref.id,\n                        \"recipientUid\" to recipientUid,\n                        \"type\" to type,\n                        \"title\" to title.trim().take(120),\n                        \"message\" to message.trim().take(300),\n                        \"senderUid\" to senderUid,\n                        \"publicationId\" to publicationId,\n                        \"read\" to false,\n                        \"createdAt\" to FieldValue.serverTimestamp()\n                    )\n                )\n            )\n            Unit\n        }\n    }\n\n    /**\n     * Aviso de cambio de estado de un trabajo (FASE 8→14).\n     * [otherUid] es la contraparte de quien ejecuta la transición.\n     */\n    suspend fun pushJobEvent(\n        recipientUid: String,\n        senderUid: String,\n        to: String,\n        publicationTitle: String,\n        publicationId: String = \"\"\n    ): Result<Unit> {\n        val (type, title, message) = when (to) {\n            com.proyecto.chambaya.data.model.JobStatus.IN_PROGRESS -> Triple(\n                com.proyecto.chambaya.data.model.NotificationType.JOB_IN_PROGRESS,\n                \"Trabajo en curso\",\n                \"Empezó el trabajo de “${publicationTitle.take(60)}”.\"\n            )\n            com.proyecto.chambaya.data.model.JobStatus.COMPLETED -> Triple(\n                com.proyecto.chambaya.data.model.NotificationType.JOB_COMPLETED,\n                \"Trabajo completado\",\n                \"Se marcó como completado “${publicationTitle.take(60)}”. Ya puedes calificar.\"\n            )\n            com.proyecto.chambaya.data.model.JobStatus.CANCELLED -> Triple(\n                com.proyecto.chambaya.data.model.NotificationType.JOB_CANCELLED,\n                \"Trabajo cancelado\",\n                \"Se canceló el trabajo de “${publicationTitle.take(60)}”.\"\n            )\n            else -> return Result.success(Unit)\n        }\n        return push(recipientUid, type, title, message, senderUid, publicationId)\n    }"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 7.8s)

**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\NotificationRepository.kt",
  "oldString": "    fun listenMine(\n        uid: String,\n        limit: Long = 30,\n        onUpdate: (List<AppNotification>) -> Unit,\n        onError: (Exception) -> Unit\n    ): com.google.firebase.firestore.ListenerRegistration {",
  "newString": "    /**\n     * Contador en vivo para el badge (campanita): emite el nº de no leídos\n     * cada vez que cambia la bandeja. Sin costo extra de índices (reusa\n     * recipientUid + read).\n     */\n    fun listenUnreadCount(\n        uid: String,\n        onUpdate: (Int) -> Unit,\n        onError: (Exception) -> Unit\n    ): com.google.firebase.firestore.ListenerRegistration {\n        return firestore.collection(COLLECTION)\n            .whereEqualTo(\"recipientUid\", uid)\n            .whereEqualTo(\"read\", false)\n            .limit(100)\n            .addSnapshotListener { snap, e ->\n                if (e != null) { onError(e); return@addSnapshotListener }\n                onUpdate(snap?.size() ?: 0)\n            }\n    }\n\n    /**\n     * Limpieza de avisos antiguos (p. ej. >90 días): evita que la bandeja\n     * crezca sin límite. Se llama al abrir la app, a mejor esfuerzo.\n     */\n    suspend fun cleanupOld(uid: String, olderThanDays: Long = 90): Result<Int> =\n        withContext(Dispatchers.IO) {\n            runCatching {\n                if (uid.isBlank()) return@runCatching 0\n                val limite = com.google.firebase.Timestamp(\n                    java.util.Date(System.currentTimeMillis() - olderThanDays * 24 * 60 * 60 * 1000)\n                )\n                val snap = Tasks.await(\n                    firestore.collection(COLLECTION)\n                        .whereEqualTo(\"recipientUid\", uid)\n                        .whereLessThan(\"createdAt\", limite)\n                        .limit(100)\n                        .get()\n                )\n                if (snap.isEmpty) return@runCatching 0\n                val batch = firestore.batch()\n                snap.documents.forEach { batch.delete(it.reference) }\n                Tasks.await(batch.commit())\n                snap.size()\n            }\n        }\n\n    fun listenMine(\n        uid: String,\n        limit: Long = 30,\n        onUpdate: (List<AppNotification>) -> Unit,\n        onError: (Exception) -> Unit\n    ): com.google.firebase.firestore.ListenerRegistration {"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 3.5s)

_Thinking:_



**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\NotificationRepository.kt",
  "oldString": "    /** Restaura un aviso borrado (botón Deshacer): conserva contenido y hora. */\n    suspend fun restore(uid: String, n: AppNotification): Result<Unit> = withContext(Dispatchers.IO) {\n        runCatching {\n            require(uid.isNotBlank() && n.recipientUid == uid) { \"Aviso no válido.\" }\n            Tasks.await(\n                firestore.collection(COLLECTION).add(\n                    mapOf(\n                        \"recipientUid\" to uid,\n                        \"type\" to n.type,\n                        \"title\" to n.title,\n                        \"message\" to n.message,\n                        \"senderUid\" to n.senderUid,\n                        \"publicationId\" to n.publicationId,\n                        \"read\" to n.read,\n                        \"createdAt\" to (n.createdAt ?: FieldValue.serverTimestamp())\n                    )\n                )\n            )\n            Unit\n        }\n    }",
  "newString": "    /** Restaura un aviso borrado (botón Deshacer): conserva contenido y hora. */\n    suspend fun restore(uid: String, n: AppNotification): Result<Unit> = withContext(Dispatchers.IO) {\n        runCatching {\n            require(uid.isNotBlank() && n.recipientUid == uid) { \"Aviso no válido.\" }\n            val ref = firestore.collection(COLLECTION).document()\n            Tasks.await(\n                ref.set(\n                    mapOf(\n                        \"notificationId\" to ref.id,\n                        \"recipientUid\" to uid,\n                        \"type\" to n.type,\n                        \"title\" to n.title,\n                        \"message\" to n.message,\n                        \"senderUid\" to n.senderUid,\n                        \"publicationId\" to n.publicationId,\n                        \"read\" to n.read,\n                        \"createdAt\" to (n.createdAt ?: FieldValue.serverTimestamp())\n                    )\n                )\n            )\n            Unit\n        }\n    }"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 3.6s)

_Thinking:_



Ahora conecto los avisos de trabajos y likes.

**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\JobRepository.kt",
  "oldString": "/**\n * FASE 8 — Trabajos que nacen al aceptar una postulación.\n */\nclass JobRepository(\n    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()\n) {",
  "newString": "/**\n * FASE 8 — Trabajos que nacen al aceptar una postulación.\n *\n * Cada transición avisa a la contraparte (FASE 14): al completar se habilita\n * la calificación en ambas direcciones.\n */\nclass JobRepository(\n    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),\n    private val notifications: NotificationRepository = NotificationRepository(firestore)\n) {"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 3.2s)

**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\JobRepository.kt",
  "oldString": "                if (to == JobStatus.COMPLETED) {\n                    cambios[\"completedAt\"] = FieldValue.serverTimestamp()\n                }\n                Tasks.await(ref.update(cambios))\n                Tasks.await(ref.get()).toJob()",
  "newString": "                if (to == JobStatus.COMPLETED) {\n                    cambios[\"completedAt\"] = FieldValue.serverTimestamp()\n                }\n                Tasks.await(ref.update(cambios))\n                val updated = Tasks.await(ref.get()).toJob()\n                // Aviso a la contraparte (mejor esfuerzo, no bloquea).\n                val other = if (uid == updated.workerUid) updated.employerUid else updated.workerUid\n                notifications.pushJobEvent(\n                    recipientUid = other,\n                    senderUid = uid,\n                    to = to,\n                    publicationTitle = updated.publicationTitle,\n                    publicationId = updated.publicationId\n                )\n                updated"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 3.3s)

_Thinking:_



**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\PublicationInteractionRepository.kt",
  "oldString": "class PublicationInteractionRepository(\n    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()\n) {",
  "newString": "class PublicationInteractionRepository(\n    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),\n    private val notifications: NotificationRepository = NotificationRepository(firestore)\n) {"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 3.9s)

**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\PublicationInteractionRepository.kt",
  "oldString": "                } else {\n                    Tasks.await(\n                        ref.set(\n                            mapOf(\n                                \"publicationId\" to publicationId,\n                                \"userUid\" to uid,\n                                \"createdAt\" to FieldValue.serverTimestamp()\n                            )\n                        )\n                    )\n                    runCatching { Tasks.await(pubRef.update(\"statistics.likes\", FieldValue.increment(1))) }\n                    true\n                }",
  "newString": "                } else {\n                    Tasks.await(\n                        ref.set(\n                            mapOf(\n                                \"publicationId\" to publicationId,\n                                \"userUid\" to uid,\n                                \"createdAt\" to FieldValue.serverTimestamp()\n                            )\n                        )\n                    )\n                    runCatching { Tasks.await(pubRef.update(\"statistics.likes\", FieldValue.increment(1))) }\n                    // Aviso al dueño (mejor esfuerzo; push ignora si es su propio like).\n                    runCatching {\n                        val pub = Tasks.await(pubRef.get())\n                        val owner = pub.getString(\"ownerUid\").orEmpty()\n                        val title = pub.getString(\"title\").orEmpty()\n                        if (owner.isNotBlank()) {\n                            notifications.push(\n                                recipientUid = owner,\n                                type = com.proyecto.chambaya.data.model.NotificationType.NEW_LIKE,\n                                title = \"Nuevo me gusta\",\n                                message = \"A alguien le gustó tu chamba “${title.take(60)}”.\",\n                                senderUid = uid,\n                                publicationId = publicationId\n                            )\n                        }\n                    }\n                    true\n                }"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 3.5s)

_Thinking:_



**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\ui\\jobs\\NotificationsSheet.kt",
  "oldString": "            fun bind(n: AppNotification) {\n                tvTitle.text = n.title.ifBlank {\n                    when (n.type) {\n                        NotificationType.NEW_APPLICATION -> \"Nueva postulación\"\n                        NotificationType.APPLICATION_ACCEPTED -> \"¡Fuiste seleccionado!\"\n                        NotificationType.APPLICATION_REJECTED -> \"Postulación decidida\"\n                        else -> \"Aviso\"\n                    }\n                }",
  "newString": "            fun bind(n: AppNotification) {\n                tvTitle.text = n.title.ifBlank { NotificationType.defaultTitle(n.type) }"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 4.8s)

_Thinking:_



Fase 14 lista. Ahora Fases 17-19: perfil público.

**Tool: todowrite**

**Input:**
```json
{
  "todos": [
    {
      "content": "Explorar notificaciones, perfil público, historial y registro actual",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Completar Fase 14 Notificaciones (bandeja + triggers)",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Completar Fases 17-19 Perfil público / % completado / historial",
      "status": "in_progress",
      "priority": "high"
    },
    {
      "content": "Implementar unicidad DNI/RUC con identity_reservations",
      "status": "pending",
      "priority": "high"
    },
    {
      "content": "Verificar compilación y documentar test E2E",
      "status": "pending",
      "priority": "medium"
    }
  ]
}
```

**Output:**
```
[
  {
    "content": "Explorar notificaciones, perfil público, historial y registro actual",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Completar Fase 14 Notificaciones (bandeja + triggers)",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Completar Fases 17-19 Perfil público / % completado / historial",
    "status": "in_progress",
    "priority": "high"
  },
  {
    "content": "Implementar unicidad DNI/RUC con identity_reservations",
    "status": "pending",
    "priority": "high"
  },
  {
    "content": "Verificar compilación y documentar test E2E",
    "status": "pending",
    "priority": "medium"
  }
]
```

---

## Assistant (Build · Muse Spark 1.3 Free · 4.1s)

**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\res\\layout\\bottom_sheet_public_profile.xml",
  "oldString": "    <TextView\n        android:id=\"@+id/tvPublicBio\"\n        android:layout_width=\"match_parent\"\n        android:layout_height=\"wrap_content\"\n        android:layout_marginTop=\"14dp\"\n        android:lineSpacingMultiplier=\"1.35\"\n        android:textColor=\"@color/text_primary\"\n        android:textSize=\"14sp\"\n        android:visibility=\"gone\"\n        tools:visibility=\"visible\"\n        tools:text=\"Empresa ayacuchana dedicada a la construcción y remodelación.\" />",
  "newString": "    <TextView\n        android:id=\"@+id/tvPublicBio\"\n        android:layout_width=\"match_parent\"\n        android:layout_height=\"wrap_content\"\n        android:layout_marginTop=\"14dp\"\n        android:lineSpacingMultiplier=\"1.35\"\n        android:textColor=\"@color/text_primary\"\n        android:textSize=\"14sp\"\n        android:visibility=\"gone\"\n        tools:visibility=\"visible\"\n        tools:text=\"Empresa ayacuchana dedicada a la construcción y remodelación.\" />\n\n    <!-- FASE 17 — distrito y oficios/habilidades (solo datos públicos). -->\n    <TextView\n        android:id=\"@+id/tvPublicDistrict\"\n        android:layout_width=\"match_parent\"\n        android:layout_height=\"wrap_content\"\n        android:layout_marginTop=\"8dp\"\n        android:textColor=\"@color/text_secondary\"\n        android:textSize=\"13sp\"\n        android:visibility=\"gone\"\n        tools:visibility=\"visible\"\n        tools:text=\"Carmen Alto, Huamanga\" />\n\n    <TextView\n        android:id=\"@+id/tvPublicSpecialties\"\n        android:layout_width=\"match_parent\"\n        android:layout_height=\"wrap_content\"\n        android:layout_marginTop=\"8dp\"\n        android:textColor=\"@color/text_primary\"\n        android:textSize=\"13sp\"\n        android:visibility=\"gone\"\n        tools:visibility=\"visible\"\n        tools:text=\"Albañilería · Pintura · Jardinería\" />"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 5.0s)

**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\ui\\jobs\\PublicProfileSheet.kt",
  "oldString": "            val bio = perfil.bio.trim()\n            val tvBio = view.findViewById<TextView>(R.id.tvPublicBio)\n            if (bio.isNotBlank()) { tvBio.visibility = View.VISIBLE; tvBio.text = bio }",
  "newString": "            val bio = perfil.bio.trim()\n            val tvBio = view.findViewById<TextView>(R.id.tvPublicBio)\n            if (bio.isNotBlank()) { tvBio.visibility = View.VISIBLE; tvBio.text = bio }\n\n            // ── FASE 17: distrito + oficios (sin datos privados) ──\n            val distrito = listOf(perfil.district, perfil.province)\n                .filter { it.isNotBlank() }.joinToString(\", \")\n            if (distrito.isNotBlank()) {\n                view.findViewById<TextView>(R.id.tvPublicDistrict).apply {\n                    visibility = View.VISIBLE\n                    text = distrito\n                }\n            }\n            // Trabajador: especialidades + habilidades. Contratante: sector.\n            val oficios = if (!esEmpresa) {\n                (perfil.worker.specialties + perfil.worker.skills).distinct().take(6)\n                    .joinToString(\" · \")\n            } else {\n                perfil.employer.sector.trim()\n            }\n            if (oficios.isNotBlank()) {\n                view.findViewById<TextView>(R.id.tvPublicSpecialties).apply {\n                    visibility = View.VISIBLE\n                    text = oficios\n                }\n            }"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 3.3s)

**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\ui\\jobs\\PublicProfileSheet.kt",
  "oldString": "            view.findViewById<TextView>(R.id.tvPublicRating).text =\n                if (ratings.isNotEmpty()) {\n                    String.format(\"%.1f\", ratings.map { it.rating }.average())\n                } else \"—\"",
  "newString": "            // FASE 17/19: promedio + nº de calificaciones (reputación visible).\n            view.findViewById<TextView>(R.id.tvPublicRating).text =\n                if (ratings.isNotEmpty()) {\n                    String.format(\"%.1f (%d)\", ratings.map { it.rating }.average(), ratings.size)\n                } else \"—\""
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 2.3s)

_Thinking:_



**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\model\\PublicProfileModels.kt"
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\app\src\main\java\com\proyecto\chambaya\data\model\PublicProfileModels.kt</path>
<type>file</type>
<content>
1: package com.proyecto.chambaya.data.model
2: 
3: import com.google.firebase.firestore.DocumentSnapshot
4: 
5: /**
6:  * Perfil PÚBLICO (FASE 6/7, base de FASE 17).
7:  *
8:  * Colección: public_profiles/{uid} — solo datos visibles para todos.
9:  * Nunca DNI, correo, teléfono ni dirección exacta. Los agregados
10:  * (publicaciones, trabajos, reputación) se calculan en vivo desde las
11:  * colecciones abiertas, nunca se duplican aquí.
12:  */
13: data class PublicWorkerBlock(
14:     val enabled: Boolean = false,
15:     val experienceYears: Int = 0,
16:     val specialties: List<String> = emptyList(),
17:     val skills: List<String> = emptyList()
18: )
19: 
20: data class PublicEmployerBlock(
21:     val enabled: Boolean = false,
22:     val employerType: String = "",
23:     val businessName: String = "",
24:     val commercialName: String = "",
25:     val sector: String = "",
26:     val workplaceId: String? = null
27: )
28: 
29: data class PublicProfile(
30:     val uid: String = "",
31:     val fullName: String = "",
32:     val username: String = "",
33:     val photoUrl: String = "",
34:     val bio: String = "",
35:     val district: String = "",
36:     val province: String = "",
37:     val identityVerified: Boolean = false,
38:     val worker: PublicWorkerBlock = PublicWorkerBlock(),
39:     val employer: PublicEmployerBlock = PublicEmployerBlock()
40: ) {
41:     /** Nombre visible: la entidad si hay, si no la persona. */
42:     fun displayName(): String =
43:         employer.businessName.ifBlank { fullName.ifBlank { "Usuario ChambAYA" } }
44: }
45: 
46: @Suppress("UNCHECKED_CAST")
47: fun DocumentSnapshot.toPublicProfile(): PublicProfile {
48:     val w = get("worker") as? Map<*, *>
49:     val e = get("employer") as? Map<*, *>
50:     fun str(m: Map<*, *>?, key: String): String = (m?.get(key) as? String).orEmpty()
51:     fun strs(m: Map<*, *>?, key: String): List<String> =
52:         (m?.get(key) as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
53:     return PublicProfile(
54:         uid = getString("uid") ?: id,
55:         fullName = getString("fullName").orEmpty(),
56:         username = getString("username").orEmpty(),
57:         photoUrl = getString("photoUrl").orEmpty(),
58:         bio = getString("bio").orEmpty(),
59:         district = getString("district").orEmpty(),
60:         province = getString("province").orEmpty(),
61:         identityVerified = getBoolean("identityVerified") ?: false,
62:         worker = PublicWorkerBlock(
63:             enabled = (w?.get("enabled") as? Boolean) ?: false,
64:             experienceYears = (w?.get("experienceYears") as? Number)?.toInt() ?: 0,
65:             specialties = strs(w, "specialties"),
66:             skills = strs(w, "skills")
67:         ),
68:         employer = PublicEmployerBlock(
69:             enabled = (e?.get("enabled") as? Boolean) ?: false,
70:             employerType = str(e, "employerType"),
71:             businessName = str(e, "businessName"),
72:             commercialName = str(e, "commercialName"),
73:             sector = str(e, "sector"),
74:             workplaceId = str(e, "workplaceId").ifBlank { null }
75:         )
76:     )
77: }

(End of file - total 77 lines)
</content>
```

---

## Assistant (Build · Muse Spark 1.3 Free · 4.6s)

_Thinking:_



Perfil público listo. Ahora la unicidad de DNI/RUC.

**Tool: todowrite**

**Input:**
```json
{
  "todos": [
    {
      "content": "Explorar notificaciones, perfil público, historial y registro actual",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Completar Fase 14 Notificaciones (bandeja + triggers)",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Completar Fases 17-19 Perfil público / % completado / historial",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Implementar unicidad DNI/RUC con identity_reservations",
      "status": "in_progress",
      "priority": "high"
    },
    {
      "content": "Verificar compilación y documentar test E2E",
      "status": "pending",
      "priority": "medium"
    }
  ]
}
```

**Output:**
```
[
  {
    "content": "Explorar notificaciones, perfil público, historial y registro actual",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Completar Fase 14 Notificaciones (bandeja + triggers)",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Completar Fases 17-19 Perfil público / % completado / historial",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Implementar unicidad DNI/RUC con identity_reservations",
    "status": "in_progress",
    "priority": "high"
  },
  {
    "content": "Verificar compilación y documentar test E2E",
    "status": "pending",
    "priority": "medium"
  }
]
```

---

## Assistant (Build · Muse Spark 1.3 Free · 6.5s)

**Tool: write**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\IdentityReservationRepository.kt",
  "content": "package com.proyecto.chambaya.data.repository\n\nimport com.google.android.gms.tasks.Tasks\nimport com.google.firebase.firestore.FieldValue\nimport com.google.firebase.firestore.FirebaseFirestore\nimport com.proyecto.chambaya.data.model.IdentityDocumentTypes\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.withContext\n\n/**\n * Unicidad global de DNI/RUC — colección `identity_reservations`.\n *\n * Un documento por documento de identidad, id determinístico:\n * `identity_reservations/{TIPO_NUMERO}` (p. ej. `DNI_72345678`).\n * La transacción garantiza que dos cuentas no puedan registrar el mismo\n * documento a la vez: solo la primera reserva consigue el `create`.\n *\n * Privacidad: `get` puntual permitido a autenticados (para comprobar el\n * propio documento), `list` denegado (nadie puede enumerar DNIs). El número\n * completo solo viaja en el id que el usuario ya conoce (su propio DNI/RUC).\n *\n * Casos límite:\n * - Reintento del mismo usuario (registro a medias): si la reserva ya es\n *   mía (`uid` coincide), se considera disponible.\n * - Duplicados legacy (anteriores a esta colección): gana quien primero\n *   reserve; el segundo recibe [DocumentoYaRegistrado] y debe usar otro\n *   documento o recuperar su cuenta original.\n */\nclass IdentityReservationRepository(\n    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()\n) {\n\n    /**\n     * Reserva el documento para [uid]. Idempotente para el dueño.\n     * Lanza [DocumentoYaRegistrado] si pertenece a otra cuenta.\n     */\n    suspend fun reserve(documentType: String, documentNumber: String, uid: String): Result<Unit> =\n        withContext(Dispatchers.IO) {\n            runCatching {\n                val tipo = documentType.trim().uppercase()\n                val numero = documentNumber.filter(Char::isDigit)\n                require(uid.isNotBlank()) { \"Sesión no válida.\" }\n                require(esFormatoValido(tipo, numero)) { \"Documento no válido.\" }\n                val ref = firestore.collection(COLLECTION).document(reservationId(tipo, numero))\n                Tasks.await(\n                    firestore.runTransaction { tx ->\n                        val snap = tx.get(ref)\n                        if (snap.exists()) {\n                            val duenio = snap.getString(\"uid\").orEmpty()\n                            if (duenio != uid) {\n                                throw DocumentoYaRegistrado(tipo, numero)\n                            }\n                            // Ya es mía: refresco auditoría y listo.\n                            tx.update(ref, \"updatedAt\", FieldValue.serverTimestamp())\n                        } else {\n                            tx.set(\n                                ref,\n                                mapOf(\n                                    \"documentType\" to tipo,\n                                    \"documentNumber\" to numero,\n                                    \"uid\" to uid,\n                                    \"createdAt\" to FieldValue.serverTimestamp(),\n                                    \"updatedAt\" to FieldValue.serverTimestamp()\n                                )\n                            )\n                        }\n                        null\n                    }\n                )\n                Unit\n            }\n        }\n\n    /**\n     * ¿El documento está libre para [uid]? true si no existe o si ya es mío.\n     * Se usa en el sub-paso 1 para avisar temprano sin bloquear.\n     */\n    suspend fun isAvailable(documentType: String, documentNumber: String, uid: String): Boolean =\n        withContext(Dispatchers.IO) {\n            val tipo = documentType.trim().uppercase()\n            val numero = documentNumber.filter(Char::isDigit)\n            if (!esFormatoValido(tipo, numero)) return@withContext false\n            runCatching {\n                val snap = Tasks.await(\n                    firestore.collection(COLLECTION).document(reservationId(tipo, numero)).get()\n                )\n                !snap.exists() || snap.getString(\"uid\") == uid || uid.isBlank()\n            }.getOrDefault(false)\n        }\n\n    companion object {\n        const val COLLECTION = \"identity_reservations\"\n\n        fun reservationId(documentType: String, documentNumber: String): String =\n            \"${documentType.trim().uppercase()}_${documentNumber.filter(Char::isDigit)}\"\n\n        fun esFormatoValido(tipo: String, numero: String): Boolean =\n            (tipo == IdentityDocumentTypes.DNI && numero.matches(Regex(\"^[0-9]{8}$\"))) ||\n                (tipo == IdentityDocumentTypes.RUC && numero.matches(Regex(\"^[0-9]{11}$\")))\n    }\n}\n\n/** El DNI/RUC ya pertenece a otra cuenta de ChambAYA. */\nclass DocumentoYaRegistrado(val documentType: String, val documentNumber: String) :\n    Exception(\"Ese $documentType ya está registrado en otra cuenta. Si es tuyo, inicia sesión con esa cuenta.\")\n"
}
```

**Output:**
```
Wrote file successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 3.3s)

_Thinking:_



**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\RegistrationRepository.kt",
  "oldString": "class RegistrationRepository(\n    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()\n) {",
  "newString": "class RegistrationRepository(\n    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),\n    private val reservations: IdentityReservationRepository =\n        IdentityReservationRepository(firestore)\n) {"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 4.0s)

**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\RegistrationRepository.kt",
  "oldString": "    suspend fun finalizeRegistration(\n        pending: PendingRegistration,\n        firebaseUser: FirebaseUser? = null\n    ): Result<Unit> = withContext(Dispatchers.IO) {\n        runCatching {\n            val userRef = firestore.collection(COLLECTION_USERS).document(pending.uid)\n            val existing = Tasks.await(userRef.get())",
  "newString": "    suspend fun finalizeRegistration(\n        pending: PendingRegistration,\n        firebaseUser: FirebaseUser? = null\n    ): Result<Unit> = withContext(Dispatchers.IO) {\n        runCatching {\n            // Unicidad global DNI/RUC: dos cuentas no pueden compartir documento.\n            // Idempotente para reintentos del mismo uid; lanza\n            // DocumentoYaRegistrado si el documento es de otra cuenta.\n            reservations.reserve(\n                pending.identity.documentType,\n                pending.identity.documentNumber,\n                pending.uid\n            ).getOrThrow()\n\n            val userRef = firestore.collection(COLLECTION_USERS).document(pending.uid)\n            val existing = Tasks.await(userRef.get())"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 3.0s)

_Thinking:_



**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\ProfileRepository.kt",
  "oldString": "class ProfileRepository(\n    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),\n    private val identityService: IdentityValidationService = IdentityValidationService()\n) {",
  "newString": "class ProfileRepository(\n    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),\n    private val identityService: IdentityValidationService = IdentityValidationService(),\n    private val reservations: IdentityReservationRepository =\n        IdentityReservationRepository(firestore)\n) {"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 4.8s)

**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\app\\src\\main\\java\\com\\proyecto\\chambaya\\data\\repository\\ProfileRepository.kt",
  "oldString": "            val documentoCoincide = actual.identity.documentType == draft.documentType &&\n                actual.identity.documentNumber == cleanDocument\n\n            val identityName = if (documentoCoincide) {",
  "newString": "            val documentoCoincide = actual.identity.documentType == draft.documentType &&\n                actual.identity.documentNumber == cleanDocument\n\n            // Unicidad: si el contratante declara un documento DISTINTO al de\n            // su identidad, ese documento también se reserva (otra cuenta no\n            // puede usarlo). Si coincide, ya quedó reservado en el registro.\n            if (!documentoCoincide) {\n                reservations.reserve(draft.documentType, cleanDocument, uid).getOrThrow()\n            }\n\n            val identityName = if (documentoCoincide) {"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 2.6s)

_Thinking:_



Ahora las reglas de seguridad para la nueva colección.

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\firestore.rules",
  "offset": 698,
  "limit": 32
}
```

**Output:**
```
<path>C:\Users\cpan1\Documents\PROYECTO APP\APP-ChambaAYA\firestore.rules</path>
<type>file</type>
<content>
698: 
699:       // Al cambiar o borrar el perfil, el @usuario viejo queda libre.
700:       allow delete: if request.auth != null
701:         && resource.data.uid == request.auth.uid;
702:     }
703: 
704:     // =============================================================
705:     //  workplaces/{workplaceId} — FASE 4 Lugar / establecimiento
706:     // =============================================================
707:     //
708:     // Lectura para autenticados (la Fase 5-6 muestra la sede en publicaciones
709:     // y búsquedas cercanas); escritura solo del dueño con campos validados.
710:     match /workplaces/{workplaceId} {
711:       allow get: if request.auth != null;
712: 
713:       // Sin listado masivo: la Fase 6 filtra por dueño o cercanía con
714:       // consultas acotadas, no barriendo la colección.
715:       allow list: if request.auth != null;
716: 
717:       allow create: if request.auth != null
718:         && isValidWorkplaceCreate(workplaceId);
719: 
720:       allow update: if request.auth != null
721:         && resource.data.get('ownerUid', '') == request.auth.uid
722:         && isValidWorkplaceUpdate();
723: 
724:       allow delete: if request.auth != null
725:         && resource.data.get('ownerUid', '') == request.auth.uid;
726:     }
727: 
728:     // =============================================================
729:     //  FASE 5 — Publicaciones publications/{publicationId}

(Showing lines 698-729 of 1272. Use offset=730 to continue.)
</content>
```

---

## Assistant (Build · Muse Spark 1.3 Free · 4.4s)

**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\firestore.rules",
  "oldString": "      // Al cambiar o borrar el perfil, el @usuario viejo queda libre.\n      allow delete: if request.auth != null\n        && resource.data.uid == request.auth.uid;\n    }\n\n    // =============================================================\n    //  workplaces/{workplaceId} — FASE 4 Lugar / establecimiento\n    // =============================================================",
  "newString": "      // Al cambiar o borrar el perfil, el @usuario viejo queda libre.\n      allow delete: if request.auth != null\n        && resource.data.uid == request.auth.uid;\n    }\n\n    // =============================================================\n    //  identity_reservations/{TIPO_NUMERO} — unicidad global DNI/RUC\n    // =============================================================\n    //\n    // Un documento por documento de identidad, id determinístico\n    // `DNI_72345678` / `RUC_20123456789`. La transacción del cliente es la\n    // que garantiza que dos cuentas no tomen el mismo documento a la vez.\n    // `get` puntual para autenticados (comprobar el propio), `list` denegado\n    // (nadie enumera DNIs). Inmutable: no se actualiza ni se borra desde la\n    // app (solo el backend podría liberar en un caso de soporte).\n    match /identity_reservations/{docId} {\n      allow get: if request.auth != null;\n      allow list: if false;\n      allow create: if request.auth != null\n        && request.resource.data.get('uid', '') == request.auth.uid\n        && docId == request.resource.data.get('documentType', '') + '_'\n          + request.resource.data.get('documentNumber', '')\n        && isValidDocumentType(request.resource.data.get('documentType', ''))\n        && isValidDocumentNumber(\n          request.resource.data.get('documentType', ''),\n          request.resource.data.get('documentNumber', ''))\n        && request.resource.data.get('createdAt', null) != null;\n      allow update, delete: if false;\n    }\n\n    // =============================================================\n    //  workplaces/{workplaceId} — FASE 4 Lugar / establecimiento\n    // ============================================================="
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 3.9s)

_Thinking:_



Ahora el backend (Functions) para que la reserva también valga del lado servidor.

**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\functions\\index.js",
  "oldString": "    // Preservar `createdAt` si el documento ya existía\n    let existingCreatedAt = null;\n    try {\n        const existingUserDoc = await userDocRef.get();\n        if (existingUserDoc.exists && existingUserDoc.data().createdAt) {\n            existingCreatedAt = existingUserDoc.data().createdAt;\n        }\n    } catch (readErr) {\n        console.warn(\"[verifyEmailOtp] No se pudo leer el documento previo:\", readErr.message);\n    }",
  "newString": "    // Preservar `createdAt` si el documento ya existía\n    let existingCreatedAt = null;\n    try {\n        const existingUserDoc = await userDocRef.get();\n        if (existingUserDoc.exists && existingUserDoc.data().createdAt) {\n            existingCreatedAt = existingUserDoc.data().createdAt;\n        }\n    } catch (readErr) {\n        console.warn(\"[verifyEmailOtp] No se pudo leer el documento previo:\", readErr.message);\n    }\n\n    // Unicidad global DNI/RUC: `identity_reservations/{TIPO_NUMERO}`.\n    // Si el documento ya pertenece a otra cuenta, se rechaza la verificación\n    // para que el cliente no complete un registro duplicado.\n    if (identity) {\n        const reservationId = `${identity.documentType}_${identity.documentNumber}`;\n        const reservationRef = db.collection(\"identity_reservations\").doc(reservationId);\n        const reservationDoc = await reservationRef.get();\n        if (reservationDoc.exists && reservationDoc.data().uid !== uid) {\n            throw new functions.https.HttpsError(\n                \"already-exists\",\n                \"Ese documento de identidad ya está registrado en otra cuenta.\"\n            );\n        }\n    }"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 3.8s)

**Tool: edit**

**Input:**
```json
{
  "filePath": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA\\functions\\index.js",
  "oldString": "    batch.set(userDocRef, buildUserDocument({\n        uid: uid,\n        role: userRole,\n        email: userEmail,\n        provider: data && data.provider,\n        authMethod: data && data.authMethod,\n        identity: identity,\n        profile: data ? data.profile : null,\n        existingCreatedAt: existingCreatedAt,\n        otpVerified: true\n    }), { merge: true });\n\n    await batch.commit();",
  "newString": "    batch.set(userDocRef, buildUserDocument({\n        uid: uid,\n        role: userRole,\n        email: userEmail,\n        provider: data && data.provider,\n        authMethod: data && data.authMethod,\n        identity: identity,\n        profile: data ? data.profile : null,\n        existingCreatedAt: existingCreatedAt,\n        otpVerified: true\n    }), { merge: true });\n\n    // Reserva del documento (misma transacción lógica que users/{uid}).\n    if (identity) {\n        const reservationRef = db.collection(\"identity_reservations\")\n            .doc(`${identity.documentType}_${identity.documentNumber}`);\n        batch.set(reservationRef, {\n            documentType: identity.documentType,\n            documentNumber: identity.documentNumber,\n            uid: uid,\n            createdAt: admin.firestore.FieldValue.serverTimestamp(),\n            updatedAt: admin.firestore.FieldValue.serverTimestamp()\n        }, { merge: true });\n    }\n\n    await batch.commit();"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Muse Spark 1.3 Free · 4.7s)

_Thinking:_



Verifico que todo compile.

**Tool: todowrite**

**Input:**
```json
{
  "todos": [
    {
      "content": "Explorar notificaciones, perfil público, historial y registro actual",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Completar Fase 14 Notificaciones (bandeja + triggers)",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Completar Fases 17-19 Perfil público / % completado / historial",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Implementar unicidad DNI/RUC con identity_reservations",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Verificar compilación y documentar test E2E",
      "status": "in_progress",
      "priority": "medium"
    }
  ]
}
```

**Output:**
```
[
  {
    "content": "Explorar notificaciones, perfil público, historial y registro actual",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Completar Fase 14 Notificaciones (bandeja + triggers)",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Completar Fases 17-19 Perfil público / % completado / historial",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Implementar unicidad DNI/RUC con identity_reservations",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Verificar compilación y documentar test E2E",
    "status": "in_progress",
    "priority": "medium"
  }
]
```

---

## Assistant (Build · Muse Spark 1.3 Free · 4.5s)

**Tool: bash**

**Input:**
```json
{
  "command": "node --check functions/index.js; echo NODE_CHECK_DONE",
  "workdir": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA"
}
```

**Output:**
```
NODE_CHECK_DONE

```

---

## Assistant (Build · Muse Spark 1.3 Free)

**Tool: bash**

**Input:**
```json
{
  "command": ".\\gradlew :app:assembleDebug --offline -q",
  "timeout": 600000,
  "workdir": "C:\\Users\\cpan1\\Documents\\PROYECTO APP\\APP-ChambaAYA"
}
```

---

