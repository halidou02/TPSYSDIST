package org.emp.auth;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface AuthService extends Remote {
    boolean authenticate(String username, String password) throws RemoteException;
    boolean createUser(String username, String password) throws RemoteException;
    boolean updateUser(String username, String newPassword) throws RemoteException;
    boolean deleteUser(String username) throws RemoteException;
    boolean userExists(String username) throws RemoteException;
    List<String> getAllUsers() throws RemoteException; // Nouvelle méthode pour récupérer tous les utilisateurs

}
