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
    String loginName;      // login клиента, имя папки.

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
    JButton deleteServerButton;
    JButton downloadButton;
    JButton renameServerButton;
    JButton deleteClientButton;
    JButton renameClientButton;

    // элементы панели авторизации.
    JTextField loginField;
    JButton authButton;
    JButton newUserButton;
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
                    System.out.println("Авторизация: login1 pass1, login2 pass2, login3 pass3, login4 pass4, login5 pass5, login6 pass6" );

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
                                newUserButton.setEnabled(false);
                                loginName = null;
                            }
                            // если такой пользователь уже активен.
                            else if (x == 11)
                            {
                                commentArea.setText(loginName + " is already in process.");
                                loginField.requestFocus();
                                authButton.setEnabled(false);
                                newUserButton.setEnabled(false);
                                loginName = null;
                            }
                            // если авторизация прошла успешно.
                            else if (x == 2)
                            {
                                cont = true;  // если авторизация прошла удачно.
                                Files.createDirectories(Paths.get("ClientFiles", loginName));
                                setTitle("User " + loginName);
                                authPanel.setVisible(false);
                                basicPanel.setVisible(true);
                                filesOnClient();
                                out.write(23);
                            }
                            // если не получилось создание нового клиента.
                            else if(x == 3)
                            {
                                commentArea.setText("User " + loginName + " is already exists.");
                                loginField.requestFocus();
                                authButton.setEnabled(false);
                                newUserButton.setEnabled(false);
                                loginName = null;
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

    // создание окна.
    public void prepareGUI() {
        // Параметры окна
        setBounds(600, 300, 600, 600);
        setTitle("User");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // панель авторизации.
        authPanel = new JPanel(new BorderLayout());
        // составляющие панели авторизации.
        JPanel upAuthPanel = new JPanel(new FlowLayout());  // верхняя панель.
        commentArea = new JTextArea();
        commentArea.setEditable(false);
        JLabel loginLabel = new JLabel("Enter login");
        loginField = new JTextField();
        JLabel passwordLabel = new JLabel("Enter password");
        loginField.setPreferredSize(new Dimension(80, 20));
        JPasswordField passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(80, 20));
        authButton = new JButton("Auth");
        newUserButton = new JButton("New User");
        upAuthPanel.add(loginLabel);
        upAuthPanel.add(loginField);
        upAuthPanel.add(passwordLabel);
        upAuthPanel.add(passwordField);
        upAuthPanel.add(authButton);
        upAuthPanel.add(newUserButton);
        authButton.setEnabled(false);
        newUserButton.setEnabled(false);
        authPanel.add(upAuthPanel, BorderLayout.NORTH);
        authPanel.add(commentArea, BorderLayout.CENTER);
        add(authPanel, BorderLayout.NORTH);
        loginField.requestFocus();

        // основная панель
        basicPanel = new JPanel(new GridLayout(1, 3));
        // составляющие основной панели.
        clientList = new JList(clientListModel);
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serverList = new JList(serverListModel);
        serverList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel clientPanel = new JPanel(new BorderLayout());  // 1-я часть со списком клиента.
//        JPanel serverPanel = new JPanel(new BorderLayout());  // 2-я часть со списком сервера.
        JLabel clientRepo = new JLabel("Client repo");
//        JLabel serverRepo = new JLabel("Server repo");
        clientPanel.add(clientRepo, BorderLayout.NORTH);
        clientPanel.add(new JScrollPane(clientList), BorderLayout.CENTER);
        JPanel serverPanel = new JPanel(new BorderLayout());  // 2-я часть со списком сервера.
        JLabel serverRepo = new JLabel("Server repo");
        serverPanel.add(serverRepo, BorderLayout.NORTH);
        serverPanel.add(new JScrollPane(serverList), BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(null);          // 3-я часть с кнопками.
        uploadButton = new JButton("Upload");
        deleteServerButton = new JButton("<<Delete");
        downloadButton = new JButton("Download");
        renameServerButton = new JButton("<<Rename");
        deleteClientButton = new JButton("Delete>>");
        renameClientButton = new JButton("Rename>>");
        downloadButton.setBounds(40, 10,  100, 20);
        deleteServerButton.setBounds(40,50,100,20);
        renameServerButton.setBounds(40, 90, 100, 20);
        uploadButton.setBounds(40,130,100,20);
        deleteClientButton.setBounds(40, 170, 100, 20);
        renameClientButton.setBounds(40, 210, 100, 20);
        bottomPanel.add(uploadButton);
        bottomPanel.add(deleteServerButton);
        bottomPanel.add(renameServerButton);
        bottomPanel.add(downloadButton);
        bottomPanel.add(deleteClientButton);
        bottomPanel.add(renameClientButton);
        basicPanel.add(serverPanel);
        basicPanel.add(bottomPanel);
        basicPanel.add(clientPanel);
        add(basicPanel, BorderLayout.CENTER);
        basicPanel.setVisible(false);           // сначала видна только панель авторизации.


        // обработка элементов панели авторизации.
        // обработка элементов как для выполнения необходимых функций, так и для максимального предотвращения ошибок.
        loginField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e)
            {
            }

            // кнопки панели активации активны только при наличии текста в loginField и passwordField
            @Override
            public void keyReleased(KeyEvent e)
            {
                if(loginField.getText().length() == 0 || loginField.getText()== null
                        || passwordField.getPassword().length == 0 || passwordField.getPassword() == null)
                {
                    authButton.setEnabled(false);
                    newUserButton.setEnabled(false);
                }
                else
                {
                    authButton.setEnabled(true);
                    newUserButton.setEnabled(true);
                }
            }
        });
        // использование клавиши "Enter"
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
        // кнопки панели активации активны только при наличии текста в loginField и passwordField
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
                    newUserButton.setEnabled(false);
                }
                else
                {
                    authButton.setEnabled(true);
                    newUserButton.setEnabled(true);
                }
            }
        });
        // использование клавиши "Enter", можно авторизоваться.
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

        // авторизация.
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

        // регистрация нового пользователя и авторизация, в случае успеха.
        newUserButton.addActionListener(new ActionListener() {
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
                    registrationClient(str.split(" "));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        // обработка элементов основной панели.
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
                    uploadFile(str);
                    out.write(23);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        deleteServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str = serverListModel.getElementAt(serverList.getSelectedIndex());
                int result = JOptionPane.showConfirmDialog(ClientGraph.this,
                        "Do you really want to delete ServerFiles/" + loginName + "/" + str, "Info", JOptionPane.OK_CANCEL_OPTION);
                if(result == JOptionPane.CANCEL_OPTION)
                {
                    return;
                }
                try {
                    deleteFile(str);
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
                            "File " + str + " is alredy exist. Rewrite it?", "Info", JOptionPane.OK_CANCEL_OPTION);
                    if(result == JOptionPane.CANCEL_OPTION)
                    {
                        return;
                    }
                }
                try {
                    downLoadFile(str);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        renameServerButton.addActionListener(new ActionListener() {
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
                            "File ServerFiles/" + loginName + "/" + newName + " is alredy exist. Rewrite it?", "Info", JOptionPane.OK_CANCEL_OPTION);
                    if(result == JOptionPane.CANCEL_OPTION)
                    {
                        return;
                    }
                    try {
                        deleteFile(newName);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
                try {
                    renameFile(str, newName);
                    out.write(23);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            }
        });

        deleteClientButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str = clientListModel.getElementAt(clientList.getSelectedIndex());
                int result = JOptionPane.showConfirmDialog(ClientGraph.this,
                        "Do you really want to delete ClientFiles/" + loginName + "/" + str, "Info", JOptionPane.OK_CANCEL_OPTION);
                if(result == JOptionPane.CANCEL_OPTION)
                {
                    return;
                }
                try
                {
                    Files.deleteIfExists(Paths.get("ClientFiles/" + loginName + "/" + str));
                    filesOnClient();
                }
                catch (IOException ioException)
                {
                    ioException.printStackTrace();
                }
            }

        });
        renameClientButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str = clientListModel.getElementAt(clientList.getSelectedIndex());
                String newName = JOptionPane.showInputDialog(ClientGraph.this, "Enter new name");
                if(newName == null || newName.length() == 0)
                {
                    return;
                }
                if(checkClientFile(newName))
                {
                    int result = JOptionPane.showConfirmDialog(ClientGraph.this,
                            "File ClientFiles/" + loginName + "/" + newName + " is alredy exist. Rewrite it?", "Info", JOptionPane.OK_CANCEL_OPTION);
                    if(result == JOptionPane.CANCEL_OPTION)
                    {
                        return;
                    }
                    try
                    {
                        Files.deleteIfExists(Paths.get("ClientFiles/" + loginName + "/" + newName));
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
                try {
                    Files.move(Paths.get("ClientFiles/" +loginName + "/" + str), Paths.get("ClientFiles/" + loginName + "/" + newName));
                    filesOnClient();
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

    // регистрация нового клиента.
    public void registrationClient(String[] tokens) throws IOException {
        out.write(7);
        loginName = tokens[0];
        short loginSize = (short) tokens[0].length();
        out.writeShort(loginSize);
        out.write(tokens[0].getBytes());
        short passSize = (short) tokens[1].length();
        out.writeShort(passSize);
        out.write(tokens[1].getBytes());
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
    public void uploadFile(String str) throws IOException
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
    public void deleteFile(String str) throws IOException {
        out.write(17);
        short delFileNameSize = (short) str.length();
        out.writeShort(delFileNameSize);
        out.write(str.getBytes());
    }


    // переименование файла на сервере.
    public void renameFile(String str1, String str2) throws IOException
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
    public void downLoadFile(String str) throws IOException {
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

    // получение списка файлов с сервера, здесь же происходит проверка для неактивности
    // соответствующих кнопок при отсутствии файлов в репозитории User на сервере.
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
            deleteServerButton.setEnabled(false);
            renameServerButton.setEnabled(false);
            downloadButton.setEnabled(false);
        }
        else
        {
            deleteServerButton.setEnabled(true);
            renameServerButton.setEnabled(true);
            downloadButton.setEnabled(true);
            serverList.setSelectedIndex(index);
        }

        System.out.println();
    }

    // получение списка файлов от клиента, здесь же происходит проверка для неактивности
    // соответствующих кнопок при отсутствии файлов в репозитории User на клиенте.
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
            deleteClientButton.setEnabled(false);
            renameClientButton.setEnabled(false);
        }
        else
        {
            uploadButton.setEnabled(true);
            deleteClientButton.setEnabled(true);
            renameClientButton.setEnabled(true);
            clientList.setSelectedIndex(index);
        }
        System.out.println();
    }

    // проверяет на наличие файлы в репозитории User на клиенте.
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

    // проверяет на наличие файлы в репозитории User на сервере.
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
