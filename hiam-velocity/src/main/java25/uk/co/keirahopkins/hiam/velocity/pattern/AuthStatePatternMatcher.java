package uk.co.keirahopkins.hiam.velocity.pattern;

import uk.co.keirahopkins.hiam.velocity.model.AuthState;
import uk.co.keirahopkins.hiam.velocity.model.CredentialType;

public class AuthStatePatternMatcher {
    
    public static String describeAuthState(AuthState state) {
        return switch (state) {
            case REGISTERED -> "User has been registered";
            case UNREGISTERED -> "User requires registration";
            case SESSION_ACTIVE -> "User session is active";
            case SESSION_EXPIRED -> "User session has expired";
            case LOCKED -> "User account is locked";
            case PREMIUM_VERIFIED -> "User is verified as premium";
            case AWAITING_CONFIRMATION -> "User awaits confirmation";
        };
    }

    public static boolean requiresPasswordStorage(CredentialType type) {
        return switch (type) {
            case PASSWORD, HASHED_ARGON2ID -> true;
            case JAVA_UUID, EAGLERCRAFT_UUID -> false;
        };
    }

    public static int getPriority(CredentialType type) {
        return switch (type) {
            case HASHED_ARGON2ID -> 1;
            case PASSWORD -> 2;
            case JAVA_UUID, EAGLERCRAFT_UUID -> 3;
        };
    }
}
