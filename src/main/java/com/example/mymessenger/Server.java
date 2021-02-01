package com.example.mymessenger;

import com.example.mymessenger.network.Connection;
import com.example.mymessenger.network.models.Message;
import com.example.mymessenger.tools.ConsoleHelper;
import com.example.mymessenger.network.models.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    // server name
    private static String serverName = "Server";
    // server password
    private static String serverPassword;
    // <UserName, Connection>
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ConsoleHelper.writeMessage("Input server port: ");
        int serverPort = ConsoleHelper.readInt();
        ConsoleHelper.writeMessage("Enter the password to log in to the server: ");
        serverPassword = ConsoleHelper.readString();

        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {

            ConsoleHelper.writeMessage("The server is running");

            while (true) {
                Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                handler.start();
            }
        }

    }


    /**
     * отправка сообщения для всех
     **/
    public static void sendBroadcastMessage(Message message) {

        try {
            ConsoleHelper.writeMessage(message.getUserName() + ": " + message.getText());
            for (Connection connection : connectionMap.values()) {
                connection.send(message);
            }

        } catch (Exception e) {
            e.printStackTrace();
            ConsoleHelper.writeMessage("The message is not sent");
        }

    }


    /**
     * обработчик Handler, в котором будет происходить обмен сообщениями с клиентом
     **/
    private static class Handler extends Thread {

        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }


        @Override
        public void run() {

            ConsoleHelper.writeMessage("A connection is established with the address " + socket.getRemoteSocketAddress());
            String userName = null;
            try (Connection connection = new Connection(socket)) {
                ConsoleHelper.writeMessage("Connecting to a port: " + connection.getRemoteSocketAddress());
                userName = serverHandshake(connection);
                //Рассылать всем участникам чата информацию об имени присоединившегося участника (сообщение с типом USER_ADDED)
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName, new Date()));
                sendListOfUsers(connection, userName);

                serverMainLoop(connection, userName);
            } catch (ClassNotFoundException | IOException e) {
                ConsoleHelper.writeMessage("Error when exchanging data with a remote address");
            }

            //если участник вышел
            connectionMap.remove(userName);
            sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName, new Date()));

            ConsoleHelper.writeMessage("The connection to the remote address is closed");
        }

        /**
         * "Знакомимся" с клиентом
         **/
        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {

            if (passwordRequest(connection)) {
                connection.send(new Message(MessageType.NAME_REQUEST, serverName, new Date()));
                Message message = connection.receive();

                if (message.getType() == MessageType.USER_NAME) {
                    if (message.getText() != null && !message.getText().isEmpty()) {
                        //пользователь с таким именем еще не подключен и имя не занято сервером
                        if (connectionMap.get(message.getText()) == null && !message.getText().equalsIgnoreCase(serverName)) {
                            connectionMap.put(message.getText(), connection);
                            // Отправить клиенту команду информирующую, что его имя принято
                            connection.send(new Message(MessageType.NAME_ACCEPTED, serverName, new Date()));
                            return message.getText();
                        }
                        //иначе сообщаем об этом
                        else
                            connection.send(new Message(MessageType.NAME_NOT_ACCEPTED, serverName, new Date()));
                    }
                }
            }

            //пароль или имя не верные, эмитируем ошибку и разрываем соединение
            throw new IOException();
        }

        private boolean passwordRequest(Connection connection) throws IOException, ClassNotFoundException {
            connection.send(new Message(MessageType.PASSWORD_REQUEST, serverName, new Date()));
            Message message = connection.receive();

            if (message.getType() == MessageType.PASSWORD) {
                if (message.getText().equals(serverPassword)) {
                    connection.send(new Message(MessageType.PASSWORD_ACCEPTED, serverName, new Date()));
                    return true;
                }
            }

            connection.send(new Message(MessageType.PASSWORD_NOT_ACCEPTED, serverName, new Date()));
            return false;
        }


        /**
         * Отправка списка всех пользователей
         **/
        private void sendListOfUsers(Connection connection, String userName) throws IOException {
            for (String key : connectionMap.keySet()) {
                Message message = new Message(MessageType.USER_ADDED, key, new Date());

                if (!key.equals(userName)) {
                    connection.send(message);
                }
            }
        }


        /**
         * Главный цикл обработки сообщений сервером
         **/
        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {

                    Message formattedMessage = new Message(MessageType.TEXT, userName, message.getDate(), message.getText());
                    sendBroadcastMessage(formattedMessage);
                } else {
                    ConsoleHelper.writeMessage("Error");
                }
            }
        }
    }
}
