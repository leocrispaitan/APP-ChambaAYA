package com.proyecto.chambaya.data.repository

import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Motivo legible de un fallo de Firestore, en una línea.
 *
 * Existe porque `Tasks.await` (el puente de coroutines que usa este proyecto)
 * envuelve el error real en un `ExecutionException`. Sin desenrollar esa cadena el
 * `message` sale como
 * `"com.google.firebase.firestore.FirebaseFirestoreException: 7: PERMISSION_DENIED: ..."`,
 * que es justo lo que se leía en pantalla: el nombre de la clase y el prefijo del
 * código, no el motivo.
 *
 * Con el código (`PERMISSION_DENIED`, `UNAVAILABLE`, `NOT_FOUND`, ...) sí se puede
 * saber si es un problema de reglas, de red o de un documento que no existe, y eso
 * es lo que se le enseña al usuario.
 */
fun motivoFirestore(error: Throwable): String {
    val raiz = generateSequence(error) { it.cause }.last()
    return when (raiz) {
        is FirebaseFirestoreException -> raiz.code.name
        else -> raiz.message
            ?.lineSequence()
            ?.firstOrNull { it.isNotBlank() }
            ?.take(120)
            .orEmpty()
            .ifEmpty { raiz.javaClass.simpleName }
    }
}
