package ClientPart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TempFileMessage
{
    private String fileName;
    private long size;
    private byte[] bytes;

    public TempFileMessage(Path path)
    {
        try
        {
            this.fileName = path.getFileName().toString();
            this.size = Files.size(path);
            this.bytes = Files.readAllBytes(path);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
