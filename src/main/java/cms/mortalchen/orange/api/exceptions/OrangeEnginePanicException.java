package cms.mortalchen.orange.api.exceptions;

public class OrangeEnginePanicException extends Exception{
    public Exception cause;
    public OrangeEnginePanicException(Exception cause){
        super("Orange Engine Panic");
        this.cause = cause;
    }
}
