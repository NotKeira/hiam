package uk.co.keirahopkins.hiam.velocity.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordHasher {
    private static final Logger logger = LoggerFactory.getLogger(PasswordHasher.class);
    private static final int ITERATIONS = 3;
    private static final int MEMORY = 65536; // 64 MB
    private static final int PARALLELISM = 4;
    
    private final Argon2 argon2;

    public PasswordHasher() {
        this.argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
    }

    public String hash(String password) {
        try {
            return argon2.hash(ITERATIONS, MEMORY, PARALLELISM, password.toCharArray());
        } catch (Exception e) {
            logger.error("Failed to hash password", e);
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    public boolean verify(String hash, String password) {
        try {
            return argon2.verify(hash, password.toCharArray());
        } catch (Exception e) {
            logger.error("Failed to verify password", e);
            return false;
        }
    }

    public void cleanup() {
        // No cleanup needed for de.mkammerer:argon2-jvm
    }
}
