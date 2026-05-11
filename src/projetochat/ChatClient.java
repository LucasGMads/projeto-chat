package projetochat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ChatClient {
    private final String host;
    private final int port;
    private final String name;
    private final MessageListener listener;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread readerThread;
    private volatile boolean connected = false;

    public ChatClient(String host, int port, String name, MessageListener listener) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.listener = listener;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        connected = true;
        listener.onMessage(new Message("Sistema", "Conectado a " + host + ":" + port + "."));

        readerThread = new Thread(this::readLoop, "ClientReader");
        readerThread.start();
    }

    private void readLoop() {
        try {
            while (connected && socket != null && !socket.isClosed()) {
                Object received = in.readObject();
                if (received instanceof Message) {
                    listener.onMessage((Message) received);
                } else if (received instanceof FileMessage) {
                    listener.onFileMessage((FileMessage) received);
                }
            }
        } catch (Exception e) {
            if (connected) {
                listener.onMessage(new Message("Sistema", "Conexão perdida: " + e.getMessage()));
            }
        } finally {
            disconnect();
        }
    }

    public void sendMessage(String text) {
        if (!connected) {
            listener.onMessage(new Message("Sistema", "Não está conectado ao servidor."));
            return;
        }
        try {
            Message message = new Message(name, text);
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            listener.onMessage(new Message("Sistema", "Falha ao enviar mensagem: " + e.getMessage()));
        }
    }

    public void sendFile(File file) {
        if (!connected) {
            listener.onMessage(new Message("Sistema", "Não está conectado ao servidor."));
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = fis.readAllBytes();
            FileMessage fileMessage = new FileMessage(name, file.getName(), data);
            out.writeObject(fileMessage);
            out.flush();
            listener.onMessage(new Message("Sistema", "Arquivo enviado: " + file.getName()));
        } catch (IOException e) {
            listener.onMessage(new Message("Sistema", "Erro ao enviar arquivo: " + e.getMessage()));
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {
        }
        try {
            if (out != null) out.close();
        } catch (IOException ignored) {
        }
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
        listener.onStatusChanged(false);
    }

    public boolean isConnected() {
        return connected;
    }

    public interface MessageListener {
        void onMessage(Message message);

        void onFileMessage(FileMessage fileMessage);

        void onStatusChanged(boolean connected);
    }
}
