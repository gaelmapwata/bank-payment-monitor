CREATE TABLE app_users (
                           id BIGSERIAL PRIMARY KEY,

                           username VARCHAR(100) NOT NULL UNIQUE,

                           password VARCHAR(255) NOT NULL,

                           branch_code VARCHAR(50),

                           role VARCHAR(20) NOT NULL,

                           enabled BOOLEAN NOT NULL
);