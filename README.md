# PowerHour

A web app that reads hourly Nord Pool day-ahead electricity prices for Lithuania, Latvia and Estonia and finds the cheapest time to run household appliances.

## Team

| Member  | Area |
|---------|------|
| Austris | Accounts — User entity, registration, login, roles USER/ADMIN, password hashing |
| Edgars  | Appliances — Appliance CRUD, validation rules, the appliance list page |
| Illia   | Prices — BiddingZone and HourlyPrice, the Elering client, the scheduled import |
| Karyna  | Planning — PlannedRun, the cheapest-window calculation, cost estimate, the price chart |
| Saidalo | Reports and admin — Savings report, admin screens, global exception handler, logging, Swagger |

## Logging and exceptions

### Logging

Requests, logins, logouts, access denied and exceptions are already logged. Logs go to the console and `logs/powerhour.log`.

To log in your own class, add `@Slf4j`:

```java
log.info("Appliance created: id={}", appliance.getId());
```

- `debug` for development details, `info` for normal events, `warn` for bad input or missing data, `error` for failures (pass the exception last: `log.error("Import failed", ex)`).
- Use `{}` placeholders, not string concatenation.
- Never log passwords or tokens.

### Exceptions

For "not found", extend `NotFoundException` (package `exception`) and throw it from the service. The global handler returns 404 (JSON for REST controllers, `error.html` for pages):

```java
public class ApplianceNotFoundException extends NotFoundException {
    public ApplianceNotFoundException(Long id) {
        super("Appliance", id);
    }
}
```

- `IllegalArgumentException` returns 400.
- An exception with `@ResponseStatus(...)` returns that status.
- Anything else returns 500 with a generic message; details are only in the log.
- Throw from services, not controllers, and don't catch exceptions just to hide them.
