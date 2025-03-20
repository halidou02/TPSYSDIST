package org.emp.gl;

public enum SMTPState {
    START,            // État initial, en attente de HELO/EHLO
    HELO_RECEIVED,    // Après réception de la commande HELO/EHLO
    MAIL_FROM,        // Après réception de MAIL FROM
    RCPT_TO_RECEIVED, // Après réception d’au moins un RCPT TO
    DATA,             // Après commande DATA, en cours de réception du contenu du message
    MESSAGE_RECEIVED, // Après la fin du message (ligne contenant uniquement ".")
    QUIT_RECEIVED     // Après la commande QUIT
}
