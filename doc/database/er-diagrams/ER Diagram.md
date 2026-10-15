# ER Diagram: ER Diagram

## Tables

| # | Table | Columns |
|---|-------|---------|
| 1 | **databasechangelog** | id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id |
| 2 | **databasechangeloglock** | id, locked, lockgranted, lockedby |
| 3 | **oauth2_authorization** | id, registered_client_id, principal_name, authorization_grant_type, authorized_scopes, attributes, state, authorization_code_* (value, issued_at, expires_at, metadata), access_token_* (value, issued_at, expires_at, metadata, type, scopes), oidc_id_token_* (value, issued_at, expires_at, metadata), refresh_token_* (value, issued_at, expires_at, metadata), user_code_* (value, issued_at, expires_at, metadata), device_code_* (value, issued_at, expires_at, metadata) |
| 4 | **oauth2_authorization_consent** | registered_client_id, principal_name, authorities |
| 5 | **oauth2_registered_client** | id, client_id, client_id_issued_at, client_secret, client_secret_expires_at, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, post_logout_redirect_uris, scopes, client_settings, token_settings |
| 6 | **permission** | id, created_at, name |
| 7 | **role** | id, created_at, name |
| 8 | **roles_permissions** | role_id, permission_id |
| 9 | **user_table** | id, created_at, username, password, email, status, verification_code |
| 10 | **users_roles** | user_id, role_id |

The `oauth2_*` tables (Spring Authorization Server schema, `2_create-oauth2-authorization-server-tables.yml`) have
no foreign keys; `registered_client_id` refers to `oauth2_registered_client.id` by convention.

## Entity Relationship Diagram

```plantuml
@startuml ER Diagram
' ER Diagram: ER Diagram

!theme plain
hide empty methods
skinparam linetype ortho
skinparam entity {
  BackgroundColor #FEFECE
  BorderColor #A80036
  ArrowColor #A80036
}

entity "databasechangelog" as databasechangelog {
  *id : varchar(255)
  *author : varchar(255)
  *filename : varchar(255)
  *dateexecuted : timestamp
  *orderexecuted : integer
  *exectype : varchar(10)
  md5sum : varchar(35)
  description : varchar(255)
  comments : varchar(255)
  tag : varchar(255)
  liquibase : varchar(20)
  contexts : varchar(255)
  labels : varchar(255)
  deployment_id : varchar(10)
}

entity "databasechangeloglock" as databasechangeloglock {
  *id : integer <<PK>>
  --
  *locked : boolean
  lockgranted : timestamp
  lockedby : varchar(255)
}

entity "oauth2_registered_client" as oauth2_registered_client {
  *id : varchar(100) <<PK>>
  --
  *client_id : varchar(100)
  *client_id_issued_at : timestamptz
  client_secret : varchar(200)
  client_secret_expires_at : timestamptz
  *client_name : varchar(200)
  *client_authentication_methods : varchar(1000)
  *authorization_grant_types : varchar(1000)
  redirect_uris : varchar(1000)
  post_logout_redirect_uris : varchar(1000)
  *scopes : varchar(1000)
  *client_settings : varchar(2000)
  *token_settings : varchar(2000)
}

entity "oauth2_authorization" as oauth2_authorization {
  *id : varchar(100) <<PK>>
  --
  *registered_client_id : varchar(100)
  *principal_name : varchar(200)
  *authorization_grant_type : varchar(100)
  authorized_scopes : varchar(1000)
  attributes : text
  state : varchar(500)
  authorization_code_* : value, issued_at, expires_at, metadata
  access_token_* : value, issued_at, expires_at, metadata, type, scopes
  oidc_id_token_* : value, issued_at, expires_at, metadata
  refresh_token_* : value, issued_at, expires_at, metadata
  user_code_* : value, issued_at, expires_at, metadata
  device_code_* : value, issued_at, expires_at, metadata
}

entity "oauth2_authorization_consent" as oauth2_authorization_consent {
  *registered_client_id : varchar(100) <<PK>>
  *principal_name : varchar(200) <<PK>>
  --
  *authorities : varchar(1000)
}

entity "permission" as permission {
  *id : bigint <<PK>>
  --
  *created_at : timestamp
  *name : varchar <<UK>>
}

entity "role" as role {
  *id : bigint <<PK>>
  --
  *created_at : timestamp
  *name : varchar <<UK>>
}

entity "roles_permissions" as roles_permissions {
  *role_id : bigint <<PK>> <<FK>>
  *permission_id : bigint <<PK>> <<FK>>
}

entity "user_table" as user_table {
  *id : bigint <<PK>>
  --
  *created_at : timestamp
  *username : varchar <<UK>>
  password : varchar
  *email : varchar <<UK>>
  *status : varchar
  verification_code : varchar
}

entity "users_roles" as users_roles {
  *user_id : bigint <<PK>> <<FK>>
  *role_id : bigint <<PK>> <<FK>>
}

roles_permissions }o--|| role : "role_id"
roles_permissions }o--|| permission : "permission_id"
users_roles }o--|| user_table : "user_id"
users_roles }o--|| role : "role_id"

@enduml
```
