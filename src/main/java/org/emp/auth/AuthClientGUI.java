package org.emp.auth;

import javax.swing.*;
import javax.swing.border.EmptyBorder; // Pour ajouter des marges
import java.awt.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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

    private AuthService authService; // Référence au service RMI

    public AuthClientGUI() {
        super("Gestion des Utilisateurs (RMI/MySQL)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Appliquer le Look and Feel Nimbus pour une apparence plus moderne
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Si Nimbus n'est pas disponible, utiliser le L&F par défaut du système
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        connectToRmiService();
        initComponents(); // Initialiser les composants après avoir défini le L&F
        refreshUserList();
        pack(); // Ajuster la taille de la fenêtre au contenu
        setLocationRelativeTo(null); // Centrer la fenêtre
        setVisible(true);
    }

    private void connectToRmiService() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            authService = (AuthService) registry.lookup("AuthService");
            if (authService == null) {
                showError("Impossible de trouver le service RMI 'AuthService'.");
                System.exit(1); // Quitter si le service n'est pas trouvé
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur de connexion au service RMI: " + e.getMessage());
            System.exit(1); // Quitter en cas d'erreur RMI
        }
    }

    private void initComponents() {
        // Utiliser un panneau principal avec une bordure pour l'espacement
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10)); // Espacement horizontal et vertical
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); // Marges intérieures

        // Panneau pour les champs de saisie et boutons d'action immédiate (GridBagLayout)
        JPanel inputActionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Espacement autour des composants
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username Label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE; // Ne pas étirer le label
        inputActionPanel.add(new JLabel("Utilisateur :"), gbc);

        // Username Field
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0; // Permettre au champ de s'étirer horizontalement
        gbc.fill = GridBagConstraints.HORIZONTAL;
        usernameField = new JTextField(15); // Taille préférée
        inputActionPanel.add(usernameField, gbc);
        gbc.weightx = 0; // Réinitialiser

        // Password Label
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        inputActionPanel.add(new JLabel("Mot de passe :"), gbc);

        // Password Field
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        passwordField = new JPasswordField(15);
        inputActionPanel.add(passwordField, gbc);

        // Authenticate Button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        authenticateButton = new JButton("Authentifier");
        inputActionPanel.add(authenticateButton, gbc);

        // Refresh Button
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        refreshButton = new JButton("Rafraîchir Liste");
        inputActionPanel.add(refreshButton, gbc);

        mainPanel.add(inputActionPanel, BorderLayout.NORTH);

        // Liste des utilisateurs
        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(userList);
        // Ajouter un titre à la liste
        scrollPane.setBorder(BorderFactory.createTitledBorder("Utilisateurs Existants"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Panneau pour les boutons de gestion (Créer, Mettre à jour, Supprimer)
        JPanel managementButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5)); // Centré avec espacement
        createButton = new JButton("Créer Utilisateur");
        updateButton = new JButton("Mettre à jour MDP");
        deleteButton = new JButton("Supprimer Utilisateur");
        managementButtonPanel.add(createButton);
        managementButtonPanel.add(updateButton);
        managementButtonPanel.add(deleteButton);

        mainPanel.add(managementButtonPanel, BorderLayout.SOUTH);

        add(mainPanel); // Ajouter le panneau principal à la fenêtre

        // --- Ajout des écouteurs d'événements ---
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
                    passwordField.setText(""); // Effacer le mot de passe lors de la sélection
                    passwordField.requestFocusInWindow(); // Mettre le focus sur le champ MDP
                }
            }
        });
    }

    private void createUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            showError("Le nom d'utilisateur et le mot de passe ne peuvent pas être vides.");
            return;
        }
        try {
            boolean success = authService.createUser(username, password);
            if (success) {
                showMessage("Utilisateur créé avec succès.");
                refreshUserList();
                clearFields();
            } else {
                showError("Création échouée : l'utilisateur existe déjà.");
            }
        } catch (RemoteException ex) {
            handleRemoteException("création", ex);
        }
    }

    private void updateUser() {
        String username = usernameField.getText().trim();
        String newPassword = new String(passwordField.getPassword());
         if (username.isEmpty() || newPassword.isEmpty()) {
            showError("Le nom d'utilisateur et le nouveau mot de passe ne peuvent pas être vides.");
            return;
        }
        try {
            // Confirmation pour la mise à jour
            int confirm = JOptionPane.showConfirmDialog(this, "Confirmer la mise à jour du mot de passe pour " + username + " ?",
                    "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            boolean success = authService.updateUser(username, newPassword); // Utilise updateUser de l'interface
            if (success) {
                showMessage("Mot de passe mis à jour avec succès.");
                clearFields();
            } else {
                showError("Mise à jour échouée : l'utilisateur n'existe pas.");
            }
        } catch (RemoteException ex) {
            handleRemoteException("mise à jour", ex);
        }
    }

    private void deleteUser() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
             showError("Veuillez sélectionner ou entrer un nom d'utilisateur.");
             return;
        }
        try {
            int confirm = JOptionPane.showConfirmDialog(this, "Confirmer la suppression de " + username + " ?",
                    "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = authService.deleteUser(username);
                if (success) {
                    showMessage("Utilisateur supprimé avec succès.");
                    refreshUserList();
                    clearFields();
                } else {
                    showError("Suppression échouée : l'utilisateur n'existe pas.");
                }
            }
        } catch (RemoteException ex) {
            handleRemoteException("suppression", ex);
        }
    }

    private void authenticateUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
         if (username.isEmpty() || password.isEmpty()) {
            showError("Le nom d'utilisateur et le mot de passe ne peuvent pas être vides.");
            return;
        }
        try {
            boolean success = authService.authenticate(username, password); // Utilise authenticate de l'interface
            if (success) {
                showMessage("Authentification réussie.");
            } else {
                showError("Authentification échouée.");
            }
            passwordField.setText(""); // Effacer le mot de passe après tentative
        } catch (RemoteException ex) {
            handleRemoteException("authentification", ex);
        }
    }

    private void refreshUserList() {
        try {
            List<String> users = authService.getAllUsers(); // Utilise getAllUsers de l'interface
            listModel.clear();
            if (users != null) { // Vérifier si la liste n'est pas nulle
                for (String user : users) {
                    listModel.addElement(user);
                }
            } else {
                 showError("La liste des utilisateurs retournée est nulle.");
            }
        } catch (RemoteException ex) {
            handleRemoteException("rafraîchissement de la liste", ex);
            listModel.clear(); // Vider la liste en cas d'erreur
        }
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        userList.clearSelection();
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private void handleRemoteException(String action, RemoteException ex) {
        ex.printStackTrace();
        showError("Erreur RMI pendant l'action '" + action + "': " + ex.getMessage());
    }

    public static void main(String[] args) {
        // Assurer que l'interface est lancée dans l'Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new AuthClientGUI());
    }
}
