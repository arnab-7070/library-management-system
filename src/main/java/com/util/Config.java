package com.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/** Reads settings from config.properties, or from environment variables if they are set. */
public class Config {
	private static final Properties props = new Properties();

	static {
		try (FileInputStream in = new FileInputStream("config.properties")) {
			props.load(in);
		} catch (IOException e) {
			// no file: fall back to environment variables and defaults
		}
	}

	public static String get(String key, String envName, String defaultValue) {
	    String env = System.getenv(envName);
	    if (env != null && !env.isBlank()) {
	        return env.trim();
	    }
	    return props.getProperty(key, defaultValue).trim();
	}
}
