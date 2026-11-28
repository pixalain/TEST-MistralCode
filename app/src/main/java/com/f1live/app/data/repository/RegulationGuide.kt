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
            title = "🔧 Moteurs et groupes motopropulseurs (2026)",
            sections = listOf(
                RegulationSection(
                    title = "Architecture du V6 hybride",
                    points = listOf(
                        "Moteur thermique : V6 turbo de 1,6 litre (ICE), injection directe, régime limité à 15 000 tr/min.",
                        "En 2026, la partie électrique est fortement renforcée : le MGU-K (récupération au freinage) passe à environ 350 kW, soit près de la moitié de la puissance totale.",
                        "Le MGU-H (récupération à l'échappement) est supprimé en 2026 pour simplifier le groupe motopropulseur.",
                        "Puissance totale d'une F1 2026 : environ 1 000 chevaux (thermique + électrique)."
                    )
                ),
                RegulationSection(
                    title = "Énergie, batterie et déploiement",
                    points = listOf(
                        "L'énergie électrique est stockée dans une batterie (ERS) puis redéployée par le pilote via des modes moteur.",
                        "Le pilote gère lui-même le déploiement de l'énergie électrique (boutons au volant) : attaque, défense, récolte ('harvest').",
                        "La consommation est limitée à environ 100 kg de carburant par course (réservoir ~110 L), d'où l'importance de la récupération d'énergie."
                    )
                ),
                RegulationSection(
                    title = "Carburant et durabilité",
                    points = listOf(
                        "Carburant 100 % durable obligatoire depuis 2026 (biocarburant avancé ou e-fuel synthétique).",
                        "L'huile moteur doit aussi être durable."
                    )
                ),
                RegulationSection(
                    title = "Limitation des moteurs",
                    points = listOf(
                        "Chaque pilote dispose d'un nombre limité de groupes motopropulseurs par saison (moteur thermique, turbo, MGU-K, MGU-H, batterie, électronique).",
                        "Dépasser ce quota entraîne des pénalités de places sur la grille (ex. -5, -10 places selon le composant)."
                    )
                )
            )
        ),
        RegulationCategory(
            title = "🚗 Châssis et carrosserie (2026)",
            sections = listOf(
                RegulationSection(
                    title = "Dimensions et poids",
                    points = listOf(
                        "Largeur maximale : 190 cm (2 m avec les pneus), hauteur ~95 cm, empattement (distance entre essieux) limité à 340 cm.",
                        "Poids minimal voiture + pilote + carburant : environ 770 kg en début de course (chiffre indicatif du règlement 2026).",
                        "4 roues obligatoires, seules les roues arrière sont motrices, seules les 2 roues avant sont directrices.",
                    )
                ),
                RegulationSection(
                    title = "Sécurité : cellule de survie, halo, crash tests",
                    points = listOf(
                        "Le châssis intègre une cellule de survie (monocoque en carbone) autour du cockpit, avec le réservoir derrière le siège.",
                        "Le halo (arceau en titane au-dessus du casque) protège le pilote des débris et des chocs frontaux.",
                        "Des crash tests obligatoires : choc frontal à 30 mph, choc arrière, choc latéral, écrasement de l'arceau, colonne de direction."
                    )
                ),
                RegulationSection(
                    title = "Aérodynamique active 2026 (remplace le DRS)",
                    points = listOf(
                        "En 2026, le DRS classique disparaît : il est remplacé par l'aérodynamique active.",
                        "Mode 'ligne droite' (X) : aileron avant et arrière s'ouvrent pour réduire la traînée et gagner de la vitesse de pointe, utilisable quand la piste est dégagée.",
                        "Mode 'virage' (Z) : ailerons fermés pour maximiser l'appui dans les courbes.",
                        "Le pilote (ou le système) bascule entre les deux modes selon les zones définies par le circuit."
                    )
                ),
                RegulationSection(
                    title = "Suspension, freins et boîte",
                    points = listOf(
                        "Suspension interdite d'être active : chaque roue est indépendante, réglée passivement.",
                        "Freins à disques carbone, 4 roues freinées, répartiteur de freinage réglable par le pilote.",
                        "Boîte de vitesses séquentielle à 8 rapports, commande au volant (palettes)."
                    )
                )
            )
        ),
        RegulationCategory(
            title = "🅿️ Stands, parc fermé et dépassements",
            sections = listOf(
                RegulationSection(
                    title = "Parc fermé",
                    points = listOf(
                        "Dès la fin des qualifications, les voitures sont en parc fermé : plus aucun réglage n'est autorisé (sauf exceptions comme les pneus pluie).",
                        "On peut travailler sur la voiture dans les stands pendant la course, mais pas avant le départ."
                    )
                ),
                RegulationSection(
                    title = "Dépasser en 2026 : aéro active au lieu du DRS",
                    points = listOf(
                        "Le DRS n'existe plus en 2026. Pour dépasser, les pilotes comptent sur l'aérodynamique active (mode ligne droite) et la meilleure traction en sortie de courbe.",
                        "L'aspiration (rester dans le sillage d'une voiture) reste le meilleur allié pour préparer un dépassement."
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
            title = "🧠 Stratégie de course",
            sections = listOf(
                RegulationSection(
                    title = "Undercut et overcut",
                    points = listOf(
                        "Undercut : s'arrêter avant son rival pour ressortir devant avec des pneus neufs et plus rapides.",
                        "Overcut : rester en piste plus longtemps pour profiter d'une piste dégagée et signer de meilleurs chronos avant de rentrer.",
                        "Le choix du moment du pit-stop (la 'fenêtre') est décisif pour gagner des places."
                    )
                ),
                RegulationSection(
                    title = "Le départ",
                    points = listOf(
                        "Départ arrêté : les feux s'éteignent aléatoirement (entre 0 et 5 s après le dernier feu rouge) pour éviter les départs anticipés.",
                        "Un faux départ est pénalisé (drive-through ou stop-and-go de 10 s).",
                        "La grille est formée de 2 voitures par rangée, espacées d'environ 8 m ; le poleman part à droite ou à gauche selon le côté le plus rapide."
                    )
                ),
                RegulationSection(
                    title = "Gestion de course",
                    points = listOf(
                        "Les pilotes doivent gérer leurs pneus, leur carburant et leur batterie sur toute la distance.",
                        "Un drapeau rouge arrête la course ; elle reprend par un départ relancé ou derrière la safety car."
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
