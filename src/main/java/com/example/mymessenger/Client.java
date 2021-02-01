package com.example.mymessenger;

import com.example.mymessenger.network.Connection;
import com.example.mymessenger.network.models.Message;
import com.example.mymessenger.network.models.MessageType;
import com.example.mymessenger.tools.ConsoleHelper;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

/**
 * Console client
 * */
public class Client {

    protected Connection connection;
    private volatile boolean clientConnected = false;
    private String userName ="test";


    /** PSVM Client **/
    public static void main(String[] args) {

        Client client = new Client();
        client.run();
    }


    /** Methods **/
    /** run **/
    public void run() {

        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        // Wait while not notify from another thread
        try {
            synchronized (this) {
                this.wait();
            }
        } catch (InterruptedException e) {
            ConsoleHelper.writeMessage("Error!");
            return;
        }

        //After notification, check clientConnected
        if (clientConnected) {
            ConsoleHelper.writeMessage("The connection is established. To exit, type the command 'exit'.");

            while (clientConnected) {
                String message;
                if (!(message = ConsoleHelper.readString()).equalsIgnoreCase("exit")) {
                    if (shouldSentTextFromConsole()) {
                        sendTextMessage(message);
                    }
                } else {
                    return;
                }
            }
        }
        else {
            ConsoleHelper.writeMessage("An error occurred while the client was running.");
        }
    }


    protected String getServerAddress() {

        ConsoleHelper.writeMessage("Enter the server address:");
        return ConsoleHelper.readString();
    }


    protected int getServerPort() {

        ConsoleHelper.writeMessage("Enter the server port:");
        return ConsoleHelper.readInt();
    }


    protected String getUserName() {

        ConsoleHelper.writeMessage("Enter the user name:");
        return ConsoleHelper.readString();
    }


    protected String getPassword() {

        ConsoleHelper.writeMessage("Enter your password:");
        return ConsoleHelper.readString();
    }


    protected boolean shouldSentTextFromConsole() {

        return true;
    }


    protected SocketThread getSocketThread() {

        return new SocketThread();
    }


    protected void sendTextMessage(String text) {

        try {
            connection.send(new Message(MessageType.TEXT, userName, new Date(), text));

        } catch (IOException e) {
            ConsoleHelper.writeMessage("Sending error");
            clientConnected = false;
        }
    }


    /** SocketThread **/
    public class SocketThread extends Thread {

        /** Methods **/
        public void run() {

            try {
                Socket socket = new Socket(getServerAddress(), getServerPort());

                Client.this.connection = new Connection(socket);


                clientHandshake();
                clientMainLoop();


            } catch (IOException e) {
                notifyConnectionStatusChanged(false);
            } catch (ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }

        }


        protected void clientMainLoop() throws IOException, ClassNotFoundException {

            while (true) {
                Message message = connection.receive();

                switch (message.getType()) {

                    case TEXT:
                        processIncomingMessage(message.getUserName(), message.getText());
                        break;

                    case USER_ADDED:
                        informAboutAddingNewUser(message.getUserName());
                        break;

                    case USER_REMOVED:
                        informAboutDeletingNewUser(message.getUserName());
                        break;

                    default:
                        throw new IOException("Unexpected MessageType");
                }
            }
        }


        protected void clientHandshake() throws IOException, ClassNotFoundException {

            while (true) {

                Message message = connection.receive();

                switch (message.getType()) {

                    case NAME_REQUEST: {

                        userName = getUserName();
                        connection.send(new Message(MessageType.USER_NAME, userName, new Date(), userName));
                        break;
                    }

                    case PASSWORD_REQUEST: {

                        String password = getPassword();
                        connection.send(new Message(MessageType.PASSWORD, userName, new Date(), password));
                        break;
                    }

                    case PASSWORD_ACCEPTED: break;

                    case NAME_ACCEPTED: {

                        notifyConnectionStatusChanged(true);
                        return;
                    }

                    default: {
                        throw new IOException("Unexpected MessageType");
                    }
                }
            }
        }


        protected void processIncomingMessage(String userName, String message) {
            ConsoleHelper.writeMessage(userName +": "+message);
        }


        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage("participant " + userName + " joined the chat");
        }


        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage("participant " + userName + " left the chat");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {


            Client.this.clientConnected = clientConnected;

            synchronized (Client.this) {
                Client.this.notify();
            }
        }
    }
}