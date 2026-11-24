package com.controller;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.dao.*;
import com.model.*;

/** Library class: the business rules. The web layer calls these methods. */
public class Library {
	private final BookDao books = new BookDao();
	private final UserDao users = new UserDao();
	private final BorrowingDao borrowings = new BorrowingDao();

	// ---------- books ----------
	public List<Book> listBooks(String search) throws SQLException {
		return books.findAll(search == null ? "" : search.trim());
	}

	public Book addBook(String title, String author, double price, int quantity) throws SQLException {
		if (title.isBlank() || author.isBlank()) {
			throw new IllegalArgumentException("Title and author are required.");
		}
		if (price < 0) {
			throw new IllegalArgumentException("Price cannot be negative.");
		}
		if (quantity < 1) {
			throw new IllegalArgumentException("Quantity must be at least 1.");
		}
		return books.addOrRestock(title, author, price, quantity);
	}

	public void deleteBook(long id) throws SQLException {
		try {
			if (!books.delete(id)) {
				throw new IllegalArgumentException("That book does not exist.");
			}
		} catch (SQLException e) {
			if ("23503".equals(e.getSQLState())) { // foreign key violation
				throw new IllegalStateException("This book has borrowing history, so it cannot be deleted.");
			}
			throw e;
		}
	}

	// ---------- members ----------
	public List<User> listUsers() throws SQLException {
		return users.findAll();
	}

	public User addUser(String name, String phone) throws SQLException {
		if (name.isBlank()) {
			throw new IllegalArgumentException("Name is required.");
		}
		if (!phone.matches("\\d{10,15}")) {
			throw new IllegalArgumentException("Phone number must be 10 to 15 digits, with no spaces or symbols.");
		}
		try {
			return users.add(name, phone);
		} catch (SQLException e) {
			if ("23505".equals(e.getSQLState())) { // unique violation
				throw new IllegalStateException("A member with this phone number already exists.");
			}
			throw e;
		}
	}

	// ---------- borrow / return ----------
	public void borrowBook(long userId, long bookId, int quantity) throws SQLException {
		if (quantity < 1) {
			throw new IllegalArgumentException("Quantity must be at least 1.");
		}
		borrowings.borrow(userId, bookId, quantity);
	}

	public void returnBook(long borrowingId) throws SQLException {
		borrowings.giveBack(borrowingId);
	}

	public List<Borrowing> listBorrowings(boolean activeOnly) throws SQLException {
		return borrowings.findAll(activeOnly);
	}

	public Map<String, Long> stats() throws SQLException {
		return borrowings.stats();
	}
}
