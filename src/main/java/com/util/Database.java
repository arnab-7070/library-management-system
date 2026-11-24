package com.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Opens JDBC connections and creates the tables on startup. */
public class Database {

	public static Connection getConnection() throws SQLException {
		String url = Config.get("db.url", "DB_URL", "jdbc:postgresql://localhost:5432/library_db");
		String user = Config.get("db.user", "DB_USER", "postgres");
		String password = Config.get("db.password", "DB_PASSWORD", "");
		return DriverManager.getConnection(url, user, password);
	}

	public static void initSchema() throws SQLException, IOException {
		try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
			String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			try (Connection c = getConnection(); Statement st = c.createStatement()) {
				st.execute(sql);
			}
		}
	}
}
