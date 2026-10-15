# Déploiement local

Les quatre API partagent PostgreSQL dans le namespace `hotel-lab`. Les images Java utilisent les JAR compilés par `mvn clean verify`. Construire les images comme dans le workflow CI, puis les charger dans le cluster Kind choisi :

```sh
kind load docker-image course7-hotel-rest:1.0.0 course7-hotel-soap:1.0.0 course7-hotel-grpc:1.0.0 course7-hotel-graphql:1.0.0 course7-hotel-frontend:1.0.0 postgres:16.15 --name course7
kubectl apply -k .
kubectl -n hotel-lab rollout status deployment/rest-api --timeout=300s
kubectl -n hotel-lab port-forward svc/frontend 13092:80
```

Ouvrir `http://localhost:13092` (`student / labpass`). Le client est compilé avec `VITE_API_URL=/backend` ; Nginx transmet les appels au service REST interne. Pour vérifier la réplication : `kubectl -n hotel-lab scale deployment/rest-api --replicas=2`, puis `kubectl -n hotel-lab rollout status deployment/rest-api`.

Les services `soap-api`, `graphql-api` et `grpc-api` exposent respectivement les ports 19602, 19603 et 19605 dans le cluster. Le volume PostgreSQL conserve les réservations lors du remplacement d'un pod. Les identifiants fournis servent à ce laboratoire local.
