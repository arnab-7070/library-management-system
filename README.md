# Library Management System

A full-stack web app for running a small library: manage books and members, issue and return copies, and see what is currently out on loan. It started as a Java console project that kept everything in ArrayLists. It now stores data in PostgreSQL behind a REST API, with a browser frontend on top.

## Features

- Librarian account with a strong-password rule, salted PBKDF2 password hashing and token-based sessions
- Add books, search by title or author, and delete books with no loan history
- Adding a book that already exists (same title and author) adds copies instead of creating a duplicate
- Register members, with a unique phone number for each
- Issue and return books, with stock and loan history updated together in one database transaction
- Dashboard showing catalogue totals and the list of books currently on loan
- Clear error messages for out-of-stock books, unknown members, duplicate members and invalid input

## Tech stack

| Layer | Technology |
|-------|-----------|
| Frontend | HTML5, CSS, vanilla JavaScript (`fetch` API) |
| Backend | Java 17, built-in `com.sun.net.httpserver`, Gson |
| Database | PostgreSQL with JDBC |
| Build tool | Maven |

## How it fits together

```
Browser (frontend/)  --fetch + JSON-->  ApiHandler  -->  Library / Auth  -->  DAO classes  --JDBC-->  PostgreSQL
```

| Package | Responsibility |
|---------|----------------|
| `com.view` | `Main` starts the server |
| `com.api` | HTTP routes (`ApiHandler`) and static file serving |
| `com.controller` | Business rules (`Library`) and login (`Auth`) |
| `com.dao` | All SQL, one class per table |
| `com.model` | `Book`, `User`, `Borrowing` and the custom exceptions |
| `com.util` | `Config` and `Database` (connections and schema setup) |

The frontend talks to the backend only through `frontend/js/api.js`.

## Getting started

### Prerequisites

- JDK 17 or newer
- Maven (or Eclipse, which includes it)
- PostgreSQL

### Setup

1. Clone the repository:

   ```
   git clone https://github.com/arnab-7070/library-management-system.git
   cd library-management-system
   ```

2. Create an empty database in PostgreSQL:

   ```sql
   CREATE DATABASE library_db;
   ```

   The tables are created automatically when the app starts.

3. Copy `config.example.properties` to `config.properties` and enter your own PostgreSQL details:

   ```
   db.url=jdbc:postgresql://localhost:5432/library_db
   db.user=postgres
   db.password=your_password_here
   server.port=8080
   ```

   `config.properties` is listed in `.gitignore`, so your password is never committed.

4. Start the app:

   ```
   mvn compile exec:java
   ```

   In
