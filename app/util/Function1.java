package util;

@FunctionalInterface
public interface Function1<T, R> {

    R apply(T t) throws Exception;

}
