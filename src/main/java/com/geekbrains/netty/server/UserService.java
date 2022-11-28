package com.geekbrains.netty.server;

import java.sql.SQLException;

public interface UserService {

    void start();
    void stop();
    boolean authenticate(String login, String password) throws SQLException;
    boolean register(String username, String login, String password) throws SQLException;

}
