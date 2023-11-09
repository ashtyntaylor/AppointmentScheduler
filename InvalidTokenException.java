public class InvalidTokenException extends Exception {
  public InvalidTokenException() {
    super("Your token is invalid");
  }
}