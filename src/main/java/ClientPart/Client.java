package ClientPart;

import ServerPart.AuthService;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client implements Runnable
{
    Socket socket;
    DataInputStream in;
    DataOutputStream out;
    boolean cont;          // переменная, отображающая состояние авторизации.

    final String IP_ADRESS = "localhost";
    final int PORT = 8189;

    public Client() throws IOException {
        this.socket = new Socket(IP_ADRESS, PORT);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.cont = false;
    }

    @Override
    public void run()
    {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Runnable r = ()-> {
                System.out.println("client on");
                while (true)
                {
                    Scanner sc = new Scanner(System.in);
                    String str = sc.nextLine();
                    String[] tokens = str.split(" ");
                    try
                    {
                        if (!cont)    // аиторизация.
                        {
                            out.write(9);
                            short loginSize = (short) tokens[0].length();
                            out.writeShort(loginSize);
                            out.write(tokens[0].getBytes());
                            short passSize = (short) tokens[1].length();
                            out.writeShort(passSize);
                            out.write(tokens[1].getBytes());

                        } else
                            {
                                // запись файла.
                                if (tokens[0].equalsIgnoreCase("/w"))
                                {
//                                    TempFileMessage tmp = new TempFileMessage(Paths.get("ClientFiles/" + tokens[1]));
//                                    out.write(15);
//                                    short fileNameSize = (short) tmp.getFileName().length();
//                                    out.writeShort(fileNameSize);
//                                    out.write(tmp.getFileName().getBytes());
//                                    out.writeLong(tmp.getSize());
//                                    out.write(tmp.getBytes());
                                    out.write(15);
                                    short fileNameSize = (short) tokens[1].length();
                                    out.writeShort(fileNameSize);
                                    out.write(tokens[1].getBytes());
                                    out.writeLong(new File("ClientFiles/" + tokens[1]).length());
                                    byte[] buf = new byte[1024];
                                    int n;
                                    try(InputStream inFile = new FileInputStream("ClientFiles/" + tokens[1]))
                                    {
                                        while ((n = inFile.read(buf)) != -1)
                                        {
                                            out.write(buf, 0, n);
                                        }
                                    }

                                }
                                // удаление файла.
                                else if (tokens[0].equalsIgnoreCase("/d"))
                                {
                                    out.write(17);
                                    short delFileNameSize = (short) tokens[1].length();
                                    out.writeShort(delFileNameSize);
                                    out.write(tokens[1].getBytes());

                                }
                                // переименование файла.
                                else if (tokens[0].equalsIgnoreCase("/rn"))
                                {
                                    out.write(19);
                                    short srcFileSize = (short) tokens[1].length();
                                    out.writeShort(srcFileSize);
                                    out.write(tokens[1].getBytes());
                                    short dstFileSize = (short) tokens[2].length();
                                    out.writeShort(dstFileSize);
                                    out.write(tokens[2].getBytes());
                                }
                                // окончание работы.
                                else if (tokens[0].equalsIgnoreCase("/end"))
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
                }
        };
        executor.submit(r);
        executor.shutdown();

            try
            {
                while (true)
                {
                    int x = in.read();
                    if (x == 1)
                    {
                        System.out.println("wrong");
                    }
                    else if (x == 2)
                    {
                        cont = true;  // если авторизация прошла удачно.
                        System.out.println("right");
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

   public static void main(String[] args) throws IOException
   {
       new Thread(new Client()).start();
   }
}
