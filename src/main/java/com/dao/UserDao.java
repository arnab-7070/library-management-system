package com.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.model.User;
import com.util.Database;

/** All SQL that touches the users (members) table. */
public class UserDao {

	public List<User> findAll() throws SQLException {
		try (Connection c = Database.getConnection();
				Statement st = c.createStatement();
				ResultSet rs = st.executeQuery("SELECT id, name, phone FROM users ORDER BY name")) {
			List<User> list = new ArrayList<>();
			while (rs.next()) {
				list.add(new User(rs.getLong("id"), rs.getString("name"), rs.getString("phone")));
			}
			return list;
		}
	}

	/** Throws SQLException with state 23505 if the phone number is already registered. */
	public User add(String name, String phone) throws SQLException {
		try (Connection c = Database.getConnection();
				PreparedStatement ps = c
						.prepareStatement("INSERT INTO users (name, phone) VALUES (?, ?) RETURNING id")) {
			ps.setString(1, name);
			ps.setString(2, phone);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return new User(rs.getLong("id"), name, phone);
			}
		}
	}
}
