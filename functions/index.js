/**
 * Backend Cloud Functions para ChambAYA
 * FASE 3: Verificación de Correo mediante OTP de 6 dígitos
 * 
 * Tecnologías:
 * - Firebase Functions (v1 / v2 compatible)
 * - Firebase Admin SDK (Firestore & Auth)
 * - Node.js crypto (Generación y hash seguro SHA-256)
 * - Resend API / Nodemailer (Envío de correo transaccional)
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");

// Inicializar Admin SDK una sola vez
if (!admin.apps.length) {
    admin.initializeApp();
}

const db = admin.firestore();

// Constantes de seguridad
const OTP_LENGTH = 6;
const OTP_EXPIRATION_MS = 5 * 60 * 1000; // 5 minutos
const RESEND_COOLDOWN_MS = 60 * 1000;     // 60 segundos
const MAX_ATTEMPTS = 5;                   // Máximo 5 intentos por código

// =============================================================
//  Constantes FASE 1 - Esquema de `users/{uid}`
// =============================================================
const VALID_ROLES = ["TRABAJADOR", "CONTRATANTE"];
const VALID_DOCUMENT_TYPES = ["DNI", "RUC"];
const VALID_IDENTITY_SOURCES = ["RENIEC", "SUNAT"];
const DEFAULT_COUNTRY = "Peru";

/**
 * Versión segura del número de documento para vistas públicas.
 */
function maskDocumentNumber(documentType, documentNumber) {
    if (documentType === "RUC") {
        return `***${documentNumber.slice(-4)}`;
    }
    return `****${documentNumber.slice(-4)}`;
}

/**
 * Valida y normaliza el bloque `identity` que envía el cliente.
 * Devuelve `null` si el documento no tiene formato válido (DNI/RUC).
 */
function normalizeIdentity(rawIdentity) {
    if (!rawIdentity || typeof rawIdentity !== "object") {
        return null;
    }

    const documentType = String(rawIdentity.documentType || "").toUpperCase();
    const documentNumber = String(rawIdentity.documentNumber || "").trim();
    const identityName = String(rawIdentity.identityName || "").trim();

    if (VALID_DOCUMENT_TYPES.indexOf(documentType) === -1) {
        return null;
    }

    const expectedLength = documentType === "DNI" ? 8 : 11;
    if (!/^[0-9]+$/.test(documentNumber) || documentNumber.length !== expectedLength) {
        return null;
    }

    if (identityName.length <= 2) {
        return null;
    }

    const verifiedWith = VALID_IDENTITY_SOURCES.indexOf(rawIdentity.verifiedWith) !== -1
        ? rawIdentity.verifiedWith
        : (documentType === "RUC" ? "SUNAT" : "RENIEC");

    return {
        documentType: documentType,
        documentNumber: documentNumber,
        documentNumberMasked: String(rawIdentity.documentNumberMasked || "").trim()
            || maskDocumentNumber(documentType, documentNumber),
        identityVerified: true,
        verifiedWith: verifiedWith,
        identityName: identityName,
        identityStatus: String(rawIdentity.identityStatus || "").trim(),
        location: String(rawIdentity.location || "").trim(),
        firstName: String(rawIdentity.firstName || "").trim(),
        lastName: String(rawIdentity.lastName || "").trim()
    };
}

/**
 * Arma el documento de `users/{uid}` de la FASE 1: nombre oficial del padrón,
 * DNI/RUC, roles y verificaciones. Nunca incluye la contraseña.
 * Incluye el espejo de campos raíz que la app ya leía (`email`, `role`, etc.).
 */
function buildUserDocument(options) {
    const uid = options.uid;
    const identity = options.identity || null;
    const profile = options.profile || {};
    const now = admin.firestore.FieldValue.serverTimestamp();
    const safeRole = VALID_ROLES.indexOf(options.role) !== -1 ? options.role : "TRABAJADOR";
    const email = options.email || "";
    const otpVerified = options.otpVerified === true;
    const photoUrl = String(profile.profilePhotoUrl || "").trim();
    const fullName = identity && identity.identityName
        ? identity.identityName
        : String(profile.fullName || "").trim();

    const userDoc = {
        uid: uid,
        accountStatus: "ACTIVE",
        registrationStatus: "VERIFIED",
        roles: [safeRole],
        activeRole: safeRole,
        auth: {
            provider: options.provider === "GOOGLE" ? "GOOGLE" : "EMAIL",
            email: email,
            emailVerified: true,
            otpVerified: otpVerified,
            verificationMethod: otpVerified ? "CHAMBAYA_OTP" : "FIREBASE_EMAIL_LINK"
        },
        profile: {
            firstName: identity ? identity.firstName : "",
            lastName: identity ? identity.lastName : "",
            fullName: fullName,
            profilePhotoUrl: photoUrl,
            profilePhotoPublicId: "",
            profilePhotoSource: photoUrl ? "GOOGLE" : "DEFAULT",
            country: DEFAULT_COUNTRY
        },
        createdAt: options.existingCreatedAt || admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: now,
        lastLoginAt: now,
        // Espejo de compatibilidad con las versiones previas de la app
        email: email,
        role: safeRole,
        emailVerified: true,
        otpVerified: otpVerified,
        authMethod: options.authMethod === "GOOGLE" ? "GOOGLE" : "EMAIL_PASSWORD",
        verifiedAt: now
    };

    if (identity) {
        userDoc.identity = Object.assign({}, identity, { verifiedAt: now });
    }

    return userDoc;
}

/**
 * Obtener la sal secreta del entorno
 */
function getOtpSalt() {
    return process.env.OTP_SALT || "chambaya_secure_otp_default_salt_2026";
}

/**
 * Generar un OTP criptográficamente seguro de 6 dígitos (puede iniciar con 0)
 */
function generateSecureOtp() {
    const min = 0;
    const max = 1000000;
    const num = crypto.randomInt(min, max);
    return num.toString().padStart(OTP_LENGTH, "0");
}

/**
 * Generar el hash SHA-256 del OTP combinado con sal y UID
 */
function hashOtp(otp, uid) {
    const salt = getOtpSalt();
    return crypto.createHash("sha256").update(`${otp}:${salt}:${uid}`).digest("hex");
}

/**
 * Enviar correo con el código OTP utilizando Resend o Nodemailer
 */
async function sendOtpEmail(email, otp) {
    const resendApiKey = process.env.RESEND_API_KEY;
    const fromEmail = process.env.RESEND_FROM_EMAIL || "ChambAYA <onboarding@resend.dev>";

    const htmlContent = `
    <!DOCTYPE html>
    <html lang="es">
    <head>
      <meta charset="utf-8">
      <title>Código de verificación ChambAYA</title>
      <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #F8FAFC; margin: 0; padding: 24px; color: #1E293B; }
        .container { max-width: 500px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; padding: 32px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05); border: 1px solid #E2E8F0; }
        .header { text-align: center; margin-bottom: 24px; }
        .logo { font-size: 28px; font-weight: 800; color: #0284C7; letter-spacing: -0.5px; }
        .title { font-size: 20px; font-weight: 700; color: #0F172A; margin-top: 16px; margin-bottom: 8px; }
        .subtitle { font-size: 14px; color: #64748B; margin: 0; }
        .code-box { background: #F0F9FF; border: 2px dashed #0284C7; border-radius: 12px; padding: 20px; text-align: center; margin: 28px 0; }
        .otp-code { font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #0369A1; font-family: monospace; }
        .footer { font-size: 12px; color: #94A3B8; text-align: center; margin-top: 32px; border-top: 1px solid #F1F5F9; padding-top: 16px; line-height: 1.5; }
        .badge { display: inline-block; background: #E0F2FE; color: #0369A1; font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 20px; margin-top: 8px; }
      </style>
    </head>
    <body>
      <div class="container">
        <div class="header">
          <div class="logo">Chamb<span style="color:#0EA5E9;">AYA</span></div>
          <div class="title">Verifica tu correo electrónico</div>
          <p class="subtitle">Usa el siguiente código de 6 dígitos para continuar tu registro en ChambAYA.</p>
        </div>
        
        <div class="code-box">
          <div class="otp-code">${otp}</div>
          <div class="badge">Válido durante 5 minutos</div>
        </div>
        
        <p style="font-size: 13px; color: #475569; line-height: 1.5;">
          Por seguridad, no compartas este código con nadie. El equipo de ChambAYA nunca te pedirá tu código por teléfono ni mensaje.
        </p>
        
        <div class="footer">
          Si no solicitaste este código, puedes ignorar este mensaje de forma segura.<br>
          © ${new Date().getFullYear()} ChambAYA. Todos los derechos reservados.
        </div>
      </div>
    </body>
    </html>
    `;

    const textContent = `Hola,\n\nTu código de verificación de ChambAYA es: ${otp}\n\nEste código es válido durante 5 minutos.\nSi no solicitaste este código, puedes ignorar este mensaje.\n\nEquipo ChambAYA`;

    if (resendApiKey) {
        try {
            const { Resend } = require("resend");
            const resend = new Resend(resendApiKey);
            await resend.emails.send({
                from: fromEmail,
                to: email,
                subject: "Tu código de verificación de ChambAYA",
                html: htmlContent,
                text: textContent
            });
            console.log(`[Resend] OTP enviado exitosamente a ${email}`);
            return true;
        } catch (error) {
            console.error("[Resend Error]", error);
            throw new Error(`Error enviando correo: ${error.message}`);
        }
    } else {
        // En entorno de desarrollo o previo a configuración de la API Key,
        // registramos el evento en el logger del backend para no romper el flujo.
        console.warn(`[DEV/TEST] RESEND_API_KEY no configurada. Código generado para ${email}: [${otp}]`);
        return true;
    }
}

/**
 * Cloud Function Callable: requestEmailOtp
 * Genera y envía un código OTP de 6 dígitos al correo del usuario autenticado.
 */
exports.requestEmailOtp = functions.https.onCall(async (data, context) => {
    // 1. Validar autenticación
    if (!context.auth || !context.auth.uid) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "Debes estar autenticado para solicitar un código de verificación."
        );
    }

    const uid = context.auth.uid;
    const userRecord = await admin.auth().getUser(uid);
    const email = (data && data.email) ? data.email.trim().toLowerCase() : (userRecord.email || "").toLowerCase();

    if (!email) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "No se encontró una dirección de correo asociada a la cuenta."
        );
    }

    const now = Date.now();
    const verificationRef = db.collection("email_verifications").doc(uid);
    const existingDoc = await verificationRef.get();

    // 2. Control de Reenvíos (Cooldown de 60 segundos)
    if (existingDoc.exists) {
        const existingData = existingDoc.data();
        if (existingData.resendAvailableAt) {
            const resendAvailableAtMs = existingData.resendAvailableAt.toMillis();
            if (now < resendAvailableAtMs) {
                const waitSeconds = Math.ceil((resendAvailableAtMs - now) / 1000);
                throw new functions.https.HttpsError(
                    "resource-exhausted",
                    `Debes esperar ${waitSeconds} segundos antes de solicitar otro código.`
                );
            }
        }
    }

    // 3. Generación criptográfica segura de 6 dígitos
    const otp = generateSecureOtp();
    const hashedOtp = hashOtp(otp, uid);

    const expiresAt = admin.firestore.Timestamp.fromMillis(now + OTP_EXPIRATION_MS);
    const resendAvailableAt = admin.firestore.Timestamp.fromMillis(now + RESEND_COOLDOWN_MS);

    // 4. Guardar información temporal en Firestore (el OTP nunca se guarda en texto plano)
    await verificationRef.set({
        uid: uid,
        email: email,
        otpHash: hashedOtp,
        expiresAt: expiresAt,
        resendAvailableAt: resendAvailableAt,
        attempts: 0,
        maxAttempts: MAX_ATTEMPTS,
        verified: false,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        createdAt: existingDoc.exists ? (existingDoc.data().createdAt || admin.firestore.FieldValue.serverTimestamp()) : admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    // 5. Enviar el correo electrónico
    try {
        await sendOtpEmail(email, otp);
    } catch (mailErr) {
        console.error("Error al enviar el correo:", mailErr);
        throw new functions.https.HttpsError(
            "internal",
            "No pudimos enviar el correo de verificación. Intenta nuevamente."
        );
    }

    return {
        success: true,
        message: "Código de verificación enviado correctamente.",
        expiresAtMillis: now + OTP_EXPIRATION_MS,
        resendCooldownSeconds: 60
    };
});

/**
 * Cloud Function Callable: verifyEmailOtp
 * Valida el código de 6 dígitos introducido por el usuario.
 */
exports.verifyEmailOtp = functions.https.onCall(async (data, context) => {
    // 1. Validar autenticación
    if (!context.auth || !context.auth.uid) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "Debes estar autenticado para verificar tu correo."
        );
    }

    const uid = context.auth.uid;
    const inputOtp = (data && data.otp) ? String(data.otp).trim() : "";

    if (!inputOtp || inputOtp.length !== OTP_LENGTH || !/^\d{6}$/.test(inputOtp)) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "El código debe contener exactamente 6 dígitos numéricos."
        );
    }

    const verificationRef = db.collection("email_verifications").doc(uid);
    const verificationDoc = await verificationRef.get();

    if (!verificationDoc.exists) {
        throw new functions.https.HttpsError(
            "not-found",
            "No se encontró una solicitud de verificación activa. Solicita un nuevo código."
        );
    }

    const verificationData = verificationDoc.data();

    // Ya verificado
    if (verificationData.verified === true) {
        return {
            success: true,
            verified: true,
            message: "Tu correo electrónico ya ha sido verificado."
        };
    }

    const now = Date.now();

    // 2. Validar límite de intentos
    const currentAttempts = verificationData.attempts || 0;
    if (currentAttempts >= MAX_ATTEMPTS) {
        // Invalidar OTP actual
        await verificationRef.update({
            otpHash: null,
            attempts: currentAttempts + 1,
            invalidatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        throw new functions.https.HttpsError(
            "failed-precondition",
            "Has superado el número de intentos. Solicita un nuevo código para continuar."
        );
    }

    // 3. Validar expiración (5 minutos con reloj de servidor)
    if (!verificationData.expiresAt || now > verificationData.expiresAt.toMillis()) {
        throw new functions.https.HttpsError(
            "deadline-exceeded",
            "El código ha expirado. Solicita un nuevo código para continuar."
        );
    }

    // 4. Validar coincidencia de código mediante comparación segura
    const expectedHash = verificationData.otpHash;
    const computedHash = hashOtp(inputOtp, uid);

    if (!expectedHash || expectedHash !== computedHash) {
        const nextAttempts = currentAttempts + 1;
        const remaining = Math.max(0, MAX_ATTEMPTS - nextAttempts);

        await verificationRef.update({
            attempts: nextAttempts,
            lastFailedAttempt: admin.firestore.FieldValue.serverTimestamp()
        });

        if (remaining === 0) {
            throw new functions.https.HttpsError(
                "failed-precondition",
                "Has superado el número de intentos. Solicita un nuevo código para continuar."
            );
        }

        throw new functions.https.HttpsError(
            "invalid-argument",
            `Código incorrecto. Verifica el código e inténtalo nuevamente. Te quedan ${remaining} intento(s).`
        );
    }

    // FASE 1: preparar `users/{uid}` con el nombre oficial y el DNI/RUC
    const userDocRef = db.collection("users").doc(uid);
    const userRole = (data && data.role) ? data.role : "TRABAJADOR";
    const userEmail = (data && data.email) ? String(data.email).trim().toLowerCase() : (verificationData.email || "");
    const identity = normalizeIdentity(data ? data.identity : null);

    if ((data && data.identity) && !identity) {
        // El cliente envió una identidad con formato inválido: no se marca el OTP
        // como usado para que el cliente pueda reintentar la verificación.
        throw new functions.https.HttpsError(
            "invalid-argument",
            "Los datos de identidad enviados no son válidos. Vuelve a verificar tu DNI o RUC."
        );
    }

    // Preservar `createdAt` si el documento ya existía
    let existingCreatedAt = null;
    try {
        const existingUserDoc = await userDocRef.get();
        if (existingUserDoc.exists && existingUserDoc.data().createdAt) {
            existingCreatedAt = existingUserDoc.data().createdAt;
        }
    } catch (readErr) {
        console.warn("[verifyEmailOtp] No se pudo leer el documento previo:", readErr.message);
    }

    // 5. Código correcto: Actualización atómica en backend
    const batch = db.batch();

    // Invalidar OTP para que sea de un solo uso y marcar como verificado
    batch.update(verificationRef, {
        verified: true,
        otpHash: null, // Destruir el hash para que sea estrictamente de un solo uso
        verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
        attempts: currentAttempts
    });


    batch.set(userDocRef, buildUserDocument({
        uid: uid,
        role: userRole,
        email: userEmail,
        provider: data && data.provider,
        authMethod: data && data.authMethod,
        identity: identity,
        profile: data ? data.profile : null,
        existingCreatedAt: existingCreatedAt,
        otpVerified: true
    }), { merge: true });

    await batch.commit();

    console.log(`[OTP Success] Usuario ${uid} (${userEmail}) verificado correctamente con rol ${userRole}`);

    return {
        success: true,
        verified: true,
        registrationStatus: "VERIFIED",
        identitySaved: identity !== null,
        message: "Código verificado exitosamente."
    };
});
