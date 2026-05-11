package projetochat;

import java.io.Serializable;

public class FileMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sender;
    private final String fileName;
    private final byte[] content;

    public FileMessage(String sender, String fileName, byte[] content) {
        this.sender = sender;
        this.fileName = fileName;
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getContent() {
        return content;
    }
}
