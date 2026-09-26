# FASE 1 — Registro Completo (implementación)

**Estado:** implementada y compilada (`:app:assembleDebug` → BUILD SUCCESSFUL)
**Alcance:** solo FASE 1 del plan maestro `CHAMBAYA_IMPLEMENTACION_FASES.md`.
No se implementó FASE 2 ni superior (perfil completo, contratante, publicaciones, chat).

---

## 1. Problema que se corrigió

Antes, `users/{uid}` solo guardaba el correo. El **nombre oficial (RENIEC/SUNAT)** y el
**DNI/RUC validado** se mostraban en pantalla, pero vivían únicamente en `TextView`s:

- se perdían al recrear la Activity (rotación o muerte de proceso),
- nunca llegaban a Firestore,
- el documento se armaba "a mano" en **tres** lugares distintos
  (`RegistroActivity`, fallback de OTP y Cloud Function), con esquemas incompatibles.

Ahora hay una **capa de datos única** (`data/`) y el documento `users/{uid}` se crea
una sola vez, con el nombre oficial y el documento de identidad.

---

## 2. Archivos modificados

### Nuevos (`app/src/main/java/com/proyecto/chambaya/data/`)

| Archivo | Responsabilidad |
|---|---|
| `model/RegistrationModels.kt` | Constantes de dominio (`UserRoles`, `IdentityDocumentTypes`, `AuthProviders`, `AuthMethods`, `EmailVerificationMethods`, `AccountStatuses`, `RegistrationStatuses`, `IdentitySources`, `ProfilePhotoSources`) + `ValidatedIdentity`, `RegistrationDraft`, `PendingRegistration` |
| `model/IdentityNameParser.kt` | Separa `nombre_completo` de RENIEC en `firstName` / `lastName` y aplica `Display Case` |
| `remote/IdentityValidationService.kt` | Consultation HTTP a RENIEC/SUNAT. Mismo `baseUrl`, mismo token y mismo timeout (10 s) que ya usaba la Activity. Devuelve `IdentityValidationResult.Success / Rejected / ServiceError / NetworkError` |
| `local/PendingRegistrationStore.kt` | Persistencia del registro en curso en `SharedPreferences` (borrador sin `uid` + pendiente con `uid`) |
| `repository/RegistrationRepository.kt` | **Única** fuente de verdad de `users/{uid}`: `finalizeRegistration()`, `touchLastLogin()`, `buildRegistrationDocument()`, `buildRefreshDocument()` |

### Modificados

| Archivo | Cambio |
|---|---|
| `RegistroActivity.kt` | Nuevo estado (`validatedIdentity`, `googleDisplayName/PhotoUrl`); consultas RENIEC/SUNAT vía `IdentityValidationService` + `lifecycleScope`; escritura de `users/{uid}` solo vía `RegistrationRepository`; payload `identity`/`profile` enviado a la Cloud Function; `PendingRegistrationStore` para sobrevivir rotación y muerte de proceso; se eliminó la escritura duplicada a `users` en el fallback de OTP |
| `LoginActivity.kt` | Lee `auth.email`, `activeRole` y `roles` con *fallback* a los campos raíz legacy; acepta `accountStatus == ACTIVE`; actualiza `lastLoginAt` al entrar |
| `res/values/strings.xml` | `register_step5_subtitle`, `register_btn_finish` ("Entrar a ChambAYA") y nueva clave `register_btn_saving` |
| `firestore.rules` | Reglas endurecidas (ver sección 4) |
| `functions/index.js` | `verifyEmailOtp` recibe y escribe la nueva estructura + espejo legacy; valida el formato del documento; preserva `createdAt` |

---

## 3. Estructura final de `users/{uid}`

```
users/{uid}
├── uid                      "el-uid-de-firebase-auth"
├── accountStatus            "ACTIVE"
├── registrationStatus       "VERIFIED"
├── roles                    [ "TRABAJADOR" ]  |  [ "CONTRATANTE" ]
├── activeRole               "TRABAJADOR"    |  "CONTRATANTE"
│
├── auth
│   ├── provider             "EMAIL" | "GOOGLE"
│   ├── email                "usuario@correo.com"      (normalizado en minúsculas)
│   ├── emailVerified        true
│   ├── otpVerified          true  (solo si el correo se confirmó con el OTP de ChambAYA)
│   └── verificationMethod   "CHAMBAYA_OTP" | "FIREBASE_EMAIL_LINK"
│
├── identity
│   ├── documentType         "DNI" | "RUC"
│   ├── documentNumber       "72345678" | "20123456789"   (validado en RENIEC/SUNAT)
│   ├── documentNumberMasked "****5678" | "***6789"       (para vistas públicas)
│   ├── identityVerified     true
│   ├── verifiedWith         "RENIEC" | "SUNAT"
│   ├── identityName         "JUAN CARLOS PEREZ"  |  "EMPRESA SAC S.A.C."
│   ├── identityStatus       "HABIDO / ACTIVO"  (RUC)
│   ├── location             "Lima, Lima"        (RENIEC)
│   └── verifiedAt           Timestamp
│
├── profile
│   ├── firstName            "Juan Carlos"
│   ├── lastName             "Perez"
│   ├── fullName             "JUAN CARLOS PEREZ"   (nombre oficial del padrón)
│   ├── profilePhotoUrl      "https://lh3.googleusercontent.com/..."
│   ├── profilePhotoPublicId ""
│   ├── profilePhotoSource   "GOOGLE" | "DEFAULT"
│   └── country              "Peru"
│
├── createdAt                Timestamp
├── updatedAt                Timestamp
└── lastLoginAt              Timestamp
│
└── ESPEJO LEGACY (raíz) — se conserva para no romper lecturas ya existentes
    ├── email                "usuario@correo.com"
    ├── role                 "TRABAJADOR" | "CONTRATANTE"
    ├── emailVerified        true
    ├── otpVerified          true | false
    ├── authMethod           "EMAIL_PASSWORD" | "GOOGLE"
    └── verifiedAt           Timestamp
```

### Reglas de la FASE 1

- **Nunca** se guarda la contraseña: vive en Firebase Authentication.
- **Nunca** se guardan imágenes ni Base64: solo la URL de Google.
- **No** se crea `employer` (pertenece a FASE 3).
- **No** se crea `profile.username` (pertenece a FASE 2).
- `identity.documentNumber` es de **solo lectura del propietario**; para exponerlo
  públicamente se usará `identity.documentNumberMasked`.
- Para RUC: `profile.firstName`/`lastName` van vacíos y `profile.fullName` es la razón social.
- `auth.otpVerified` es `true` solo si el correo se confirmó con el OTP de ChambAYA.
  La ruta Google+correo verificado por Firebase usa `FIREBASE_EMAIL_LINK`.

---

## 4. Reglas de seguridad (`firestore.rules`)

```text
users/{uid}
  allow list:   if false                                   # sin listados públicos
  allow get:    if request.auth.uid == uid
  allow create: if es el propio uid
                && registrationStatus == 'VERIFIED'
                && accountStatus == 'ACTIVE'
                && roles/activeRole coherentes
                && auth válido (provider, email, emailVerified)
                && identity válido (DNI 8 dígitos / RUC 11 dígitos,
                                    verifiedWith RENIEC|SUNAT, identityName)
  allow update: (a) solo ['updatedAt','lastLoginAt','lastSeenAt'], o
                (b) migración única si el documento no tenía bloque `identity`, o
                (c) completar el registro si aún no estaba VERIFIED
                    (el número de documento no puede cambiar)
  allow delete: if false

email_verifications/{uid}
  read:  solo el dueño
  create/update:  sesión OTP válida y `verified == false` (reenvío)
  update:  única transición permitida `verified: false -> true`
```

---

## 5. Flujo de escritura (único)

```
Paso 1  DNI/RUC  ──► IdentityValidationService ──► ValidatedIdentity
                                        │
                                        ▼
                        PendingRegistrationStore (SharedPreferences)
                                        │
Paso 2  Correo+contraseña | Google ─────┤
                                        ▼
Paso 3  OTP ChambAYA | correo Firebase  │   ← el Cloud Function puede
                                        │      escribir el mismo documento
                                        ▼
                     RegistrationRepository.finalizeRegistration()
                                        ▼
                              users/{uid} (completo)
                                        │
Paso 4  Resumen ──► "Entrar a ChambAYA" ──► LoginActivity
                       (limpia el store)
```

- El **fallback de OTP directo contra Firestore** (plan Spark) ya **no** escribe `users`:
  solo invalida el OTP. La escritura autoritativa es la del repositorio.
- `finalizeRegistration()` es **idempotente**: si el documento ya tiene `identity` y
  `profile`, solo refresca `updatedAt`/`lastLoginAt` (respeta las Rules). Si quedó
  incompleto (versión previa de la app o Cloud Function sin `identity`), completa campos.
- `LoginActivity` llama a `touchLastLogin(uid)`, que solo escribe auditoría.

---

## 6. Verificación realizada

| Prueba | Resultado |
|---|---|
| `:app:compileDebugKotlin` | OK |
| `:app:assembleDebug` (limpio) | **BUILD SUCCESSFUL** — `app/build/outputs/apk/debug/app-debug.apk` |
| `node --check functions/index.js` | OK |
| 36 pruebas unitarias de los helpers de `functions/index.js` (`normalizeIdentity`, `buildUserDocument`, `maskDocumentNumber`) | **TODAS PASARON** |

Las pruebas cubrieron: rechazo de DNI de 7 dígitos, RUC de 10 dígitos, letras en el
número, nombre demasiado corto, `identity` nulo, tipo de documento desconocido,
enmascarado, coherencia `roles`/`activeRole`, preservación de `createdAt`, rol inválido
→ `TRABAJADOR`, y ausencia de contraseñas en el documento.

---

## 7. Pendiente para FASE 16 (deuda de seguridad conocida)

Estas decisiones se tomaron para **no romper** el funcionamiento actual, pero deben
corregirse cuando la app pase a plan Blaze con Functions desplegadas:

1. **El token de RENIEC/SUNAT está en el cliente** (`IdentityValidationService` y el
   companion object de `RegistroActivity`). Cualquiera con el APK puede escribir un DNI
   falso. Debe moverse a una Cloud Function.
2. **El hash del OTP se calcula en el cliente** con una sal embebida. Debe validarse
   solo en backend.
3. **No hay unicidad global de DNI/RUC**: dos cuentas pueden registrarse con el mismo
   documento. Requiere una colección de reservas (`identity_reservations`) con
   transacción, o un claim por documento.
4. `email_verifications` es accesible por el cliente (compatible con Spark). Con Blaze
   debe pasar a ser de solo lectura/escritura del backend.

---

## 8. Cómo probar el resultado

1. Registrarse como **Trabajador** con DNI real → tarjeta "✓ Verificación confirmada con RENIEC".
2. Continuar con correo + contraseña o Google.
3. Verificar el OTP (o el correo de Firebase).
4. En Firestore → `users/{uid}`:
   - `identity.identityName` debe coincidir con el nombre de RENIEC.
   - `identity.documentNumber` = el DNI consultado.
   - `profile.fullName` = el nombre oficial.
   - `auth.email` en minúsculas, `auth.emailVerified: true`.
   - `roles: ["TRABAJADOR"]`, `activeRole: "TRABAJADOR"`.
5. Repetir como **Contratante** con un RUC ACTIVO/HABIDO →
   `identity.verifiedWith: "SUNAT"`, `profile.fullName` = razón social.
6. Entrar a la app → `lastLoginAt` se actualiza.
7. Matar la app en el paso 1 y reabrirla → la tarjeta de verificado sigue poblada
   (gracias a `PendingRegistrationStore`).
