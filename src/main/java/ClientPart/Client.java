package ClientPart;

import ServerPart.AuthService;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Client
{
    Socket socket;
    DataInputStream in;
    DataOutputStream out;
    boolean cont;          // переменная, отображающая состояние авторизации.
    Scanner sc;
    String loginName;

    final String IP_ADRESS = "localhost";
    final int PORT = 8189;

    public Client()
    {
        try
        {
            this.socket = new Socket(IP_ADRESS, PORT);
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.cont = false;

            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    System.out.println("client on");
                    try
                    {
                        while (true)
                        {
                            sc = new Scanner(System.in);
                            String str = sc.nextLine();
                            String[] tokens = str.split(" ");
                            if (!cont)  // авторизация
                            {
                                authorizationClient(tokens);

                            }
                            else
                                {
                                    // запись файла на сервер.
                                    if (tokens[0].equalsIgnoreCase("/ul"))
                                    {
                                        uploadFile(tokens);
                                    }
                                    //  удаление файла
                                    else if (tokens[0].equalsIgnoreCase("/d"))
                                    {
                                        deleteFile(tokens);
                                    }
                                    // переименование файла.
                                    else if (tokens[0].equalsIgnoreCase("/rn"))
                                    {
                                        renameFile(tokens);

                                    }
                                    // скачивание файла.
                                    else if (tokens[0].equalsIgnoreCase("/dl"))
                                    {
                                        downloadFile(tokens);
                                    }
                                    // запрос на получение списка файлов на сервере.
                                    else if (tokens[0].equalsIgnoreCase("/ls"))
                                    {
                                        out.write(23);
                                    }
                                    // запрос на получение списка файлов на клиенте.
                                    else if (tokens[0].equalsIgnoreCase("/lc"))
                                    {
                                        filesOnClient();
                                    }
                                    // окончание работы.
                                    else if (tokens[0].equalsIgnoreCase("/end"))
                                    {
                                        out.write(99);
                                        System.out.println("End client");
                                        break;
                                    }
                                }
                            int x = in.read();
                            // если авторизация не прошла.
                            if (x == 1)
                            {
                                System.out.println("wrong");
                                loginName = null;
                            }
                            // если такой пользователь уже активен.
                            else if (x == 11)
                            {
                                System.out.println(loginName + " is already exists.");
                                loginName = null;
                            }
                            // если авторизация прошла успешно.
                            else if (x == 2)
                            {
                                cont = true;  // если авторизация прошла удачно.
                                Files.createDirectories(Paths.get("ClientFiles", loginName));
                                filesOnClient();
                            }
                            // получение и запись загруженного с сервера файла в репозиторий клиента.
                            else if (x == 21)
                            {
                                writeFile();

                            }
                            // получения списка файлов от сервера.
                            else if (x == 23)
                            {
                                filesFromServer();
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
                            socket.close();
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //   авторизация.
    public void authorizationClient(@org.jetbrains.annotations.NotNull String[] tokens) throws IOException
    {
        out.write(9);
        loginName = tokens[0];
        short loginSize = (short) tokens[0].length();
        out.writeShort(loginSize);
        out.write(tokens[0].getBytes());
        short passSize = (short) tokens[1].length();
        out.writeShort(passSize);
        out.write(tokens[1].getBytes());
    }

    //  выгрузка файла с клиента на сервер.
    public void uploadFile(String[] tokens) throws IOException
    {
        out.write(15);
        short fileNameSize = (short) tokens[1].length();
        out.writeShort(fileNameSize);
        out.write(tokens[1].getBytes());
        out.writeLong(new File("ClientFiles/" + loginName+ "/" + tokens[1]).length());
        byte[] buf = new byte[1024];
        int n;
        try (InputStream inFile = new FileInputStream("ClientFiles/"+ loginName + "/" + tokens[1]))
        {
            while ((n = inFile.read(buf)) != -1)
            {
                out.write(buf, 0, n);
            }
        }
    }

    // удаление файла на сервере.
    public void deleteFile(String[] tokens) throws IOException
    {
        out.write(17);
        short delFileNameSize = (short) tokens[1].length();
        out.writeShort(delFileNameSize);
        out.write(tokens[1].getBytes());
    }

    // переименование файла на сервере.
    public void renameFile(String[] tokens) throws IOException
    {
        out.write(19);
        short srcFileSize = (short) tokens[1].length();
        out.writeShort(srcFileSize);
        out.write(tokens[1].getBytes());
        short dstFileSize = (short) tokens[2].length();
        out.writeShort(dstFileSize);
        out.write(tokens[2].getBytes());
    }

    // загрузка файла с сервера на клиент.
    public void downloadFile(String[] tokens) throws IOException
    {
        out.write(21);
        short necessaryFileSize = (short) tokens[1].length();
        out.writeShort(necessaryFileSize);
        out.write(tokens[1].getBytes());
    }

    // получение и запись загруженного с сервера файла в репозиторий клиента.
    public void writeFile() throws IOException
    {
        short fileNameSize = in.readShort();
        byte[] fileNameBytes = new byte[fileNameSize];
        in.read(fileNameBytes);
        String fileName = new String(fileNameBytes);
        long fileSize = in.readLong();
        try (OutputStream outFile = new BufferedOutputStream(new FileOutputStream( "ClientFiles/"+ loginName + "/" + fileName)))
        {
            for(int i = 0; i <fileSize; i++)
            {
                outFile.write(in.read());
            }
        }
    }

    // получение списка файлов с сервера.
    public void filesFromServer() throws IOException
    {
        System.out.print("Server:  ");
        int numberFile = in.read();
        for(int i = 0; i < numberFile; i++)
        {
            short FileNameSize = in.readShort();
            byte[] FileNameBytes = new byte[FileNameSize];
            in.read(FileNameBytes);
            System.out.print(new String(FileNameBytes) + " ");
        }
        System.out.println();
    }

    // получение списка файлов от клиента.
    public void filesOnClient()
    {
        System.out.print("Client:  ");
        for(File file : new File("ClientFiles/" + loginName).listFiles())
        {
            System.out.print(file.getName() + " ");
        }
        System.out.println();
    }


    public static void main(String[] args) throws IOException
    {
        new Client();
    }
}
