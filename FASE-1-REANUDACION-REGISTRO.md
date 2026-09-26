# FASE 1 — Reanudación de registros a medias

**Estado:** implementado y compilado (`:app:assembleDebug` → BUILD SUCCESSFUL)
**Alcance:** corrección del flujo de registro entre los sub-pasos 1 (DNI/RUC), 2
(credenciales) y 3 (verificación). No cambia el modelo final de `users/{uid}`, no toca
la validación DNI/RUC (RENIEC/SUNAT) ni introduce nada de FASE 2 o superior.

---

## 1. El bug

La cuenta de Firebase Authentication se creaba en el **sub-paso 2**, pero el documento
`users/{uid}` solo se escribía al confirmar la verificación (**sub-paso 4**). Durante esa
ventana la app no distinguía dos situaciones muy distintas y las trataba igual:

| Situación real | Qué hacía la app antes |
|---|---|
| Cuenta con registro **completo** | «Ya existe una cuenta registrada» → correcto |
| Cuenta creada en Auth, **sin** `users/{uid}` (el usuario cerró la app antes del OTP) | «Ya existe una cuenta registrada» → **bloqueo incorrecto** |

Como el DNI/RUC del sub-paso 1 solo se conservaba en el borrador local y el pendiente
ligado al `uid` se guardaba **únicamente al finalizar**, el usuario que cerraba la app en
el sub-paso 3 perdía además la posibilidad de recuperar su identidad validada.

---

## 2. Solución

### 2.1 Se consulta el estado real de `users/{uid}` antes de bloquear

`RegistrationRepository.fetchRegistrationState(uid)` devuelve un estado de cuatro valores:

| Estado | Significado |
|---|---|
| `MISSING` | No existe `users/{uid}` (cuenta huérfana de Auth) |
| `INCOMPLETE` | Existe, pero el registro no se completó |
| `COMPLETE` | `registrationStatus ∈ {VERIFIED, COMPLETED}` o `accountStatus == ACTIVE` |
| `UNKNOWN` | No se pudo leer (sin red / sin permiso): no se afirma nada |

El criterio de `COMPLETE` es **exactamente el mismo que usa `LoginActivity`** para dar
acceso a la app. Esto garantiza el invariante:

> Si el usuario puede entrar por login, no se le puede ofrecer un registro nuevo.

### 2.2 La contraseña escrita es la prueba de titularidad

En el sub-paso 2, cuando Firebase responde `email-already-in-use`, ya no se muestra el
error. Se intenta `signInWithEmailAndPassword` con la contraseña que el usuario acaba de
escribir:

- **La contraseña coincide** → la cuenta es del usuario, ya hay sesión y por tanto ya puede
  leer su `users/{uid}` (las reglas solo permiten leerlo al dueño) → se decide con el
  estado real: `COMPLETE` → «ya está registrada» + ir a login; `MISSING`/`INCOMPLETE` →
  **se retoma el registro**.
- **La contraseña no coincide** → no se intenta leer el documento de otra cuenta
  (Firestore lo prohíbe) → diálogo «esa cuenta ya existe, la contraseña no coincide» con
  acceso al inicio de sesión. No se filtra información: es el mismo dato que ya revelaba
  el error de Firebase.

### 2.3 El DNI/RUC se guarda ligado a la cuenta en el sub-paso 2

`persistPendingAccountForResume()` se llama **en el momento en que Auth crea la cuenta**
(antes de enviar el correo o el OTP), no al final. `PendingRegistrationStore` guarda:

- el **borrador** (rol + identidad, sin `uid`) — ya existía;
- el **pendiente** ligado al `uid` (rol, correo, proveedor, método de acceso, identidad);
- un **índice correo → uid**, para recuperar el registro aunque el usuario haya perdido la
  sesión de Firebase.

Así el DNI/RUC sobrevive al cierre de la app y no hay que volver a escribirlo.

### 2.4 Reanudación explícita

- `offerToResumePendingRegistration()`: al abrir la app, si la sesión actual tiene un
  registro a medias, aparece el diálogo «Retoma tu registro» con acceso directo al
  sub-paso 3. Solo se ofrece con `savedInstanceState == null` (no en rotación) y nunca
  con estado `COMPLETE` o `UNKNOWN`.
- `resumeAtVerificationStep()`: lleva al sub-paso 3 conservando la verificación obligatoria.
  Si Firebase ya tiene el correo verificado de un intento anterior, la verificación ya está
  cumplida y se cierra el registro con `checkEmailVerificationAndProceed()`.
- `adoptStoredIdentityIfMissing()`: si la identidad no está en memoria, la recupera del
  pendiente y repinta la tarjeta de DNI/RUC y el rol.

---

## 3. Archivos modificados

| Archivo | Cambio | Por qué |
|---|---|---|
| `data/repository/RegistrationRepository.kt` | Nuevo `enum UserRegistrationState` (`MISSING`, `INCOMPLETE`, `COMPLETE`, `UNKNOWN`), `fetchRegistrationState(uid)` e `isRegistered(snapshot)` | Necesario para distinguir «cuenta registrada» de «registro a medias» con el mismo criterio que el login |
| `data/model/RegistrationModels.kt` | `RegistrationStatuses.COMPLETED` y `RegistrationStatuses.REGISTERED` | El literal `"COMPLETED"` estaba hardcodeado en `LoginActivity`; ahora hay una única fuente |
| `data/local/PendingRegistrationStore.kt` | `savePending()` normaliza el correo y escribe un índice `correo → uid`; nuevo `loadPendingByEmail()` | Recuperar el registro por correo aunque no exista sesión de Auth |
| `RegistroActivity.kt` | `handleExistingAccountOnEmailRegister()`, `handleExistingAccountOnGoogleRegister()`, `resolveRegistrationAfterAuth()`, `resumeRegistrationOrAskIdentity()`, `persistPendingAccountForResume()`, `adoptStoredIdentityIfMissing()`, `showResumeRegistrationDialog()`, `resumeAtVerificationStep()`, `showAlreadyRegisteredDialog()`, `showExistingAccountDialog()`, `offerToResumePendingRegistration()`; `restoreRegistrationDraft()` refactorizado en `restoreRoleIntoUi()` + `applyIdentityIntoUi()` | Núcleo de la corrección. El refactor evita duplicar el pintado de DNI/RUC y rol |
| `LoginActivity.kt` | `EXTRA_PREFILL_EMAIL` + `prefillEmailFromIntent()` | Al detectar «cuenta ya registrada», el correo llega precargado en el login |

**No se modificaron:** `firestore.rules`, `functions/index.js`, `strings.xml` ni ningún
layout. La validación DNI/RUC, el modelo de `users/{uid}` y la colección
`email_verifications` quedan intactos.

---

## 4. Tabla de decisión (sub-paso 2)

| Caso | Resultado |
|---|---|
| Correo nuevo, correo+contraseña | Se crea la cuenta, se guarda el pendiente, se envía la verificación → sub-paso 3 |
| Correo nuevo, Google | Se crea la cuenta, se guarda el pendiente → modal de éxito → sub-paso 3 |
| Correo existente **completo**, misma contraseña | «Esta cuenta ya está registrada» → **Iniciar sesión** (con correo precargado) |
| Correo existente **completo**, Google | «Esta cuenta ya está registrada» → **Iniciar sesión** |
| **Registro a medias**, misma contraseña | «Retoma tu registro» → sub-paso 3, sin volver a crear la cuenta ni escribir el DNI |
| **Registro a medias**, Google (misma cuenta) | «Retoma tu registro» → se reenvía el OTP → sub-paso 3 |
| **Registro a medias**, Google (correo de una cuenta con contraseña) | Aviso: esa cuenta se creó con correo y contraseña, que inicie sesión para completarla |
| Correo existente con **otra** contraseña | «Esa cuenta ya existe, la contraseña no coincide» → **Ir a iniciar sesión** / **Usar otro correo** |

---

## 5. Verificación obligatoria

No se saltó ningún control: `updateStep(4)` sigue bloqueado mientras `!isOtpVerified`, y el
documento `users/{uid}` en su forma final **solo** se escribe por
`RegistrationRepository.finalizeRegistration()` después de que la verificación responde
correctamente (OTP de ChambAYA para Google, enlace de Firebase para correo+contraseña).

## 6. Seguridad

- La reanudación **no permite tomar cuentas ajenas**: exige la contraseña correcta o la
  credencial de Google del propio usuario.
- Sin sesión no se lee ningún documento ajeno (reglas de Firestore: `users` es
  legible solo por el dueño, `list: if false`).
- El DNI/RUC se muestra enmascarado en los diálogos (`maskEmail`, y el documento solo se
  completa si el usuario lo tiene en memoria).
- La conclusión «ya está registrada» solo se afirma con una lectura exitosa
  (`UNKNOWN` no afirma nada).

## 7. Limitaciones conocidas

- El pendiente local es de **un solo slot**: si el usuario abandona un registro y empieza
  otro con otro correo, el anterior deja de ser recuperable localmente (el documento en
  Firestore, si llegó a escribirse, no se ve afectado).
- El estado `INCOMPLETE` cubre documentos antiguos: un usuario previo a la FASE 1 con
  `registrationStatus == "VERIFIED"` pero sin bloque `identity` se considera `COMPLETE`
  (puede entrar por login, luego no debe poder re-registrarse). La migración de esos
  documentos no forma parte de esta corrección.
- Si un documento existente tiene `identity` y todavía no está `VERIFIED`, las reglas
  impiden cambiar el `documentNumber`; el cliente muestra «No pudimos guardar tu cuenta»
  y el usuario debe reintentar con el mismo documento.

## 8. Cómo probar en un dispositivo

Caso 1 — registro a medias con correo y contraseña (el bug reportado)
1. Registro nuevo: rol → DNI (válido en RENIEC) → correo + contraseña.
2. Cierra la app **antes** de confirmar la verificación.
3. Reabre la app: aparece «Retoma tu registro» → *Continuar verificación* → sub-paso 3
   con el DNI ya validado. O, si se descarta el diálogo, repite DNI + mismo correo +
   misma contraseña: no aparece «cuenta ya registrada», aparece «Retoma tu registro».
4. Confirma la verificación → se crea `users/{uid}` con `identity` y `profile`.

Caso 2 — registro a medias con Google
1. Mismo proceso por el botón de Google, cerrando antes del OTP.
2. Al volver, «Continuar con Google» con la misma cuenta → «Retoma tu registro» → llega
   al sub-paso 3 con un OTP nuevo.

Caso 3 — cuenta ya completa (no debe cambiar el comportamiento)
1. Entra con una cuenta ya registrada: «Esta cuenta ya está registrada» → *Iniciar
   sesión* → el login aparece con el correo precargado.
2. Repetido por Google: mismo mensaje.

Caso 4 — credenciales que no coinciden
1. Con una cuenta existente, escribe el mismo correo con otra contraseña válida de 8+
   caracteres → «Esa cuenta ya existe, la contraseña no coincide», sin crear nada.
