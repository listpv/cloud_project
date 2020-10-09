package ClientPart;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientTwoThread
{
    Socket socket;
    DataInputStream in;
    DataOutputStream out;
    boolean cont;          // переменная, отображающая состояние авторизации.
    Scanner sc;
    String loginName;

    final String IP_ADRESS = "localhost";
    final int PORT = 8189;

    public ClientTwoThread()
    {
        try
        {
            this.socket = new Socket(IP_ADRESS, PORT);
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.cont = false;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Runnable r = () -> {
                        System.out.println("Авторизация: login1 pass1, login2 pass2, login3 pass3, login4 pass4. \n" +
                                "Запросы (после авторизации): /ul имя_файла - запись файла на сервер; /d имя_файла - удаление файла на сервере; \n" +
                                "/rn имя_исходного_файла требуемое_имя_файла - переименование файла на сервере; \n" +
                                "/ls - получение списка файлов на сервере; /lc - получение списка файлов на клиенте \n" +
                                "/end - окончание работы клиента.");

                        while (true)
                        {
                            sc = new Scanner(System.in);
                            String str = sc.nextLine();
                            String[] tokens = str.split(" ");
                            try
                            {
                                if (!cont)    // авторизация.
                                {
                                    authorizationClient(tokens);

                                } else {
                                    // запись файла на сервер.
                                    if (tokens[0].equalsIgnoreCase("/ul"))
                                    {
                                        uploadFile(tokens);
                                    }
                                    // удаление файла.
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
                                        break;
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    executor.submit(r);
                    executor.shutdown();

                    try
                    {
                        while (true)
                        {
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
                                out.write(23);
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
                            // окончание работы.
                            else if (x == 99)
                            {
                                System.out.println("End client");
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
        out.writeLong(new File("ClientFiles/" + loginName + "/" + tokens[1]).length());
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
        new ClientTwoThread();
    }
}


//    public class Client implements Runnable
//    {
//        Socket socket;
//        DataInputStream in;
//        DataOutputStream out;
//        boolean cont;          // переменная, отображающая состояние авторизации.
//
//        final String IP_ADRESS = "localhost";
//        final int PORT = 8189;
//        public Client() throws IOException
//        {
//            this.socket = new Socket(IP_ADRESS, PORT);
//            this.in = new DataInputStream(socket.getInputStream());
//            this.out = new DataOutputStream(socket.getOutputStream());
//            this.cont = false;
//        }
//
//        @Override
//        public void run ()
//        {
//            ExecutorService executor = Executors.newSingleThreadExecutor();
//            Runnable r = () -> {
//                System.out.println("client on");
//                while (true) {
//                    Scanner sc = new Scanner(System.in);
//                    String str = sc.nextLine();
//                    String[] tokens = str.split(" ");
//                    try {
//                        if (!cont)    // аиторизация.
//                        {
//                            out.write(9);
//                            short loginSize = (short) tokens[0].length();
//                            out.writeShort(loginSize);
//                            out.write(tokens[0].getBytes());
//                            short passSize = (short) tokens[1].length();
//                            out.writeShort(passSize);
//                            out.write(tokens[1].getBytes());
//
//                        } else {
//                            // запись файла.
//                            if (tokens[0].equalsIgnoreCase("/w")) {
////                                    TempFileMessage tmp = new TempFileMessage(Paths.get("ClientFiles/" + tokens[1]));
////                                    out.write(15);
////                                    short fileNameSize = (short) tmp.getFileName().length();
////                                    out.writeShort(fileNameSize);
////                                    out.write(tmp.getFileName().getBytes());
////                                    out.writeLong(tmp.getSize());
////                                    out.write(tmp.getBytes());
//                                out.write(15);
//                                short fileNameSize = (short) tokens[1].length();
//                                out.writeShort(fileNameSize);
//                                out.write(tokens[1].getBytes());
//                                out.writeLong(new File("ClientFiles/" + tokens[1]).length());
//                                byte[] buf = new byte[1024];
//                                int n;
//                                try (InputStream inFile = new FileInputStream("ClientFiles/" + tokens[1])) {
//                                    while ((n = inFile.read(buf)) != -1) {
//                                        out.write(buf, 0, n);
//                                    }
//                                }
//
//                            }
//                            // удаление файла.
//                            else if (tokens[0].equalsIgnoreCase("/d")) {
//                                out.write(17);
//                                short delFileNameSize = (short) tokens[1].length();
//                                out.writeShort(delFileNameSize);
//                                out.write(tokens[1].getBytes());
//
//                            }
//                            // переименование файла.
//                            else if (tokens[0].equalsIgnoreCase("/rn")) {
//                                out.write(19);
//                                short srcFileSize = (short) tokens[1].length();
//                                out.writeShort(srcFileSize);
//                                out.write(tokens[1].getBytes());
//                                short dstFileSize = (short) tokens[2].length();
//                                out.writeShort(dstFileSize);
//                                out.write(tokens[2].getBytes());
//                            }
//                            // окончание работы.
//                            else if (tokens[0].equalsIgnoreCase("/end")) {
//                                out.write(99);
//                                break;
//                            }
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            };
//            executor.submit(r);
//            executor.shutdown();
//
//            try {
//                while (true) {
//                    int x = in.read();
//                    if (x == 1) {
//                        System.out.println("wrong");
//                    } else if (x == 2) {
//                        cont = true;  // если авторизация прошла удачно.
//                        System.out.println("right");
//                    }
//                }
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                try {
//                    socket.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//
//        }
//
//        public static void main(String[] args) throws IOException
//        {
//            new Thread(new Client()).start();
//        }
//    }

