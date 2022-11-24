package com.geekbrains.netty.server;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;

@Slf4j
public class DatabaseUserServiceImpl implements UserService{

    private Connection connection;

    private Statement statement;

    @Override
    public void start() {
        try {
            connect();
            log.info("Network service started.");
        } catch (ClassNotFoundException e) {
            log.error("JDBC driver for SQL driver manager was not found.", e);
        } catch (SQLException e) {
            if (statement == null) {
                log.error("Method executeUpdate() was called on a statement that is null.", e);
            } else {
                log.error("SQL database error occurred.", e);
            }
        }
    }

    @Override
    public void stop() {
        try {
            disconnect();
            log.info("Network service stopped.");
        } catch (SQLException e) {
            log.error("SQL database error occurred during shutdown.", e);
        }
    }

    @Override
    public boolean authenticate(String login, String password) throws SQLException {

        PreparedStatement prepStat = connection.prepareStatement(
                "SELECT id FROM clients WHERE login = ? AND password = ?");
        prepStat.setString(1, login);
        prepStat.setString(2, password);
        prepStat.addBatch();
        return prepStat.executeQuery().next();
    }

    @Override
    public boolean register(String username, String login, String password) throws SQLException {

        PreparedStatement prepStat = connection.prepareStatement(
                "SELECT id FROM clients WHERE login = ? OR username = ?"
        );
        prepStat.setString(1, login);
        prepStat.setString(2, password);
        prepStat.addBatch();
        if (!prepStat.executeQuery().next()) {
            prepStat = connection.prepareStatement("INSERT INTO clients (login, password, username) VALUES (?, ?, ?)");
            prepStat.setString(1, login);
            prepStat.setString(2, password);
            prepStat.setString(3, username);
            prepStat.addBatch();

            //not sure about it
            if (prepStat.executeUpdate() == 0) {
                //something went wrong
                throw new SQLException("SQL command INSERT INTO returned 0 when it shouldn't.");
            } else {
                return true;
            }
        }
        //not unique user
        return false;
    }


    private void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection(
                "jdbc:sqlite:src/main/java/com/geekbrains/netty/server/server.db");
        statement = connection.createStatement();
        statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    login TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    username TEXT NOT NULL UNIQUE);
                    """);

    }

    private void disconnect() throws SQLException {
        if (connection != null) {
            connection.close();
        }
        if (statement != null) {
            statement.close();
        }
    }
}
