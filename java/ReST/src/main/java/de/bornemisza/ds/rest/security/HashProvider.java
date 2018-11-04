package de.bornemisza.ds.rest.security;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.primeframework.jwt.Signer;
import org.primeframework.jwt.Verifier;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.primeframework.jwt.hmac.HMACVerifier;

public abstract class HashProvider {

    private static final String algorithm = "HmacSHA256";
    private Mac mac;
    private Signer signer;
    private Verifier verifier;

    public HashProvider() {
    }

    protected abstract char[] getServerSecret();

    protected void init() {
        try {
            char[] serverSecret = getServerSecret();
            SecretKeySpec key = new SecretKeySpec(toBytes(serverSecret), algorithm);
            String secret = String.valueOf(serverSecret);
            signer = HMACSigner.newSHA256Signer(secret);
            verifier = HMACVerifier.newVerifier(secret);
            Arrays.fill(serverSecret, '*');
            mac = Mac.getInstance(algorithm);
            mac.init(key);
        }
        catch (InvalidKeyException | NoSuchAlgorithmException ex) {
            throw new RuntimeException("Problem generating secret key", ex);
        }
    }

    public String hmacDigest(String msg) {
        try {
            byte[] bytes = mac.doFinal(msg.getBytes("ASCII"));
            return createHexString(bytes);
        }
        catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Problem generating Hash", ex);
        }
    }

    public String encodeJasonWebToken(String userName) {
        JWT jwt = new JWT()
                .setIssuer(System.getProperty("FQDN"))
                .setSubject(userName);
        return JWT.getEncoder().encode(jwt, signer);
    }

    public JWT decodeJasonWebToken(String encodedJWT) {
        return JWT.getDecoder().decode(encodedJWT, verifier);
    }

    private String createHexString(byte[] bytes) {
        StringBuilder hash = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hash.append('0');
            }
            hash.append(hex);
        }
        return hash.toString();
    }

    private byte[] toBytes(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }

}
