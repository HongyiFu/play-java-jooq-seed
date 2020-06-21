package jooq;

import javax.inject.Singleton;
import java.sql.Connection;
import java.util.ArrayDeque;
import java.util.Deque;

@Singleton
public class ConnectionContext extends ThreadLocal<Deque<Connection>> {

    @Override
    protected Deque<Connection> initialValue() {
        return new ArrayDeque<>();
    }

    @Override
    public Deque<Connection> get() {
        return super.get();
    }
}
