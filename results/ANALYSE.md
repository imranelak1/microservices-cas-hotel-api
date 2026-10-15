# Résultats mesurés

Campagne complète du 15 octobre 2026 : [run 38671019384](https://github.com/imranelak1/microservices-cas-hotel-api/actions/runs/38671019384), commit `1091c775b6f5cc50e3e77442d3c21b9e2999a7cd`. Les 144 phases sont terminées, avec trois répétitions par combinaison et au moins trois intervalles CPU entièrement couverts par phase. Les CSV bruts compressés sont dans `raw/` ; chaque environnement et fenêtre de phase se trouve dans `runs/`. Les essais locaux et les smokes sont exclus de ces chiffres.

## Latence moyenne des appels réussis

En millisecondes, à **100 utilisateurs**, trois répétitions regroupées. Les erreurs restent comptées séparément ; `comparison.csv` contient aussi les latences de toutes les tentatives et les p50/p95/p99 calculés sur les observations regroupées.

| Kio | Opération | REST | SOAP | GraphQL | gRPC |
| --- | --- | --- | --- | --- | --- |
| 1 | Créer | 35.93 | 41.01 | 95.91 | 27.85 |
| 1 | Consulter | 31.73 | 34.65 | 87.77 | 23.54 |
| 1 | Modifier | 30.70 | 31.90 | 106.53 | 25.28 |
| 1 | Supprimer | 16.28 | 17.65 | 66.23 | 14.84 |
| 10 | Créer | 40.57 | 47.38 | 111.70 | 34.40 |
| 10 | Consulter | 34.16 | 41.11 | 110.95 | 30.62 |
| 10 | Modifier | 34.19 | 38.41 | 133.15 | 32.13 |
| 10 | Supprimer | 17.22 | 18.80 | 81.90 | 17.69 |
| 100 | Créer | 139.04 | 147.65 | 196.02 | 93.86 |
| 100 | Consulter | 115.29 | 128.54 | 204.30 | 94.13 |
| 100 | Modifier | 113.42 | 119.94 | 240.93 | 103.86 |
| 100 | Supprimer | 43.63 | 44.86 | 125.90 | 48.67 |

## Débit utile

Appels CRUD réussis par seconde, message métier de **1 Kio**. Le dénominateur est la somme des durées réelles des trois phases. Les appels échoués ne gonflent pas ce débit.

| Utilisateurs | REST | SOAP | GraphQL | gRPC |
| --- | --- | --- | --- | --- |
| 10 | 1851.62 | 1487.38 | 938.46 | 2620.13 |
| 100 | 2947.58 | 2732.18 | 1053.92 | 3887.01 |
| 500 | 2964.40 | 2732.27 | 1021.02 | 3662.31 |
| 1000 | 3257.12 | 2938.30 | 991.90 | 3772.62 |

## CPU et mémoire du serveur

Pour **1 Kio**, moyenne des trois phases. La CPU est mesurée par différences de compteurs `/proc/1/stat` : 100 % correspond à un cœur, donc 400 % aux quatre vCPU. Les intervalles qui chevauchent une limite de phase sont exclus. La mémoire est le `VmRSS` instantané en Mio ; les relevés ne sont pas interpolés. Les maxima figurent dans `comparison.csv`.

| Utilisateurs | CPU REST (%) | SOAP (%) | GraphQL (%) | gRPC (%) |
| --- | --- | --- | --- | --- |
| 10 | 168.45 | 172.43 | 94.69 | 143.34 |
| 100 | 129.16 | 131.94 | 107.24 | 137.62 |
| 500 | 118.08 | 121.41 | 110.67 | 131.79 |
| 1000 | 122.25 | 126.06 | 113.29 | 143.92 |

| Utilisateurs | RSS REST (Mio) | SOAP | GraphQL | gRPC |
| --- | --- | --- | --- | --- |
| 10 | 223.54 | 270.35 | 95.39 | 252.77 |
| 100 | 274.84 | 322.68 | 98.76 | 288.88 |
| 500 | 329.26 | 394.90 | 110.58 | 358.60 |
| 1000 | 354.36 | 414.97 | 124.22 | 459.86 |

## Limite observée à 100 Kio et 1 000 utilisateurs

| API | Tentatives | Échecs | Échecs (%) | Appels réussis/s |
| --- | --- | --- | --- | --- |
| REST | 23958 | 14 | 0.06 | 637.54 |
| SOAP | 23718 | 94 | 0.40 | 688.85 |
| GraphQL | 24000 | 0 | 0.00 | 439.41 |
| gRPC | 9665 | 4510 | 46.66 | 104.90 |

Le serveur gRPC atteint sa limite de mémoire directe. Son journal contient `OutOfMemoryError: Cannot reserve ... direct buffer memory`, avec une limite de 162 201 600 octets. Les trois passages concernés échouent respectivement à 31,42 %, 77,05 % et 91,86 %. Les autres paliers gRPC ne présentent pas d’erreur. Ce résultat décrit cette configuration à heap 160 Mio et son plafond natif ; il ne permet pas de conclure que gRPC serait intrinsèquement moins scalable. Le dimensionnement des buffers directs et du nombre de requêtes en vol doit être revu avant une charge de ce niveau.

Les rares échecs REST et SOAP apparaissent surtout lors du premier passage aux fortes concurrences. Les journaux conservés ne prouvent pas leur cause : elle reste indéterminée. Apollo termine ici sans échec. Toutes les tentatives restent présentes dans les fichiers bruts, y compris les requêtes dont la création a échoué et dont les étapes suivantes n’ont donc pas été tentées.

## Conditions et portée

Chaque protocole utilise un runner distinct avec quatre vCPU, les mêmes données initiales et 8 Gio de limites cumulées : serveur 1 Gio, PostgreSQL 1 Gio et générateur 6 Gio. Les trois JVM serveur utilisent 160 Mio de heap ; Node est également limité à 160 Mio de heap, le client Java 21 à 1 Gio. Le serveur, le client et PostgreSQL partagent les quatre vCPU. Ce montage n’est pas un serveur applicatif dédié de 8 Go avec générateur séparé. Les machines déclarent `AMD EPYC 7763 64-Core Processor`; les versions et identifiants d’images réellement exécutés sont archivés, ainsi que la taille réelle du disque du runner.

Les répétitions sont successives sur la même instance, sans redéploiement entre elles. Les effets du JIT, des caches et de l’épuisement des ressources restent donc visibles. Les appels de traçage et d’export de logs sont désactivés pour ces campagnes ; les captures Grafana, Jaeger et Kibana proviennent des vérifications fonctionnelles locales. Dans la capture Grafana, une CPU Java à `-1` est la sentinelle Micrometer d’une mesure locale indisponible sous Windows ; elle n’entre dans aucun calcul Linux ci-dessus. La taille 1/10/100 Kio décrit le JSON métier canonique. Les octets enregistrés correspondent aux corps réellement sérialisés, sans en-têtes ni trames réseau.

REST garde un client HTTP simple et s’intègre directement à React. SOAP fournit un contrat WSDL et des messages XML plus volumineux. GraphQL permet de choisir les champs ; le test demande volontairement les mêmes données complètes que les autres API. gRPC demande les stubs Protobuf et HTTP/2, avec des corps compacts et une gestion explicite de ses ressources natives. Le choix dépend donc aussi des clients, du contrat et de l’exploitation, pas seulement du débit de ce test.

## Implémentation et sécurité

Le temps de réalisation et les jours d’apprentissage n’ont pas été chronométrés. `complexite.csv` compte les lignes physiques non vides des sources serveur, avec code commun, contrats, générateur et code produit par Protobuf séparés. Ce comptage ne mesure pas à lui seul la simplicité. Les quatre API vérifient le même JWT ; TLS est validé localement, y compris gRPC HTTP/2. Les contraintes SQL ont aussi résisté à 20 réservations concurrentes sur une chambre : une validation et 19 rollbacks. Ces preuves figurent dans `evidence/` à la racine du dépôt.
