La collection Postman commence par la connexion, mémorise le JWT et l'identifiant créé, puis permet de lire, modifier et supprimer la réservation. Les requêtes GraphQL utilisent le même jeton.

Dans SoapUI 5.7.0, importer `../soapui/hotel-soapui-project.xml`. Obtenir un jeton sur POST `http://localhost:19602/auth/token`, puis renseigner la propriété de projet `jwt`. Exécuter `createReservation`, copier l'identifiant renvoyé dans `reservationId`, puis lancer les requêtes de consultation, modification et suppression. Adapter les dates pour un nouveau séjour.

Pour BloomRPC, importer `../grpc-api/src/main/proto/hotel.proto`, choisir `localhost:19605` et ajouter la métadonnée `authorization: Bearer <jwt>`. L'introspection GraphQL est accessible sur `/graphql` avec ce même en-tête.
