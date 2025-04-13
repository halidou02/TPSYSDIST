package org.emp.auth;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AuthServer {
    public static void main(String[] args) {
        try {
            // Création de l'instance du service d'authentification
            AuthService authService = new AuthServiceImpl();
            // Démarrage du registre RMI sur le port 1099 (par défaut)
            Registry registry = LocateRegistry.createRegistry(1099);
            // Liaison de l'objet distant dans le registre
            registry.rebind("AuthService", authService);
            System.out.println("AuthService est démarré et lié dans le registre RMI");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
