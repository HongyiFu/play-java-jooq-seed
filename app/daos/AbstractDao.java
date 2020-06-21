package daos;

import org.jooq.DSLContext;

import javax.inject.Provider;
import java.util.Objects;

public abstract class AbstractDao {

    private final Provider<DSLContext> ctxProvider;
    private DSLContext ctx;

    /**
     * This constructor is for when you want to instantiate the DAO manually.
     *
     * <p>This can be useful when you have nested transactions and you want to differentiate the DAOs
     * instantiated in the inner/outer transaction.
     *
     * @param ctx the DSLContext (i.e. Connection} for this DAO to operate on.
     */
    protected AbstractDao(DSLContext ctx) {
        this.ctxProvider = null;
        this.ctx = ctx;
    }

    protected AbstractDao(Provider<DSLContext> ctxProvider) {
        this.ctxProvider = ctxProvider;
        this.ctx = null;
    }

    /**
     * Sets a specific {@link DSLContext}. This can be useful in nested transactions where you want the DAO
     * to perform actions for the outer transaction inside the inner scope.
     *
     * <p>The supplied DSLContext will be used from this point on unless cleared by calling
     * {@link #clearTemporaryContext()}.
     *
     * <p>The user MUST remember to {@link #clearTemporaryContext()} if this DAO is intends to be used past
     * current transaction.
     *
     * @param ctx the DSLContext to set.
     */
    public void setTemporaryContext(DSLContext ctx) {
        this.ctx = Objects.requireNonNull(ctx);
    }

    /**
     * If you have set a temporary DSLContext previously by calling {@link #setTemporaryContext(DSLContext)},
     * you need to call this method to reset the context if you intend to continue to use this instance,
     * so the context (i.e. connection) can be obtained from ThreadLocal (i.e. from {@link jooq.ConnectionContext}).
     */
    public void clearTemporaryContext() {
        this.ctx = null;
    }

    protected DSLContext ctx() {
        if (this.ctx != null)
            return this.ctx;
        return this.ctxProvider.get();
    }

}
