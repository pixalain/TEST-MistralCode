package com.latraverse.app.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Concert(
    val artist: String,
    val date: LocalDate,
    val time: String,
    val genre: String,
    val support: String? = null,
    val description: String? = null
) {
    val isPast: Boolean
        get() = date.isBefore(LocalDate.now())

    fun formattedDate(): String =
        date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy"))
}

object Concerts {

    val all: List<Concert> = listOf(
        // --- Concerts passés (saison 2025-2026) ---
        Concert(
            artist = "The Commoners",
            date = LocalDate.of(2025, 11, 27),
            time = "20h30",
            genre = "Rock / Pop folk",
            description = "Groupe canadien de rock sudiste et de roots américaines."
        ),
        Concert(
            artist = "Beverly Jo Scott & Divine Rebels",
            date = LocalDate.of(2025, 11, 29),
            time = "20h30",
            genre = "Soul / Rock",
            description = "La voix puissante de BJS accompagnée des Divine Rebels."
        ),
        Concert(
            artist = "Jovin Webb",
            date = LocalDate.of(2025, 12, 6),
            time = "20h30",
            genre = "Blues",
            description = "Batteur et chanteur de blues de Bâton Rouge, Louisiane."
        ),
        Concert(
            artist = "Connor Selby",
            date = LocalDate.of(2025, 12, 14),
            time = "20h30",
            genre = "Rock / Blues",
            description = "Auteur-compositeur britannique, blues rock saisissant."
        ),
        Concert(
            artist = "Tiwayo",
            date = LocalDate.of(2026, 4, 10),
            time = "20h30",
            genre = "Soul",
            description = "Chanteur de soul parisien à l'énergie communicative."
        ),
        Concert(
            artist = "Yaniss Odua",
            date = LocalDate.of(2026, 4, 30),
            time = "20h30",
            genre = "Reggae",
            description = "Figure emblématique du reggae-dancehall français, originaire de Martinique."
        ),
        Concert(
            artist = "No One Is Innocent",
            date = LocalDate.of(2026, 5, 21),
            time = "20h30",
            genre = "Rock / Nu metal",
            support = "Snekkar",
            description = "Le rock français légendaire, avec le nu metal havrais de Snekkar en première partie."
        ),
        Concert(
            artist = "Jarl + Robert Jon and the Wreck",
            date = LocalDate.of(2026, 10, 24),
            time = "20h30",
            genre = "Indie folk / Blues rock",
            description = "L'indie folk français de Jarl et le rock sudiste californien de Robert Jon and the Wreck."
        ),
        Concert(
            artist = "Francesco Piu & Lakeetra Knowles + Robert Finley",
            date = LocalDate.of(2026, 11, 5),
            time = "20h30",
            genre = "Soul blues",
            description = "Guitariste italien et chanteuse américaine, puis le blues soul de Robert Finley."
        ),
        Concert(
            artist = "Chicago Blues Festival",
            date = LocalDate.of(2026, 11, 8),
            time = "15h30",
            genre = "Blues",
            description = "Avec Zac Harmon, Lady Adrena et Kingston Livingston, trois générations de blues moderne."
        ),

        // --- Concerts à venir ---
        Concert(
            artist = "Carolyn Wonderland",
            date = LocalDate.of(2026, 11, 13),
            time = "20h30",
            genre = "Blues",
            support = "Nina Attal",
            description = "Guitariste et chanteuse de blues texane, mêlant blues, rock, soul et country. Avec la française Nina Attal en première partie."
        ),
        Concert(
            artist = "Magma",
            date = LocalDate.of(2026, 11, 17),
            time = "20h30",
            genre = "Zeuhl / Rock progressif",
            description = "Tournée « Zeuhl Kommandöh » : Christian Vander et ses musiciens sur la scène de Cléon."
        ),
        Concert(
            artist = "Manu Lanvin",
            date = LocalDate.of(2026, 11, 21),
            time = "20h30",
            genre = "Blues rock",
            support = "Little Odetta",
            description = "Le blues rock français de Manu Lanvin, avec Little Odetta en ouverture."
        ),
        Concert(
            artist = "FFF",
            date = LocalDate.of(2026, 11, 26),
            time = "20h30",
            genre = "Groove rock",
            support = "LAKLÉ",
            description = "Le « fonck » français de FFF, date unique en Seine-Maritime de leur tournée 2026."
        ),
        Concert(
            artist = "The Buttshakers + 24e Tremplin de la Traverse",
            date = LocalDate.of(2026, 12, 13),
            time = "15h30",
            genre = "Soul",
            description = "Le 24e Tremplin réunit des groupes normands en audition publique, suivi du concert soul et funk des Buttshakers."
        ),
        Concert(
            artist = "The Last Internationale",
            date = LocalDate.of(2027, 2, 12),
            time = "20h30",
            genre = "Rock USA",
            description = "Duo de rock new-yorkais aux performances live puissantes et aux textes engagés."
        )
    )

    fun upcoming(reference: LocalDate = LocalDate.now()): List<Concert> =
        all.filter { !it.date.isBefore(reference) }.sortedBy { it.date }

    fun past(reference: LocalDate = LocalDate.now()): List<Concert> =
        all.filter { it.date.isBefore(reference) }.sortedByDescending { it.date }
}

object Venue {
    const val NAME = "La Traverse"
    const val ADDRESS = "37 rue Luis Corvalan, 76410 Cléon"
    const val PHONE = "02 35 81 25 25"
    const val WEBSITE = "https://www.latraverse.org/"
    const val MAP_URL = "https://maps.google.com/?q=La+Traverse+37+rue+Luis+Corvalan+76410+Cl%C3%A9on"
}
