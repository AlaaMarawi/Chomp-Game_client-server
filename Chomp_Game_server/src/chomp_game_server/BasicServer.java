/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chomp_game_server;

import game.Message;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alaam
 */
class ListenServerThread extends Thread {

    private BasicServer server;

    // client listen start function
    public void run() {
        try {
            while (!this.server.sSocket.isClosed()) {
// private Socket :
                Socket cSocket = this.server.sSocket.accept();// accept client and return its socket
                sClient newClient = new sClient(cSocket);//make new sClient instance
                server.clientList.add(newClient); //add it to client list
                System.out.println("new client connected: client no:" + newClient.no + " | Socket: " + newClient.socket);
             
            }
        } catch (IOException ex) {
            Logger.getLogger(sClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

public class BasicServer {

    /**
     * @param args the command line arguments
     */
    public static ServerSocket sSocket; 
    public static Socket cSocket;
    public static ArrayList<sClient> clientList;
    //semaphore instance (for lock _2 player):
    public static Semaphore pairLock = new Semaphore(1, true); //1 thread (client can be in),true= FIFO
    public static ListenServerThread ServerThread;

    public static void startServer(int port) throws IOException {//start server by given port
        sSocket = new ServerSocket(port);//initiate server socket
        System.out.println("server has been set for port: " + port);
        clientList = new ArrayList<sClient>();

        BasicServer.ServerThread = new ListenServerThread();
        BasicServer.ServerThread.start(); //start thread to listen for/accept new clients
        System.out.println("server has been started");
    }

    public static void StopServer() throws IOException {
        sSocket.close();
        System.out.println("server has been stopped");
    }

    public static void sendMessage(game.Message msg, sClient destClient) throws IOException {
        //  System.out.println("destClient:" + destClient.no + "|" + destClient.socket);
        destClient.sendMessage(msg);
    }

}
