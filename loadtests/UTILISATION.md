# Refaire les mesures

Le workflow manuel « Comparaison isolée des quatre API » démarre quatre runners Ubuntu 24.04 indépendants, un par protocole. `smoke=true` vérifie huit appels CRUD réels ; `false` exécute 36 phases : 1/10/100 Kio × 10/100/500/1 000 utilisateurs × trois répétitions. Les résultats courts restent séparés des mesures retenues.

Chaque runner héberge un serveur, PostgreSQL 16.15 et le même client Java 21 à threads virtuels. Les limites sont 4 CPU et 8 Gio au total : serveur 1 Gio, base 1 Gio, générateur 6 Gio. Les trois serveurs Java restent sous Java 17 avec `-Xmx160m` ; Apollo utilise Node 22 et `--max-old-space-size=160`. Le générateur dispose de `-Xmx1g` pour conserver les messages des 1 000 appels sans limiter les serveurs mesurés. Le traçage et l'envoi des logs sont désactivés pendant la comparaison ; les preuves d'observabilité proviennent des tests fonctionnels locaux.

Le client démarre chaque lot avec une barrière commune. Chaque utilisateur crée, lit, modifie puis supprime sa réservation, dans une chambre distincte. Une phase complète répète ces lots pendant au moins dix secondes, puis attend la fin du dernier lot. Les dates changent entre lots. Le smoke conserve exactement huit appels, sans durée minimale. Si la création échoue, les opérations suivantes ne sont pas comptées comme des appels tentés. La taille concerne le JSON métier canonique ; les colonnes d'octets mesurent les corps réellement sérialisés, sans les en-têtes ni les trames réseau. SOAP ajoute son XML et gRPC utilise Protobuf. Chaque API renvoie les mêmes champs métier.

Les CSV bruts compressés, les fenêtres exactes des phases, les percentiles, erreurs et compteurs CPU/RSS sont conservés dans les artefacts GitHub pendant un jour. Les sorties temporaires vont dans `run-output/`, ignoré par Git. La CPU vient de `/proc/1/stat` : 100 % représente un cœur, donc le maximum théorique est 400 %. La mémoire est le vrai `VmRSS` du serveur. Chaque phase complète exige au moins trois observations réelles ; une couverture absente fait échouer l'analyse, sans interpolation. Les tables finales sont archivées dans `results/` avec les identifiants des runs et leur commit.

## Outils complémentaires

Ils vérifient les scénarios avec les versions indiquées dans le sujet. Leurs résultats courts figurent dans `evidence/tool-smokes.json` et ne sont pas mélangés avec les quatre campagnes comparatives.

```sh
# Apache JMeter 5.5, depuis la racine du dépôt ; adapter sizeKiB à 1, 10 ou 100.
jmeter -n -t loadtests/hotel-rest.jmx -l hotel.jtl -JsizeKiB=1

# k6 0.42.0 : PROTOCOL vaut rest, soap, graphql ou grpc.
cd loadtests
PROTOCOL=grpc SIZE_KIB=10 k6 run k6.js

# Locust 2.15.1 : parcours REST avec pauses utilisateur.
locust -f locustfile.py --host=http://localhost:19601 --headless -u 100 -r 10 -t 30s

# Gatling 3.9.0 : arrivées progressives, distinctes du modèle à barrière.
cd gatling
mvn gatling:test -Dgatling.noReports=true -Dsize=1
```

Pour une simple vérification : JMeter accepte `-Jusers1=2 -Jusers2=2 -Jusers3=2 -Jusers4=2 -Jramp=1`, k6 `SMOKE=1`, Locust `-u 2 -r 2 -t 5s` et Gatling `-Dsmoke=true`. Le code du client commun se trouve dans `grpc-api/src/main/java/ma/imrane/hotel/benchmark/BenchmarkClient.java`.
