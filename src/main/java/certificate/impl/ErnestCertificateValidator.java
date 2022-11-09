package certificate.impl;

import certificate.CertificateValidator;

// TODO Demo code only
public class ErnestCertificateValidator implements CertificateValidator {
    @Override
    public boolean validateCertificate(byte[] certificate,String host) {
        return true;
    }

    @Override
    public byte[] getPublicKey(byte[] certificate) {
        // TODO Demo only.
        return certificate;
    }
}
