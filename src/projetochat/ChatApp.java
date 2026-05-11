package projetochat;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ChatApp implements ChatClient.MessageListener {
    private final String windowTitle;
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField nameField;
    private JTextField hostField;
    private JTextField portField;
    private JTextField inputField;
    private JLabel statusLabel;
    private JButton connectButton;
    private JButton serverButton;
    private JButton sendButton;
    private JButton fileButton;
    private ChatClient client;
    private ChatServer server;

    public ChatApp() {
        this("Chat de Inspetores do Rio Tietê");
    }

    public ChatApp(String windowTitle) {
        this.windowTitle = windowTitle;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatApp app1 = new ChatApp("Chat de Inspetores - Janela 1");
            app1.createAndShowGui();
            ChatApp app2 = new ChatApp("Chat de Inspetores - Janela 2");
            app2.createAndShowGui();
        });
    }

    private void createAndShowGui() {
        frame = new JFrame(windowTitle);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(720, 520);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(8, 8));

        frame.add(createTopPanel(), BorderLayout.NORTH);
        frame.add(createChatPanel(), BorderLayout.CENTER);
        frame.add(createBottomPanel(), BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(8, 8, 0, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Nome:"), gbc);

        nameField = new JTextField("Inspetor", 12);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Host:"), gbc);

        hostField = new JTextField("localhost", 12);
        gbc.gridx = 3;
        panel.add(hostField, gbc);

        gbc.gridx = 4;
        panel.add(new JLabel("Porta:"), gbc);

        portField = new JTextField("5000", 6);
        gbc.gridx = 5;
        panel.add(portField, gbc);

        connectButton = new JButton("Conectar");
        connectButton.addActionListener(e -> connectToServer());
        gbc.gridx = 6;
        panel.add(connectButton, gbc);

        serverButton = new JButton("Iniciar servidor");
        serverButton.addActionListener(e -> toggleServer());
        gbc.gridx = 7;
        panel.add(serverButton, gbc);

        JButton newWindowButton = new JButton("Nova janela");
        newWindowButton.addActionListener(e -> openNewWindow());
        gbc.gridx = 8;
        panel.add(newWindowButton, gbc);

        statusLabel = new JLabel("Status: desconectado");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 8;
        panel.add(statusLabel, gbc);

        return panel;
    }

    private JScrollPane createChatPanel() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        return new JScrollPane(chatArea);
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(0, 8, 8, 8));

        inputField = new JTextField();
        panel.add(inputField, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        sendButton = new JButton("Enviar");
        sendButton.addActionListener(e -> sendMessage());
        buttons.add(sendButton);

        fileButton = new JButton("Enviar arquivo");
        fileButton.addActionListener(e -> sendFile());
        buttons.add(fileButton);

        buttons.add(createEmoticonButton("🙂"));
        buttons.add(createEmoticonButton("🚨"));
        buttons.add(createEmoticonButton("📎"));
        buttons.add(createEmoticonButton("📝"));

        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    private JButton createEmoticonButton(String emoticon) {
        JButton button = new JButton(emoticon);
        button.addActionListener(e -> {
            inputField.setText(inputField.getText() + emoticon);
            inputField.requestFocusInWindow();
        });
        return button;
    }

    private void connectToServer() {
        if (client != null && client.isConnected()) {
            client.disconnect();
            return;
        }

        String name = nameField.getText().trim();
        String host = hostField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());

        client = new ChatClient(host, port, name.isEmpty() ? "Inspetor" : name, this);
        try {
            client.connect();
            statusLabel.setText("Status: conectado a " + host + ":" + port);
            connectButton.setText("Desconectar");
        } catch (IOException ex) {
            appendChat("[Sistema] Não foi possível conectar: " + ex.getMessage());
        }
    }

    private void toggleServer() {
        if (server == null) {
            int port = Integer.parseInt(portField.getText().trim());
            server = new ChatServer(port);
            server.start();
            appendChat("[Sistema] Servidor iniciado na porta " + port);
            serverButton.setText("Parar servidor");
        } else {
            server.stopServer();
            server = null;
            appendChat("[Sistema] Servidor parado.");
            serverButton.setText("Iniciar servidor");
        }
    }

    private void openNewWindow() {
        SwingUtilities.invokeLater(() -> {
            ChatApp newApp = new ChatApp("Chat de Inspetores - Janela " + System.currentTimeMillis());
            newApp.createAndShowGui();
        });
    }

    private void sendMessage() {
        if (client == null || !client.isConnected()) {
            appendChat("[Sistema] Conecte-se antes de enviar mensagens.");
            return;
        }
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        client.sendMessage(text);
        inputField.setText("");
    }

    private void sendFile() {
        if (client == null || !client.isConnected()) {
            appendChat("[Sistema] Conecte-se antes de enviar arquivos.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        int option = chooser.showOpenDialog(frame);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            client.sendFile(file);
        }
    }

    private void appendChat(String text) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(text + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    @Override
    public void onMessage(Message message) {
        appendChat(message.format());
    }

    @Override
    public void onFileMessage(FileMessage fileMessage) {
        String notice = String.format("[Arquivo] %s enviou %s (%d bytes).", fileMessage.getSender(), fileMessage.getFileName(), fileMessage.getContent().length);
        appendChat(notice);
        saveReceivedFile(fileMessage);
    }

    @Override
    public void onStatusChanged(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            if (!connected) {
                statusLabel.setText("Status: desconectado");
                connectButton.setText("Conectar");
            }
        });
    }

    private void saveReceivedFile(FileMessage fileMessage) {
        try {
            File downloads = new File(System.getProperty("user.home"), "Downloads");
            if (!downloads.exists()) {
                downloads.mkdirs();
            }
            File target = new File(downloads, fileMessage.getFileName());
            try (FileOutputStream fos = new FileOutputStream(target)) {
                fos.write(fileMessage.getContent());
            }
            appendChat("[Sistema] Arquivo salvo em: " + target.getAbsolutePath());
        } catch (IOException e) {
            appendChat("[Sistema] Falha ao salvar arquivo recebido: " + e.getMessage());
        }
    }
}
