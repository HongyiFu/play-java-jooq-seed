package daos;

import models.jooq.generated.tables.records.AccountRecord;
import org.jooq.DSLContext;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.UUID;

import static models.jooq.generated.Tables.ACCOUNT;

public class AccountDao extends AbstractDao {

    public AccountDao(DSLContext ctx) {
        super(ctx);
    }

    @Inject
    private AccountDao(Provider<DSLContext> ctxProvider) {
        super(ctxProvider);
    }

    public List<AccountRecord> findAllByUserId(UUID userId) {
        return ctx()
                .selectFrom(ACCOUNT)
                .where(ACCOUNT.USER_ID.eq(userId))
                .fetch();
    }
}
