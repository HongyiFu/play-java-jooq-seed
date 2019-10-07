package controllers;

import play.db.Database;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import repositories.PersonRepo;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static play.libs.Json.toJson;

public class PersonController extends Controller {

    private final Database db;
    private final HttpExecutionContext ec;

    @Inject
    public PersonController(Database db, HttpExecutionContext ec) {
        this.db = db;
        this.ec = ec;
    }

    public CompletionStage<Result> index() {
        return supplyAsync(() -> db.withTransaction(conn -> {
            var repo    = new PersonRepo(conn);
            var persons = repo.list();
            return ok(toJson(persons));
        }), ec.current());
    }

    public CompletionStage<Result> addPerson(String name) {
        return supplyAsync(() -> db.withTransaction(conn -> {
            var repo   = new PersonRepo(conn);
            var person = repo.add(name);
            return created(toJson(person));
        }), ec.current());
    }

}
