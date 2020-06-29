package arhinserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientHandler implements Runnable {
    private Server server;
    private PrintWriter outMessage;
    private BufferedReader inMessage;
    private static final String HOST = "localhost";
    private static final int PORT = 9999;

    private Socket clientSocket = null;

    public String nickname = null;
    public Boolean connected = false;

    public ClientHandler(Socket socket, Server server) {
        try {
            this.server = server;
            this.clientSocket = socket;
            this.outMessage = new PrintWriter(socket.getOutputStream(), true);
            this.inMessage = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        listen();
    }

    void listen() {
        try {
            while (true) {
                String msg = inMessage.readLine();
                server.print(msg);
                if (msg.startsWith("Login:")) {
                    String nick = msg.split(":", 2)[1];
                    login(nick);
                } else if (msg.equals("Logout")) {
                    logout();
                } else if (msg.startsWith("PublicMessage:")) {

                    for (ClientHandler client : server.clients) {
                        if (client.connected) {
                            client.send("From: " + nickname + " : " + msg.split(":", 3)[1]);
                        }
                    }
                } else if (msg.startsWith("PM:")) {
                    String[] lmsg = msg.split(":", 3);
                    String to = lmsg[1];
                    String message = lmsg[2];
                    boolean success = false;
                    for (ClientHandler client : server.clients) {
                        if (client.nickname.equals(to)) {
                            client.send("PMFrom: " + nickname + " : " + message);
                            success = true;
                        }
                        else if(client.nickname.equals(nickname)){
                            client.send("PMTo: " + nickname + " : " + message);
                        }
                    }
                    if (!success) {
                        send("Error! Client not found!");
                    }
                } else {
                    send(msg);
                }

            }
        } catch (SocketException e) {
            if (connected) {
                try {
                    logout();
                    server.clients.remove(this);
                    outMessage.close();
                    inMessage.close();
                    clientSocket.close();
                } catch (Exception d) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void login(String nick) {
        if (connected) {
            send("Already Connected!");
            return;
        }
        boolean exists = false;
//        System.out.println("Login: " + nick);
        for (ClientHandler client : server.clients) {
            if (client.nickname != null && client.nickname.equals(nick)) {
                exists = true;
                break;
            }
        }
        if (exists) {
            send("Nickname already exist!");
        } else {
            nickname = nick;
            connected = true;
            server.sendMessageToAllClients(nickname + " is connected!");
        }
        server.upd_userlist();
    }

    void logout() {
        if (connected) {
            connected = false;
            server.sendMessageToAllClients(nickname + " is disconnected!");
            nickname = null;
        } else {
            outMessage.println("Not Logged in !!");
        }
        server.upd_userlist();
    }

    synchronized void send(String msg) {
        outMessage.println(msg);
    }
}
