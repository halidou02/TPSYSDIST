package org.emp.auth;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQLProcedureService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/maildb";
    private static final String DB_USER = "root";       // À adapter selon votre configuration
    private static final String DB_PASS = "";     // À adapter selon votre configuration

    // Authentification via la procédure stockée authenticate_user
    public static boolean authenticateUser(String username, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            CallableStatement cstmt = conn.prepareCall("{CALL authenticate_user(?, ?, ?)}");
            cstmt.setString(1, username);
            cstmt.setString(2, password);
            cstmt.registerOutParameter(3, Types.TINYINT);
            cstmt.execute();
            int isValid = cstmt.getInt(3);
            return (isValid == 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Création d'utilisateur via la procédure stockée create_user
    public static boolean createUser(String username, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE PROCEDURE IF NOT EXISTS create_user(IN user VARCHAR(50), IN pass VARCHAR(50)) "
                + "BEGIN "
                + "INSERT INTO users (username, password) VALUES (user, pass); "
                + "END");
            CallableStatement cstmt = conn.prepareCall("{CALL create_user(?, ?)}");
            cstmt.setString(1, username);
            cstmt.setString(2, password);
            cstmt.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Mise à jour du mot de passe via la procédure stockée update_password
    public static boolean updateUser(String username, String newPassword) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            CallableStatement cstmt = conn.prepareCall("{CALL update_password(?, ?)}");
            cstmt.setString(1, username);
            cstmt.setString(2, newPassword);
            cstmt.execute();
            return (cstmt.getUpdateCount() > 0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Suppression d'un utilisateur via la procédure stockée delete_user
    public static boolean deleteUser(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            CallableStatement cstmt = conn.prepareCall("{CALL delete_user(?)}");
            cstmt.setString(1, username);
            cstmt.execute();
            return (cstmt.getUpdateCount() > 0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Récupération de tous les utilisateurs (ici via un SELECT direct)
    public static List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT username FROM users");
            while(rs.next()){
                users.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}
