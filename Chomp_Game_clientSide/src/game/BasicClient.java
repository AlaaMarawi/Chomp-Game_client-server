/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package game;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import static game.BasicClient.cInStream;
import static game.Game.newGame;
import static game.Game.panel;
import javax.swing.JOptionPane;

/**
 *
 * @author alaam
 */
//server messages reading/listening thread
class Listen extends Thread {

    public void run() {
        //loop as socet is connected
        while (BasicClient.cSocket.isConnected() && !BasicClient.cSocket.isClosed()) {//when game form is closed socket is closed
            try {
                Message rcv = (Message) cInStream.readObject(); // reads/listen comming messages
                String Rname = "Anonymous"; //rivalName
                switch (rcv.type) {//act depending on message type
                    case Name:
                        break;
                    case Welcome://server recieved name and sent welcome msg
                        Game.btn_connect.setEnabled(false);
                        Game.lbl_servermsg.setText(rcv.content.toString());//display msg in label
                        break;
                    case RivalConnected://rival connected/paired
                        Rname = rcv.content.toString();
                        Game.txt_rivalname.setText(Rname);//display rival name
                        Game.txt_myname.setEditable(false);//prevent editing player name
                        Game.lbl_servermsg.setText("Rival Connected, Start Game");
                        Game.lbl_rivalStatus.setText("connected");
                        break;
                    case Disconnect: //rival  disconnected
                        if (Game.turn == -1) {//in case game is finished  or didnt start do nothing
                            break;
                        }
                        Game.lbl_rivalStatus.setText("disconnected");
                        Game.lbl_servermsg.setText("rival disconnectd");
                        Game.txt_rivalname.setText("");
                        //show option pane to ask for new game
                        Object[] options = {"New Game"};
                        int selected = JOptionPane.showOptionDialog(panel, "Rival Disconnected..", "Rival Disconnected", JOptionPane.DEFAULT_OPTION,
                                JOptionPane.WARNING_MESSAGE, null, options, options[0]);

                        if (selected == 0 || selected == JOptionPane.CLOSED_OPTION) {// if ok pressed : open new game, search for a rival
                            newGame();
                            Message newStart = new Message(Message.Message_Type.Start);//start new game(search for a pair)
                            try {
                                BasicClient.cOutStream.writeObject(newStart);
                            } catch (IOException ex) {
                                Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        break;
                    case Text://contains server messages to client
                        Game.ThisGame.lbl_servermsg.setText(rcv.content.toString());//display in the bottom label
                        break;
                    case Pressed://contains rival press action as (i,j)
                        Game.ThisGame.rivalPress(rcv.content.toString());//reflect rival press on my frame
                        break;
                    case Turn://server descided who begins
                        if (rcv.content.equals(1)) { //it is my turn
                            Game.lbl_turn.setText("YOU");
                            Game.turn = 1;
                        } else if (rcv.content.equals(0)) { //it is rival turn
                            Game.lbl_turn.setText("RIVAL");
                            Game.turn = 0;
                        }
                        break;
                    case End://rival ended(lost) the game
                        Game.youWon();
                        break;
                }

            } catch (IOException ex) {
                //                BasicClient.cSocket.close();
                //  Logger.getLogger(BasicClient.class.getName()).log(Level.SEVERE, null, ex);

            } catch (ClassNotFoundException ex) {
                //                BasicClient.cSocket.close();
                Logger.getLogger(BasicClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}

public class BasicClient {

    public static Socket cSocket; //new socket for every new client
    public static ObjectInputStream cInStream;   //object to recieve data
    public static ObjectOutputStream cOutStream;  //object to send data
    public static Listen listenMe;    //thread to liten to server

    public static void Start(String ip, int port) {
        try {

            BasicClient.cSocket = new Socket(ip, port);
            System.out.println("Connected to server __ " + cSocket.toString());
            Game.lbl_servermsg.setText("player connected");

            cInStream = new ObjectInputStream(BasicClient.cSocket.getInputStream());// input stream
            cOutStream = new ObjectOutputStream(BasicClient.cSocket.getOutputStream());// output stream

            //start with sending name to server
            Message msg = new Message(Message.Message_Type.Name);
            if (!Game.txt_myname.getText().isEmpty()) {
                msg.content = Game.txt_myname.getText();

            } else {
                msg.content = "Anonymous";
            }

            BasicClient.cOutStream.writeObject(msg);//write to server
            BasicClient.listenMe = new Listen();//new thread to listen
            BasicClient.listenMe.start();//start listening for server messages
        } catch (IOException ex) {
            Logger.getLogger(BasicClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
