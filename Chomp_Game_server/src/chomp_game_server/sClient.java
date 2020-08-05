/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chomp_game_server;

import game.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.Thread.sleep;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alaam
 */
public class sClient {

    public class ListenClientThread extends Thread {

        private sClient client;

        public ListenClientThread(sClient gClient) {
            this.client = gClient;

        }

        // function to listen for client
        public void run() {
            try {
                while (this.client.socket.isConnected()) {

                    Message rcvMsg = (Message) this.client.cInStream.readObject();
                    //bahave as msg type :
                    switch (rcvMsg.type) {
                        case Name: //player sent his name
                            client.name = rcvMsg.content.toString();
                            //sending welcome message to the new player
                            Message p1msj = new Message(Message.Message_Type.Welcome);
                            p1msj.content = (String) "welcome " + this.client.name + " " + this.client.no + " _ Waiting for Rival";
                            this.client.sendMessage(p1msj);
                            // after recieving name, start pairing
                            client.pairThread.start();
                            break;
                        case Start: //player needs a new game

                            client.paired = false;
                            client.rival = null;
                            client.pairThread = new PairingThread(client);//re-initiate its thread to be able restart it
                            client.pairThread.start();//start pairing(looking for a pair)
                            break;
                        case Disconnect:// player disconnected
                            if (client.rival != null) {
                                BasicServer.sendMessage(rcvMsg, client.rival);//forward to rival
                                this.client.rival.rival = null;
                            }

                            if (!this.client.socket.isClosed()) {//if its socket is not closed->close it
                                this.client.socket.close();
                            }

                            BasicServer.clientList.remove(client);//remove disconnected client form client list
                            System.out.println("player " + client.no + " disconnected");
                            return;
                        case Text:
                            break;
                        case Pressed:
                            //rival pressed button's koordinates has been sent:
                            //System.out.println("pressed i,j : " + rcvMsg.content.toString());
                            BasicServer.sendMessage(rcvMsg, client.rival);//forward to rival
                            break;
                        case End: //loser sends End message
                            System.out.println("player " + client.name + " finished the game");
                            BasicServer.sendMessage(rcvMsg, client.rival);//forward to rival

                            break;
                    }

                }
            } catch (IOException ex) {
                try { //come here when client disconnect
                    this.client.socket.close();
                } catch (IOException ex1) {
                  //  Logger.getLogger(sClient.class.getName()).log(Level.SEVERE, null, ex1);
                }
                BasicServer.clientList.remove(client);
                //Logger.getLogger(sClient.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                try {
                    this.client.socket.close();
                } catch (IOException ex1) {
                  //  Logger.getLogger(sClient.class.getName()).log(Level.SEVERE, null, ex1);
                }
                BasicServer.clientList.remove(client);
                //Logger.getLogger(sClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public static int clientNo = 0;
    int no;
    public String name = "Anonymous";
    Socket socket;
    sClient rival; //rival player
    ObjectInputStream cInStream;
    ObjectOutputStream cOutStream;
    ListenClientThread listenThread; //clientten gelenleri dinleme threadi
    PairingThread pairThread; // pairing thread (2player)
    public boolean paired = false; // pairing status

    public sClient(Socket gelenSocket) {
        sClient.clientNo++;
        this.no = sClient.clientNo;
        this.socket = gelenSocket;

        try {
            this.cOutStream = new ObjectOutputStream(this.socket.getOutputStream());
            this.cInStream = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(sClient.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.listenThread = new ListenClientThread(this);
        this.listenThread.start();//start thread to listen for clients' messages
        this.pairThread = new PairingThread(this);//initiate pairing thread

    }

    public void sendMessage(game.Message message) throws IOException {//send message to client
        this.cOutStream.writeObject(message);
    }

    //every client has its own pairing thread
    class PairingThread extends Thread {

        sClient TheClient;

        PairingThread(sClient TheClient) {
            this.TheClient = TheClient;
        }

        public void run() {
            //loop as socket is connected and has no pair
            while (TheClient.socket.isConnected() && TheClient.paired == false) {
                try {
                    //lock mechanism :only  one client can be in,
                    // others must wait him to releas
                    BasicServer.pairLock.acquire(1); //lock

                    if (!TheClient.paired) { //if player is not paired
                        sClient cRival = null;
                        //loop until get paired
                        while (cRival == null && TheClient.socket.isConnected()) {
                            //looking for a pair in the list
                            for (sClient clnt : BasicServer.clientList) {
                                if (TheClient != clnt && clnt.rival == null) {
                                    //then can be paired as rivals and start game
                                    cRival = clnt;
                                    cRival.paired = true;
                                    cRival.rival = TheClient;
                                    TheClient.rival = cRival;
                                    TheClient.paired = true;
                                    break;
                                }
                            }
                            // sleep thread for 1 second
                            sleep(1000);
                        }
                        //pairing proccess is done
                        //send pairing message to both
                        //descide turn and send to both
                        //start game
                        try {
                            Message msg1 = new Message(Message.Message_Type.RivalConnected);
                            msg1.content = TheClient.name;
                            BasicServer.sendMessage(msg1, TheClient.rival);
                            Message msg2 = new Message(Message.Message_Type.RivalConnected);
                            msg2.content = TheClient.rival.name;
                            BasicServer.sendMessage(msg2, TheClient);
                            //send turn :
                            msg1 = new Message(Message.Message_Type.Turn);
                            msg1.content = 1;// it's your turn
                            BasicServer.sendMessage(msg1, TheClient);
                            msg2 = new Message(Message.Message_Type.Turn);
                            msg1.content = 0;// it's not your turn
                            BasicServer.sendMessage(msg1, TheClient.rival);

                        } catch (IOException ex) {
                            Logger.getLogger(sClient.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                    //release the lock, otherwise,deadlock will occure
                    BasicServer.pairLock.release(1);
                } catch (InterruptedException ex) {
                    Logger.getLogger(PairingThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
