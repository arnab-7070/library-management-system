package com.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.model.*;
import com.util.Database;

/** Borrow and return use transactions, so stock and history can never disagree. */
public class BorrowingDao {

	public List<Borrowing> findAll(boolean activeOnly) throws SQLException {
		String sql = "SELECT br.id, br.user_id, u.name, br.book_id, b.title, br.quantity, br.borrowed_at, br.returned_at "
				+ "FROM borrowings br JOIN users u ON u.id = br.user_id JOIN books b ON b.id = br.book_id "
				+ (activeOnly ? "WHERE br.returned_at IS NULL " : "") + "ORDER BY br.borrowed_at DESC, br.id DESC";
		try (Connection c = Database.getConnection();
				Statement st = c.createStatement();
				ResultSet rs = st.executeQuery(sql)) {
			List<Borrowing> list = new ArrayList<>();
			while (rs.next()) {
				Timestamp returned = rs.getTimestamp("returned_at");
				list.add(new Borrowing(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4), rs.getString(5),
						rs.getInt(6), rs.getTimestamp("borrowed_at").toLocalDateTime().toString(),
						returned == null ? null : returned.toLocalDateTime().toString()));
			}
			return list;
		}
	}

	public void borrow(long userId, long bookId, int quantity) throws SQLException {
		try (Connection c = Database.getConnection()) {
			c.setAutoCommit(false); // start a transaction
			try {
				if (!exists(c, "users", userId)) {
					throw new UserDoesNotExistException("No member found with that ID.");
				}
				// take the copies out of stock only if enough are left
				try (PreparedStatement ps = c
						.prepareStatement("UPDATE books SET quantity = quantity - ? WHERE id = ? AND quantity >= ?")) {
					ps.setInt(1, quantity);
					ps.setLong(2, bookId);
					ps.setInt(3, quantity);
					if (ps.executeUpdate() == 0) {
						if (!exists(c, "books", bookId)) {
							throw new IllegalArgumentException("That book does not exist.");
						}
						throw new BookOutOfStockException("Not enough copies in stock.");
					}
				}
				try (PreparedStatement ps = c
						.prepareStatement("INSERT INTO borrowings (user_id, book_id, quantity) VALUES (?, ?, ?)")) {
					ps.setLong(1, userId);
					ps.setLong(2, bookId);
					ps.setInt(3, quantity);
					ps.executeUpdate();
				}
				c.commit();
			} catch (SQLException | RuntimeException e) {
				c.rollback(); // undo everything if any step failed
				throw e;
			}
		}
	}

	public void giveBack(long borrowingId) throws SQLException {
		try (Connection c = Database.getConnection()) {
			c.setAutoCommit(false);
			try {
				long bookId;
				int quantity;
				try (PreparedStatement ps = c.prepareStatement("UPDATE borrowings SET returned_at = NOW() "
						+ "WHERE id = ? AND returned_at IS NULL RETURNING book_id, quantity")) {
					ps.setLong(1, borrowingId);
					try (ResultSet rs = ps.executeQuery()) {
						if (!rs.next()) {
							throw new IllegalStateException("This book was already returned, or the record does not exist.");
						}
						bookId = rs.getLong(1);
						quantity = rs.getInt(2);
					}
				}
				try (PreparedStatement ps = c
						.prepareStatement("UPDATE books SET quantity = quantity + ? WHERE id = ?")) {
					ps.setInt(1, quantity);
					ps.setLong(2, bookId);
					ps.executeUpdate();
				}
				c.commit();
			} catch (SQLException | RuntimeException e) {
				c.rollback();
				throw e;
			}
		}
	}

	public Map<String, Long> stats() throws SQLException {
		String sql = "SELECT (SELECT COUNT(*) FROM books), (SELECT COALESCE(SUM(quantity), 0) FROM books), "
				+ "(SELECT COUNT(*) FROM users), "
				+ "(SELECT COALESCE(SUM(quantity), 0) FROM borrowings WHERE returned_at IS NULL)";
		try (Connection c = Database.getConnection();
				Statement st = c.createStatement();
				ResultSet rs = st.executeQuery(sql)) {
			rs.next();
			Map<String, Long> m = new LinkedHashMap<>();
			m.put("titles", rs.getLong(1));
			m.put("copiesInStock", rs.getLong(2));
			m.put("members", rs.getLong(3));
			m.put("copiesOut", rs.getLong(4));
			return m;
		}
	}

	private boolean exists(Connection c, String table, long id) throws SQLException {
		// table is always one of our own constant names, never user input
		try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM " + table + " WHERE id = ?")) {
			ps.setLong(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}
}
