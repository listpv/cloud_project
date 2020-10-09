package ServerPart;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.ResultSet;
import java.sql.SQLException;

// класс, отвечающий за работу сервера с клиентом.
public class ClientHandler
{
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Server server;
    private String nick;   // имя папки клиента, соответствующее его логину.
    private Path dir;      // директория , где нахолится папка клиента. Особого смысла нет!!!

    public ClientHandler(Server server, Socket socket) {
        try
        {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        while (true)
                        {
                            int x = in.read();
                            // авторизация.
                            if(x == 9)
                            {
                                authorization();
                            }
                            // передача файла.
                            else if(x == 15)
                            {
                                gettingFile();
                            }
                            // удаление файла.
                            else if (x == 17)
                            {
                                deleteFile();

                            }
                            // переименование файла.
                            else  if (x == 19)
                            {
                                renameFile();
                            }
                            // отправка файла.
                            else if (x == 21)
                            {
                                sendingFile();
                            }
                            //  передача содержимого репозитория клиента.
                            else if(x == 23)
                            {
                                fileListToClient();

                            }
                            // окончание работы клиента.  для варианта с двумя потоками  !!!
                            else if (x == 99)
                            {
                                out.write(99);
                                break;
                            }

                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        try
                        {
                            in.close();
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                        try
                        {
                            out.close();
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                        try
                        {
                            socket.close();
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                        server.unsubscribe(ClientHandler.this);
                    }

                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // авторизация.
    public void authorization() throws IOException
    {
        short loginSize = in.readShort();
        byte[] loginBytes = new byte[loginSize];
        in.read(loginBytes);
        String login = new String(loginBytes);
        short passSize = in.readShort();
        byte[] passBytes = new byte[passSize];
        in.read(passBytes);
        String pass = new String(passBytes);
        if(server.isNickAlready(login))  //  если такой клиент уже в работе.
        {
            out.write(11);
        }
        else
        {
            String newNick = AuthService.getNickByLogAndPass(login, pass);
            if(newNick == null) // неуспешно.
            {
                out.write(1);
            }
            else   // успешно.
            {
                nick = newNick;
                dir = Files.createDirectories(Paths.get("ServerFiles", nick));
                server.subscribe(ClientHandler.this);  // клиент вносится в список.
                out.write(2);
            }
        }
    }

    // передача файла.
    public void gettingFile() throws IOException
    {
        short fileNameSize = in.readShort();
        byte[] fileNameBytes = new byte[fileNameSize];
        in.read(fileNameBytes);
        String fileName = new String(fileNameBytes);
        long fileSize = in.readLong();
//        try(OutputStream out = new BufferedOutputStream(new FileOutputStream("ServerFiles/" + nick + "/" + fileName)))
        try (OutputStream outFile = new BufferedOutputStream(new FileOutputStream(dir + "/" + fileName)))
        {
            for(int i = 0; i <fileSize; i++)
            {
                outFile.write(in.read());
            }
        }
//        out.write(5);       //   для варианта с одним потоком  !!!
    }

    // удаление файла.
    public void deleteFile() throws IOException
    {
        short delFileNameSize = in.readShort();
        byte[] delFileNameBytes = new byte[delFileNameSize];
        in.read(delFileNameBytes);
        String delFileName = new String(delFileNameBytes);
        Files.deleteIfExists(Paths.get("ServerFiles/" + nick + "/" + delFileName));
//        out.write(5);       //   для варианта с одним потоком !!!
    }

    // переименование файла.
    public void renameFile() throws IOException
    {
        short srcFileNameSize = in.readShort();
        byte[] srcFileNameBytes = new byte[srcFileNameSize];
        in.read(srcFileNameBytes);
        String srcFileName = new String(srcFileNameBytes);
        short dstFileNameSize = in.readShort();
        byte[] dstFileNameBytes = new byte[dstFileNameSize];
        in.read(dstFileNameBytes);
        String dstFileName = new String(dstFileNameBytes);
//        Files.copy(Paths.get("ServerFiles/" + nick + "/" + srcFileName), Paths.get("ServerFiles/" + nick + "/" + dstFileName));
//        Files.delete(Paths.get("ServerFiles/" + nick + "/" + srcFileName));
        Files.move(Paths.get("ServerFiles/" + nick + "/" + srcFileName), Paths.get("ServerFiles/" + nick + "/" + dstFileName));
//        out.write(5);       //   для варианта с одним потоком  !!!
    }

    // отправка файла
    public void sendingFile() throws IOException
    {
        short sendFileNameSize = in.readShort();
        byte[] sendFileNameBytes = new byte[sendFileNameSize];
        in.read(sendFileNameBytes);
        String sendFileName = new String(sendFileNameBytes);
        out.write(21);
        out.writeShort(sendFileNameSize);
        out.write(sendFileNameBytes);
        out.writeLong(Files.size(Paths.get("ServerFiles/" + nick + "/" + sendFileName)));
        byte[] buf = new byte[1024];
        int n;
        try (InputStream inFile = new FileInputStream("ServerFiles/" + nick + "/" + sendFileName))
        {
            while ((n = inFile.read(buf)) != -1)
            {
                out.write(buf, 0, n);
            }
        }
    }

    //  передача содержимого репозитория клиента
    public void fileListToClient() throws IOException
    {
        out.write(23);
        int sumFiles = new File(String.valueOf(dir)).listFiles().length;
        out.write(sumFiles);
        Files.walkFileTree(dir, new FileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                String fileName = file.getFileName().toString();
                short fileNameSize = (short) fileName.length();
                out.writeShort(fileNameSize);
                out.write(fileName.getBytes());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.TERMINATE;
            }
        });
    }




    public String getNick()
    {
        return nick;
    }
}
