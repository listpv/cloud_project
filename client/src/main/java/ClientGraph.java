import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class ClientGraph extends JFrame
{
    Socket socket;
    DataInputStream in;
    DataOutputStream out;
    boolean cont;          // переменная, отображающая состояние авторизации.
    Scanner sc;
    String loginName;

    // Два списка и котейнеры для их содержимого.
    DefaultListModel<String> clientListModel = new DefaultListModel();
    DefaultListModel <String> serverListModel = new DefaultListModel();
    JList clientList;
    JList serverList;

    // Основные панели, для авторизации и для основной части.
    JPanel authPanel;
    JPanel basicPanel;


    // кнопки основной панели.
    JButton uploadButton;
    JButton deleteButton;
    JButton downloadButton;
    JButton renameButton;

    // элементы панели авторизации.
    JTextField loginField;
    JButton authButton;
    JTextArea commentArea;


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

            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    System.out.println("Авторизация: login1 pass1, login2 pass2, login3 pass3, login4 pass4, login5 pass5." );

                    try
                    {
                        while (true)
                        {

                            int x = in.read();
                            // если авторизация не прошла.
                            if (x == 1)
                            {
                                commentArea.setText("Wrong personal data.");
                                loginField.requestFocus();
                                authButton.setEnabled(false);
                                loginName = null;
                            }
                            // если такой пользователь уже активен.
                            else if (x == 11)
                            {
                                commentArea.setText(loginName + " is already in process.");
                                loginField.requestFocus();
                                authButton.setEnabled(false);
                                loginName = null;
                            }
                            // если авторизация прошла успешно.
                            else if (x == 2)
                            {
                                cont = true;  // если авторизация прошла удачно.
                                Files.createDirectories(Paths.get("ClientFiles", loginName));
                                setTitle(loginName);
                                authPanel.setVisible(false);
                                basicPanel.setVisible(true);
                                filesOnClient();
                                out.write(23);
                            }
                            // получение и запись загруженного с сервера файла в репозиторий клиента.
                            else if (x == 21)
                            {
                                writeFile();
                                filesOnClient();
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
    }

    public void prepareGUI() {
        // Параметры окна
        setBounds(600, 300, 600, 600);
        setTitle("Клиент");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);


        authPanel = new JPanel(new BorderLayout());
        // составляющие панели авторизации.
        JPanel upAuthPanel = new JPanel(new FlowLayout());
        commentArea = new JTextArea();
        commentArea.setEditable(false);
        JLabel loginLabel = new JLabel("Enter login");
        loginField = new JTextField();
        JLabel passwordLabel = new JLabel("Enter password");
        loginField.setPreferredSize(new Dimension(80, 20));
        JPasswordField passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(80, 20));
        authButton = new JButton("Auth");
        upAuthPanel.add(loginLabel);
        upAuthPanel.add(loginField);
        upAuthPanel.add(passwordLabel);
        upAuthPanel.add(passwordField);
        upAuthPanel.add(authButton);
        authButton.setEnabled(false);
        authPanel.add(upAuthPanel, BorderLayout.NORTH);
        authPanel.add(commentArea, BorderLayout.CENTER);
        add(authPanel, BorderLayout.NORTH);
        loginField.requestFocus();

        basicPanel = new JPanel(new GridLayout(1, 3));
        // составляющие основной панели.
        clientList = new JList(clientListModel);
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serverList = new JList(serverListModel);
        serverList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel clientPanel = new JPanel(new BorderLayout());  // 1-я часть.
        JPanel serverPanel = new JPanel(new BorderLayout());  // 2-я часть.
        JLabel clientRepo = new JLabel("Client repo");
        JLabel serverRepo = new JLabel("Server repo");
        clientPanel.add(clientRepo, BorderLayout.NORTH);
        clientPanel.add(new JScrollPane(clientList), BorderLayout.CENTER);
        serverPanel.add(serverRepo, BorderLayout.NORTH);
        serverPanel.add(new JScrollPane(serverList), BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(null);          // 3-я часть
        uploadButton = new JButton("Upload");
        deleteButton = new JButton("Delete");
        downloadButton = new JButton("Download");
        renameButton = new JButton("Rename");
        uploadButton.setBounds(40,10,100,20);
        deleteButton.setBounds(40,50,100,20);
        downloadButton.setBounds(40, 90,  100, 20);
        renameButton.setBounds(40, 130, 100, 20);
        bottomPanel.add(uploadButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(downloadButton);
        bottomPanel.add(renameButton);
        basicPanel.add(serverPanel);
        basicPanel.add(bottomPanel);
        basicPanel.add(clientPanel);
        add(basicPanel, BorderLayout.CENTER);
        basicPanel.setVisible(false);


        // обработка элементов как для выполнения необходимых функций, так и для максимального предотвращения ошибок.
        loginField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e)
            {
            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                if(loginField.getText().length() == 0 || loginField.getText()== null
                        || passwordField.getPassword().length == 0 || passwordField.getPassword() == null)
                {
                    authButton.setEnabled(false);
                }
                else
                {
                    authButton.setEnabled(true);
                }
            }
        });
        loginField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if(loginField.getText() == null || loginField.getText().length() == 0)
                {
                    commentArea.setText("Enter login.");
                    loginField.requestFocus();
                    return;
                }
                passwordField.requestFocus();
            }
        });
        passwordField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e)
            {
            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                if(loginField.getText().length() == 0 || loginField.getText()== null
                        || passwordField.getPassword().length == 0 || passwordField.getPassword() == null)
                {
                    authButton.setEnabled(false);
                }
                else
                {
                    authButton.setEnabled(true);
                }
            }
        });
        passwordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if(!authButton.isEnabled())
                {
                    commentArea.setText("Enter complete data.");
                    return;
                }
                String str;
                char[] passChar = passwordField.getPassword();
                String passStr = "";
                for(int i = 0; i < passChar.length; i++)
                {
                    passStr = passStr + passChar[i];
                    passChar[i] = '$';
                }
                str = loginField.getText() + " " + passStr;
                loginField.setText("");
                passwordField.setText("");
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
                char[] passChar = passwordField.getPassword();
                String passStr = "";
                for(int i = 0; i < passChar.length; i++)
                {
                    passStr = passStr + passChar[i];
                    passChar[i] = '$';
                }
                str = loginField.getText() + " " + passStr;
                loginField.setText("");
                passwordField.setText("");
                try {
                    authorizationClient(str.split(" "));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            }
        });

        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String str = clientListModel.getElementAt(clientList.getSelectedIndex());
                if(checkServerFile(str))
                {
                    int result = JOptionPane.showConfirmDialog(ClientGraph.this,
                            "File " + str + " i alredy exist. Rewrite it?", "Info", JOptionPane.OK_CANCEL_OPTION);
                    if(result == JOptionPane.CANCEL_OPTION)
                    {
                        return;
                    }
                }
                try {
                    uploadFile1(str);
                    out.write(23);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str = serverListModel.getElementAt(serverList.getSelectedIndex());
                try {
                    deleteFile1(str);
                    out.write(23);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String str = serverListModel.getElementAt(serverList.getSelectedIndex());
                if(checkClientFile(str))
                {
                    int result = JOptionPane.showConfirmDialog(ClientGraph.this,
                            "File " + str + " i alredy exist. Rewrite it?", "Info", JOptionPane.OK_CANCEL_OPTION);
                    if(result == JOptionPane.CANCEL_OPTION)
                    {
                        return;
                    }
                }
                try {
                    downLoadFile1(str);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        renameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String str = serverListModel.getElementAt(serverList.getSelectedIndex());
                String newName = JOptionPane.showInputDialog(ClientGraph.this, "Enter new name");
                if(newName == null || newName.length() == 0)
                {
                    return;
                }
                if(checkServerFile(newName))
                {
                    int result = JOptionPane.showConfirmDialog(ClientGraph.this,
                            "File " + newName + " i alredy exist. Rewrite it?", "Info", JOptionPane.OK_CANCEL_OPTION);
                    if(result == JOptionPane.CANCEL_OPTION)
                    {
                        return;
                    }
                    try {
                        deleteFile1(newName);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
                try {
                    renameFile1(str, newName);
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

    //  выгрузка файла с клиента на сервер.
    public void uploadFile1(String str) throws IOException
    {
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


    // удаление файла на сервере.
    public void deleteFile1(String str) throws IOException {
        out.write(17);
        short delFileNameSize = (short) str.length();
        out.writeShort(delFileNameSize);
        out.write(str.getBytes());
    }


    // переименование файла на сервере.
    public void renameFile1(String str1, String str2) throws IOException
    {
        out.write(19);
        short srcFileSize = (short) str1.length();
        out.writeShort(srcFileSize);
        out.write(str1.getBytes());
        short dstFileSize = (short) str2.length();
        out.writeShort(dstFileSize);
        out.write(str2.getBytes());
    }

    // загрузка файла с сервера на клиент.
    public void downLoadFile1(String str) throws IOException {
        out.write(21);
        short necessaryFileSize = (short) str.length();
        out.writeShort(necessaryFileSize);
        out.write(str.getBytes());
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
        serverListModel.removeAllElements();
        int numberFile = in.read();
        for(int i = 0; i < numberFile; i++)
        {
            short FileNameSize = in.readShort();
            byte[] FileNameBytes = new byte[FileNameSize];
            in.read(FileNameBytes);
            serverListModel.addElement(new String(FileNameBytes));
            validate();              //  приводит интерфейс в соответствие с новыми данными списка.
        }
        int index = serverListModel.size() - 1;
        if(index < 0)
        {
            deleteButton.setEnabled(false);
            renameButton.setEnabled(false);
            downloadButton.setEnabled(false);
        }
        else
        {
            deleteButton.setEnabled(true);
            renameButton.setEnabled(true);
            downloadButton.setEnabled(true);
            serverList.setSelectedIndex(index);
        }

        System.out.println();
    }

    // получение списка файлов от клиента.
    public void filesOnClient()
    {
        clientListModel.removeAllElements();
        for(File file : new File("ClientFiles/" + loginName).listFiles())
        {
            clientListModel.addElement(file.getName());
            validate();
        }
        int index = clientListModel.size() - 1;
        if(index < 0)
        {
            uploadButton.setEnabled(false);
        }
        else
        {
            uploadButton.setEnabled(true);
            clientList.setSelectedIndex(index);
        }
        System.out.println();
    }

    // проверяет на наличие файлы на клиенте.
    public boolean checkClientFile(String str)
    {
        for(int i = 0; i < clientListModel.size(); i++)
        {
            if(clientListModel.getElementAt(i).equals(str))
            {
                return true;
            }
        }
        return false;
    }

    // проверяет на наличие файлы на сервере.
    public boolean checkServerFile(String str)
    {
        for(int i = 0; i < serverListModel.size(); i++)
        {
            if(serverListModel.getElementAt(i).equals(str))
            {
                return true;
            }
        }
        return false;
    }





    public static void main(String[] args) throws IOException
    {
        new ClientGraph();
    }
}
