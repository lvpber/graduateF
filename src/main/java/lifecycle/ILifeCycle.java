package lifecycle;

public interface ILifeCycle {
    void init()     throws Throwable;
    void destroy()  throws Throwable;
}
