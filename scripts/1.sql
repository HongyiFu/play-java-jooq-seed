-- PostgreSQL 12.3

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS citext;

-- Enum values should match 1-1 to Java's enum (case sensitive), else it would map to the last enum value
-- See https://github.com/jOOQ/jOOQ/issues/7076
CREATE TYPE account_status AS ENUM ('ACTIVE', 'INACTIVE');

CREATE TABLE "user" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v1(),
    name CITEXT NOT NULL,
    created_on TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES "user"(id),
    account_status ACCOUNT_STATUS NOT NULL DEFAULT 'ACTIVE',
    created_on TIMESTAMP NOT NULL DEFAULT now()
);
