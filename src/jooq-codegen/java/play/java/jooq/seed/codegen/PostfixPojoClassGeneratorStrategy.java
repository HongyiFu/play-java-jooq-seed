package play.java.jooq.seed.codegen;

import org.jooq.codegen.DefaultGeneratorStrategy;
import org.jooq.meta.Definition;

import static org.jooq.codegen.GeneratorStrategy.Mode.POJO;

public class PostfixPojoClassGeneratorStrategy extends DefaultGeneratorStrategy {

    @Override
    public String getJavaClassName(Definition definition, Mode mode) {
        if (mode == POJO)
            return super.getJavaClassName(definition, POJO) + "Pojo";
        return super.getJavaClassName(definition, mode);
    }
}
