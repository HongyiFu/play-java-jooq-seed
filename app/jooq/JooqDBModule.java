package jooq;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import play.db.ConnectionPool;
import play.db.DefaultConnectionPool;
import play.db.NamedDatabase;
import play.db.NamedDatabaseImpl;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;

public class JooqDBModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(JooqDBModule.class);

    private final Environment environment;
    private final Config config;

    public JooqDBModule(Environment environment, Config config) {
        this.environment = environment;
        this.config = config;
    }

    @Override
    protected void configure() {
        configureDatabase();
    }

    /**
     * Copied from {@link play.db.DBModule}, with minor modifications to use custom {@link play.db.DBApi}.
     */
    private void configureDatabase() {
        String dbKey = config.getString("play.db.config");
        String defaultDb = config.getString("play.db.default");

        bind(ConnectionContext.class).asEagerSingleton();
        bind(ConnectionPool.class).to(DefaultConnectionPool.class).asEagerSingleton();
        bind(play.db.DBApi.class).to(jooq.DBApi.class).asEagerSingleton();

        try {
            Set<String> dbs = config.getConfig(dbKey).root().keySet();
            for (String db : dbs) {
                bind(play.db.Database.class).annotatedWith(named(db)).toProvider(new NamedDatabaseProvider(db));
                bind(jooq.Database.class).annotatedWith(named(db)).toProvider(new NamedDatabaseProvider(db));
                if (db.equals(defaultDb)) {
                    bind(play.db.Database.class).toProvider(new NamedDatabaseProvider(db));
                    bind(jooq.Database.class).toProvider(new NamedDatabaseProvider(db));
                }
            }
        } catch (ConfigException.Missing ex) {
            logger.warn("Configuration not found for database: {}", ex.getMessage());
        }
    }

    private NamedDatabase named(String name) {
        return new NamedDatabaseImpl(name);
    }

    @Provides
    private DSLContext provideDSLContext(ConnectionContext connectionContext) {
        var conn = connectionContext.get().peek();
        if (conn != null)
            return DSL.using(conn, SQLDialect.POSTGRES);
        return null;
    }

    /**
     * Inject provider for named databases.
     *
     * <p>Mostly similar to {@link play.db.DBModule.NamedDatabaseProvider} except we provide {@link jooq.Database}
     * instead of {@link play.db.Database}.
     */
    public static class NamedDatabaseProvider implements Provider<jooq.Database> {
        @Inject private play.db.DBApi dbApi;
        private final String name;

        public NamedDatabaseProvider(String name) {
            this.name = name;
        }

        public jooq.Database get() {
            return (jooq.Database) dbApi.getDatabase(name);
        }
    }

}
