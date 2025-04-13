package org.emp.auth;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQLAuthService extends UnicastRemoteObject implements AuthService {

    private static final long serialVersionUID = 1L;

    // Paramètres de connexion JDBC
    private static final String URL = "jdbc:mysql://localhost:3306/maildb";
    private static final String USER = "root";       // À adapter
    private static final String PASSWORD = ""; // À adapter

    public MySQLAuthService() throws RemoteException {
        super();
        // Vous pouvez créer la table ici si besoin, ou le faire via script SQL
    }

    @Override
    public boolean authenticate(String username, String password) throws RemoteException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String sql = "SELECT password FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    return storedPassword.equals(password);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean createUser(String username, String password) throws RemoteException {
        // Vérifier si l'utilisateur existe déjà
        if (userExists(username)) {
            return false;
        }
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean updateUser(String username, String newPassword) throws RemoteException {
        if (!userExists(username)) {
            return false;
        }
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String sql = "UPDATE users SET password = ? WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newPassword);
                ps.setString(2, username);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deleteUser(String username) throws RemoteException {
        if (!userExists(username)) {
            return false;
        }
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String sql = "DELETE FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean userExists(String username) throws RemoteException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String sql = "SELECT id FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<String> getAllUsers() throws RemoteException {
        List<String> userList = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String sql = "SELECT username FROM users";
            try (Statement st = conn.createStatement()) {
                ResultSet rs = st.executeQuery(sql);
                while (rs.next()) {
                    userList.add(rs.getString("username"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userList;
    }
}
