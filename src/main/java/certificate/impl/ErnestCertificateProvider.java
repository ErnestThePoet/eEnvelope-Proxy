package certificate.impl;

import certificate.CertificateProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

// TODO Demo code only
public class ErnestCertificateProvider implements CertificateProvider {
    @Override
    public byte[] getCertificate() {
        // TODO Demo only.
        String publicKey;
        try {
            publicKey= Files.readString(Path.of("./cert/public.pem"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return Base64.getDecoder().decode(
                publicKey.replace("BEGIN PUBLIC KEY","")
                        .replace("END PUBLIC KEY","")
                        .replace("-","")
                        .replace("\r","")
                        .replace("\n",""));
    }

    @Override
    public byte[] getPrivateKey() {
        // TODO Demo only.
        String privateKey;
        try {
            privateKey= Files.readString(Path.of("./cert/private.pem"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return Base64.getDecoder().decode(
                privateKey.replace("BEGIN PRIVATE KEY","")
                .replace("END PRIVATE KEY","")
                .replace("-","")
                .replace("\r","")
                .replace("\n",""));
    }
}
