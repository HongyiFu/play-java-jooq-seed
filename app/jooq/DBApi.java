package jooq;

import play.db.Database;
import play.libs.Scala;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class DBApi implements play.db.DBApi {

    private final play.api.db.DBApi dbApi;
    private final List<Database> databases;
    private final Map<String, Database> databaseByName = new HashMap<>();

    @Inject
    public DBApi(play.api.db.DBApi dbApi, ConnectionContext connectionContext) {
        this.dbApi = dbApi;
        var javaDbs = new ArrayList<Database>();
        for (play.api.db.Database scalaDb : Scala.asJava(dbApi.databases())) {
            var javaDb = new jooq.Database(scalaDb, connectionContext);
            javaDbs.add(javaDb);
            databaseByName.put(javaDb.getName(), javaDb);
        }
        this.databases = List.copyOf(javaDbs);
    }

    @Override
    public List<Database> getDatabases() {
        return databases;
    }

    @Override
    public Database getDatabase(String name) {
        return databaseByName.get(name);
    }

    @Override
    public void shutdown() {
        dbApi.shutdown();
    }

}
