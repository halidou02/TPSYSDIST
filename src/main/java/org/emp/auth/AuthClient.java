package org.emp.auth;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class AuthClient {
    public static void main(String[] args) {
        try {
            // Connexion au registre RMI
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            AuthService authService = (AuthService) registry.lookup("AuthService");

            Scanner scanner = new Scanner(System.in);
            System.out.println("Entrez la commande (authenticate / create / update / delete) :");
            String command = scanner.nextLine();

            switch (command.toLowerCase()) {
                case "authenticate":
                    System.out.println("Nom d'utilisateur :");
                    String username = scanner.nextLine();
                    System.out.println("Mot de passe :");
                    String password = scanner.nextLine();
                    boolean authResult = authService.authenticate(username, password);
                    System.out.println("Authentification " + (authResult ? "réussie" : "échouée"));
                    break;
                case "create":
                    System.out.println("Nom d'utilisateur :");
                    username = scanner.nextLine();
                    System.out.println("Mot de passe :");
                    password = scanner.nextLine();
                    boolean createResult = authService.createUser(username, password);
                    System.out.println("Création de l'utilisateur " + (createResult ? "réussie" : "échouée"));
                    break;
                case "update":
                    System.out.println("Nom d'utilisateur :");
                    username = scanner.nextLine();
                    System.out.println("Nouveau mot de passe :");
                    password = scanner.nextLine();
                    boolean updateResult = authService.updateUser(username, password);
                    System.out.println("Mise à jour de l'utilisateur " + (updateResult ? "réussie" : "échouée"));
                    break;
                case "delete":
                    System.out.println("Nom d'utilisateur :");
                    username = scanner.nextLine();
                    boolean deleteResult = authService.deleteUser(username);
                    System.out.println("Suppression de l'utilisateur " + (deleteResult ? "réussie" : "échouée"));
                    break;
                default:
                    System.out.println("Commande inconnue");
            }
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
