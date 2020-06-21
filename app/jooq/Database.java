package jooq;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.ConnectionCallable;
import play.db.ConnectionRunnable;
import play.db.TransactionIsolationLevel;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;
import util.Consumer1;
import util.Function1;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This copies 99% from the {@link play.db.DefaultDatabase} with minor modifications to put the
 * {@link Connection} object in {@link ThreadLocal} (specifically, the {@link ConnectionContext})
 * on creation. The connection can then be acquired from the ThreadLocal for purposes of Dependency
 * Injection.
 *
 * <p>NOTE: This is only defined for the Java API.
 *
 * <p>NOTE2: For the methods {@link #getConnection()} and {@link #getConnection(boolean)}, the user is
 * responsible for closing the connection when done (i.e. calling {@link Connection#close()}.
 * Also, the connection acquired this way is not put in the {@link ConnectionContext}.
 */
public class Database implements play.db.Database {

    private static final Logger logger = LoggerFactory.getLogger(Database.class);

    public final ConnectionContext connectionContext;
    private final play.api.db.Database scalaDb;

    public Database(play.api.db.Database database, ConnectionContext connectionContext) {
        this.scalaDb = database;
        this.connectionContext = connectionContext;
    }

    @Override
    public String getName() {
        return scalaDb.name();
    }

    @Override
    public DataSource getDataSource() {
        return scalaDb.dataSource();
    }

    @Override
    public String getUrl() {
        return scalaDb.url();
    }

    /**
     * Get a JDBC connection from the underlying data source. Autocommit is enabled by default.
     *
     * <p>Don't forget to release the connection at some point by calling close().
     *
     * <p>Connection obtained this way is not put in ConnectionContext.
     *
     * @return a JDBC connection
     */
    @Override
    public Connection getConnection() {
        return scalaDb.getConnection();
    }

    /**
     * Get a JDBC connection from the underlying data source.
     *
     * <p>Don't forget to release the connection at some point by calling close().
     *
     * <p>Connection obtained this way is not put in ConnectionContext.
     *
     * @param autocommit determines whether to autocommit the connection
     * @return a JDBC connection
     */
    @Override
    public Connection getConnection(boolean autocommit) {
        return scalaDb.getConnection(autocommit);
    }

    @Override
    public void withConnection(ConnectionRunnable block) {
        boolean[] connAcquired = { false };
        try {
            scalaDb.withConnection(conn -> {
                connectionContext.get().push(conn);
                connAcquired[0] = true;
                connectionFunction(block).apply(conn);
                return null;
            });
        } finally {
            if (connAcquired[0])
                connectionContext.get().pop();
        }
    }

    @Override
    public <A> A withConnection(ConnectionCallable<A> block) {
        boolean[] connAcquired = { false };
        try {
            return scalaDb.withConnection(conn -> {
                connectionContext.get().push(conn);
                connAcquired[0] = true;
                return connectionFunction(block).apply(conn);
            });
        } finally {
            if (connAcquired[0])
                connectionContext.get().pop();
        }
    }

    @Override
    public void withConnection(boolean autocommit, ConnectionRunnable block) {
        boolean[] connAcquired = { false };
        try {
            scalaDb.withConnection(autocommit, conn -> {
                connectionContext.get().push(conn);
                connAcquired[0] = true;
                connectionFunction(block).apply(conn);
                return null;
            });
        } finally {
            if (connAcquired[0])
                connectionContext.get().pop();
        }
    }

    @Override
    public <A> A withConnection(boolean autocommit, ConnectionCallable<A> block) {
        boolean[] connAcquired = { false };
        try {
            return scalaDb.withConnection(autocommit, conn -> {
                connectionContext.get().push(conn);
                connAcquired[0] = true;
                return connectionFunction(block).apply(conn);
            });
        } finally {
            if (connAcquired[0])
                connectionContext.get().pop();
        }
    }

    @Override
    public void withTransaction(ConnectionRunnable block) {
        withTransaction0(null, true, conn -> {
            block.run(conn);
            return null;
        });
    }

    @Override
    public void withTransaction(TransactionIsolationLevel isolationLevel, ConnectionRunnable block) {
        withTransaction0(isolationLevel, false, conn -> {
            block.run(conn);
            return null;
        });
    }

    @Override
    public <A> A withTransaction(ConnectionCallable<A> block) {
        return withTransaction0(null, true, block);
    }

    @Override
    public <A> A withTransaction(TransactionIsolationLevel isolationLevel, ConnectionCallable<A> block) {
        return withTransaction0(isolationLevel, false, block);
    }

    private <A> A withTransaction0(TransactionIsolationLevel isolationLevel, boolean nullableIsolationLevel,
                                   ConnectionCallable<A> block) {
        try {
            return transactionEx0(isolationLevel, nullableIsolationLevel, ctx -> {
                // return ctx.connectionResult(block::call);
                var conn = connectionContext.get().peek();
                return block.call(conn);
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        scalaDb.shutdown();
    }

    // ---------- Extra methods to use org.jooq.DSLContext directly ----------
    // These methods throws Exception, and they also allows callers throwing Exception in the block.
    // They also allow transaction to commit in spite of exception if the exception thrown is instance
    // of NoRollbackException.

    // ----- Variant which propagate Exception: method names ends with "Ex" -----

    public void transactionEx(Consumer1<DSLContext> block) throws Exception {
        transactionEx0(null, true, ctx -> {
            block.accept(ctx);
            return null;
        });
    }

    public void transactionEx(TransactionIsolationLevel isolationLevel, Consumer1<DSLContext> block) throws Exception {
        transactionEx0(isolationLevel, false, ctx -> {
            block.accept(ctx);
            return null;
        });
    }

    public <T> T transactionEx(Function1<DSLContext, T> block) throws Exception {
        return transactionEx0(null, true, block);
    }

    public <T> T transactionEx(TransactionIsolationLevel isolationLevel, Function1<DSLContext, T> block) throws Exception {
        return transactionEx0(isolationLevel, false, block);
    }

    /**
     * We still have to use try-catch-finally instead of try-with-resources because in the latter, resources
     * are closed before the catch/finally block is run.
     *
     * <p>This violates the recommendation in {@link java.sql.Connection#close} which states that we should rollback
     * or commit prior to closing.
     */
    @SuppressWarnings({"MagicConstant", "ThrowFromFinallyBlock"})
    private <T> T transactionEx0(TransactionIsolationLevel isolationLevel, boolean nullableIsolationLevel,
                                 Function1<DSLContext, T> block) throws Exception {
        if ( !nullableIsolationLevel )
            Objects.requireNonNull(isolationLevel);
        Throwable throwable = null;
        Integer oldIsolationLevel = null;
        boolean connAcquired = false;
        var connection = scalaDb.getConnection(false);
        try {
            connectionContext.get().push(connection);
            connAcquired = true;
            if (isolationLevel != null)
                oldIsolationLevel = connection.getTransactionIsolation();
            try {
                if (isolationLevel != null)
                    connection.setTransactionIsolation(isolationLevel.getId());
                var ctx = DSL.using(connection, SQLDialect.POSTGRES);
                T t = block.apply(ctx);
                connection.commit();
                return t;
            } catch (Throwable t) {
                if (t instanceof NoRollbackException) {
                    try {
                        connection.commit();
                    } catch (SQLException ex) {
                        t.addSuppressed(ex);
                        logger.error("Could not commit transaction", ex);
                    }
                } else {
                    try {
                        connection.rollback();
                    } catch (SQLException ex) {
                        t.addSuppressed(ex);
                        logger.error("Could not rollback transaction", ex);
                    }
                }
                throwable = t;
                throw t;
            } finally {
                if (isolationLevel != null) {
                    connection.setTransactionIsolation(oldIsolationLevel);
                }
            }
        } finally {
            SQLException sqlException = null;
            try {
                // the important part!
                connection.close();
            } catch (SQLException ex) {
                if (throwable != null) {
                    throwable.addSuppressed(ex);
                } else {
                    sqlException = ex;
                }
                logger.error("Could not close connection", ex);
            }
            if (connAcquired)
                connectionContext.get().pop();
            if (sqlException != null)
                throw sqlException;
        }
    }

    // ----- Variant which does not propagate Exception: method names does not end with "Ex" -----

    public void transaction(Consumer<DSLContext> block) {
        transaction0(null, true, ctx -> {
            block.accept(ctx);
            return null;
        });
    }

    public void transaction(TransactionIsolationLevel isolationLevel, Consumer<DSLContext> block) {
        transaction0(isolationLevel, false, ctx -> {
            block.accept(ctx);
            return null;
        });
    }

    public <T> T transaction(Function<DSLContext, T> block) {
        return transaction0(null, true, block);
    }

    public <T> T transaction(TransactionIsolationLevel isolationLevel, Function<DSLContext, T> block) {
        return transaction0(isolationLevel, false, block);
    }

    private <T> T transaction0(TransactionIsolationLevel isolationLevel, boolean nullableIsolationLevel,
                               Function<DSLContext, T> block) {
        try {
            return transactionEx0(isolationLevel, nullableIsolationLevel, block::apply);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copied from {@link play.db.DefaultDatabase}'s connectionFunction.
     */
    private AbstractFunction1<Connection, BoxedUnit> connectionFunction(final ConnectionRunnable block) {
        return new AbstractFunction1<>() {
            public BoxedUnit apply(Connection connection) {
                try {
                    block.run(connection);
                    return BoxedUnit.UNIT;
                } catch (java.sql.SQLException e) {
                    throw new RuntimeException("Connection runnable failed", e);
                }
            }
        };
    }

    /**
     * Copied from {@link play.db.DefaultDatabase}'s connectionFunction method.
     */
    private <A> AbstractFunction1<Connection, A> connectionFunction(final ConnectionCallable<A> block) {
        return new AbstractFunction1<>() {
            public A apply(Connection connection) {
                try {
                    return block.call(connection);
                } catch (java.sql.SQLException e) {
                    throw new RuntimeException("Connection callable failed", e);
                }
            }
        };
    }

}
