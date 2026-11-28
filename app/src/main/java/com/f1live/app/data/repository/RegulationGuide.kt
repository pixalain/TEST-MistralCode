package com.f1live.app.data.repository

/**
 * Guide du règlement F1 2026, rédigé pour les néophytes.
 * Contenu éditorial maison, organisé par grandes catégories.
 */
fun builtInRegulations(): Regulations = Regulations(
    categories = listOf(
        RegulationCategory(
            title = "🏁 Les bases — c'est quoi la F1 ?",
            sections = listOf(
                RegulationSection(
                    title = "Le championnat",
                    points = listOf(
                        "La Formule 1 est un championnat du monde : 10 écuries, 20 pilotes, et une vingtaine de Grands Prix par saison dans le monde entier.",
                        "Deux titres sont attribués chaque année : le championnat du monde des pilotes et celui des constructeurs (écuries).",
                        "Chaque écurie engage 2 pilotes ; les points des deux pilotes comptent pour le championnat des constructeurs."
                    )
                ),
                RegulationSection(
                    title = "Une voiture, un pilote",
                    points = listOf(
                        "Chaque pilote dispose de sa propre monoplace, réglée à sa taille et à son style de pilotage.",
                        "Les voitures doivent respecter un règlement technique très strict (dimensions, poids, moteur, aérodynamique)."
                    )
                )
            )
        ),
        RegulationCategory(
            title = "📅 Le week-end de course",
            sections = listOf(
                RegulationSection(
                    title = "Les séances",
                    points = listOf(
                        "Vendredi/samedi : essais libres (FP1, FP2, FP3) pour régler la voiture et tester les pneus.",
                        "Qualifications (Q) : décide de la grille de départ de la course. Format en 3 manches (Q1, Q2, Q3) : les plus lents sont éliminés à chaque manche.",
                        "Sur certains week-ends, un Sprint a lieu : une course courte (environ 100 km) qui rapporte des points et peut décider de la grille du dimanche."
                    )
                ),
                RegulationSection(
                    title = "La course",
                    points = listOf(
                        "Le dimanche, les 20 pilotes partent dans l'ordre des qualifications, sur une grille de 2 par 2.",
                        "La course dure environ 300 km (ou 2 heures max) avec un nombre de tours fixé selon le circuit.",
                        "Le premier arrivé remporte le Grand Prix."
                    )
                )
            )
        ),
        RegulationCategory(
            title = "🏆 Les points",
            sections = listOf(
                RegulationSection(
                    title = "Barème de la course",
                    points = listOf(
                        "Course : 25, 18, 15, 12, 10, 8, 6, 4, 2, 1 point pour les 10 premiers.",
                        "+ 1 point pour le pilote qui signe le tour le plus rapide, s'il termine dans le top 10.",
                        "Sprint : 8, 7, 6, 5, 4, 3, 2, 1 point pour les 8 premiers."
                    )
                ),
                RegulationSection(
                    title = "Pour gagner le championnat",
                    points = listOf(
                        "À la fin de la saison, celui qui a le plus de points est champion du monde.",
                        "En cas d'égalité, le plus grand nombre de victoires l'emporte."
                    )
                )
            )
        ),
        RegulationCategory(
            title = "🛞 Les pneus",
            sections = listOf(
                RegulationSection(
                    title = "Trois gommes par week-end",
                    points = listOf(
                        "Pluie extrême (bleu), pluie (vert), ou slicks : tendre (rouge, rapide mais usure rapide), médium (jaune), dur (blanc, résistant).",
                        "Chaque pilote reçoit un nombre limité de trains de pneus pour tout le week-end.",
                        "En course sèche, chaque pilote doit utiliser au moins deux composés différents (obligation de changer de pneus)."
                    )
                )
            )
        ),
        RegulationCategory(
            title = "🚦 Drapeaux et neutralisations",
            sections = listOf(
                RegulationSection(
                    title = "Les drapeaux",
                    points = listOf(
                        "🟩 vert : course en cours · 🟨 jaune : danger, ralentir · 🟥 rouge : arrêt immédiat · 🏁 à carreaux : fin de course.",
                        "🔵 bleu : une voiture rapide arrive, laisser passer · ⚫⚪ noir et blanc : avertissement · ⚫🟠 disque noir-orange : problème mécanique, rentrer · ⚫ noir : disqualification."
                    )
                ),
                RegulationSection(
                    title = "Safety car et VSC",
                    points = listOf(
                        "Safety Car (voiture de sécurité) : tout le monde roule lentement derrière elle, les écarts se resserrent.",
                        "VSC (Virtual Safety Car) : même principe mais sans voiture en piste, via un temps minimum à respecter.",
                        "La course reprend avec un départ relancé (ou non) une fois le danger écarté."
                    )
                )
            )
        ),
        RegulationCategory(
            title = "⚖️ Pénalités",
            sections = listOf(
                RegulationSection(
                    title = "Les sanctions en course",
                    points = listOf(
                        "Drive-through : passer par les stands sans s'arrêter (10 s de pénalité).",
                        "Stop-and-go : s'arrêter 10 s dans le stand.",
                        "Pénalité de temps ajoutée après la course (5 s, 10 s...), ou perte de places sur la grille suivante.",
                        "Exemples d'infractions : départ anticipé, excès de vitesse dans les stands, manœuvre dangereuse, non-respect des drapeaux."
                    )
                ),
                RegulationSection(
                    title = "Points de licence",
                    points = listOf(
                        "Chaque pilote cumule des points de pénalité sur 12 mois.",
                        "À 12 points : suspension d'un Grand Prix. Les points expirent un an après."
                    )
                )
            )
        ),
        RegulationCategory(
            title = "🔧 Le règlement technique 2026 (simplifié)",
            sections = listOf(
                RegulationSection(
                    title = "Moteur et énergie",
                    points = listOf(
                        "Moteur V6 turbo hybride 1.6 L, avec une part électrique renforcée en 2026 (presque 50 % de la puissance).",
                        "Carburant 100 % durable obligatoire.",
                        "Récupération d'énergie au freinage (MGU-K) et à l'échappement (MGU-H)."
                    )
                ),
                RegulationSection(
                    title = "Aérodynamique et châssis",
                    points = listOf(
                        "Aérodynamique active en 2026 : ailerons mobiles pour réduire la traînée en ligne droite (mode 'X') et augmenter l'appui en courbe (mode 'Z').",
                        "Poids minimum voiture + pilote : 770 kg en 2026 (à vérifier selon la version finale du règlement FIA).",
                        "Largeur max 190 cm, 4 roues, uniquement les roues arrière motrices, 2 roues avant directrices."
                    )
                )
            )
        ),
        RegulationCategory(
            title = "🅿️ Stands, parc fermé et DRS",
            sections = listOf(
                RegulationSection(
                    title = "Parc fermé",
                    points = listOf(
                        "Dès la fin des qualifications, les voitures sont en parc fermé : plus aucun réglage n'est autorisé (sauf exceptions comme les pneus pluie).",
                        "On peut travailler sur la voiture dans les stands pendant la course, mais pas avant le départ."
                    )
                ),
                RegulationSection(
                    title = "Le DRS (aileron arrière mobile)",
                    points = listOf(
                        "En course, un pilote à moins d'1 seconde de la voiture devant peut activer le DRS dans les zones prévues.",
                        "L'aileron s'ouvre pour réduire la traînée et faciliter le dépassement. Interdit sous drapeau jaune et dans le 1er tour."
                    )
                ),
                RegulationSection(
                    title = "Les stands (pit lane)",
                    points = listOf(
                        "Vitesse limitée dans la voie des stands (80 km/h sur la plupart des circuits).",
                        "Changer les 4 roues prend environ 2 secondes chez les meilleures écuries."
                    )
                )
            )
        ),
        RegulationCategory(
            title = "💰 Le plafond budgétaire (cost cap)",
            sections = listOf(
                RegulationSection(
                    title = "Pourquoi ?",
                    points = listOf(
                        "Pour niveler le niveau entre les grosses et les petites écuries, la FIA impose un plafond de dépenses par saison (hors salaires des pilotes et du top management).",
                        "En 2026, le plafond est d'environ 135 millions de dollars par écurie.",
                        "Une écurie qui dépasse le plafond risque une amende, une réduction de ses essais en soufflerie, voire une exclusion du championnat."
                    )
                )
            )
        )
    )
)
