# ms-andesstay-audit

Microservicio de auditoría del caso **AndesStay** (DSY1107 - Desarrollo Cloud Native I).

Consume el tópico Kafka `reservations.events` (publicado por `ms-andesstay-reservations`),
guarda cada evento en PostgreSQL para trazabilidad (quién hizo qué y cuándo) y expone un
endpoint **de solo lectura** para consultarlos con filtros. Solo pueden verlo los roles
**ADMIN** y **AUDITOR**.

```
ms-andesstay-reservations --publica--> Kafka "reservations.events" --consume--> ms-andesstay-audit --> PostgreSQL
                                                                                       |
                                                    mensajes con falla (tras 3 reintentos)
                                                                                       v
                                                              Kafka "reservations.events.audit.DLT"

Angular --Bearer JWT--> API Gateway --> ms-andesstay-bff --> GET /api/audit
```

## Stack
- Java 17, Spring Boot 3.3.4 (Web, Data JPA, Validation, Security, OAuth2 Resource Server, Kafka, Actuator)
- PostgreSQL
- Apache Kafka (consumer, y producer solo para publicar en el DLT)
- Micrometer + Prometheus (métricas en `/actuator/prometheus`)

## Variables de entorno
Copiar `.env.example` a `.env` (está en `.gitignore`, **nunca se sube al repositorio**).

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `DB_URL` | URL JDBC de PostgreSQL (RDS en AWS) | `jdbc:postgresql://localhost:5432/andesstay_audit` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales de la base | `postgres` / *(sin valor)* |
| `KAFKA_BOOTSTRAP_SERVERS` | Brokers de Kafka | `localhost:9092` |
| `KAFKA_TOPIC_REPLICAS` | Réplicas del tópico DLT (3 con el clúster de 3 brokers) | `1` |
| `KAFKA_RETRY_ATTEMPTS` / `KAFKA_RETRY_INTERVAL_MS` | Reintentos antes de enviar al DLT | `3` / `1000` |
| `COGNITO_REGION`, `COGNITO_USER_POOL_ID`, `COGNITO_APP_CLIENT_ID` | Mismo User Pool que catalog, reservations y BFF | — |
| `FRONTEND_URL` | Origen permitido por CORS (varios separados por coma) | `http://localhost:4200` |
| `SERVER_PORT` | Puerto del servicio | `8083` |

## Cómo correrlo local
1. Levantar Kafka + Zookeeper (`infra/kafka/compose.yml` del proyecto).
2. Crear la base: `psql -U postgres -c "CREATE DATABASE andesstay_audit;"`
3. Exportar `DB_PASSWORD` (y las demás variables si no usas los valores por defecto).
4. Arrancar:
   - **Sin Cognito** (perfil `nosec`, solo desarrollo; deja los endpoints abiertos):
     `mvn spring-boot:run -Dspring-boot.run.profiles=nosec`
   - **Con seguridad real**: exportar las variables de Cognito y ejecutar `mvn spring-boot:run`.

> El perfil `nosec` **no se debe usar** en la demo ni en AWS.

## Docker
```bash
docker build -t ms-andesstay-audit .
docker run -d --name audit --env-file .env -p 8083:8083 ms-andesstay-audit
```
En la EC2, `KAFKA_BOOTSTRAP_SERVERS` debe apuntar a la IP privada del `ec2-kafka`.

## Endpoint

| Método | Ruta | Descripción | Roles |
|---|---|---|---|
| GET | `/api/audit` | Lista eventos de auditoría, paginado (más recientes primero) | ADMIN, AUDITOR |
| GET | `/actuator/health` | Estado del servicio (público, lo usa Docker/EC2) | — |
| GET | `/actuator/prometheus` | Métricas para Prometheus (**no publicar en el API Gateway**) | — |

Parámetros (todos opcionales): `usuario`, `tipoEvento`, `desde`, `hasta` (ISO-8601, ej. `2026-09-01T00:00:00Z`),
`page` (≥ 0, por defecto 0) y `size` (1 a 100, por defecto 20).

```
GET /api/audit?tipoEvento=RESERVA_CONFIRMADA&desde=2026-09-01T00:00:00Z
Authorization: Bearer <access_token>
```

### Formato de errores (igual que el BFF y reservations)
```json
{ "timestamp": "2026-11-11T05:00:00Z", "status": 403, "error": "forbidden",
  "message": "No tienes permisos (rol) suficientes para realizar esta operacion.", "path": "/api/audit" }
```
| Código | Cuándo |
|---|---|
| 400 | Filtros inválidos: fecha con mal formato, `desde` posterior a `hasta`, `size` fuera de rango |
| 401 | Sin token, token vencido o emitido para otro `client_id` |
| 403 | Token válido pero sin rol ADMIN o AUDITOR |

## Comportamiento del consumer
- **Idempotente:** el `eventId` es único (`uk_audit_event_id`); si Kafka reentrega un evento, se ignora.
- **Evento inválido** (JSON roto, sin `eventId` o sin `type`): no se reintenta y va directo al DLT.
- **Falla transitoria** (ej. PostgreSQL caído): se reintenta 3 veces, 1 s entre intentos; si sigue
  fallando, el mensaje va al DLT. **No se pierde ningún mensaje** y el consumer no se bloquea.
- **DLT:** tópico `reservations.events.audit.DLT` (3 particiones), creado al arrancar. Los headers del
  mensaje traen la excepción y el tópico/partición/offset de origen. Se puede revisar en Kafka UI.
- Tolera campos nuevos en el evento (`@JsonIgnoreProperties(ignoreUnknown = true)`), por lo que
  reservations puede agregar datos (ej. `unitId`) sin romper este servicio.

## Seguridad
- JWT de AWS Cognito: se validan firma, emisor (`iss`), `client_id` y vigencia.
- Roles desde el claim `cognito:groups` → `ROLE_ADMIN`, `ROLE_AUDITOR`.
- CORS para el frontend Angular según `FRONTEND_URL`.

## Pruebas
```bash
mvn test
```
- `AuditSecurityTest`: 401/403/200 según rol, errores 400 en JSON, 404 en JSON y preflight CORS.
- `AuditEventConsumerTest`: guardado, idempotencia, eventos inválidos y propagación de fallas.

## Equipo
Jean Flores, Christian Fuentes, Emmanuel Valenzuela.
