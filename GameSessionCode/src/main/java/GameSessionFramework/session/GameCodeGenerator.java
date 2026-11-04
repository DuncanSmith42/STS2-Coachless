package GameSessionFramework.session;

import java.security.SecureRandom;

public class GameCodeGenerator {
    // No 0/O 1/I/L
    private static final String CHARACTERS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    public String generateCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return builder.toString();
    }
}
