package certificate;

import certificate.impl.ErnestCertificateProvider;

public interface CertificateProvider {
    /**
     * 提供证书字节序列。
     *
     * @return  UTF-8编码的证书字节序列。
     */
    byte[] getCertificate();

    /**
     * 提取证书私钥。
     *
     * @return  返回证书私钥字节序列。
     */
    byte[] getPrivateKey();

    static CertificateProvider getInstance(){
        return new ErnestCertificateProvider();
    }
}
