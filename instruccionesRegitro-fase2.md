Quiero continuar el desarrollo de mi aplicación Android "ChambAYA".

IMPORTANTE:
NO quiero que implementes todavía la FASE 3.
En esta tarea debes trabajar ÚNICAMENTE en la FASE 2: "CREDENCIALES".

Antes de modificar código, revisa la implementación existente, la estructura del proyecto, los modelos/JSON relacionados con métodos de acceso y las Activity/XML actuales para reutilizar la arquitectura existente y evitar duplicar lógica.

==================================================
CONTEXTO DEL REGISTRO
==================================================

La aplicación tiene dos roles:

1. Trabajador
2. Contratante

Ambos utilizan el mismo flujo general de registro de 4 pasos:

FASE 1 → Identidad
FASE 2 → Credenciales
FASE 3 → Verificar
FASE 4 → Listo

La FASE 1 ya está implementada y NO debes modificar su lógica.

Actualmente ya tengo implementada la validación de DNI/RUC.

Archivos principales de la FASE 1:

- actividad_registro.xml
- RegistroActivity.kt

La ruta actual es:

app/src/main/res/layout/actividad_registro.xml

app/src/main/java/com/proyecto/chambaya/RegistroActivity.kt

También existe un JSON/configuración donde ya están definidos los métodos de acceso disponibles, incluyendo:

- Google Auth
- Correo electrónico + contraseña

REVISA primero cómo está implementado actualmente antes de crear nuevas clases o modificar archivos.

==================================================
OBJETIVO DE ESTA TAREA
==================================================

Implementar correctamente SOLAMENTE la FASE 2:

"CREDENCIALES"

La FASE 2 debe permitir elegir entre:

A) Registro con correo electrónico + contraseña

B) Registro/continuar con Google

La interfaz debe comportarse dinámicamente dependiendo del método seleccionado.

==================================================
REGLA 1 — REGISTRO CON CORREO Y CONTRASEÑA
==================================================

Si el usuario utiliza el método:

"Correo electrónico + contraseña"

debe mostrarse:

- Campo Correo electrónico
- Campo Contraseña
- Campo Confirmar contraseña

NO debe mostrarse el campo de número de teléfono.

El número de teléfono se elimina completamente de la interfaz de la FASE 2 porque actualmente no será utilizado.

También elimina de la lógica de esta fase cualquier validación o requisito relacionado con teléfono.

==================================================
REGLA 2 — REGISTRO CON GOOGLE
==================================================

Si el usuario selecciona:

"Continuar con Google"

entonces:

- No mostrar el campo de correo electrónico.
- No mostrar el campo de contraseña.
- No mostrar el campo confirmar contraseña.
- No solicitar contraseña al usuario.
- No solicitar manualmente el correo electrónico.
- Utilizar Firebase Authentication con Google siguiendo la configuración existente del proyecto.

El correo electrónico y la identidad serán obtenidos de la cuenta de Google autenticada.

Después de autenticarse correctamente con Google:

- Firebase debe proporcionar el usuario autenticado.
- Debe considerarse el correo como verificado cuando Firebase indique que corresponde a una cuenta Google autenticada.
- NO enviar un correo de verificación Firebase adicional para Google.

==================================================
REGLA 3 — CORREO + CONTRASEÑA
==================================================

Cuando el usuario seleccione correo electrónico + contraseña:

1. Validar que el correo tenga un formato válido.

2. Validar que la contraseña cumpla los requisitos actuales de la aplicación:

   - mínimo 8 caracteres
   - contener al menos un número
   - contener una letra mayúscula o un símbolo

3. Validar que "Contraseña" y "Confirmar contraseña" coincidan.

4. Crear el usuario mediante Firebase Authentication con:

   createUserWithEmailAndPassword()

5. Después de crear correctamente la cuenta, enviar el correo de verificación utilizando Firebase Authentication.

Debe utilizarse:

sendEmailVerification()

6. NO considerar al usuario como verificado todavía.

7. La cuenta debe quedar en estado pendiente de verificación.

8. La aplicación debe conservar la información necesaria para continuar posteriormente con la FASE 3.

IMPORTANTE:

La FASE 2 solamente debe crear/autenticar al usuario y dejar preparado el flujo.

NO implementar todavía el OTP de la FASE 3.

==================================================
REGLA 4 — CORREO DE VERIFICACIÓN DE FIREBASE
==================================================

Para el método correo + contraseña, utilizar el mecanismo oficial de Firebase Authentication para enviar el correo de verificación.

Actualmente la plantilla configurada en Firebase es:

Remitente:
noreply@chambaya-44ce5.firebaseapp.com

Asunto:
Verifique su correo electrónico para %APP_NAME%

El enlace de verificación será generado por Firebase.

NO construyas manualmente URLs de verificación.

NO generes manualmente códigos de verificación para este correo.

NO almacenes códigos de Firebase Auth en Firestore.

==================================================
REGLA 5 — FASE 3 NO IMPLEMENTAR
==================================================

MUY IMPORTANTE:

NO implementes todavía:

- pantalla OTP
- generación de OTP
- envío de OTP
- colección email_verifications
- código de 6 dígitos
- expiración de OTP
- Cloud Functions para OTP
- Resend
- SendGrid
- validación OTP
- actualización de estado mediante OTP
- backend de verificación
- reglas específicas de Firestore para OTP

Todo eso pertenece a la FASE 3.

Puedes dejar interfaces, modelos o puntos de extensión solamente si son estrictamente necesarios para que la FASE 2 quede correctamente preparada.

Pero NO implementes la lógica de FASE 3.

==================================================
REGLA 6 — GOOGLE Y FINAL DE FASE 2
==================================================

Para Google Auth, cuando la autenticación termine correctamente, mostrar un modal de éxito profesional.

El modal debe indicar, por ejemplo:

"¡Registro exitoso!"

"Tu cuenta se ha creado correctamente."

Debe incluir un botón:

"Continuar"

Al pulsar "Continuar":

- avanzar al siguiente estado del flujo de registro según la arquitectura actual;
- dejar preparada la transición hacia FASE 3;
- NO implementar todavía la lógica interna de FASE 3.

==================================================
REGLA 7 — CORREO Y CONTRASEÑA
==================================================

Para correo + contraseña, después de crear correctamente la cuenta y enviar el correo de verificación:

NO mostrar todavía el OTP.

NO implementar la pantalla OTP.

La aplicación debe dejar claro al usuario que debe revisar su correo para verificar su dirección.

Ejemplo de mensaje:

"Revisa tu correo"

"Hemos enviado un enlace de verificación a tu dirección de correo electrónico."

El diseño debe ser consistente con el estilo visual actual de ChambAYA.

==================================================
REGLA 8 — NÚMERO DE TELÉFONO
==================================================

Eliminar completamente de la FASE 2:

- Label "Número de teléfono"
- Selector de país
- Código +51
- Campo de teléfono
- Validación de teléfono
- Cualquier lógica relacionada con teléfono

Actualmente NO se utilizará teléfono durante el registro.

No agregues un reemplazo para el teléfono.

==================================================
DISEÑO DE LA FASE 2
==================================================

Utiliza como referencia visual las capturas adjuntas.

El diseño actual utiliza:

- Azul principal de ChambAYA
- Fondo azul en el encabezado
- Tarjeta blanca con bordes superiores redondeados
- Stepper de 4 pasos
- Tipografía limpia y moderna
- Campos con bordes redondeados
- Iconos dentro de los campos
- Botones grandes y redondeados
- Estética moderna similar a aplicaciones móviles actuales

Mantén la identidad visual existente.

No rediseñes toda la aplicación.

Solo mejora la FASE 2 cuando sea necesario para que el flujo sea claro y profesional.

==================================================
STEPPER
==================================================

El stepper debe representar:

1. Identidad
2. Credenciales
3. Verificar
4. Listo

En FASE 2:

- Identidad → completado
- Credenciales → activo
- Verificar → pendiente
- Listo → pendiente

No cambies la lógica del stepper de las demás fases salvo que sea estrictamente necesario para conectar la FASE 2.

==================================================
VALIDACIONES DE UI
==================================================

La interfaz debe:

- Mostrar errores claros debajo o dentro del campo correspondiente.
- No permitir continuar si faltan datos.
- No permitir continuar si el correo es inválido.
- No permitir continuar si las contraseñas no coinciden.
- No permitir continuar si la contraseña no cumple los requisitos.
- Mostrar estado de carga mientras Firebase procesa el registro/login.
- Evitar doble clic/doble envío.
- Mostrar errores amigables cuando Firebase devuelva un error.

No mostrar stack traces al usuario.

Los mensajes técnicos deben registrarse mediante Logcat si son necesarios para depuración.

==================================================
FIREBASE AUTH
==================================================

Revisa primero cómo está configurado Firebase en el proyecto.

NO crees una segunda instancia de Firebase.

NO dupliques configuración.

Reutiliza la instancia existente de FirebaseAuth si ya existe.

Revisa:

- google-services.json
- configuración Gradle
- FirebaseAuth existente
- proveedores de autenticación existentes
- configuración actual de Google Sign-In

Si Google Auth todavía no está completamente configurado, identifica exactamente qué falta antes de modificar la arquitectura.

==================================================
FIRESTORE
==================================================

En esta tarea NO es necesario implementar la lógica de OTP.

Sin embargo, si actualmente el proyecto ya tiene una estructura de usuario en Firestore, respétala.

No crees una colección nueva solamente por esta tarea.

No guardes contraseñas en Firestore.

No guardes tokens sensibles en Firestore.

La contraseña debe ser gestionada exclusivamente por Firebase Authentication.

==================================================
ROLES
==================================================

El flujo debe funcionar para ambos roles:

- Trabajador
- Contratante

No dupliques innecesariamente el código.

Si la arquitectura actual permite compartir la lógica de autenticación, crea/reutiliza componentes comunes.

El rol seleccionado durante la FASE 1 debe conservarse al pasar por la FASE 2.

NO cambies el rol seleccionado por el usuario.

==================================================
DATOS QUE DEBEN CONSERVARSE
==================================================

Al terminar la FASE 2, deben conservarse correctamente los datos necesarios del registro, incluyendo:

- UID de Firebase
- correo electrónico, cuando corresponda
- método de autenticación utilizado
- rol seleccionado
- información de identidad obtenida en la FASE 1
- estado necesario para continuar hacia FASE 3

No guardar contraseñas.

==================================================
IMPORTANTE SOBRE SEGURIDAD
==================================================

No generes OTP en el cliente en esta fase.

No guardes contraseñas.

No guardes credenciales de Google.

No hardcodees API keys privadas.

No hardcodees secretos de servicios externos.

No pongas claves de Resend, SendGrid u otros servicios en la aplicación Android.

La futura FASE 3 deberá realizar la generación/envío/validación del OTP de forma segura desde backend.

==================================================
ARCHIVOS
==================================================

Antes de modificar cualquier archivo:

1. Inspecciona RegistroActivity.kt.
2. Inspecciona actividad_registro.xml.
3. Busca las clases relacionadas con:
   - autenticación
   - Firebase
   - Google Auth
   - modelos de registro
   - métodos de acceso
   - navegación entre fases
4. Revisa el JSON existente de métodos de acceso.
5. Identifica cómo se conserva actualmente el rol.
6. Identifica cómo se conserva actualmente la información de la FASE 1.

Después modifica únicamente los archivos necesarios.

No crees archivos duplicados con nombres similares.

==================================================
CRITERIOS DE ACEPTACIÓN
==================================================

Consideraré terminada la FASE 2 únicamente si se cumplen TODOS estos puntos:

[ ] El registro sigue funcionando para Trabajador.

[ ] El registro sigue funcionando para Contratante.

[ ] La FASE 1 continúa funcionando.

[ ] La FASE 2 muestra correctamente "Credenciales".

[ ] El teléfono fue eliminado de la FASE 2.

[ ] Correo + contraseña muestra correo, contraseña y confirmar contraseña.

[ ] Google Auth no muestra correo ni contraseña manuales.

[ ] Correo + contraseña utiliza Firebase Authentication.

[ ] Se envía sendEmailVerification() después de crear la cuenta.

[ ] Google Auth no solicita verificación de correo adicional.

[ ] Se muestra un estado de carga durante operaciones Firebase.

[ ] Se controlan errores de Firebase.

[ ] Se validan los campos antes de enviar.

[ ] Las contraseñas deben coincidir.

[ ] La contraseña cumple los requisitos definidos.

[ ] No se almacena ninguna contraseña en Firestore.

[ ] El rol seleccionado en FASE 1 se conserva.

[ ] La transición hacia FASE 3 queda preparada.

[ ] NO existe implementación OTP todavía.

[ ] NO se implementó Resend.

[ ] NO se implementó SendGrid.

[ ] NO se implementó Cloud Function para OTP.

[ ] NO se creó lógica de FASE 3.

[ ] El código compila.

[ ] No existen errores de Kotlin/XML.

==================================================
FORMA DE TRABAJO
==================================================

NO empieces modificando código inmediatamente.

Primero analiza el proyecto y explícame brevemente:

1. Cómo está implementado actualmente el registro.
2. Qué archivos vas a modificar.
3. Qué lógica existente vas a reutilizar.
4. Cómo integrarás Firebase Auth.
5. Cómo manejarás Google Auth.
6. Cómo manejarás correo + contraseña.
7. Cómo quedará preparada la transición hacia FASE 3.

Después de ese análisis, realiza los cambios.

Al finalizar:

- Indica exactamente qué archivos modificaste.
- Explica qué cambió en cada archivo.
- Indica si agregaste alguna dependencia.
- Indica si hubo que modificar Gradle.
- Indica si hubo algún cambio en Firebase.
- Verifica que el proyecto compile.
- Si encuentras algún problema de configuración de Firebase/Google Auth que no puedas resolver automáticamente, indícalo claramente.

RESTRICCIÓN FINAL:

NO IMPLEMENTES LA FASE 3.

La tarea actual termina cuando la FASE 2 "Credenciales" queda correctamente implementada y preparada para conectar con la FASE 3.