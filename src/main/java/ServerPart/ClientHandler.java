package ServerPart;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

            new Thread(new Runnable() {
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
                                short loginSize = in.readShort();
                                byte[] loginBytes = new byte[loginSize];
                                in.read(loginBytes);
                                String login = new String(loginBytes);
                                short passSize = in.readShort();
                                byte[] passBytes = new byte[passSize];
                                in.read(passBytes);
                                String pass = new String(passBytes);
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
                            // пердача файла.
                            else if(x == 15)
                            {
                                short fileNameSize = in.readShort();
                                byte[] fileNameBytes = new byte[fileNameSize];
                                in.read(fileNameBytes);
                                String fileName = new String(fileNameBytes);
                                long fileSize = in.readLong();
//                                try(OutputStream out = new BufferedOutputStream(new FileOutputStream("ServerFiles/" + nick + "/" + fileName)))
                               try (OutputStream outFile = new BufferedOutputStream(new FileOutputStream(dir + "/" + fileName)))
                               {
                                   for(int i = 0; i <fileSize; i++)
                                   {
                                       outFile.write(in.read());
                                   }
                               }
                            }
                            // удаление файла.
                            else if (x == 17)
                            {
                                short delFileNameSize = in.readShort();
                                byte[] delFileNameBytes = new byte[delFileNameSize];
                                in.read(delFileNameBytes);
                                String delFileName = new String(delFileNameBytes);
                                Files.deleteIfExists(Paths.get("ServerFiles/" + nick + "/" + delFileName));
                            }
                            // переименование файла.
                            else  if (x == 19)
                            {
                                short srcFileNameSize = in.readShort();
                                byte[] srcFileNameBytes = new byte[srcFileNameSize];
                                in.read(srcFileNameBytes);
                                String srcFileName = new String(srcFileNameBytes);
                                short dstFileNameSize = in.readShort();
                                byte[] dstFileNameBytes = new byte[dstFileNameSize];
                                in.read(dstFileNameBytes);
                                String dstFileName = new String(dstFileNameBytes);
//                                Files.copy(Paths.get("ServerFiles/" + nick + "/" + srcFileName), Paths.get("ServerFiles/" + nick + "/" + dstFileName));
//                                Files.delete(Paths.get("ServerFiles/" + nick + "/" + srcFileName));
                                Files.move(Paths.get("ServerFiles/" + nick + "/" + srcFileName), Paths.get("ServerFiles/" + nick + "/" + dstFileName));
                            }
                            // окончание работы клиента.
                            else if (x == 99)
                            {
//                                out.write(99);
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


    public String getNick()
    {
        return nick;
    }
}
