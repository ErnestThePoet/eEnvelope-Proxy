package proxy.serverimpl;

import config.serverimpl.ServerConfigManager;
import proxy.EeProxy;
import utils.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerEeProxy implements EeProxy {
    @Override
    public void start(int port) {
        try(ServerSocket proxyServerSocket=new ServerSocket(port)) {
            Log.info(String.format(
                    "Successfully started e-Envelope Proxy in SERVER mode, port %d",
                    ServerConfigManager.getPort()));

            while(true){
                Socket clientSocket;

                try {
                    clientSocket=proxyServerSocket.accept();
                } catch (IOException e) {
                    Log.error(e.getMessage());
                    continue;
                }

                new Thread(new ServerRequestHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
