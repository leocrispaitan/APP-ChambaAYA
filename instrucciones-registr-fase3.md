Quiero continuar el desarrollo de mi aplicación Android "ChambAYA".

IMPORTANTE:
La FASE 1 y la FASE 2 ya están implementadas.

Ahora quiero implementar ÚNICAMENTE la FASE 3:

"VERIFICAR"

NO modifiques innecesariamente la FASE 1 ni la FASE 2.

Antes de modificar código, analiza cómo quedó implementada la autenticación en la FASE 2 y reutiliza la arquitectura existente.

==================================================
CONTEXTO GENERAL
==================================================

La aplicación tiene dos roles:

1. Trabajador
2. Contratante

El registro completo tiene 4 pasos:

FASE 1 → Identidad
FASE 2 → Credenciales
FASE 3 → Verificar
FASE 4 → Listo

FASE 1:
- DNI/RUC
- Identidad
- Ya está implementada.

FASE 2:
- Google Auth
- Correo electrónico + contraseña
- Firebase Authentication
- Ya está implementada.

FASE 3:
- Verificación mediante código OTP de 6 dígitos enviado al correo electrónico.

FASE 4:
- Registro terminado.

==================================================
OBJETIVO DE ESTA TAREA
==================================================

Implementar completamente la FASE 3 "Verificar".

El usuario debe recibir un código OTP de 6 dígitos en su correo electrónico.

La pantalla debe permitir:

- ingresar el código de 6 dígitos;
- confirmar el código;
- solicitar reenvío;
- mostrar tiempo de expiración;
- mostrar errores;
- manejar correctamente estados de carga;
- continuar a FASE 4 cuando la verificación sea correcta.

El sistema debe funcionar tanto para:

A) Usuario registrado mediante Google.

B) Usuario registrado mediante correo electrónico + contraseña.

==================================================
IMPORTANTE — DOS TIPOS DE VERIFICACIÓN
==================================================

Hay que diferenciar:

1. Verificación de correo electrónico de Firebase Authentication.

2. Verificación OTP propia de ChambAYA.

NO reemplazar una por la otra.

Para correo + contraseña:

Firebase Authentication puede enviar su propio enlace mediante:

sendEmailVerification()

Ese proceso sigue siendo válido.

Después, ChambAYA tendrá además su propio OTP de 6 dígitos para completar la FASE 3.

Para Google:

Firebase considera el correo de una cuenta Google como verificado cuando corresponda según el proveedor/autenticación.

Por lo tanto, NO enviar un enlace de verificación Firebase adicional para Google.

Pero Google también debe pasar por la FASE 3 de ChambAYA:

Google Auth
↓
Firebase Authentication
↓
OTP ChambAYA
↓
FASE 4

==================================================
ARQUITECTURA DEL OTP
==================================================

IMPORTANTE:

NO quiero generar el OTP de forma confiable solamente en Android/Kotlin.

NO hagas esto:

Random() → Kotlin → Firestore

El cliente Android NO debe ser la autoridad que decide cuál es el OTP válido.

La generación y validación del OTP debe realizarse desde backend.

La arquitectura deseada es:

Android
   ↓
Solicitar OTP
   ↓
Backend / Cloud Function
   ↓
Generar OTP seguro
   ↓
Guardar información temporal en Firestore
   ↓
Enviar OTP por correo
   ↓
Usuario recibe código
   ↓
Android muestra pantalla OTP
   ↓
Usuario introduce código
   ↓
Android solicita validación al backend
   ↓
Backend valida OTP
   ↓
Firestore actualiza estado
   ↓
Android continúa a FASE 4

==================================================
BACKEND
==================================================

Primero inspecciona el proyecto para determinar si ya existe:

- Firebase Functions
- carpeta functions/
- Cloud Functions
- configuración Firebase
- configuración Firestore
- Firebase CLI
- proyecto asociado

NO crees una segunda configuración de Firebase.

Si ya existe backend, reutilízalo.

Si no existe backend, crea la estructura necesaria siguiendo la configuración actual del proyecto.

Antes de elegir una solución específica de envío de correo, verifica qué servicios y configuración ya existen.

==================================================
SERVICIO DE CORREO
==================================================

El OTP debe enviarse al correo electrónico del usuario.

La implementación puede utilizar un proveedor externo de correo transaccional.

Opciones posibles:

- Resend
- SendGrid
- otro proveedor compatible
- Firebase/Google Cloud si la configuración actual lo permite

IMPORTANTE:

NO asumas que una característica es gratuita solamente porque históricamente tuvo un plan free.

Antes de implementarla, revisa la configuración actual del proyecto y los requisitos actuales del proveedor.

La solución debe minimizar costos y, si existe una alternativa gratuita adecuada para este proyecto, utilizarla.

NO pongas API keys privadas dentro de:

- Kotlin
- XML
- BuildConfig público
- strings.xml
- Firestore
- código de Android

Las credenciales del proveedor de correo deben permanecer en backend/secrets.

==================================================
ESTRUCTURA DE FIRESTORE
==================================================

Diseña una estructura segura para almacenar temporalmente la información del OTP.

Puedes utilizar una colección como:

email_verifications

o una estructura equivalente si la arquitectura actual del proyecto ya tiene una mejor alternativa.

No dupliques colecciones innecesariamente.

El documento debe estar asociado al UID de Firebase.

Ejemplo conceptual:

email_verifications/{uid}

Con información equivalente a:

{
    uid,
    email,
    otpHash,
    expiresAt,
    attempts,
    resendAvailableAt,
    verified,
    createdAt
}

IMPORTANTE:

NO es obligatorio guardar el OTP en texto plano.

Preferiblemente almacena un hash del OTP y compara el hash del código recibido durante la validación.

Si la implementación necesita guardar temporalmente el OTP en otra forma por compatibilidad con la arquitectura, explica el motivo y aplica las medidas de seguridad correspondientes.

==================================================
OTP
==================================================

El OTP debe:

- tener exactamente 6 dígitos;
- utilizar generación criptográficamente segura en backend;
- poder comenzar con cero;
- tener expiración;
- ser de un solo uso;
- invalidarse después de ser utilizado correctamente.

Ejemplos válidos:

004821
183920
927451

No asumir que el primer dígito debe ser distinto de cero.

==================================================
EXPIRACIÓN
==================================================

El OTP debe tener una duración limitada.

Usar inicialmente:

5 minutos

La expiración debe determinarse utilizando tiempo del servidor/backend.

NO confiar únicamente en el reloj del dispositivo Android.

En Firestore/backend guardar:

expiresAt

Al validar:

currentServerTime < expiresAt

Si el código expiró:

- rechazarlo;
- indicar al usuario que el código expiró;
- permitir solicitar un nuevo código.

==================================================
INTENTOS
==================================================

Implementar protección contra intentos ilimitados.

Por ejemplo:

- máximo 5 intentos por OTP.

Después de superar el límite:

- invalidar el OTP actual;
- exigir solicitar un nuevo código.

NO permitir que el usuario pruebe infinitamente códigos.

Si consideras necesario aplicar también rate limiting por UID/email/IP, implementarlo de acuerdo con las capacidades actuales del backend.

==================================================
REENVIAR CÓDIGO
==================================================

La pantalla debe tener:

"Reenviar código"

Pero NO debe poder utilizarse infinitamente.

Implementar un cooldown.

Inicialmente:

60 segundos entre solicitudes.

Durante el cooldown mostrar algo como:

"Reenviar código en 45 s"

Cuando llegue a 0:

"Reenviar código"

Cada nuevo código debe:

- invalidar el OTP anterior;
- generar un OTP nuevo;
- actualizar expiresAt;
- reiniciar intentos;
- enviar el nuevo código.

No permitir que un OTP anterior siga siendo válido después de generar uno nuevo.

==================================================
PANTALLA FASE 3
==================================================

Crear o adaptar la pantalla de la FASE 3 siguiendo exactamente la identidad visual actual de ChambAYA.

La pantalla debe representar:

1. Identidad → completado
2. Credenciales → completado
3. Verificar → activo
4. Listo → pendiente

Referencia visual:

La pantalla debe mantener el estilo de las capturas proporcionadas:

- encabezado azul ChambAYA;
- stepper;
- tarjeta blanca;
- diseño limpio;
- bordes redondeados;
- azul principal;
- tipografía moderna;
- botones grandes;
- espacios adecuados.

==================================================
CONTENIDO DE LA PANTALLA
==================================================

Mostrar un bloque visual relacionado con correo + seguridad.

Título:

"Verifica tu correo"

Texto:

"Hemos enviado un código de 6 dígitos a"

Después mostrar el correo parcialmente oculto cuando sea apropiado.

Ejemplo:

za******@gmail.com

No mostrar información innecesaria.

Debajo:

4/6? NO.

El OTP debe tener exactamente:

6 campos

Ejemplo visual:

[  ] [  ] [  ] [  ] [  ] [  ]

Cada campo representa un dígito.

==================================================
COMPORTAMIENTO DE LOS CAMPOS OTP
==================================================

Implementar una experiencia moderna:

- aceptar únicamente números;
- avanzar automáticamente al siguiente campo;
- retroceder con backspace;
- permitir pegar un código completo de 6 dígitos;
- distribuir automáticamente los dígitos;
- enfocar automáticamente el primer campo;
- mostrar teclado numérico;
- no permitir letras.

Si el usuario pega:

123456

debe quedar:

[1] [2] [3] [4] [5] [6]

==================================================
BOTÓN CONFIRMAR
==================================================

Botón:

"Confirmar código"

Debe:

- estar deshabilitado mientras el código tenga menos de 6 dígitos;
- mostrar loading mientras se valida;
- evitar doble envío;
- enviar el código al backend;
- esperar la respuesta;
- mostrar el resultado.

==================================================
VALIDACIÓN CORRECTA
==================================================

Si el OTP es correcto:

1. Marcar la verificación como completada en backend/Firestore.

2. Invalidar el OTP para que no pueda reutilizarse.

3. Actualizar el estado correspondiente del registro.

4. Mantener asociado el UID de Firebase.

5. Mantener el rol seleccionado:

   - Trabajador
   - Contratante

6. Continuar a la FASE 4.

NO crear una nueva cuenta Firebase.

NO volver a ejecutar el registro.

==================================================
FASE 4
==================================================

Después de una validación OTP correcta:

FASE 3
   ↓
OTP correcto
   ↓
estado verificado
   ↓
FASE 4

La FASE 4 mostrará posteriormente el registro terminado.

Si la FASE 4 ya existe, reutilízala.

Si todavía no existe:

NO implementes una FASE 4 completa.

Solamente deja la navegación preparada o muestra temporalmente el estado correspondiente según la arquitectura actual.

==================================================
SI EL OTP ES INCORRECTO
==================================================

Mostrar un mensaje amigable:

"Código incorrecto"

"Verifica el código e inténtalo nuevamente."

No mostrar:

- hashes;
- errores internos;
- detalles de Firestore;
- stack traces;
- información del backend.

Incrementar el contador de intentos en backend.

==================================================
SI EL OTP EXPIRÓ
==================================================

Mostrar:

"El código ha expirado"

"Solicita un nuevo código para continuar."

Permitir:

"Reenviar código"

==================================================
SI SUPERA LOS INTENTOS
==================================================

Mostrar:

"Has superado el número de intentos."

"Solicita un nuevo código para continuar."

Invalidar el OTP anterior.

==================================================
ERRORES DE RED
==================================================

Si no existe conexión o el backend no responde:

Mostrar algo como:

"No pudimos verificar el código."

"Revisa tu conexión e inténtalo nuevamente."

No perder innecesariamente el código que el usuario ya escribió.

==================================================
ESTADO DEL REGISTRO
==================================================

Debe existir un estado claro de verificación.

Por ejemplo, conceptualmente:

registrationStatus:

IDENTITY_PENDING
CREDENTIALS_PENDING
OTP_PENDING
VERIFIED
COMPLETED

No es obligatorio utilizar exactamente estos nombres.

Adapta los nombres a la arquitectura actual.

Pero debe ser imposible considerar terminado el registro si la FASE 3 no fue validada.

==================================================
USUARIOS GOOGLE
==================================================

Para usuarios Google:

Firebase Auth
↓
UID
↓
correo Google
↓
solicitar OTP ChambAYA
↓
correo con código
↓
validar OTP
↓
FASE 4

No enviar:

sendEmailVerification()

para Google como parte de este OTP.

El OTP de ChambAYA es independiente de la verificación de Google/Firebase.

==================================================
USUARIOS CORREO + CONTRASEÑA
==================================================

Para usuarios registrados con correo + contraseña:

Firebase Auth
↓
sendEmailVerification()
↓
OTP ChambAYA
↓
FASE 3
↓
FASE 4

IMPORTANTE:

La verificación Firebase mediante enlace y el OTP ChambAYA son mecanismos distintos.

NO sustituyas automáticamente uno por otro.

Si la arquitectura actual exige que el enlace de Firebase sea obligatorio antes del OTP, respétalo.

Si no existe esa restricción, conserva ambos estados de forma independiente y explícita.

==================================================
SEGURIDAD FIRESTORE
==================================================

Revisa las reglas actuales de Firestore.

NO permitas que un usuario pueda simplemente escribir desde el cliente:

verified = true

o:

otpVerified = true

o:

registrationStatus = COMPLETED

sin pasar por el backend.

La verificación final debe estar protegida.

El cliente Android no debe tener autoridad para marcarse a sí mismo como verificado.

Revisa las reglas para impedir modificaciones arbitrarias de:

- otpHash
- attempts
- expiresAt
- verified
- registrationStatus

si esos campos deben ser controlados por backend.

==================================================
NO EXPONER SECRETOS
==================================================

Nunca colocar:

- API keys privadas
- tokens
- claves de proveedor de correo
- secretos Firebase de backend

en el APK.

No poner secretos en:

- strings.xml
- XML
- Kotlin
- repositorio Git
- Firestore

Usar Secret Manager / configuración segura de backend cuando corresponda.

==================================================
NO DUPLICAR USUARIOS
==================================================

La FASE 3 no debe crear usuarios Firebase.

Debe trabajar con el UID ya creado en la FASE 2.

Si el usuario ya existe:

NO crear otro usuario.

NO llamar nuevamente a createUserWithEmailAndPassword().

==================================================
PROTECCIÓN CONTRA REENVÍOS
==================================================

Implementar límites razonables de envío.

Como mínimo:

- 60 segundos entre reenvíos.

También evita que un usuario pueda solicitar cientos de correos continuamente.

Si la arquitectura lo permite, implementar un límite adicional por UID/email durante un período determinado.

==================================================
CORREO OTP
==================================================

Crear una plantilla profesional.

Ejemplo conceptual:

Asunto:

"Tu código de verificación de ChambAYA"

Contenido:

"Hola,

Tu código de verificación de ChambAYA es:

123456

Este código es válido durante 5 minutos.

Si no solicitaste este código, puedes ignorar este mensaje.

Equipo ChambAYA"

No colocar información sensible adicional.

==================================================
FIRESTORE TIMESTAMPS
==================================================

Preferir timestamps del servidor:

serverTimestamp()

o la solución equivalente del backend.

No confiar únicamente en:

System.currentTimeMillis()

del teléfono para determinar si el OTP expiró.

==================================================
LOGS
==================================================

Agregar logs útiles únicamente para desarrollo.

NO registrar:

- OTP en producción;
- contraseñas;
- tokens;
- API keys;
- secretos.

Si se registra el flujo:

"OTP requested"
"OTP verification success"
"OTP verification failed"

pero nunca:

"OTP = 123456"

==================================================
ARQUITECTURA ANDROID
==================================================

Antes de crear nuevas clases, revisa si el proyecto utiliza:

- MVVM
- ViewModel
- Repository
- StateFlow
- LiveData
- Navigation Component
- Activities
- Fragments
- otras capas existentes

Respeta la arquitectura actual.

No introduzcas una arquitectura completamente diferente solo para esta fase.

Si ya existe un Repository para Firebase, reutilízalo.

Si existe un ViewModel para registro, reutilízalo.

==================================================
DEPENDENCIAS
==================================================

Antes de agregar dependencias:

1. Revisa build.gradle / build.gradle.kts.
2. Revisa libs.versions.toml si existe.
3. Reutiliza las dependencias existentes.
4. Agrega únicamente lo estrictamente necesario.

No agregues librerías innecesarias para implementar los 6 campos OTP.

==================================================
ARCHIVOS
==================================================

Primero localiza los archivos relacionados con:

- RegistroActivity
- FASE 2
- autenticación
- FirebaseAuth
- Firestore
- Google Auth
- navegación
- modelos de registro
- métodos de acceso
- configuración Firebase
- backend/functions

No asumas nombres de archivos.

Busca primero.

==================================================
PROCESO OBLIGATORIO ANTES DE MODIFICAR
==================================================

Antes de escribir código:

1. Inspecciona el proyecto.
2. Identifica cómo funciona actualmente la FASE 2.
3. Identifica cómo se obtiene el UID de Firebase.
4. Identifica cómo se obtiene el email.
5. Identifica cómo se conserva el rol.
6. Identifica dónde se almacena la información del registro.
7. Revisa Firestore.
8. Revisa Firestore Security Rules.
9. Revisa si ya existe Cloud Functions.
10. Revisa si ya existe algún servicio de correo.
11. Revisa configuración Firebase.
12. Propón la arquitectura de FASE 3.

NO empieces creando archivos antes de realizar este análisis.

==================================================
PLAN DE IMPLEMENTACIÓN
==================================================

Quiero que primero me muestres un plan breve con:

1. Archivos existentes que vas a reutilizar.
2. Archivos que vas a modificar.
3. Archivos nuevos que necesitas crear.
4. Estructura Firestore propuesta.
5. Endpoint/Cloud Function propuesta.
6. Flujo de generación OTP.
7. Flujo de envío de correo.
8. Flujo de validación OTP.
9. Protección contra intentos.
10. Protección contra reenvíos.
11. Cambios en Firestore Rules.
12. Cambios en Android.
13. Cómo se conecta con FASE 4.

Después de mostrar el plan, implementa la solución.

==================================================
CRITERIOS DE ACEPTACIÓN
==================================================

La FASE 3 estará terminada solamente si:

[ ] Funciona para Trabajador.

[ ] Funciona para Contratante.

[ ] Funciona con Google Auth.

[ ] Funciona con correo + contraseña.

[ ] Se obtiene correctamente el UID Firebase.

[ ] Se obtiene correctamente el email.

[ ] Se genera un OTP de exactamente 6 dígitos.

[ ] El OTP se genera de forma segura en backend.

[ ] El cliente Android NO decide cuál es el OTP válido.

[ ] El OTP expira después de 5 minutos.

[ ] El OTP anterior se invalida al solicitar uno nuevo.

[ ] Existe límite de intentos.

[ ] Existe cooldown de reenvío.

[ ] El OTP solamente puede utilizarse una vez.

[ ] El código se envía al correo.

[ ] Las credenciales del servicio de correo no están en Android.

[ ] El usuario puede pegar un código de 6 dígitos.

[ ] Los campos avanzan automáticamente.

[ ] Backspace funciona correctamente.

[ ] Solo se aceptan números.

[ ] El botón Confirmar código funciona.

[ ] Existe estado de loading.

[ ] Se evita doble envío.

[ ] Se muestran errores amigables.

[ ] No se muestran errores internos al usuario.

[ ] El backend controla la verificación final.

[ ] El cliente no puede marcarse arbitrariamente como verificado.

[ ] Firestore Rules impiden manipular directamente el estado sensible.

[ ] El OTP no se guarda en texto plano si la arquitectura permite hash.

[ ] No se almacenan contraseñas.

[ ] No se almacenan secretos en Android.

[ ] El rol Trabajador/Contratante se conserva.

[ ] El UID se conserva.

[ ] El estado de registro se actualiza correctamente.

[ ] Después de verificar correctamente se puede continuar a FASE 4.

[ ] No se crean usuarios Firebase duplicados.

[ ] La FASE 1 continúa funcionando.

[ ] La FASE 2 continúa funcionando.

[ ] No se rompe Google Auth.

[ ] No se rompe correo + contraseña.

==================================================
RESTRICCIONES
==================================================

NO:

- recrear Firebase Auth;
- crear usuarios duplicados;
- guardar contraseñas;
- guardar secretos en Android;
- confiar en el reloj del teléfono;
- permitir que el cliente marque verified=true;
- generar el OTP como autoridad en Kotlin;
- permitir OTP ilimitados;
- permitir reenvíos ilimitados;
- enviar el OTP desde el APK usando una API key privada;
- implementar nuevamente la FASE 1;
- reescribir innecesariamente la FASE 2;
- cambiar el diseño global de ChambAYA.

==================================================
RESULTADO FINAL
==================================================

Al terminar, explícame:

1. Qué archivos modificaste.
2. Qué archivos nuevos creaste.
3. Cómo funciona el flujo OTP.
4. Cómo se genera el OTP.
5. Dónde se almacena.
6. Cómo se expira.
7. Cómo se limita el número de intentos.
8. Cómo funciona el reenvío.
9. Qué servicio de correo utilizaste y por qué.
10. Dónde se almacenan sus secretos.
11. Qué cambios hiciste en Firestore Rules.
12. Cómo se valida el OTP.
13. Cómo se actualiza el estado del usuario.
14. Cómo se conserva el rol.
15. Cómo se conecta con FASE 4.
16. Qué configuración manual debo realizar en Firebase Console.
17. Qué configuración manual debo realizar en el proveedor de correo.
18. Cómo probar todo el flujo.

Finalmente ejecuta/verifica la compilación del proyecto y reporta cualquier error real de compilación.

IMPORTANTE:

No des por terminado el trabajo simplemente porque el código fue escrito.

Verifica que la arquitectura sea coherente con Firebase, Firestore y el backend existente.

La FASE 3 debe quedar funcional, segura y preparada para conectarse con la FASE 4.