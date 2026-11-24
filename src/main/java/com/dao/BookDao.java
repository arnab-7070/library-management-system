package com.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.model.Book;
import com.util.Database;

/** All SQL that touches the books table. */
public class BookDao {

	private Book map(ResultSet rs) throws SQLException {
		return new Book(rs.getLong("id"), rs.getString("title"), rs.getString("author"), rs.getDouble("price"),
				rs.getInt("quantity"));
	}

	public List<Book> findAll(String search) throws SQLException {
		String sql = "SELECT id, title, author, price, quantity FROM books "
				+ "WHERE title ILIKE ? OR author ILIKE ? ORDER BY title";
		try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, "%" + search + "%");
			ps.setString(2, "%" + search + "%");
			List<Book> list = new ArrayList<>();
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(map(rs));
				}
			}
			return list;
		}
	}

	/** Adds a new book, or adds copies if the same title and author already exist. */
	public Book addOrRestock(String title, String author, double price, int quantity) throws SQLException {
		String sql = "INSERT INTO books (title, author, price, quantity) VALUES (?, ?, ?, ?) "
				+ "ON CONFLICT (LOWER(title), LOWER(author)) "
				+ "DO UPDATE SET quantity = books.quantity + EXCLUDED.quantity "
				+ "RETURNING id, title, author, price, quantity";
		try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, title);
			ps.setString(2, author);
			ps.setDouble(3, price);
			ps.setInt(4, quantity);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return map(rs);
			}
		}
	}

	public boolean delete(long id) throws SQLException {
		try (Connection c = Database.getConnection();
				PreparedStatement ps = c.prepareStatement("DELETE FROM books WHERE id = ?")) {
			ps.setLong(1, id);
			return ps.executeUpdate() > 0;
		}
	}
}
