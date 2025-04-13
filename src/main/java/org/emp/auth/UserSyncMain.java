package org.emp.auth;

public class UserSyncMain {
    public static void main(String[] args) {
        Thread syncThread = new Thread(new UserSyncService());
        syncThread.start();
        System.out.println("User synchronization service started.");
    }
}
