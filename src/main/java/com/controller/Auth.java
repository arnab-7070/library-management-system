package com.controller;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.dao.LibrarianDao;
import com.model.PasswordNotStrongException;

/** Librarian account, password hashing and login tokens. */
public class Auth {
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Map<String, String> sessions = new ConcurrentHashMap<>(); // token -> username
	private final LibrarianDao dao = new LibrarianDao();

	public boolean hasLibrarian() throws SQLException {
		return dao.anyExists();
	}

	// the same password rules as the original console version
	public static boolean isValidPassword(String pwd) {
		if (pwd.length() < 8 || pwd.contains(" ")) {
			return false;
		}
		boolean upper = false, lower = false, number = false, special = false;
		for (char ch : pwd.toCharArray()) {
			if (ch >= 'a' && ch <= 'z') lower = true;
			else if (ch >= 'A' && ch <= 'Z') upper = true;
			else if (ch >= '0' && ch <= '9') number = true;
			else special = true;
		}
		return lower && upper && number && special;
	}

	/** First-time setup: only works while no librarian account exists. */
	public void register(String username, String password) throws SQLException {
		if (dao.anyExists()) {
			throw new IllegalStateException("A librarian account already exists. Please sign in.");
		}
		if (username.isBlank()) {
			throw new IllegalArgumentException("Librarian ID is required.");
		}
		if (!isValidPassword(password)) {
			throw new PasswordNotStrongException(
					"Use at least 8 characters with an upper-case letter, a lower-case letter, a number and a symbol. No spaces.");
		}
		dao.create(username, hash(password));
	}

	public String login(String username, String password) throws SQLException {
		String stored = dao.findHash(username);
		if (stored == null || !verify(password, stored)) {
			throw new SecurityException("Invalid ID or password.");
		}
		byte[] raw = new byte[24];
		RANDOM.nextBytes(raw);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
		sessions.put(token, username);
		return token;
	}

	/** header looks like: "Bearer abc123" */
	public void requireLogin(String authHeader) {
		String token = tokenFrom(authHeader);
		if (token == null || !sessions.containsKey(token)) {
			throw new SecurityException("Please sign in first.");
		}
	}

	public void logout(String authHeader) {
		String token = tokenFrom(authHeader);
		if (token != null) {
			sessions.remove(token);
		}
	}

	private static String tokenFrom(String header) {
		return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
	}

	// ---------- password hashing (PBKDF2 with a random salt) ----------
	private static String hash(String password) {
		byte[] salt = new byte[16];
		RANDOM.nextBytes(salt);
		Base64.Encoder b64 = Base64.getEncoder();
		return b64.encodeToString(salt) + ":" + b64.encodeToString(pbkdf2(password, salt));
	}

	private static boolean verify(String password, String stored) {
		String[] parts = stored.split(":");
		byte[] salt = Base64.getDecoder().decode(parts[0]);
		byte[] expected = Base64.getDecoder().decode(parts[1]);
		return MessageDigest.isEqual(expected, pbkdf2(password, salt));
	}

	private static byte[] pbkdf2(String password, byte[] salt) {
		try {
			PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120_000, 256);
			return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}
}
