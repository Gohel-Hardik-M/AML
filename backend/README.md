# AML Backend

The backend requires runtime configuration through environment variables. Copy the variable names from `src/main/resources/application-example.properties` and provide real values through the deployment environment or a secrets manager.

Required values include `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `AML_ENCRYPTION_KEY`. Mail credentials are required when onboarding or creating officers. `TENANT_JDBC_URL_TEMPLATE` controls tenant database URLs and defaults to the local PostgreSQL pattern.

The application does not load a development credential file. Missing required values should fail startup rather than silently using shared credentials.
