

public class Implementor1  {
    public static void main( String... args ) {
        Class<T> proxyClass = (Class<T>) Proxy.getProxyClass(
                ReflectionHelper.getClassLoaderFromClass( descriptor.type() ),
                descriptor.type()
        );
    }
}