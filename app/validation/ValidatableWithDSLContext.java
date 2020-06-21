package validation;

import org.jooq.DSLContext;

import javax.inject.Provider;

/**
 * The validation has to be done in a DB transaction or else it will fail.
 */
public interface ValidatableWithDSLContext<T> {

    T validate(Provider<DSLContext> ctxProvider);

}
