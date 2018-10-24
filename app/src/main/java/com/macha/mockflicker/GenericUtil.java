package com.macha.mockflicker;

import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class GenericUtil {

    public String getSignature(String sec1, String url, String params, String sec2)
            throws UnsupportedEncodingException, NoSuchAlgorithmException,
            InvalidKeyException {
        /**
         * base has three parts, they are connected by "&": 1) protocol 2) URL
         * (need to be URLEncoded) 3) Parameter List (need to be URLEncoded).
         */
        StringBuilder base = new StringBuilder();
        base.append("GET&");
        base.append(url);
        base.append("&");
        base.append(params);
        System.out.println("Stirng for oauth_signature generation:" + base);

        // yea, don't ask me why, it is needed to append a "&" to the end of secret key.
        String signKey = sec1 + "&";

        if (sec2 != null) {
            signKey = signKey + sec2;
        }

        byte[] keyBytes = (signKey).getBytes("UTF-8");

        SecretKey key = new SecretKeySpec(keyBytes, "HMAC-SHA1");

        Mac mac = Mac.getInstance("HMAC-SHA1");
        mac.init(key);

        Base64 base64 = new Base64();
        // encode it, base64 it, change it to string and return.
        return new String(base64.encode(mac.doFinal(base.toString().getBytes(
                "UTF-8"))), "UTF-8").trim();
    }

    public Map<String, String> createMapFromResponse(String body) {
        Map<String, String> query_pairs = new HashMap<String, String>();
        String[] split = body.split("&");

        for (String temp : split) {
            String[] split2 = temp.split("=");
            query_pairs.put(split2[0], split2[1]);
        }

        return query_pairs;
    }
}
