package models.aggregates.user;

import daos.AccountDao;
import daos.UserDao;
import models.jooq.generated.tables.pojos.AccountPojo;
import models.jooq.generated.tables.pojos.UserPojo;
import models.jooq.generated.tables.records.AccountRecord;
import models.jooq.generated.tables.records.UserRecord;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import javax.persistence.OneToMany;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static javax.persistence.FetchType.EAGER;

/**
 * This is an aggregate root in DDD-speak.
 */
public class User {

    private static final Logger logger = LoggerFactory.getLogger(User.class);

    private final UserRecord userRecord;

    /**
     * The JPA annotation is just a marker! It does not do anything, since we did not include
     * a JPA provider.
     *
     * <p>In most cases, entities inside the same aggregate should be eagerly fetched.
     *
     * @see <a href="http://scabl.blogspot.com/2015/04/aeddd-6.html">
     *     As advocated in this series of awesome blog posts</a>
     */
    @OneToMany(fetch = EAGER)
    private final List<AccountRecord> accountRecords;
    
    private final boolean lockForUpdate;

    private final UserDao userDao;
    private final AccountDao accountDao;

    User(UserRecord userRecord, List<AccountRecord> accountRecords, boolean lockForUpdate, DSLContext ctx) {
        this.userRecord = userRecord;
        this.accountRecords = accountRecords;
        this.lockForUpdate = lockForUpdate;
        this.userDao = new UserDao(ctx);
        this.accountDao = new AccountDao(ctx);
    }

    // ----- Some getters, which can be helpful sometimes -----

    public UUID id() {
        return userRecord.getId();
    }

    public String name() {
        return userRecord.getName();
    }

    // ----- Some domain actions -----

    public String shoutName() {
        logger.info("My name is {}!", name());
        return name();
    }

    public boolean changeName(String name) {
        requireLock();
        userRecord.setName(name);
        return userDao.update(userRecord);
    }

    // ----- UI methods -----
    public Tuple2<UserPojo, List<AccountPojo>> toPojo() {
        var userPojo = userRecord.into(UserPojo.class);
        List<AccountPojo> accountPojos;
        if (accountRecords instanceof Result) {
            // jooq's implementation is similar to below actually (iterating over and mapping each record)
            accountPojos = ((Result<AccountRecord>) accountRecords).into(AccountPojo.class);
        } else {
            accountPojos = accountRecords
                    .stream()
                    .map(accountRecord -> accountRecord.into(AccountPojo.class))
                    .collect(toList());
        }
        return new Tuple2<>(userPojo, accountPojos);
    }

    // ----- private methods ------

    private void requireLock() {
        if ( !lockForUpdate)
            throw new IllegalStateException("Lock must be acquired in order to perform this action");
    }

}
