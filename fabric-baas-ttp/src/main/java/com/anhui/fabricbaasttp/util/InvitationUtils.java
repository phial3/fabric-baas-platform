package com.anhui.fabricbaasttp.util;

import com.anhui.fabricbaascommon.bean.Invitation;
import com.anhui.fabricbaascommon.util.AESUtils;
import org.springframework.util.Base64Utils;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class InvitationUtils {
    private static SecretKeySpec AES_SECRET_KEY_SPEC;

    static {
        try {
            String token = UUID.randomUUID().toString();
            AES_SECRET_KEY_SPEC = AESUtils.generateSecretKey(token);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static String getCode(Invitation invitation) throws Exception {
        String str = String.join("---",
                invitation.getInvitorOrgName(),
                invitation.getInviteeOrgName(),
                invitation.getChannelName(),
                invitation.getTimestamp().toString());
        byte[] encryptedStr = AESUtils.encrypt(str.getBytes(StandardCharsets.UTF_8), AES_SECRET_KEY_SPEC);
        return Base64Utils.encodeToString(encryptedStr);
    }

    public static Invitation parseCode(String code) throws Exception {
        byte[] encryptedData = Base64Utils.decodeFromString(code);
        byte[] decryptedData = AESUtils.decrypt(encryptedData, AES_SECRET_KEY_SPEC);
        String str = new String(decryptedData, StandardCharsets.UTF_8);
        String[] properties = str.split("---");
        assert properties.length == 4;

        Invitation invitation = new Invitation();
        invitation.setInvitorOrgName(properties[0]);
        invitation.setInviteeOrgName(properties[1]);
        invitation.setChannelName(properties[2]);
        invitation.setTimestamp(Long.parseLong(properties[3]));
        return invitation;
    }
}
