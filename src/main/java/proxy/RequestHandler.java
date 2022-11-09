package proxy;

import crypto.encryption.objs.AesKey;

import java.io.IOException;
import java.net.Socket;

public abstract class RequestHandler {
    protected Socket clientSocket;
    protected Socket serverSocket;

    protected AesKey applicationKey;

    protected RequestHandler(Socket clientSocket){
        this.clientSocket=clientSocket;
    }

    private void closeSocket(Socket socket){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void closeClientSocket(){
        this.closeSocket(this.clientSocket);
    }

    protected void closeServerSocket(){
        this.closeSocket(this.serverSocket);
    }

    protected void closeBothSockets(){
        this.closeClientSocket();
        this.closeServerSocket();
    }

    protected void connectToServer(String host,int port){
        // Note: use actual port for server side
        try {
            this.serverSocket= new Socket(host,port);
        } catch (IOException e) {
            e.printStackTrace();
            this.serverSocket=null;
        }
    }
}
