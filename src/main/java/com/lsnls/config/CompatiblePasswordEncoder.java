package com.lsnls.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt para altas y cambios. Las contraseñas antiguas en texto plano
 * siguen valiendo un login y se rehashean al entrar.
 */
public class CompatiblePasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        if (esBcrypt(encodedPassword)) {
            return bcrypt.matches(rawPassword, encodedPassword);
        }
        return encodedPassword.equals(rawPassword.toString());
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return encodedPassword == null || encodedPassword.isBlank() || !esBcrypt(encodedPassword);
    }

    static boolean esBcrypt(String encoded) {
        return encoded.startsWith("$2a$")
                || encoded.startsWith("$2b$")
                || encoded.startsWith("$2y$");
    }
}
