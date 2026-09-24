package com.warehouse.rfid.edge

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseModule {
    val client = createSupabaseClient(
        supabaseUrl = "https://bkufdfyfgkrgiwrvdnfi.supabase.co",
        supabaseKey = "sb_publishable_ZkMGvLh6IJJwdV1nwSOV3Q_m4R5ktGf"
    ) {
        install(Postgrest)
    }
}
