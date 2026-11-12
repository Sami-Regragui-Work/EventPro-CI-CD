package com.eventpro.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

public final class DatabaseInitializer {
    private static final String DB_DOES_NOT_EXIST = "3D000";
    private static final String DB_ALREADY_EXIST = "42P04";
    private static final Pattern SAFE_POSTGRES_DB_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}");

    private DatabaseInitializer() {}

    public static void ensureDatabaseExists(String urlPrefix, String dbName, String username, String password) {
        if (!urlPrefix.endsWith("/"))
            throw new IllegalArgumentException("app.database.url-prefix must end with '/': " + urlPrefix);

        if (!SAFE_POSTGRES_DB_NAME.matcher(dbName).matches())
            throw new IllegalArgumentException("Unsafe database name in app.database.name: " + dbName);

        try (var _ = DriverManager.getConnection(urlPrefix + dbName, username, password)) {
            System.out.println("Database '" + dbName + "' exists. Skipping creation.");
            return;
        } catch (SQLException e) {
            if (!DB_DOES_NOT_EXIST.equals(e.getSQLState()))
                throw new IllegalStateException("Failed to connect to database '" + dbName + "': " + e.getMessage(), e);
        }

        try (Connection connection = DriverManager.getConnection(urlPrefix + "postgres", username, password);
             Statement statement = connection.createStatement();
        ) {
            // the following text is not a comment, it's a warning suppression
            //noinspection SqlSourceToSinkFlow
            statement.executeUpdate("CREATE DATABASE \"" + dbName + "\"");
            System.out.println("Database '" + dbName + "' created successfully.");
        } catch (SQLException e) {
            if (!DB_ALREADY_EXIST.equals(e.getSQLState()))
                throw new IllegalStateException("Failed to create database '" + dbName + "': " + e.getMessage(), e);
        }
    }
}
