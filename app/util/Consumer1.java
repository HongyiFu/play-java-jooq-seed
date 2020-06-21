package util;

@FunctionalInterface
public interface Consumer1<T> {

    void accept(T t) throws Exception;

}
