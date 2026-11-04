package coachless.GameSessionFramework.session;

public class InvalidCodeException extends SessionException {
    public InvalidCodeException() {
        super("Invalid session code");
    }
}
