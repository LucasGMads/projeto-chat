package projetochat;

public class ChatServerApp {
    public static void main(String[] args) {
        int port = 5000;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        ChatServer server = new ChatServer(port);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stopServer));
    }
}
