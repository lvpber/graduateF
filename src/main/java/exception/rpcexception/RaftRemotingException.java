package exception.rpcexception;

public class RaftRemotingException extends RuntimeException{
    public RaftRemotingException() {super();}
    public RaftRemotingException(String message) {super(message);}
}
