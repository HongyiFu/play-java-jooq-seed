package validation;

import org.jooq.DSLContext;
import play.data.validation.Constraints.PlayConstraintValidator;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.Constraint;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ TYPE, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = ValidateWithDSLContext.ValidateWithDSLContextValidator.class)
public @interface ValidateWithDSLContext {

    String message() default ErrorMessage.INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * The validation has to be done in a DB transaction or else it will fail.
     */
    class ValidateWithDSLContextValidator implements PlayConstraintValidator<ValidateWithDSLContext, ValidatableWithDSLContext<?>> {

        private final Provider<DSLContext> ctxProvider;

        @Inject
        private ValidateWithDSLContextValidator(Provider<DSLContext> ctxProvider) {
            this.ctxProvider = ctxProvider;
        }

        @Override
        public boolean isValid(ValidatableWithDSLContext<?> value, ConstraintValidatorContext context) {
            return reportValidationStatus(value.validate(this.ctxProvider), context);
        }
    }
}
