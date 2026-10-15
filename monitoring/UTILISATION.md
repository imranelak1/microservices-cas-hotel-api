# Observation des quatre API

`docker compose -f compose-observability.yml up -d` lance les versions du sujet : Prometheus 2.42, Grafana 9.5.1, Jaeger 1.41 et Elastic Stack 8.7. Les ports sont 19690, 19630, 19686, 19692, 19644 et 19656. Grafana utilise `student / labpassword` ; les services sont réservés au développement local.

Prometheus collecte les métriques toutes les quinze secondes. Le dashboard versionné présente débit, p95, erreurs, CPU, mémoire et connexions SQL. Le générateur de charge conserve aussi les mesures CPU/RAM réelles par processus, pour comparer les trois JVM au processus Node.

Pour le traçage Java, télécharger l’agent OpenTelemetry officiel version 2.9.0, puis lancer chaque jar avec `-javaagent:opentelemetry-javaagent.jar`, `OTEL_SERVICE_NAME=hotel-rest` (ou soap/grpc) et `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:19618`. Le serveur Apollo utilise déjà le SDK Node lorsque cette dernière variable est définie. Les traces HTTP/gRPC et SQL apparaissent dans Jaeger.

Le profil Spring `observability` ajoute l’appender TCP Logstash ; `LOGSTASH_DEST=localhost:19644` adapte l’adresse. Apollo transmet aussi ses événements JSON quand `LOGSTASH_HOST` est défini. L’index Elasticsearch est `hotel-lab` ; Kibana permet d’en inspecter les événements et identifiants de traces.
