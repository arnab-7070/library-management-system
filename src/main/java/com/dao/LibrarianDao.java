package com.dao;

import java.sql.*;

import com.util.Database;

/** All SQL that touches the librarians table. */
public class LibrarianDao {

	public boolean anyExists() throws SQLException {
		try (Connection c = Database.getConnection();
				Statement st = c.createStatement();
				ResultSet rs = st.executeQuery("SELECT 1 FROM librarians LIMIT 1")) {
			return rs.next();
		}
	}

	public void create(String username, String passwordHash) throws SQLException {
		try (Connection c = Database.getConnection();
				PreparedStatement ps = c
						.prepareStatement("INSERT INTO librarians (username, password_hash) VALUES (?, ?)")) {
			ps.setString(1, username);
			ps.setString(2, passwordHash);
			ps.executeUpdate();
		}
	}

	/** Returns the stored hash, or null if there is no such librarian. */
	public String findHash(String username) throws SQLException {
		try (Connection c = Database.getConnection();
				PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM librarians WHERE username = ?")) {
			ps.setString(1, username);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getString(1) : null;
			}
		}
	}
}
