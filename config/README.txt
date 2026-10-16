Place firebase-service-account.json here (Firebase console -> Project settings -> Service accounts ->
Generate new private key). docker-compose mounts this folder read-only at /app/config and the app reads
FIREBASE_CREDENTIALS_PATH=/app/config/firebase-service-account.json when FIREBASE_ENABLED=true.
