package certificate;

import certificate.impl.ErnestCertificateValidator;

public interface CertificateValidator {
    /**
     * 验证证书合法性。
     *
     * @param   certificate
     *          UTF-8编码的证书字节序列。
     *
     * @return  返回{@code true}如果验证通过，否则返回{@code false}。
     */
    boolean validateCertificate(byte[] certificate);

    /**
     * 提取证书公钥。
     *
     * @param   certificate
     *          UTF-8编码的证书字节序列。
     *
     * @return  返回证书公钥字节序列。
     */
    byte[] getPublicKey(byte[] certificate);

    static CertificateValidator getInstance(){
        return new ErnestCertificateValidator();
    }
}
