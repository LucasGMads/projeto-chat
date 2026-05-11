package projetochat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChatServer extends Thread {
    private final int port;
    private ServerSocket serverSocket;
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private volatile boolean running = true;

    public ChatServer(int port) {
        this.port = port;
        setName("ChatServer");
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            this.serverSocket = server;
            System.out.println("Servidor iniciado na porta " + port);
            while (running) {
                Socket socket = server.accept();
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                handler.start();
                broadcast(new Message("Servidor", "Novo inspetor conectado."));
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Erro no servidor: " + e.getMessage());
            }
        }
    }

    public void broadcast(Object packet) {
        synchronized (clients) {
            for (ClientHandler client : new HashSet<>(clients)) {
                client.send(packet);
            }
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // ignore
        }
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.close();
            }
            clients.clear();
        }
    }

    private class ClientHandler extends Thread {
        private final Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private volatile boolean connected = true;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            setName("ClientHandler-" + socket.getRemoteSocketAddress());
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                while (connected) {
                    Object received = in.readObject();
                    if (received instanceof Message) {
                        Message message = (Message) received;
                        broadcast(message);
                    } else if (received instanceof FileMessage) {
                        FileMessage fileMessage = (FileMessage) received;
                        broadcast(fileMessage);
                    }
                }
            } catch (Exception e) {
                // cliente desconectou ou erro de rede
            } finally {
                close();
            }
        }

        public void send(Object packet) {
            try {
                if (out != null) {
                    out.writeObject(packet);
                    out.flush();
                }
            } catch (IOException e) {
                close();
            }
        }

        public void close() {
            connected = false;
            clients.remove(this);
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
