# ms-andesstay-audit

Microservicio de auditoria del caso AndesStay (DSY1107 - Desarrollo Cloud Native I).

Consume el topico Kafka `reservations.events` (publicado por `ms-andesstay-reservations`)
y guarda cada evento para trazabilidad: quien hizo que, cuando, y desde que reserva.
Expone un endpoint de solo lectura para consultarlos con filtros.

## Stack
- Java 17
- Spring Boot 3.3.4 (Web, Data JPA, Validation, Security, OAuth2 Resource Server, Kafka)
- PostgreSQL
- Apache Kafka (consumer)

## Como correrlo local

1. Levantar Kafka + Zookeeper (ver `infra/kafka/compose.yml` del proyecto):
   ```
   docker compose up -d
   ```

2. Crear la base de datos (en pgAdmin o con psql):
   ```
   psql -U postgres -c "CREATE DATABASE andesstay_audit;"
   ```

3. Exportar la variable de la contrasena de Postgres:
   - `DB_PASSWORD`

4a. **Sin Cognito todavia (perfil `nosec`, solo para desarrollo/pruebas):**
   ```
   mvn spring-boot:run -Dspring-boot.run.profiles=nosec
   ```
   Esto deja todos los endpoints abiertos, sin pedir token. Sirve para probar
   que el consumer de Kafka y el guardado en Postgres funcionan, sin depender
   de que el User Pool de Cognito ya exista.

4b. **Con seguridad real (una vez creado el User Pool de Cognito):**
   Exportar ademas `COGNITO_REGION`, `COGNITO_USER_POOL_ID`, `COGNITO_APP_CLIENT_ID`,
   y ejecutar sin el flag de perfil:
   ```
   mvn spring-boot:run
   ```

El servicio queda arriba en `http://localhost:8083`.

## Endpoint

| Metodo | Ruta | Descripcion | Roles |
|---|---|---|---|
| GET | `/api/audit` | Lista eventos de auditoria, paginado | ADMIN, AUDITOR |

Query params (todos opcionales): `usuario`, `tipoEvento`, `desde`, `hasta` (ISO-8601),
`page`, `size`.

Ejemplo:
```
GET /api/audit?tipoEvento=RESERVA_CONFIRMADA&desde=2026-09-01T00:00:00Z
Authorization: Bearer <access_token>
```

## Notas

- El consumer es idempotente: si Kafka reentrega un mensaje ya procesado (mismo
  `eventId`), se ignora en vez de duplicarlo.
- El formato exacto del mensaje que publica `ms-andesstay-reservations` puede cambiar
  mientras ese microservicio se sigue construyendo; el consumer tolera campos
  desconocidos y guarda el JSON crudo completo por si hace falta revisarlo.
- Pendiente: reemplazar los valores `CHANGE_ME` una vez que el User Pool de Cognito
  este creado.

## Equipo
Jean Flores, Christian Fuentes, Emmanuel Valenzuela.
