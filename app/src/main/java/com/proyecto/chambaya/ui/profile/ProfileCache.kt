package com.proyecto.chambaya.ui.profile

import com.proyecto.chambaya.data.model.UserProfile

/**
 * Último [UserProfile] leído de Firestore en esta sesión de la app, compartido
 * entre `FragmentoMiPerfil` y `FragmentoAjustesPerfil`.
 *
 * Por qué existe:
 *
 * `MainActivity` mantiene un único `FragmentoMiPerfil` durante toda la vida de
 * la Activity (`by lazy`), así que ESE fragmento ya reutilizaba sus propios
 * datos al volver a la pestaña. Pero cada vez que se toca el botón de
 * ajustes se crea una instancia NUEVA de `FragmentoAjustesPerfil`
 * (`FragmentoAjustesPerfil()` en el `replace(...)`), así que esa pantalla
 * siempre empezaba desde cero: pintaba lo que hubiera en el XML (antes,
 * datos mock) mientras esperaba a Firestore, en cada visita.
 *
 * Con este caché, la primera pantalla que logra leer `users/{uid}` dejar
 * el resultado aquí, y cualquier otra pantalla del perfil que se abra
 * después lo pinta de inmediato, sin depender de una nueva consulta de red
 * ni de placeholders. Firestore se sigue consultando igual (para reflejar
 * cambios), pero ya no hay pantalla en blanco ni con datos falsos mientras
 * responde.
 *
 * Vive solo en memoria: se pierde si el proceso muere (rotación de pantalla
 * no lo mata, cerrar la app sí), que es exactamente lo que se quiere — no es
 * un caché de persistencia, es solo para no repetir el parpadeo entre
 * pantallas dentro de la misma sesión.
 */
object ProfileCache {

    @Volatile
    var perfil: UserProfile? = null

    /** Limpia el caché; se usa al cerrar sesión para no arrastrar datos del usuario anterior. */
    fun limpiar() {
        perfil = null
    }
}
