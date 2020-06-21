package validation;

import javax.validation.GroupSequence;
import javax.validation.groups.Default;

public final class Constraints {

    private Constraints() {}

    public interface Phase2 {}

    @GroupSequence({ Default.class, Phase2.class })
    public interface TwoPhaseValidation {}

}
