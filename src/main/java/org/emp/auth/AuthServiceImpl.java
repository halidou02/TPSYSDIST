package org.emp.auth;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONException;

public class AuthServiceImpl extends UnicastRemoteObject implements AuthService {
    private static final String USERS_FILE = "users.json";
    private Map<String, String> users;

    protected AuthServiceImpl() throws RemoteException {
        super();
        users = new HashMap<>();
        loadUsers();
    }

    private synchronized void loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            JSONObject json = new JSONObject(sb.toString());
            for (String key : json.keySet()){
                users.put(key, json.getString(key));
            }
        } catch(IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private synchronized void saveUsers() {
        JSONObject json = new JSONObject(users);
        try (FileWriter writer = new FileWriter(USERS_FILE)) {
            writer.write(json.toString(4)); // affichage formaté
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized boolean authenticate(String username, String password) throws RemoteException {
        if (users.containsKey(username)) {
            return users.get(username).equals(password);
        }
        return false;
    }

    @Override
    public synchronized boolean createUser(String username, String password) throws RemoteException {
        if (users.containsKey(username)) {
            return false;
        }
        users.put(username, password);
        saveUsers();
        return true;
    }

    @Override
    public synchronized boolean updateUser(String username, String newPassword) throws RemoteException {
        if (!users.containsKey(username)) {
            return false;
        }
        users.put(username, newPassword);
        saveUsers();
        return true;
    }

    @Override
    public synchronized boolean deleteUser(String username) throws RemoteException {
        if (!users.containsKey(username)) {
            return false;
        }
        users.remove(username);
        saveUsers();
        return true;
    }
    @Override
    public synchronized boolean userExists(String username) throws RemoteException {
        return users.containsKey(username);
    }

    @Override
    public List<String> getAllUsers() throws RemoteException {
        return List.of();
    }

}
