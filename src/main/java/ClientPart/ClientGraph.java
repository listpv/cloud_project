package ClientPart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener;

public class ClientGraph extends JFrame
{
    Socket socket;
    DataInputStream in;
    DataOutputStream out;
    boolean cont;          // переменная, отображающая состояние авторизации.
    Scanner sc;
    String loginName;

    private JTextField msgInputField;
    private JTextArea chatArea;
    JPanel authPanel;
    JPanel basicPanel;
    JTextArea serverPart;
    JTextArea clientPart;


    final String IP_ADRESS = "localhost";
    final int PORT = 8189;

    public ClientGraph()
    {
        try
        {
            this.socket = new Socket(IP_ADRESS, PORT);
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.cont = false;

            new Thread(new Runnable() {
                @Override
                public void run()
                {
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
//                                chatArea.append("Wrong");
                                loginName = null;
                            }
                            // если такой пользователь уже активен.
                            else if (x == 11)
                            {
                                System.out.println(loginName + " is already exists.");
//                                chatArea.append(loginName + " is already exists.");
                                loginName = null;
                            }
                            // если авторизация прошла успешно.
                            else if (x == 2)
                            {
                                cont = true;  // если авторизация прошла удачно.
                                Files.createDirectories(Paths.get("ClientFiles", loginName));
                                authPanel.setVisible(false);
                                basicPanel.setVisible(true);
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
                    } catch (Exception exception) {
                        exception.printStackTrace();
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
            prepareGUI();

        }
        catch (IOException e) {
            e.printStackTrace();
        }
//        finally
//        {
//            try
//            {
//                socket.close();
//            }
//            catch (IOException e)
//            {
//                e.printStackTrace();
//            }
//        }
    }

    public void prepareGUI() {
        // Параметры окна
        setBounds(600, 300, 500, 500);
        setTitle("Клиент");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);


        authPanel = new JPanel(new FlowLayout());
        JLabel loginLabel = new JLabel("Enter login");
        JTextField loginField = new JTextField();
        JLabel passwordLabel = new JLabel("Enter password");
        loginField.setPreferredSize(new Dimension(80, 20));
        JPasswordField passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(80, 20));
        JButton authButton = new JButton("Auth");
        add(authPanel, BorderLayout.NORTH);
        authPanel.add(loginLabel);
        authPanel.add(loginField);
        authPanel.add(passwordLabel);
        authPanel.add(passwordField);
        authPanel.add(authButton);
        loginField.requestFocus();

        basicPanel = new JPanel(new GridLayout(1, 3));

        serverPart = new JTextArea(10, 60);
        serverPart.setEditable(false);
        serverPart.setLineWrap(true);
        clientPart = new JTextArea(10, 60);
        clientPart.setEditable(false);
        clientPart.setLineWrap(true);
//        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
        JPanel bottomPanel = new JPanel(null);
        JButton uploadButton = new JButton("Upload");
        JButton deleteButton = new JButton("Delete");
//        uploadButton.setPreferredSize(new Dimension(100, 50));
//        deleteButton.setPreferredSize(new Dimension(100, 50));
        uploadButton.setBounds(10,10,100,20);
        deleteButton.setBounds(10,50,100,20);
        bottomPanel.add(uploadButton);
        bottomPanel.add(deleteButton);
        basicPanel.add(new JScrollPane(serverPart));
        basicPanel.add(new JScrollPane(clientPart));
        basicPanel.add(bottomPanel);
        add(basicPanel, BorderLayout.CENTER);
        basicPanel.setVisible(false);

        // Текстовое поле для вывода сообщений
//        chatArea = new JTextArea();
//        chatArea.setEditable(false);
//        chatArea.setLineWrap(true);
//        add(new JScrollPane(chatArea), BorderLayout.CENTER);

//         Нижняя панель с полем для ввода сообщений и кнопкой отправки сообщений
//        JPanel bottomPanel = new JPanel(new BorderLayout());
//        JButton btnSendMsg = new JButton("Отправить");
//        bottomPanel.add(btnSendMsg, BorderLayout.EAST);
//        msgInputField = new JTextField();
//        add(bottomPanel, BorderLayout.SOUTH);
//        bottomPanel.add(msgInputField, BorderLayout.CENTER);
        loginField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passwordField.requestFocus();
            }
        });
        passwordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String str;
                str = loginField.getText() + " " + passwordField.getText();
                loginField.setText("");
                passwordField.setText("");
                System.out.println(str);
                try {
                    authorizationClient(str.split(" "));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        authButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str;
                str = loginField.getText() + " " + passwordField.getText();
                loginField.setText("");
                passwordField.setText("");
                System.out.println(str);
                try {
                    authorizationClient(str.split(" "));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            }
        });

        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str = clientPart.getSelectedText();
                try {
                    uploadFile1(str);
                    filesOnClient();
                    out.write(23);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str = serverPart.getSelectedText();
                try {
                    deleteFile1(str);
                    filesOnClient();
                    out.write(23);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        // Настраиваем действие на закрытие окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    out.write(99);
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });

        setVisible(true);
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

    public void uploadFile1(String str) throws IOException {
        out.write(15);
        short fileNameSize = (short) str.length();
        out.writeShort(fileNameSize);
        out.write(str.getBytes());
        out.writeLong(new File("ClientFiles/" + loginName + "/" + str).length());
        byte[] buf = new byte[1024];
        int n;
        try (InputStream inFile = new FileInputStream("ClientFiles/"+ loginName + "/" + str))
        {
            while ((n = inFile.read(buf)) != -1)
            {
                out.write(buf, 0, n);
            }
        }
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

    public void deleteFile1(String str) throws IOException {
        out.write(17);
        short delFileNameSize = (short) str.length();
        out.writeShort(delFileNameSize);
        out.write(str.getBytes());
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
        serverPart.setText("");
        int numberFile = in.read();
        for(int i = 0; i < numberFile; i++)
        {
            short FileNameSize = in.readShort();
            byte[] FileNameBytes = new byte[FileNameSize];
            in.read(FileNameBytes);
            System.out.print(new String(FileNameBytes) + " ");
            serverPart.append(new String(FileNameBytes) + "\n");
        }
        System.out.println();
    }

    // получение списка файлов от клиента.
    public void filesOnClient()
    {
        System.out.print("Client:  ");
        clientPart.setText("");
        for(File file : new File("ClientFiles/" + loginName).listFiles())
        {
            System.out.print(file.getName() + " ");
            clientPart.append(file.getName() + "\n");
        }
        System.out.println();
    }


    public static void main(String[] args) throws IOException
    {
        new ClientGraph();
    }
}
