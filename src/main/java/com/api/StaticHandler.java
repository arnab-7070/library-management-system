package com.api;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.util.Config;

/** Serves the files in the frontend/ folder (index.html, css, js) so one command runs the whole app. */
public class StaticHandler implements HttpHandler {
	private static final Map<String, String> TYPES = Map.of(".html", "text/html; charset=utf-8", ".css",
			"text/css; charset=utf-8", ".js", "text/javascript; charset=utf-8", ".svg", "image/svg+xml", ".png",
			"image/png", ".ico", "image/x-icon");
	private final Path root = Paths.get(Config.get("frontend.dir", "FRONTEND_DIR", "frontend")).toAbsolutePath()
			.normalize();

	@Override
	public void handle(HttpExchange ex) throws IOException {
		String path = URLDecoder.decode(ex.getRequestURI().getPath(), StandardCharsets.UTF_8);
		if (path.equals("/")) {
			path = "/index.html";
		}
		Path file = root.resolve(path.substring(1)).normalize();
		// refuse anything outside the frontend folder (e.g. /../config.properties)
		if (!file.startsWith(root) || !Files.isRegularFile(file)) {
			byte[] msg = "Not found".getBytes(StandardCharsets.UTF_8);
			ex.sendResponseHeaders(404, msg.length);
			try (OutputStream os = ex.getResponseBody()) {
				os.write(msg);
			}
			return;
		}
		String name = file.getFileName().toString();
		String type = TYPES.getOrDefault(name.substring(name.lastIndexOf('.')), "application/octet-stream");
		byte[] bytes = Files.readAllBytes(file);
		ex.getResponseHeaders().set("Content-Type", type);
		ex.sendResponseHeaders(200, bytes.length);
		try (OutputStream os = ex.getResponseBody()) {
			os.write(bytes);
		}
	}
}
