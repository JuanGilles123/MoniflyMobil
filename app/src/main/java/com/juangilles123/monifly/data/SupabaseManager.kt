package com.juangilles123.monifly.data

import com.juangilles123.monifly.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
// import io.github.jan.supabase.storage.Storage // Descomenta si necesitas Storage

object SupabaseManager {

    // IMPORTANTE: Reemplaza estos placeholders con tus BuildConfig fields
    // después de configurar local.properties y build.gradle.kts
    // private const val SUPABASE_URL = "TU_SUPABASE_URL_AQUI"
    // private const val SUPABASE_ANON_KEY = "TU_SUPABASE_ANON_KEY_AQUI"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            // Usa BuildConfig para mayor seguridad
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            // Instala los plugins que necesitas.
            // Auth es esencial para la autenticación de usuarios.
            install(Auth)
            // Postgrest es para interactuar con tu base de datos.
            install(Postgrest)
            // install(Storage) // Descomenta si vas a usar Supabase Storage para archivos
            // install(Realtime) // Descomenta si vas a usar Supabase Realtime
        }
    }
}