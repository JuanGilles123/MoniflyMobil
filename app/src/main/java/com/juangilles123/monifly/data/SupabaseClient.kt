package com.juangilles123.monifly.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth // Importa Auth en lugar de GoTrue directamente para install
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest // Importa Postgrest para install
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.engine.android.Android

object SupabaseClient {

    private const val SUPABASE_URL = "https://zgiwtgobrmkirrbtmhkf.supabase.co"
    private const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpnaXd0Z29icm1raXJyYnRtaGtmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTY3NTY0NDMsImV4cCI6MjA3MjMzMjQ0M30.osITwDqm6kKetgyU-HN14AoofSYOXg2qyyryO3jczdA"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        defaultSerializer = KotlinXSerializer()
        httpEngine = Android.create() // No es necesario crear uno nuevo si el default te sirve

        install(Auth) // Así se instala el plugin de Autenticación
        install(Postgrest) // Así se instala el plugin de Postgrest

        // La configuración de logging avanzada se puede añadir aquí si es necesario
    }
}
