INSERT INTO role (id, created_at, name)
VALUES (nextval('role_id_seq'), NOW(), 'ROLE_USER');

INSERT INTO role (id, created_at, name)
VALUES (nextval('role_id_seq'), NOW(), 'ROLE_ADMIN');
