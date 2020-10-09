package ServerPart;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Server
{
    private Vector<ClientHandler> clients;  // список клиентов.

    public Server()
    {
        ServerSocket server = null;
        Socket socket = null;

        try
        {
            AuthService.connect();             // связь с БД.
            server = new ServerSocket(8189);
            System.out.println("Сервер запущен. Ожидаем подключения... .");
            clients = new Vector<>();

            while (true)
            {
                socket = server.accept();
                System.out.println("Клиент подключился.");
                new ClientHandler(this, socket);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            try
            {
                socket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            try
            {
                server.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            AuthService.disconnect();
        }
    }

    // добавление клиента в список.
    public void subscribe(ClientHandler client)
    {
        clients.add(client);
        System.out.println("Клиент " + client.getNick() + " авторизовался.");
    }

    // удаление клиента из списка.
    public void unsubscribe(ClientHandler client)
    {
        System.out.println("Клиент " + client.getNick() + " устранился.");
        clients.remove(client);
    }


    // метод, проверяющий наличие пользователя
    public boolean isNickAlready(String nick)
    {
        for (ClientHandler o: clients)
        {
            if(o.getNick().equals(nick))
            {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args)
    {
//        Server serv = new Server();
        new Server();
    }
}
