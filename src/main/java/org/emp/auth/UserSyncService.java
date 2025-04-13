package org.emp.auth;

import java.io.*;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONException;

public class UserSyncService implements Runnable {
    private static final String USERS_FILE = "users.json"; // Fichier JSON contenant les utilisateurs
    private static final String MAILSERVER_DIR = "mailserver";

    @Override
    public void run() {
        while (true) {
            List<String> users = getUsersFromJson();
            syncUsers(users);
            try {
                // Synchronisation toutes les minutes
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Lit le fichier JSON et récupère la liste des utilisateurs.
     * Chaque clé du JSON représente un utilisateur.
     */
    private List<String> getUsersFromJson() {
        List<String> users = new ArrayList<>();
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            System.out.println("Fichier " + USERS_FILE + " introuvable.");
            return users;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            JSONObject json = new JSONObject(sb.toString());
            for (String key : json.keySet()) {
                users.add(key);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Pour chaque utilisateur de la liste, vérifie si le répertoire existe.
     * Si le répertoire n'existe pas, il est créé.
     */
    private void syncUsers(List<String> users) {
        File mailDir = new File(MAILSERVER_DIR);
        if (!mailDir.exists()) {
            mailDir.mkdirs();
        }
        for (String user : users) {
            File userDir = new File(mailDir, user);
            if (!userDir.exists()) {
                userDir.mkdirs();
                System.out.println("Création du répertoire pour l'utilisateur : " + user);
            }
        }
    }

    public static void main(String[] args) {
        Thread syncThread = new Thread(new UserSyncService());
        syncThread.start();
        System.out.println("Service de synchronisation des utilisateurs démarré.");
    }
}
