package proxy.serverimpl;

import certificate.CertificateProvider;
import config.serverimpl.ServerConfigManager;
import crypto.encoding.Utf8;
import crypto.encryption.Aes;
import crypto.encryption.Rsa;
import crypto.encryption.objs.AesKey;
import handshake.ServerHandshakeController;
import proxy.RequestHandler;
import utils.ByteArrayUtil;
import utils.Log;
import utils.http.HttpUtil;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerRequestHandler extends RequestHandler implements Runnable {
    public ServerRequestHandler(Socket clientSocket) {
        super(clientSocket);
    }

    record ApplicationDataParts(byte[] keyEncrypted,byte[] ivEncrypted, byte[] dataEncrypted) {
    }


    private ApplicationDataParts extractApplicationDataParts(byte[] data){
        int keyEncryptedLength=
                ByteArrayUtil.getInt32FromByteArray(Arrays.copyOfRange(data,0,4));
        int ivEncryptedLength=
                ByteArrayUtil.getInt32FromByteArray(Arrays.copyOfRange(data,4,8));

        var keyEncrypted=Arrays.copyOfRange(data,8,8+keyEncryptedLength);
        var ivEncrypted=Arrays.copyOfRange(
                data,8+keyEncryptedLength,8+keyEncryptedLength+ivEncryptedLength);

        var dataEncrypted=Arrays.copyOfRange(
                data,8+keyEncryptedLength+ivEncryptedLength,data.length);

        return new ApplicationDataParts(keyEncrypted,ivEncrypted,dataEncrypted);
    }

    private boolean decryptApplicationKey(
            ApplicationDataParts applicationDataParts,byte[] privateKey){
        byte[] key= Rsa.decrypt(applicationDataParts.keyEncrypted(),privateKey);

        byte[] iv=Rsa.decrypt(applicationDataParts.ivEncrypted(),privateKey);

        if(key==null||iv==null){
            return false;
        }

        this.applicationKey=new AesKey(key,iv);

        return true;
    }

    private void encryptAndSendToClient(byte[] data) throws IOException {
        this.clientSocket.getOutputStream().write(
                Aes.encrypt(data, this.applicationKey));
        this.clientSocket.getOutputStream().flush();
    }

    private byte[] decryptDataFromClient(byte[] data) {
        return Aes.decrypt(data, this.applicationKey);
    }

    @Override
    public void run() {
        Log.info("Sending server certificate to client");

        ServerHandshakeController handshakeController = new ServerHandshakeController(this.clientSocket);

        if(!handshakeController.sendCertificate()){
            Log.error("Failed to send server certificate");
            this.closeClientSocket();
            return;
        }

        // Receive encrypted client request data
        byte[] clientData = new byte[8 * 1024 * 1024];
        try {
            int clientDataLength = this.clientSocket.getInputStream().read(clientData);
            clientData = Arrays.copyOf(clientData, clientDataLength);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return;
        }

        var clientApplicationDataParts=
                this.extractApplicationDataParts(clientData);
        if(!this.decryptApplicationKey(
                clientApplicationDataParts, CertificateProvider.getInstance().getPrivateKey())){
            Log.error("Failed to decrypt application key with private key");
            this.closeClientSocket();
            return;
        }

        clientData=Aes.decrypt(clientApplicationDataParts.dataEncrypted(),this.applicationKey);

        // Replace host field in request header
        String newHost=ServerConfigManager.getProxyPass();
        var replaceHostResult =
                HttpUtil.replaceRequestHeaderHost(newHost,clientData);
        if (replaceHostResult.originalHost() == null) {
            Log.error("Host not found in request header");
            this.closeClientSocket();
            return;
        }

        Log.info(String.format("Replaced request header Host [%s] with [%s]",
                replaceHostResult.originalHost(), newHost));

        var serverPort = HttpUtil.extractHostPort(newHost);

        this.connectToServer(serverPort.host(), serverPort.port());

        if (this.serverSocket == null) {
            Log.error("Cannot connect to host");
            this.closeClientSocket();
            return;
        }

        // Forward request data to server
        try {
            this.serverSocket.getOutputStream().write(replaceHostResult.newRequestData());
            this.serverSocket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        Log.info("Sent request data to local server");

        // Receive response data and send encrypted data to client
        List<byte[]> headerBytes=new ArrayList<>();
        byte[] responseData = new byte[64 * 1024];
        int responseDataLength;
        byte[] actualResponseData;

        try {
            while(!Utf8.encode(ByteArrayUtil.concat(headerBytes)).contains("\r\n\r\n")){
                responseDataLength = this.serverSocket.getInputStream().read(responseData);
                actualResponseData=Arrays.copyOf(responseData, responseDataLength);
                headerBytes.add(actualResponseData);
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        actualResponseData=ByteArrayUtil.concat(headerBytes);
        responseDataLength = actualResponseData.length;
        // Determine response data transmission type
        String actualResponseString = Utf8.encode(actualResponseData);

        try {
            int contentLength = HttpUtil.getContentLength(actualResponseData);

            // Response transmission type: Chunked
            // Receive data in loop until encounter "\r\n\0\r\n"
            if (contentLength == -1) {
                Log.info("Response transmission type: Chunked");

                this.encryptAndSendToClient(actualResponseData);

                int syncLength=this.clientSocket.getInputStream().read(new byte[2]);
                if (syncLength != 1) {
                    throw new IOException("Client sync data not of length 1");
                }

                while (!Utf8.encode(actualResponseData).contains("\r\n\0\r\n")) {
                    responseDataLength = this.serverSocket.getInputStream().read(responseData);
                    actualResponseData = Arrays.copyOf(responseData, responseDataLength);
                    this.encryptAndSendToClient(actualResponseData);

                    syncLength=this.clientSocket.getInputStream().read(new byte[2]);
                    if (syncLength != 1) {
                        throw new IOException("Client sync data not of length 1");
                    }
                }
            }
            // Response transmission type: With Content-Length
            // Receive data of size contentLength
            else {
                Log.info("Response transmission type: With Content-Length");

                int bodyStartIndex = actualResponseString.indexOf("\r\n\r\n") + 4;

                if(bodyStartIndex==3){
                    throw new IOException("Response header terminator not found");
                }

                int receivedDataLength = responseDataLength - bodyStartIndex;

                this.encryptAndSendToClient(actualResponseData);

                int syncLength=this.clientSocket.getInputStream().read(new byte[2]);
                if (syncLength != 1) {
                    throw new IOException("Client sync data not of length 1");
                }

                while (receivedDataLength < contentLength) {
                    responseDataLength = this.serverSocket.getInputStream().read(responseData);
                    actualResponseData = Arrays.copyOf(responseData, responseDataLength);
                    this.encryptAndSendToClient(actualResponseData);
                    receivedDataLength += responseDataLength;

                    syncLength=this.clientSocket.getInputStream().read(new byte[2]);
                    if (syncLength != 1) {
                        throw new IOException("Client sync data not of length 1");
                    }
                }
            }

            // Send finishing signal
            this.clientSocket.getOutputStream().write(new byte[1]);
            this.clientSocket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        Log.success("All response data transmitted to client");

        this.closeBothSockets();
    }
}
