
import java.sql.*;

// класс для работы с БД.
public class AuthService
{
    private static Connection connection;
    private static Statement statement;

    // подключение БД.
    public static void connect()
    {
        try
        {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:mainDB");
            statement = connection.createStatement();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    // регистрация нового User.
    public static int registrationClient(String login, String pass)
    {
        int result = 0;
        String sql = String.format("insert into myProject (login, password) values ('%s', '%s')", login, pass);
        try
        {
            result = statement.executeUpdate(sql);
        }
        catch (SQLException throwables)
        {
//            throwables.printStackTrace();
            System.out.println(login + " is exists.");
        }
        return result;
    }

    // получение данных всех пользователей.
    public static ResultSet listClientData()  {
        ResultSet rs = null;
        try
        {
            rs = statement.executeQuery("select * from myProject");
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
        return rs;
    }

    // авторизация пользователя.
    public static String getNickByLogAndPass(String login, String pass)
    {
        String sql = String.format("select login from myProject where login = '%s' and password = '%s'", login, pass);
        try
        {
            ResultSet rs = statement.executeQuery(sql);
            if(rs.next())
            {
                return rs.getString(1);
            }
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
        return null;
    }

    // завершение работы с БД.
    public static void disconnect()
    {
        try
        {
            connection.close();
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
    }

    public static Statement getStatement()
    {
        return statement;
    }
}
