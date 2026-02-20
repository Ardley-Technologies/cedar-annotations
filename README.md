# Cedar Authorization Framework

Stop writing authorization code. Just use annotations.

This is a JAX-RS framework that lets you protect your REST APIs with AWS Verified Permissions (Cedar) using nothing but annotations on your endpoint parameters. No more `if (user.canAccess(resource))` spaghetti in every controller method.

## Why This Exists

We built a loan origination platform and got tired of writing the same authorization boilerplate everywhere. Cedar policies are great, but manually checking permissions in every endpoint is tedious and error-prone. So we made a filter that does it automatically based on annotations.

Turns out it works pretty well.

## What You Get

- **Annotations instead of code** - Put `@CedarResource` on parameters, never write permission checks again
- **Type safety** - Generic extractors mean compile-time checking instead of runtime explosions
- **Multi-resource checks** - Need to check permissions on 3 different resources in one endpoint? No problem
- **Automatic schema generation** - Scans your annotations and generates proper Cedar schemas (because nobody wants to maintain those by hand)
- **Smart caching** - Policy store lookups are cached because we're not animals

## Quick Start

### 1. Add to your project

```gradle
dependencies {
    implementation("com.ardley:cedar-jaxrs:1.0.0-SNAPSHOT")
    implementation("com.ardley:cedar-aws:1.0.0-SNAPSHOT")
}
```

### 2. Write a resource extractor

Tell the framework how to turn a user ID into something Cedar can check:

```java
@Singleton
public class UserResourceExtractor implements ResourceExtractor<String> {

    @Inject
    private UserRepository userRepository;

    @Override
    public String getResourceType() {
        return "User";
    }

    @Override
    public ResourceEntity extract(String userId, ResourceExtractionContext context) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        return ResourceEntity.builder()
            .entityType("User")
            .entityId(userId)
            .customerId(user.getCustomerId())
            .attributes(Map.of(
                "userId", userId,
                "email", user.getEmail(),
                "role", user.getRole()
            ))
            .build();
    }
}
```

### 3. Wire up the filter

```java
@ApplicationPath("/api")
public class ApplicationConfig extends ResourceConfig {

    @PostConstruct
    public void init() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new UserResourceExtractor(userRepository))
            .register(new LoanApplicationResourceExtractor(loanAppRepository))
            .build();

        CedarAuthorizationFilter filter = CedarAuthorizationFilter.builder()
            .principalExtractor(new JwtPrincipalExtractor(
                "username", "custom:customerId", "custom:role", "email"
            ))
            .policyStoreResolver(new StaticPolicyStoreResolver("PS_12345"))
            .contextEntityType("Customer")
            .extractorRegistry(registry)
            .authorizationService(new AwsVerifiedPermissionsService())
            .build();

        register(filter);
    }
}
```

### 4. Annotate your endpoints

**Check if a user CAN do something in their tenant:**
```java
@POST
@Path("/users")
@CedarSecured
@RequiresActions({"CreateUser"})
public Response createUser(@Valid UserRequest request) {
    // If they got here, they can create users. That's it.
    return Response.ok(userService.create(request)).build();
}
```

**Check if they can access a SPECIFIC resource:**
```java
@GET
@Path("/users/{userId}")
@CedarSecured
public Response getUser(
    @PathParam("userId")
    @CedarResource(type = "User", actions = {"ViewUser"})
    String userId
) {
    // Filter already checked if they can view this user
    return Response.ok(userService.get(userId)).build();
}
```

**Multiple resources? Sure:**
```java
@POST
@Path("/documents/{docId}/transfer")
@CedarSecured
public Response transferDocument(
    @PathParam("docId")
    @CedarResource(type = "Document", actions = {"ViewDocument", "DeleteDocument"})
    String docId,

    @QueryParam("toLoanAppId")
    @CedarResource(type = "LoanApplication", actions = {"ViewLoanApplication"})
    String toLoanAppId
) {
    // All checks passed. Do the thing.
    return Response.ok(documentService.transfer(docId, toLoanAppId)).build();
}
```

## Schema Generation (The Cool Part)

Cedar requires you to define schemas that map actions to resource types. Normally you'd maintain a giant JSON file by hand and pray it stays in sync with your code.

Not anymore.

### Just scan your codebase

```java
ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
    .register(new UserResourceExtractor(userRepository))
    .register(new LoanApplicationResourceExtractor(loanAppRepository))
    .build();

// This scans your API package and figures out which actions apply to which resources
String schemaJson = registry.generateSchemaFromClasspath(
    "MyApp",              // Cedar namespace
    "com.myapp.api",      // Package to scan
    "User"                // Principal entity type
);

// Deploy it to AWS
authorizationService.deploySchema("customer-123", "PS_12345", schemaJson);
```

### How it works

The scanner looks at your annotations and builds the mappings automatically:

```java
// Context-based action (can they create users at all?)
@POST
@RequiresActions({"CreateUser"})
public Response createUser() { ... }
// → Schema: CreateUser with resourceTypes: []

// Resource-based action (can they view THIS user?)
@GET
@Path("/users/{userId}")
public Response getUser(
    @PathParam("userId")
    @CedarResource(type = "User", actions = {"ViewUser"})
    String userId
) { ... }
// → Schema: ViewUser with resourceTypes: ["User"]

// Same action on different resources? It figures it out.
@GET
@Path("/users/{id}")
public Response viewUser(
    @PathParam("id")
    @CedarResource(type = "User", actions = {"View"})
    String id
) { ... }

@GET
@Path("/documents/{id}")
public Response viewDoc(
    @PathParam("id")
    @CedarResource(type = "Document", actions = {"View"})
    String id
) { ... }
// → Schema: View with resourceTypes: ["User", "Document"]
```

The generated schema is AWS-compliant and ready to deploy. No manual maintenance required.

### Optional: Define attribute schemas

If you want Cedar to validate resource attributes in policies, define them in your extractors:

```java
@Override
public Map<String, AttributeType> getAttributeSchema() {
    Map<String, AttributeType> schema = new HashMap<>();
    schema.put("email", AttributeType.STRING);
    schema.put("role", AttributeType.STRING);
    schema.put("active", AttributeType.BOOLEAN);
    return schema;
}
```

These get included in the generated schema automatically. Now your Cedar policies can reference `resource.email` and AWS will validate the schema on policy creation.

### Optional: Runtime schema validation

Want to catch bugs where your extractor doesn't return what it promised?

```java
CedarAuthorizationFilter filter = CedarAuthorizationFilter.builder()
    // ... other config ...
    .validateSchema(true)  // Fail fast on schema mismatches
    .build();
```

This checks that your extractors actually return all the attributes they claim to, with the right types. Probably don't run this in prod, but super useful in dev.

## Architecture

```
Your Endpoint
    ↓
CedarAuthorizationFilter (JAX-RS filter)
    ├─> Extract principal from JWT
    ├─> Resolve policy store (with caching)
    ├─> Check @RequiresActions (can they do X?)
    └─> Check @CedarResource on params (can they access Y?)
            ↓
    ResourceExtractor fetches resource details
            ↓
    AWS Verified Permissions makes decision
            ↓
    403 or let request through
```

The filter runs before your endpoint code, so you never see unauthorized requests. Simple.

## Building

```bash
./gradlew build
```

## Testing

```bash
./gradlew test
```

47 tests. They all pass. We're not monsters.

## Design Decisions Worth Mentioning

- **Policy store resolution happens at runtime** with caching. Multi-tenant apps can resolve different policy stores per customer without redeploying.
- **Registry is immutable** after construction. Register all your extractors upfront, then it's thread-safe forever.
- **Context entity type is configurable** because not everyone calls their tenant entity "Customer". We do, but you might not.
- **Schema validation is opt-in** because it's a development tool, not a production feature.

## Production Notes

We run this in production. Here's what we learned:

- Cache your policy store lookups. The filter does this for you.
- Don't enable schema validation in prod. It's for catching bugs during development.
- Resource extractors hit the database. Cache appropriately or you'll hate yourself.
- Cedar is fast, but network calls to AWS aren't free. Watch your bill.

## Contributing

Found a bug? Want a feature? PRs welcome. Just keep it simple - this is meant to be easy to understand, not a kitchen sink framework.

## License

Apache License 2.0

Go forth and authorize things.
