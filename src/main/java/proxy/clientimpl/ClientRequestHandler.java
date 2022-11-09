package proxy.clientimpl;

import config.clientimpl.ClientConfigManager;
import crypto.encryption.Aes;
import crypto.encryption.Rsa;
import crypto.encryption.objs.AesKey;
import handshake.ClientHandshakeController;
import proxy.RequestHandler;
import utils.ByteArrayUtil;
import utils.Log;
import utils.http.HttpUtil;

import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Arrays;

public class ClientRequestHandler extends RequestHandler implements Runnable {
    // key length(int32), iv length(int32), key and iv
    private byte[] applicationDataHeader;

    public ClientRequestHandler(Socket clientSocket) {
        super(clientSocket);
    }

    private boolean generateApplicationKey(byte[] publicKey){
        SecureRandom secureRandom=new SecureRandom();

        byte[] key=new byte[16];
        byte[] iv=new byte[16];

        secureRandom.nextBytes(key);
        secureRandom.nextBytes(iv);

        this.applicationKey=new AesKey(key,iv);

        var keyEncrypted= Rsa.encrypt(key,publicKey);
        var ivEncrypted= Rsa.encrypt(iv,publicKey);

        if(keyEncrypted==null||ivEncrypted==null){
            return false;
        }

        this.applicationDataHeader= ByteArrayUtil.concat(
                ByteArrayUtil.getByteArrayFromInt32(keyEncrypted.length),
                ByteArrayUtil.getByteArrayFromInt32(ivEncrypted.length),
                keyEncrypted,
                ivEncrypted
        );

        return true;
    }

    // applicationDataHeader must have been set before calling makeApplicationData
    private byte[] makeApplicationData(byte[] data){
        byte[] result=new byte[data.length+this.applicationDataHeader.length];
        System.arraycopy(this.applicationDataHeader,0,
                result,0,this.applicationDataHeader.length);
        System.arraycopy(data,0,result,this.applicationDataHeader.length,data.length);
        return result;
    }

    private void encryptAndSendToServer(byte[] data) throws IOException {
        this.serverSocket.getOutputStream().write(
                this.makeApplicationData(Aes.encrypt(data, this.applicationKey))
        );
        this.serverSocket.getOutputStream().flush();
    }

    private byte[] decryptDataFromServer(byte[] data) {
        return Aes.decrypt(data,this.applicationKey);
    }

    @Override
    public void run() {
        byte[] clientData = new byte[8 * 1024 * 1024];
        try {
            int clientDataLength = this.clientSocket.getInputStream().read(clientData);
            if (clientDataLength == -1) {
                throw new IOException("Read got -1");
            }
            clientData = Arrays.copyOf(clientData, clientDataLength);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return;
        }

        var hostAndShortenPathResult =
                HttpUtil.getRequestHeaderHostAndShortenPath(clientData);

        String host = hostAndShortenPathResult.host();
        String path = hostAndShortenPathResult.newPath();
        clientData = hostAndShortenPathResult.newRequestData();

        if (host == null) {
            Log.error("Cannot get host from request header");
            this.closeClientSocket();
            return;
        }

        if (path == null) {
            Log.warn("Cannot get path from request header");
        }

        if (!ClientConfigManager.isTargetHost(host)) {
            //Log.info("Ignore request to " + host + url);
            this.closeClientSocket();
            return;
        }

        var serverPort = HttpUtil.extractHostPort(host);

        this.connectToServer(serverPort.host(), serverPort.port());

        if (this.serverSocket == null) {
            Log.error("Cannot connect to host for " + host + path);
            this.closeClientSocket();
            return;
        }

        Log.info("Negotiating application key for " + host + path);

        ClientHandshakeController handshakeController
                =new ClientHandshakeController(this.serverSocket);

        var publicKey = handshakeController.getPublicKey();

        if(publicKey==null){
            Log.error("Failed to get server public key for "+host+path);
            this.closeBothSockets();
            return;
        }

        if(!this.generateApplicationKey(publicKey)){
            Log.error(
                    "Failed to encrypt application key with server public key for "+host+path);
            this.closeBothSockets();
            return;
        }

        Log.success("Successfully generated application key for " + host + path
                + " Sending encrypted request data...");

        // Forward encrypted client data
        try {
            this.encryptAndSendToServer(clientData);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        Log.info("Receiving server data and sending back to client for " + host + path);
        // Send back decrypted response data or chunked data
        byte[] serverData = new byte[2 * 1024 * 1024];
        int serverDataLength;

        try {
            while (true) {
                serverDataLength = this.serverSocket.getInputStream().read(serverData);

                if (serverDataLength == -1 || (serverDataLength == 1 && serverData[0] == 0)) {
                    break;
                }

                byte[] actualServerData =
                        this.decryptDataFromServer(Arrays.copyOf(serverData, serverDataLength));

                this.clientSocket.getOutputStream().write(actualServerData);
                this.clientSocket.getOutputStream().flush();

                // Synchronize
                this.serverSocket.getOutputStream().write(new byte[1]);
                this.serverSocket.getOutputStream().flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        Log.success("All data sent back to client for " + host + path);

        this.closeBothSockets();
    }
}
