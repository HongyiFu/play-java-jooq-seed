package models.aggregates.user;

import daos.AccountDao;
import daos.UserDao;
import jooq.Database;
import models.Service;
import models.jooq.generated.tables.pojos.AccountPojo;
import models.jooq.generated.tables.pojos.UserPojo;
import org.jooq.DSLContext;
import scala.Tuple2;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final jooq.Database database;
    private final UserDao userDao;
    private final AccountDao accountDao;
    private final Provider<DSLContext> ctxProvider;

    @Inject
    private UserService(Database database, UserDao userDao, AccountDao accountDao, Provider<DSLContext> ctxProvider) {
        this.database = database;
        this.userDao = userDao;
        this.accountDao = accountDao;
        this.ctxProvider = ctxProvider;
    }

    /**
     * @param name not nullable, validated by DB
     * @param emails not repeatable or nullable, validated by DB
     * @throws RuntimeException if name is nullable or emails are duplicated
     */
    public Tuple2<UserPojo, List<AccountPojo>> createNewUser(String name, Collection<String> emails) {
        return database.transaction(ctx -> {
            var tuple2 = userDao.create(name, emails);
            var userRecord     = tuple2._1();
            var accountRecords = tuple2._2();
            var user = new User(userRecord, accountRecords, true, ctxProvider.get());
            return user.toPojo();
        });
    }

    public Tuple2<UserPojo, List<AccountPojo>> getUser(UUID id) {
        return database.transaction(ctx -> {
            var user = _getUser(id, false);
            return user.toPojo();
        });
    }

    public String shoutNameOfUser(UUID id) {
        return database.transaction(ctx -> {
            var user = _getUser(id, false);
            return user.shoutName();
        });
    }

    public Tuple2<UserPojo, List<AccountPojo>> changeName(UUID id, String newName) {
        return database.transaction(ctx -> {
            var user = _getUser(id, true);
            user.changeName(newName);
            return user.toPojo();
        });
    }

    public boolean deleteUser(UUID userId) {
        return database.transaction(ctx -> {
            return userDao.deleteById(userId);
        });
    }

    private User _getUser(UUID id, boolean lockForUpdate) {
        var userRecord = userDao.findById(id, lockForUpdate);
        var accountRecords = accountDao.findAllByUserId(id);
        return new User(userRecord, accountRecords, lockForUpdate, ctxProvider.get());
    }

}
