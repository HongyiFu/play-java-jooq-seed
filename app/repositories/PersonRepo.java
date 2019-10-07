package repositories;

import models.jooq.generated.tables.pojos.Person;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.util.List;

import static models.jooq.generated.Tables.PERSON;
import static org.jooq.SQLDialect.MYSQL;

public class PersonRepo {

    private final DSLContext create;

    public PersonRepo(Connection conn) {
        this.create = DSL.using(conn, MYSQL);
    }

    public Person add(String name) {
        var person = create.newRecord(PERSON);
        person.setName(name);
        person.store();
        return person.into(Person.class);
    }

    public List<Person> list() {
        return create
                .selectFrom(PERSON)
                .fetchInto(Person.class);
    }
}
