package daos;

import models.jooq.generated.tables.pojos.AccountPojo;
import models.jooq.generated.tables.pojos.UserPojo;
import models.jooq.generated.tables.records.AccountRecord;
import models.jooq.generated.tables.records.UserRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep2;
import org.jooq.Result;
import scala.Tuple2;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static models.jooq.generated.Tables.ACCOUNT;
import static models.jooq.generated.Tables.USER;

/**
 * Ideally all SQL operations should go through DAO. These helps to group SQL in 1 place
 * for easier optimization if there is a need. Also if caching is desired, we can override this
 * class to provide caching.
 */
public class UserDao extends AbstractDao {

    public UserDao(DSLContext ctx) {
        super(ctx);
    }

    @Inject
    private UserDao(Provider<DSLContext> ctxProvider) {
        super(ctxProvider);
    }

    public Tuple2<UserRecord, List<AccountRecord>> create(String name, Collection<String> emails) {
        var user = ctx().newRecord(USER);
        user.setName(name);
        user.store();
        var step = ctx()
                .insertInto(ACCOUNT, ACCOUNT.USER_ID, ACCOUNT.EMAIL);
        for (var email : emails) {
            step = step.values(user.getId(), email);
        }
        var accountRecords = step
                .returning()
                .fetch();
        return new Tuple2<>(user, accountRecords);
    }

    public UserRecord findById(UUID id, boolean lockForUpdate) {
        var step = ctx()
                .selectFrom(USER)
                .where(USER.ID.eq(id));
        if (lockForUpdate)
            return step.forUpdate().fetchAny();
        return step.fetchAny();
    }

    public boolean update(UserRecord userRecord) {
        return userRecord.update() == 1;
    }

    public Result<UserRecord> findAll() {
        return ctx()
                .selectFrom(USER)
                .fetch();
    }

    public boolean deleteById(UUID id) {
        // We want to make sure this operation never fails. So we acquire lock on USER first.
        // One way it could have failed:
        //      Thread2 acquires a lock on User =>
        //      This Thread deletes accounts belonging to User =>
        //      Thread2 adds a new Account and commits the transaction =>
        //      This Thread wants to delete User but fails because there are still account rows (inserted by Thread1)
        var userRecord = ctx().selectFrom(USER)
                .where(USER.ID.eq(id))
                .limit(1)
                .forUpdate()
                .fetchAny();
        if (userRecord != null) {
            ctx().deleteFrom(ACCOUNT)
                    .where(ACCOUNT.USER_ID.eq(id))
                    .execute();
            return userRecord.delete() == 1;
        }
        return false;
    }

    public TreeMap<UserPojo, List<AccountPojo>> findAllWithAccounts() {
        var records = ctx()
                .select()
                .from(USER)
                .join(ACCOUNT).on(USER.ID.eq(ACCOUNT.USER_ID))
                .fetch();
        // TreeMap ordering based on id of userPojo is not consistent with equals! (because equals in UserPojo class is based on object identity)
        var map = new TreeMap<UserPojo, List<AccountPojo>>(comparing(UserPojo::getId));
        for (var record : records) {
            var userPojo    = record.into(USER).into(UserPojo.class); // mapping first to Record will eliminate column ambiguity
            var accountPojo = record.into(ACCOUNT).into(AccountPojo.class);
            var accountPojos = map.computeIfAbsent(userPojo, k -> new ArrayList<>());
            accountPojos.add(accountPojo);
        }
        return map;
    }
}
