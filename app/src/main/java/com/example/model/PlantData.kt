package com.example.model

/**
 * Données statiques mockées représentant le catalogue de plantes pour la démonstration.
 */
object PlantData {

    val mockPlants = listOf(
        Plant(
            id = "sauge",
            name = "Sauge Officinale",
            scientificName = "Salvia officinalis",
            category = "Plante Médicinale & Aromatique",
            shortDescription = "Considérée depuis l'Antiquité comme une panacée pour les maux de gorge et la digestion.",
            fullDescription = "La Sauge Officinale est un sous-arbrisseau vivace très réputé en phytothérapie. Ses feuilles aromatiques contiennent des huiles essentielles riches en thuyone et cinéole, idéales en infusion pour apaiser les muqueuses respiratoires et réguler la digestion.",
            ailmentsAndBenefits = listOf(
                "Mal de gorge & Gingivite",
                "Troubles digestifs & Ballonnements",
                "Transpiration excessive",
                "Fatigue temporaire"
            ),
            careInstructions = CareInstructions(
                watering = "Modéré, laisser sécher le terreau entre deux arrosages",
                sunlight = "Plein soleil, exposition chaude",
                difficulty = "Facile"
            ),
            colorHex = 0xFF3E8E5A,
            matchedKeywords = listOf(
                "gorge", "mal de gorge", "digestion", "digestif", "maux de ventre",
                "transpiration", "fatigue", "bouche", "angine", "voies respiratoires"
            ),
            contraindications = listOf(
                "Grossesse et allaitement (effet emménagogue et risque neurotoxique de la thuyone)",
                "Enfants de moins de 12 ans",
                "Antécédents de cancers hormonodépendants (effet œstrogénique)",
                "Troubles épileptiques"
            ),
            drugInteractions = listOf(
                "Traitements anti-épileptiques (diminution de l'efficacité anti-comitiale par la thuyone)",
                "Traitements anxiolytiques et sédatifs (potentialisation des effets)",
                "Traitements antidiabétiques (risque d'hypoglycémie additive)",
                "Traitements hormonaux oestrogéniques"
            )
        ),
        Plant(
            id = "camomille",
            name = "Camomille Matricaire",
            scientificName = "Matricaria chamomilla",
            category = "Plante Médicinale Apaisante",
            shortDescription = "Incontournable pour favoriser un sommeil réparateur et calmer l'anxiété légère.",
            fullDescription = "La Camomille sauvage ou Matricaire est célèbre pour ses fleurs odorantes aux propriétés adoucissantes, antispasmodiques et sédatives légères. Elle est particulièrement recommandée en tisane en fin de journée pour lutter contre l'insomnie et le stress.",
            ailmentsAndBenefits = listOf(
                "Insomnie & Troubles du sommeil",
                "Stress, Anxiété & Nervosité",
                "Crampes d'estomac & Spasmes",
                "Irritations cutanées légères"
            ),
            careInstructions = CareInstructions(
                watering = "Régulier mais sans excès",
                sunlight = "Soleil direct ou mi-ombre",
                difficulty = "Facile"
            ),
            colorHex = 0xFFF5B041,
            matchedKeywords = listOf(
                "insomnie", "sommeil", "stress", "anxiete", "anxiété", "nervosite",
                "dormir", "nuit", "spasmes", "estomac", "relaxation", "calme"
            ),
            contraindications = listOf(
                "Allergie connue aux plantes de la famille des Astéracées (composées)",
                "Grossesse : consommer avec modération en infusion uniquement"
            ),
            drugInteractions = listOf(
                "Anticoagulants oraux (risque d'augmentation légères des effets)",
                "Sédatifs et anxiolytiques de synthèse (majoration modérée de la somnolence)"
            )
        ),
        Plant(
            id = "aloe",
            name = "Aloe Vera",
            scientificName = "Aloe barbadensis Miller",
            category = "Plante Succulente Cicatrisante",
            shortDescription = "Gel naturel miraculeux pour apaiser les brûlures, coups de soleil et peaux sèches.",
            fullDescription = "L'Aloe Vera est une plante grasse succulente dont le gel contenu dans ses épaisses feuilles est gorgé de vitamines, minéraux et acides aminés. Utilisé en application cutanée, il hydrate intensément et accélère la cicatrisation cutanée.",
            ailmentsAndBenefits = listOf(
                "Brûlures superficielles & Coups de soleil",
                "Peau sèche, Eczéma & Démangeaisons",
                "Petites coupures & Cicatrisation",
                "Hydratation du visage"
            ),
            careInstructions = CareInstructions(
                watering = "Arrosage très espacé (1 fois toutes les 2-3 semaines)",
                sunlight = "Lumière vive sans soleil direct brûlant",
                difficulty = "Très facile"
            ),
            colorHex = 0xFF27AE60,
            matchedKeywords = listOf(
                "brulure", "brûlure", "coup de soleil", "peau", "eczema", "démangeaisons",
                "cicatrisation", "peau seche", "blessure", "hydratation", "visage"
            )
        ),
        Plant(
            id = "menthe",
            name = "Menthe Poivrée",
            scientificName = "Mentha x piperita",
            category = "Herbe Vivace Rafraîchissante",
            shortDescription = "Puissant tonique digestif et remède naturel contre les maux de tête.",
            fullDescription = "Issue d'un croisement naturel, la Menthe Poivrée possède un parfum mentholé intense. Son huile essentielle procure une sensation de fraîcheur immédiate qui soulage les migraines, les nausées ainsi que les lourdeurs d'estomac.",
            ailmentsAndBenefits = listOf(
                "Maux de tête & Migraines",
                "Nausées & Le mal des transports",
                "Lourdeurs digestives & Indigestion",
                "Nez bouché & Dégagement respiratoire"
            ),
            careInstructions = CareInstructions(
                watering = "Abondant, maintenir la terre humide",
                sunlight = "Mi-ombre à soleil doux",
                difficulty = "Facile"
            ),
            colorHex = 0xFF16A085,
            matchedKeywords = listOf(
                "migraine", "mal de tete", "maux de tete", "tete", "nausee", "nausées",
                "transport", "digestion", "lourdeur", "nez bouche", "rafraichissant"
            )
        ),
        Plant(
            id = "eucalyptus",
            name = "Eucalyptus Radié",
            scientificName = "Eucalyptus radiata",
            category = "Arbre Antiseptique Respiratoire",
            shortDescription = "Excellente plante pour décongestionner les voies respiratoires lors de rhumes.",
            fullDescription = "L'Eucalyptus Radié est particulièrement réputé pour sa tolérance cutanée et son action ciblée sur les voies respiratoires hautes. En inhalation ou infusion, ses feuilles riches en eucalyptol aident à dégager les bronches et combattre les frissons hivernaux.",
            ailmentsAndBenefits = listOf(
                "Rhume & Nez encombré",
                "Toux grasse & Bronchite",
                "États grippaux & Frissons",
                "Périodes épidémiques hivernales"
            ),
            careInstructions = CareInstructions(
                watering = "Régulier au début, résiste ensuite au sec",
                sunlight = "Soleil direct et endroit aéré",
                difficulty = "Modéré"
            ),
            colorHex = 0xFF2E86C1,
            matchedKeywords = listOf(
                "rhume", "nez", "toux", "grippe", "bronchite", "respiration",
                "hiver", "frissons", "froid", "voies respiratoires", "poumon"
            )
        ),
        Plant(
            id = "lavande",
            name = "Lavande Vraie",
            scientificName = "Lavandula angustifolia",
            category = "Plante Aromatique Relaxante",
            shortDescription = "Apaise les piqûres d'insectes, le stress et favorise la sérénité.",
            fullDescription = "Symbole de la Provence, la Lavande Vraie (ou Lavande Fine) est prisée pour ses sommités fleuries violettes aux vertus relaxantes, antiseptiques et cicatrisantes. Quelques gouttes d'extrait sur l'oreiller favorisent un apaisement mental immédiat.",
            ailmentsAndBenefits = listOf(
                "Piqûres d'insectes & Démangeaisons",
                "Anxiété, Nervosité & Palpitations",
                "Troubles de l'endormissement",
                "Tensions musculaires & Crampes"
            ),
            careInstructions = CareInstructions(
                watering = "Faible, supporte très bien la sécheresse",
                sunlight = "Plein soleil impératif",
                difficulty = "Très facile"
            ),
            colorHex = 0xFF8E44AD,
            matchedKeywords = listOf(
                "piqûre", "piqûre d'insecte", "moustique", "stress", "anxiété",
                "sommeil", "relaxation", "lavande", "crampe", "tension", "calme"
            )
        ),
        Plant(
            id = "romarin",
            name = "Romarin Officinal",
            scientificName = "Salvia rosmarinus",
            category = "Tonique Hépatique & Mémoire",
            shortDescription = "Stimule la concentration, protège le foie et combat la fatigue intellectuelle.",
            fullDescription = "Arbrisseau méditerranéen aux aiguilles aromatiques, le Romarin est reconnu pour soutenir la fonction hépato-biliaire et améliorer les capacités cognitives et la mémoire. Il est idéal lors des révisions ou des coups de fatigue matineux.",
            ailmentsAndBenefits = listOf(
                "Fatigue physique & Mémoire",
                "Digestion lente & Foie paresseux",
                "Crampes musculaires",
                "Circulation sanguine"
            ),
            careInstructions = CareInstructions(
                watering = "Très faible, arrosage espacé",
                sunlight = "Plein soleil chaud",
                difficulty = "Très facile"
            ),
            colorHex = 0xFF1B4D3E,
            matchedKeywords = listOf(
                "memoire", "mémoire", "concentration", "foie", "fatigue",
                "moteur", "tonique", "digestion", "romarin", "circulation"
            )
        ),
        Plant(
            id = "thym",
            name = "Thym Commun",
            scientificName = "Thymus vulgaris",
            category = "Infectiologie & Voies Respiratoires",
            shortDescription = "Puissant antiseptique naturel pour soulager la toux et renforcer l'immunité.",
            fullDescription = "Riche en thymol, le Thym est un puissant anti-infectieux et stimulant immunitaire. En infusion avec du miel et du citron, il purifie l'arbre respiratoire et apaise les toux sèches ou quinteuses.",
            ailmentsAndBenefits = listOf(
                "Toux sèche & Maux de gorge",
                "Infections ORL & Encombrement",
                "Stimulation immunitaire",
                "Ballonnements & Inconfort"
            ),
            careInstructions = CareInstructions(
                watering = "Très modéré, terreau bien drainé",
                sunlight = "Soleil direct",
                difficulty = "Facile"
            ),
            colorHex = 0xFF388E3C,
            matchedKeywords = listOf(
                "thym", "toux", "gorge", "infection", "orl",
                "immunite", "immunité", "rhume", "miel", "poumon"
            )
        ),
        Plant(
            id = "melisse",
            name = "Mélisse Officinale",
            scientificName = "Melissa officinalis",
            category = "Sédative & Antispasmodique",
            shortDescription = "Parfum citronné apaisant la nervosité cardiaque et les spasmes digestifs.",
            fullDescription = "Au doux parfum de citronnelle, la Mélisse est la plante de la nervosité viscérale. Elle calme les douleurs d'estomac d'origine nerveuse ainsi que les palpitations liées au stress émotif.",
            ailmentsAndBenefits = listOf(
                "Nervosité & Palpitations",
                "Spasmes digestifs & Sommeil",
                "Migraines d'origine nerveuse",
                "Herpès labial"
            ),
            careInstructions = CareInstructions(
                watering = "Régulier, sol frais",
                sunlight = "Mi-ombre préférée",
                difficulty = "Facile"
            ),
            colorHex = 0xFF7CB342,
            matchedKeywords = listOf(
                "melisse", "mélisse", "stress", "coeur", "palpitations",
                "estomac", "spasmes", "citron", "calme", "sommeil"
            )
        ),
        Plant(
            id = "origan",
            name = "Origan Vert",
            scientificName = "Origanum vulgare",
            category = "Aromatique & Anti-infectieux",
            shortDescription = "Tonique respiratoire et puissant bouclier contre les refroidissements.",
            fullDescription = "L'Origan est réputé pour sa richesse en carvacrol, lui conférant des propriétés antibactériennes majeures. Il réchauffe l'organisme et dégage rapidement les bronches prises par le froid.",
            ailmentsAndBenefits = listOf(
                "Coup de froid & Refroidissement",
                "Douleurs articulaires",
                "Digestion difficile",
                "Fatigue hivernale"
            ),
            careInstructions = CareInstructions(
                watering = "Modéré, résiste au sec",
                sunlight = "Soleil généreux",
                difficulty = "Facile"
            ),
            colorHex = 0xFF2E7D32,
            matchedKeywords = listOf(
                "origan", "froid", "rhume", "bronches", "antibacterien",
                "articulations", "hiver", "infection"
            )
        ),
        Plant(
            id = "millepertuis",
            name = "Millepertuis",
            scientificName = "Hypericum perforatum",
            category = "Humeur & Équilibre Émotionnel",
            shortDescription = "Le « rayon de soleil » végétal pour surmonter le blues saisonnier et les baisses de moral.",
            fullDescription = "Également appelé Herbe de la Saint-Jean, le Millepertuis est réputé pour rééquilibrer les baisses de moral légères et l'anxiété passagère. Ses sommités fleuries jaunes contiennent de l'hypericine.",
            ailmentsAndBenefits = listOf(
                "Baisse de moral & Blues saisonnier",
                "Anxiété & Tensions nerveuses",
                "Brûlures superficielles (Macerat huileux)",
                "Troubles légers de l'humeur"
            ),
            careInstructions = CareInstructions(
                watering = "Modéré",
                sunlight = "Soleil direct",
                difficulty = "Modéré"
            ),
            colorHex = 0xFFFBC02D,
            matchedKeywords = listOf(
                "moral", "depression", "dépression", "blues", "tristesse",
                "millepertuis", "soleil", "humeur", "anxiete", "anxiété"
            ),
            contraindications = listOf(
                "Grossesse et allaitement",
                "Troubles bipolaires et schizophrénie (risque de déclenchement d'épisodes maniaques)",
                "Exposition solaire ou UV intense (risque élevé de photosensibilisation cutanée)",
                "Enfants et adolescents de moins de 18 ans"
            ),
            drugInteractions = listOf(
                "Contraceptifs oraux / Pilule (risque majeur de grossesse non désirée par induction enzymatique CYP3A4)",
                "Anticoagulants oraux (AVK, Warfarine) : baisse d'efficacité et risque thrombotique grave",
                "Antidépresseurs ISRS / IRSNA : risque de syndrome sérotoninergique toxique",
                "Immunosuppresseurs (Ciclosporine, Tacrolimus) : risque majeur de rejet de greffe",
                "Antirétroviraux (VIH, Anti-protéases) : perte d'efficacité du traitement",
                "Antinéoplasiques et chimiothérapies : diminution de l'activité anticancéreuse",
                "Traitements cardiovasculaires (Digoxine, Antiarythmiques) : baisse d'efficacité"
            )
        ),
        Plant(
            id = "valeriane",
            name = "Valériane Officinale",
            scientificName = "Valeriana officinalis",
            category = "Sédative & Sommeil Profond",
            shortDescription = "L'un des plus puissants décontractants musculaires et somnifères naturels.",
            fullDescription = "La racine de Valériane favorise l'endormissement sans créer d'accoutumance ni de somnolence résiduelle le lendemain. Elle agit en relâchant les tensions musculaires liées au surmenage.",
            ailmentsAndBenefits = listOf(
                "Insomnie sévère & Réveils nocturnes",
                "Tensions musculaires & Tics nerveux",
                "Surmenage intellectuel",
                "Angoisses nocturnes"
            ),
            careInstructions = CareInstructions(
                watering = "Généreux, sol très humide",
                sunlight = "Soleil à mi-ombre",
                difficulty = "Facile"
            ),
            colorHex = 0xFF6A1B9A,
            matchedKeywords = listOf(
                "valeriane", "valériane", "sommeil", "insomnie", "nuit",
                "muscle", "tension", "somnifere", "dormir", "angoisse"
            )
        ),
        Plant(
            id = "calendula",
            name = "Calendula (Souci des Jardins)",
            scientificName = "Calendula officinalis",
            category = "Dermatologie & Cicatrisation",
            shortDescription = "Fleur adoucissante d'exception pour apaiser la peau des enfants et des adultes.",
            fullDescription = "Le Souci des Jardins est la plante reine des peaux sensibles et réactives. Ses macérats huileux calment rougeurs, rougeurs de couche, écorchures et petites gerçures du quotidien.",
            ailmentsAndBenefits = listOf(
                "Rougeurs & Peaux réactives",
                "Gerçures, Crevasses & Écorchures",
                "Coups de soleil légers",
                "Soins apaisants bébé & enfant"
            ),
            careInstructions = CareInstructions(
                watering = "Régulier",
                sunlight = "Plein soleil",
                difficulty = "Très facile"
            ),
            colorHex = 0xFFFB8C00,
            matchedKeywords = listOf(
                "calendula", "souci", "peau", "rougeur", "bebe", "bébé",
                "gercure", "gerçure", "cicatrisation", "irritation"
            )
        ),
        Plant(
            id = "gingembre",
            name = "Gingembre Officinal",
            scientificName = "Zingiber officinale",
            category = "Tonique Digestion & Vitalité",
            shortDescription = "Rhizome chauffant anti-nauséeux d'exception et stimulant immunitaire.",
            fullDescription = "Utilisé en médecine ayurvédique et traditionnelle chinoise, le rhizome de Gingembre active le feu digestif, élimine la cinétose (mal des transports) et soulage la nausée des femmes enceintes.",
            ailmentsAndBenefits = listOf(
                "Nausées de grossesse & Mal des transports",
                "Inconfort digestif & Ballonnements",
                "Coups de fatigue & Frissons",
                "Vitalité & Énergie"
            ),
            careInstructions = CareInstructions(
                watering = "Maintenir le terreau humide et chaud",
                sunlight = "Lumière indirecte chaude",
                difficulty = "Modéré"
            ),
            colorHex = 0xFFD81B60,
            matchedKeywords = listOf(
                "gingembre", "nausee", "nausée", "grossesse", "transport",
                "digestion", "energie", "énergie", "tonique", "froid"
            )
        )
    )

    /**
     * Mots-clés suggérés pour la recherche rapide.
     */
    val popularSymptoms = listOf(
        "Mal de gorge",
        "Insomnie",
        "Stress",
        "Brûlure",
        "Migraine",
        "Rhume",
        "Digestion",
        "Foie",
        "Mémoire"
    )

    fun getPlantById(id: String): Plant? {
        return mockPlants.find { it.id.equals(id, ignoreCase = true) }
    }
}

