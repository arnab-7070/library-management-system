package com.api;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.controller.Auth;
import com.controller.Library;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.model.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Receives every request that starts with /api, calls the right Library method and sends the result back as JSON.
 *
 *   POST /api/auth/register   GET  /api/auth/status   POST /api/auth/login   POST /api/auth/logout
 *   GET/POST /api/books       DELETE /api/books/{id}
 *   GET/POST /api/users
 *   GET/POST /api/borrowings  POST /api/borrowings/{id}/return
 *   GET /api/stats
 */
public class ApiHandler implements HttpHandler {
	private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
	private final Library library = new Library();
	private final Auth auth = new Auth();

	private record Reply(int status, Object body) {
	}

	@Override
	public void handle(HttpExchange ex) throws IOException {
		// CORS headers let a frontend served from another address (Live Server, GitHub Pages) call this API
		ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
		ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
		if (ex.getRequestMethod().equals("OPTIONS")) { // browser "preflight" check
			ex.sendResponseHeaders(204, -1);
			return;
		}
		Reply reply;
		try {
			reply = route(ex, ex.getRequestMethod(), ex.getRequestURI().getPath());
		} catch (ApiException e) {
			reply = error(e.getStatus(), e.getMessage());
		} catch (SecurityException e) {
			reply = error(401, e.getMessage());
		} catch (UserDoesNotExistException e) {
			reply = error(404, e.getMessage());
		} catch (BookOutOfStockException | IllegalStateException e) {
			reply = error(409, e.getMessage());
		} catch (PasswordNotStrongException | IllegalArgumentException | JsonParseException e) {
			reply = error(400, e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			reply = error(500, "Something went wrong on the server. Check the Java console for details.");
		}
		byte[] bytes = gson.toJson(reply.body()).getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		ex.sendResponseHeaders(reply.status(), bytes.length);
		try (OutputStream os = ex.getResponseBody()) {
			os.write(bytes);
		}
	}

	private Reply route(HttpExchange ex, String method, String path) throws Exception {
		String[] s = path.substring(1).split("/"); // "api/books/5" -> [api, books, 5]
		String resource = s.length > 1 ? s[1] : "";
		String authHeader = ex.getRequestHeaders().getFirst("Authorization");

		// ----- routes that do not need a login -----
		if (resource.equals("auth")) {
			String action = s.length > 2 ? s[2] : "";
			if (method.equals("GET") && action.equals("status")) {
				return ok(Map.of("hasLibrarian", auth.hasLibrarian()));
			}
			if (method.equals("POST") && action.equals("register")) {
				JsonObject b = body(ex);
				auth.register(str(b, "username"), str(b, "password"));
				return new Reply(201, Map.of("message", "Librarian account created."));
			}
			if (method.equals("POST") && action.equals("login")) {
				JsonObject b = body(ex);
				return ok(Map.of("token", auth.login(str(b, "username"), str(b, "password"))));
			}
			if (method.equals("POST") && action.equals("logout")) {
				auth.logout(authHeader);
				return ok(Map.of("message", "Signed out."));
			}
			throw new ApiException(404, "Unknown route.");
		}

		// ----- everything below needs a valid token -----
		auth.requireLogin(authHeader);

		switch (resource) {
		case "books":
			if (method.equals("GET") && s.length == 2) {
				return ok(library.listBooks(query(ex, "q")));
			}
			if (method.equals("POST") && s.length == 2) {
				JsonObject b = body(ex);
				return new Reply(201, library.addBook(str(b, "title"), str(b, "author"), num(b, "price"),
						(int) num(b, "quantity")));
			}
			if (method.equals("DELETE") && s.length == 3) {
				library.deleteBook(id(s[2]));
				return ok(Map.of("message", "Book deleted."));
			}
			break;
		case "users":
			if (method.equals("GET") && s.length == 2) {
				return ok(library.listUsers());
			}
			if (method.equals("POST") && s.length == 2) {
				JsonObject b = body(ex);
				return new Reply(201, library.addUser(str(b, "name"), str(b, "phone")));
			}
			break;
		case "borrowings":
			if (method.equals("GET") && s.length == 2) {
				return ok(library.listBorrowings("active".equals(query(ex, "status"))));
			}
			if (method.equals("POST") && s.length == 2) {
				JsonObject b = body(ex);
				library.borrowBook((long) num(b, "userId"), (long) num(b, "bookId"), (int) num(b, "quantity"));
				return new Reply(201, Map.of("message", "Book issued."));
			}
			if (method.equals("POST") && s.length == 4 && s[3].equals("return")) {
				library.returnBook(id(s[2]));
				return ok(Map.of("message", "Book returned."));
			}
			break;
		case "stats":
			if (method.equals("GET")) {
				return ok(library.stats());
			}
			break;
		default:
		}
		throw new ApiException(404, "Unknown route.");
	}

	// ---------- small helpers ----------
	private static Reply ok(Object body) {
		return new Reply(200, body);
	}

	private static Reply error(int status, String message) {
		return new Reply(status, Map.of("error", message));
	}

	private static JsonObject body(HttpExchange ex) throws IOException {
		String text = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		return text.isBlank() ? new JsonObject() : JsonParser.parseString(text).getAsJsonObject();
	}

	private static String str(JsonObject o, String key) {
		return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString().trim() : "";
	}

	private static double num(JsonObject o, String key) {
		try {
			return Double.parseDouble(str(o, key));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("'" + key + "' must be a number.");
		}
	}

	private static long id(String text) {
		try {
			return Long.parseLong(text);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid ID in the URL.");
		}
	}

	private static String query(HttpExchange ex, String name) {
		String raw = ex.getRequestURI().getRawQuery();
		if (raw != null) {
			for (String pair : raw.split("&")) {
				String[] kv = pair.split("=", 2);
				if (kv[0].equals(name) && kv.length == 2) {
					return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
				}
			}
		}
		return "";
	}
}
