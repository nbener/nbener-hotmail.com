package com.sezai.assistant

fun main() {
    val ornekler = listOf(
        "Merhaba 😀 nasılsın?" to "Merhaba nasılsın?",
        "❤️ Seni dinliyorum." to "Seni dinliyorum.",
        "Aile 👨‍👩‍👧‍👦 ve 👍🏽 tamam." to "Aile ve tamam.",
        "🇹🇷 Türkiye" to "Türkiye",
        "1️⃣ Saat 12:30, %25 ve 2+2=4." to "Saat 12:30, %25 ve 2+2=4.",
        "😀😃😂" to "",
        "Türkçe: ığüşöç İĞÜŞÖÇ." to "Türkçe: ığüşöç İĞÜŞÖÇ.",
        "İlk satır.\nİkinci satır." to "İlk satır.\nİkinci satır."
    )
    for ((girdi, beklenen) in ornekler) check(SesMetni.temizle(girdi) == beklenen) { "Ses metni eşleşmedi" }
    println("Ses metni: ${ornekler.size}/${ornekler.size} PASS")
}
