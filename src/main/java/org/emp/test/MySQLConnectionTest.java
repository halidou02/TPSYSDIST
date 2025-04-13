package org.emp.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLConnectionTest {
    // Connection details for your MySQL database
    private static final String DB_URL = "jdbc:mysql://localhost:3306/maildb?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Ajoute cette ligne pour charger le driver
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            System.out.println("Connexion réussie !");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver JDBC non trouvé !");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors de la connexion : " + e.getMessage());
        }
    }
}
