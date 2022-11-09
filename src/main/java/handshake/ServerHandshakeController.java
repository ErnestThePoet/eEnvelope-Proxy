package handshake;

import crypto.encoding.Utf8;
import certificate.CertificateProvider;
import utils.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ServerHandshakeController {
    private final Socket clientSocket;

    public ServerHandshakeController(Socket clientSocket) {
        super();
        this.clientSocket = clientSocket;
    }

    // returns whether certificate send is successful
    public boolean sendCertificate() {
        // Receive client's hello
        byte[] clientHello = new byte[64];

        try {
            int readLength = this.clientSocket.getInputStream().read(clientHello);
            if(!Utf8.encode(Arrays.copyOf(clientHello,readLength)).equals("HELLO")){
                Log.error("Client Hello invalid");
                this.closeClientSocket();
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return false;
        }

        // Send certificate

        var certificate=CertificateProvider.getInstance().getCertificate();
        if(certificate==null){
            Log.error("Could not get certificate");
            return false;
        }

        try {
            this.clientSocket.getOutputStream().write(certificate);
            this.clientSocket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return false;
        }

        return true;
    }

    private void closeClientSocket() {
        try {
            this.clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
