package com.view;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.api.ApiHandler;
import com.api.StaticHandler;
import com.sun.net.httpserver.HttpServer;
import com.util.Config;
import com.util.Database;

/** Starts the app: connects to PostgreSQL, creates the tables, then serves the API and the web pages. */
public class Main {

	public static void main(String[] args) throws Exception {
		try {
			Database.initSchema();
		} catch (Exception e) {
			System.err.println("Could not connect to PostgreSQL: " + e.getMessage());
			System.err.println("Check that PostgreSQL is running and that db.url, db.user and db.password in "
					+ "config.properties are correct.");
			return;
		}
		int port = Integer.parseInt(Config.get("server.port", "PORT", "8080"));
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/api", new ApiHandler());
		server.createContext("/", new StaticHandler());
		server.setExecutor(Executors.newFixedThreadPool(8));
		server.start();
		System.out.println("Library is running. Open http://localhost:" + port + " in your browser.");
	}
}
