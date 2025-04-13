package org.emp.auth;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AuthClientGUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton createButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton authenticateButton;
    private JButton refreshButton;
    private JList<String> userList;
    private DefaultListModel<String> listModel;

    public AuthClientGUI() {
        super("Gestion des Utilisateurs (MySQL)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 400);
        initComponents();
        refreshUserList();
        setVisible(true);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));

        inputPanel.add(new JLabel("Nom d'utilisateur :"));
        usernameField = new JTextField();
        inputPanel.add(usernameField);

        inputPanel.add(new JLabel("Mot de passe :"));
        passwordField = new JPasswordField();
        inputPanel.add(passwordField);

        authenticateButton = new JButton("Authentifier");
        inputPanel.add(authenticateButton);

        refreshButton = new JButton("Rafraîchir");
        inputPanel.add(refreshButton);

        panel.add(inputPanel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(userList);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        createButton = new JButton("Créer");
        updateButton = new JButton("Mettre à jour");
        deleteButton = new JButton("Supprimer");
        buttonPanel.add(createButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        add(panel);

        // Ajout des écouteurs d'événements
        createButton.addActionListener(e -> createUser());
        updateButton.addActionListener(e -> updateUser());
        deleteButton.addActionListener(e -> deleteUser());
        authenticateButton.addActionListener(e -> authenticateUser());
        refreshButton.addActionListener(e -> refreshUserList());

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUser = userList.getSelectedValue();
                if (selectedUser != null) {
                    usernameField.setText(selectedUser);
                }
            }
        });
    }

    private void createUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        boolean success = MySQLProcedureService.createUser(username, password);
        if (success) {
            JOptionPane.showMessageDialog(this, "Utilisateur créé avec succès.");
            refreshUserList();
        } else {
            JOptionPane.showMessageDialog(this, "Création échouée : l'utilisateur existe déjà ou une erreur s'est produite.");
        }
    }

    private void updateUser() {
        String username = usernameField.getText().trim();
        String newPassword = new String(passwordField.getPassword());
        boolean success = MySQLProcedureService.updateUser(username, newPassword);
        if (success) {
            JOptionPane.showMessageDialog(this, "Utilisateur mis à jour.");
        } else {
            JOptionPane.showMessageDialog(this, "Mise à jour échouée : l'utilisateur n'existe pas ou une erreur s'est produite.");
        }
    }

    private void deleteUser() {
        String username = usernameField.getText().trim();
        int confirm = JOptionPane.showConfirmDialog(this, "Confirmer la suppression de " + username + " ?",
                "Confirmation", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = MySQLProcedureService.deleteUser(username);
            if (success) {
                JOptionPane.showMessageDialog(this, "Utilisateur supprimé.");
                refreshUserList();
            } else {
                JOptionPane.showMessageDialog(this, "Suppression échouée : l'utilisateur n'existe pas ou une erreur s'est produite.");
            }
        }
    }

    private void authenticateUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        boolean success = MySQLProcedureService.authenticateUser(username, password);
        if (success) {
            JOptionPane.showMessageDialog(this, "Authentification réussie.");
        } else {
            JOptionPane.showMessageDialog(this, "Authentification échouée.");
        }
    }

    private void refreshUserList() {
        List<String> users = MySQLProcedureService.getAllUsers();
        listModel.clear();
        for (String user : users) {
            listModel.addElement(user);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AuthClientGUI());
    }
}
