# Étude de cas — Réservations d’hôtel

Le même métier est exposé par quatre API : REST et SOAP avec Spring Boot, GraphQL avec Apollo Server et gRPC Java. Tous les adaptateurs utilisent PostgreSQL et le fichier commun `database/queries.json`. Les contraintes de base empêchent deux réservations de se chevaucher dans la même chambre.

```bash
docker compose up -d
mvn clean verify
cd graphql-api && npm ci && npm test && npm start
```

Lancer ensuite les jars `rest-api`, `soap-api` et `grpc-api` dans des terminaux séparés. Les ports sont 19601, 19602, 19603 et 19605. Le client React se trouve dans `frontend/`. Le compte local `student / labpass` permet d’obtenir un JWT sur POST `/auth/token` du service REST.

Les contrats WSDL, SDL et Protobuf sont versionnés. Les plans de charge couvrent le CRUD, les messages de 1/10/100 Ko et la montée de 10 à 1 000 appels concurrents. Les résultats mesurés et l’analyse sont dans `results/`. Les différences avec l’environnement matériel théorique du sujet sont indiquées, sans extrapolation à une production.

![Application](screenshots/results.png)
![Client React](screenshots/frontend.png)
![Vérifications](screenshots/tests.png)
