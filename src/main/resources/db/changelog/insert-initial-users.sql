INSERT INTO user_table (id, username, email, password, status, verification_code, created_at)
VALUES (nextval('user_id_seq'), 'test-user-1', 'test-user-1@somemail.com',
        '$2a$10$9i1/tnC0VbnqkM/8XCDk0.slDworPf4twa2Q0NuNGhTKqnWaPiwPS', 'VERIFIED',
        '2d27c8cb-ac20-4597-91a1-38a2fff1e711', NOW());

INSERT INTO user_table (id, username, email, password, status, verification_code, created_at)
VALUES (nextval('user_id_seq'), 'test-user-2', 'test-user-2@somemail.com',
        '$2a$10$9i1/tnC0VbnqkM/8XCDk0.slDworPf4twa2Q0NuNGhTKqnWaPiwPS', 'VERIFIED',
        '2d27c8cb-ac20-4597-91a1-38a2fff1e722', NOW());

INSERT INTO user_table (id, username, email, password, status, verification_code, created_at)
VALUES (nextval('user_id_seq'), 'test-user-3', 'test-user-3@somemail.com',
        '$2a$10$9i1/tnC0VbnqkM/8XCDk0.slDworPf4twa2Q0NuNGhTKqnWaPiwPS', 'VERIFIED',
        '2d27c8cb-ac20-4597-91a1-38a2fff1e733', NOW());

INSERT INTO user_table (id, username, email, password, status, verification_code, created_at)
VALUES (nextval('user_id_seq'), 'admin', 'admin@somemail.com',
        '$2a$10$r/ATO2I5L8I0XoyQDQC..Ombni29ReZTwzXSVKKT3QLBZODZlEHaS', 'VERIFIED',
        'bab357ec-39ec-4e55-bf00-174e11241359', NOW());

INSERT INTO users_roles (user_id, role_id)
SELECT (SELECT id FROM user_table WHERE username = 'test-user-1'),
       (SELECT id FROM role WHERE name = 'ROLE_USER');

INSERT INTO users_roles (user_id, role_id)
SELECT (SELECT id FROM user_table WHERE username = 'test-user-2'),
       (SELECT id FROM role WHERE name = 'ROLE_USER');

INSERT INTO users_roles (user_id, role_id)
SELECT (SELECT id FROM user_table WHERE username = 'test-user-3'),
       (SELECT id FROM role WHERE name = 'ROLE_USER');

INSERT INTO users_roles (user_id, role_id)
SELECT (SELECT id FROM user_table WHERE username = 'admin'),
       (SELECT id FROM role WHERE name = 'ROLE_ADMIN');
