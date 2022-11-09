package handshake;

import crypto.encoding.Utf8;
import certificate.CertificateValidator;
import utils.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ClientHandshakeController {
    private final Socket serverSocket;


    public ClientHandshakeController(Socket serverSocket) {
        super();
        this.serverSocket = serverSocket;
    }

    public byte[] getPublicKey(String host) {
        try {
            this.serverSocket.getOutputStream().write(Utf8.decode("HELLO"));
            this.serverSocket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
            this.closeHostSocket();
            return null;
        }

        // Receive server's public key
        byte[] certificate = new byte[16384];

        try {
            int readLength = this.serverSocket.getInputStream().read(certificate);
            certificate=Arrays.copyOf(certificate,readLength);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeHostSocket();
            return null;
        }

        CertificateValidator certificateValidator=CertificateValidator.getInstance();

        if(!certificateValidator.validateCertificate(certificate,host)){
            Log.error("Certificate validation failed");
            this.closeHostSocket();
            return null;
        }

        return certificateValidator.getPublicKey(certificate);
    }

    private void closeHostSocket() {
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
