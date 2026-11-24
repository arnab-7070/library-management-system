-- Runs automatically every time the app starts (safe to run repeatedly).

CREATE TABLE IF NOT EXISTS librarians (
    id            SERIAL PRIMARY KEY,
    username      VARCHAR(50) NOT NULL UNIQUE,
    password_hash TEXT        NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS books (
    id       SERIAL PRIMARY KEY,
    title    VARCHAR(200)   NOT NULL,
    author   VARCHAR(120)   NOT NULL,
    price    NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    quantity INT            NOT NULL CHECK (quantity >= 0)
);
-- the same title + author can only exist once (adding it again adds copies)
CREATE UNIQUE INDEX IF NOT EXISTS uq_books_title_author ON books (LOWER(title), LOWER(author));

CREATE TABLE IF NOT EXISTS users (
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    phone      VARCHAR(15)  NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS borrowings (
    id          SERIAL PRIMARY KEY,
    user_id     INT       NOT NULL REFERENCES users (id),
    book_id     INT       NOT NULL REFERENCES books (id),
    quantity    INT       NOT NULL CHECK (quantity > 0),
    borrowed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    returned_at TIMESTAMP            -- NULL means the book is still out
);
