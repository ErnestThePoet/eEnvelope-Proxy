package crypto.encryption;

import javax.crypto.Cipher;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Rsa {
    private static byte[] rsaOperate(int opMode, byte[] data, byte[] key) {
        try {
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            Key keyObj;

            if(opMode==Cipher.ENCRYPT_MODE){
                X509EncodedKeySpec x509EncodedKeySpec=new X509EncodedKeySpec(key);
                keyObj=keyFactory.generatePublic(x509EncodedKeySpec);
            }
            else{
                PKCS8EncodedKeySpec pkcs8EncodedKeySpec=new PKCS8EncodedKeySpec(key);
                keyObj=keyFactory.generatePrivate(pkcs8EncodedKeySpec);
            }

            rsaCipher.init(opMode, keyObj);

            return rsaCipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decrypt(byte[] cipher, byte[] privateKey) {
        return rsaOperate(
                Cipher.DECRYPT_MODE,
                cipher,
                privateKey
        );
    }

    public static byte[] encrypt(byte[] plainText, byte[] publicKey) {
        return rsaOperate(
                Cipher.ENCRYPT_MODE,
                plainText,
                publicKey
        );
    }
}
