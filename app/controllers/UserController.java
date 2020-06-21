package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import daos.UserDao;
import jooq.Database;
import models.aggregates.user.UserService;
import models.jooq.generated.tables.pojos.AccountPojo;
import models.jooq.generated.tables.pojos.UserPojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;
import play.data.validation.Constraints.Validatable;
import play.data.validation.Constraints.Validate;
import play.data.validation.ValidationError;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import scala.Tuple2;
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;
import validation.Constraints.Phase2;
import validation.Constraints.TwoPhaseValidation;
import validation.ErrorMessage;

import javax.inject.Inject;
import java.util.*;

import static play.libs.Json.toJson;

public class UserController extends Controller {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final Database database;
    private final UserService userService;
    private final UserDao userDao;
    private final Form<Data> createForm;

    @Inject
    private UserController(Database database, UserService userService, UserDao userDao, FormFactory formFactory) {
        this.database = database;
        this.userService = userService;
        this.userDao = userDao;
        this.createForm = formFactory.form(Data.class, TwoPhaseValidation.class);
    }

    /**
     * If we are just displaying for UI, I find it convenient to just deal with records directly,
     * treating them as just data.
     */
    public Result list() {
        var userToAccounts = database.transaction(ctx -> {
            return userDao.findAllWithAccounts();
        });
        var json = userToAccounts
                .entrySet()
                .stream()
                .map(userToAcc -> userWithAccountsToJson(userToAcc.getKey(), userToAcc.getValue()))
                .collect(Json::newArray, ArrayNode::add, ArrayNode::addAll);
        return ok(json);
    }

    public Result create(Http.Request req) {
        var form = createForm.bindFromRequest(req);
        if (form.hasErrors())
            return badRequest(form.errorsAsJson());
        var data = form.get();
        var userWithAccounts = userService.createNewUser(data.name, data.emails);
        var json = userWithAccountsToJson(userWithAccounts);
        return created(json);
    }

    public Result delete(String id) {
        var uuidEither = stringToUuid(id);
        if (uuidEither.isLeft())
            return uuidEither.left().get();
        UUID uuid = uuidEither.right().get();
        if (userService.deleteUser(uuid))
            return ok();
        return notFound();
    }

    // some domain actions

    public Result shoutName(String id) {
        var uuidEither = stringToUuid(id);
        if (uuidEither.isLeft())
            return uuidEither.left().get();
        UUID uuid = uuidEither.right().get();
        String name = userService.shoutNameOfUser(uuid);
        return ok(toJson(name));
    }

    public Result changeName(String id, String newName) {
        var uuidEither = stringToUuid(id);
        if (uuidEither.isLeft())
            return uuidEither.left().get();
        UUID uuid = uuidEither.right().get();
        var userWithAccounts = userService.changeName(uuid, newName);
        var json = userWithAccountsToJson(userWithAccounts);
        return ok(json);
    }

    private JsonNode userWithAccountsToJson(Tuple2<UserPojo, ? extends Collection<AccountPojo>> userWithAccounts) {
        return userWithAccountsToJson(userWithAccounts._1(), userWithAccounts._2());
    }

    private JsonNode userWithAccountsToJson(UserPojo userPojo, Collection<AccountPojo> accountPojos) {
        var json = (ObjectNode) toJson(userPojo);
        json.set("accounts", toJson(accountPojos));
        return json;
    }

    private Either<Result, UUID> stringToUuid(String id) {
        try {
            return new Right<>(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            logger.info("Invalid string representation of UUID for {}", id, e);
            return new Left<>(badRequest(toJson("Invalid string representation of UUID")));
        }
    }

    @Validate(groups = Phase2.class)
    public static final class Data implements Validatable<ValidationError> {

        @Required
        public String name;

        @Required
        public List<@Email String> emails = new ArrayList<>();

        @Override
        public ValidationError validate() {
            var set = new HashSet<String>(emails.size());
            for (String email : emails) {
                if ( !set.add(email) )
                    return new ValidationError("emails", ErrorMessage.DUPLICATE);
            }
            return null;
        }
    }

}
