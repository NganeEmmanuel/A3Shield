package com.a3shield.enforcer.evaluator;

import com.a3shield.core.model.Intent;
import com.a3shield.core.model.Resource;
import com.a3shield.core.model.Subject;
import com.a3shield.core.model.Target;
import com.a3shield.enforcer.context.RequestContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.Program;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.interpreter.Activation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p><b>CelConditionEvaluator</b> evaluates policy conditions using CEL
 * (Common Expression Language) per ADR-0002 — the canonical engine, replacing
 * the hand-rolled DSL as the default while {@link DslConditionEvaluator}
 * remains available.</p>
 *
 * <p><b>Legacy simple-syntax facade:</b> expressions written in the original
 * DSL shape keep working — {@code AND}/{@code OR} are translated to
 * {@code &&}/{@code ||} and bareword literals (e.g. {@code USER}) are quoted
 * ({@code 'USER'}). New capabilities (parentheses, {@code in}, string
 * operators, {@code .exists()} macros for ANY-match list semantics) are CEL
 * and require native CEL syntax.</p>
 *
 * <p><b>Activation model:</b> {@code subject} (id, type, roles, permissions,
 * attributes), {@code resource} (first resource: id, type, attributes),
 * {@code resources} (all resources as maps, for {@code .exists()} macros),
 * {@code context} (ambient attributes) and {@code tenant} are exposed to
 * expressions.</p>
 *
 * <p><b>Semantics:</b> strictly typed — comparing a string field to a bare
 * numeric literal fails closed (false), unlike the legacy DSL's loose
 * coercion. Blank/null conditions are true (allow by default), matching the
 * legacy engine. Compilation and evaluation errors also fail closed.</p>
 *
 * <p><b>Safety:</b> CEL is non-Turing-complete (termination guaranteed);
 * expressions are additionally capped at {@link #MAX_EXPRESSION_LENGTH}
 * characters because policies may be admin-edited at runtime (ADR-0003).
 * Compiled programs are cached per expression string; evaluation is
 * thread-safe.</p>
 */
public class CelConditionEvaluator implements ConditionEvaluator {

    /** Runtime-edited policies must never hang or blow up a request: cap expression size. */
    public static final int MAX_EXPRESSION_LENGTH = 512;

    private static final Logger logger = LoggerFactory.getLogger(CelConditionEvaluator.class);

    private final Env env;
    private final Map<String, Optional<Program>> compiledCache = new ConcurrentHashMap<>();

    public CelConditionEvaluator() {
        // subject/resource/resources/context/tenant declared as dyn so the
        // checker accepts them and real shapes arrive via the activation map
        this.env = Env.newEnv(EnvOption.declarations(
                Decls.newVar("subject", Decls.Dyn),
                Decls.newVar("resource", Decls.Dyn),
                Decls.newVar("resources", Decls.Dyn),
                Decls.newVar("context", Decls.Dyn),
                Decls.newVar("tenant", Decls.Dyn)));
    }

    @Override
    public boolean evaluate(String expression, RequestContext context) {
        if (expression == null || expression.isBlank()) {
            return true; // no condition: allow by default (legacy semantics)
        }
        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            logger.debug("Condition exceeds {} chars, failing closed", MAX_EXPRESSION_LENGTH);
            return false;
        }

        String cel = toCel(expression);
        Program program = compiledCache.computeIfAbsent(cel, this::compile).orElse(null);
        if (program == null) {
            return false; // deterministically broken expression: fail closed
        }

        try {
            var result = program.eval(Activation.newActivation(toActivation(context)));
            return result.getVal().booleanValue();
        } catch (RuntimeException e) {
            logger.debug("CEL evaluation error for '{}': {}", cel, e.getMessage());
            return false;
        }
    }

    /**
     * Syntax-only check: does the expression compile (after legacy-syntax
     * translation)? Used by the admin API to validate policy conditions
     * before committing them (ADR-0003).
     */
    public boolean compiles(String expression) {
        if (expression == null || expression.isBlank()) {
            return true;
        }
        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            return false;
        }
        return compiledCache.computeIfAbsent(toCel(expression), this::compile).isPresent();
    }

    private Optional<Program> compile(String celExpression) {
        try {
            Env.AstIssuesTuple compiled = env.compile(celExpression);
            if (compiled.hasIssues()) {
                logger.debug("CEL compile issues for '{}': {}", celExpression, compiled.getIssues());
                return Optional.empty();
            }
            return Optional.of(env.program(compiled.getAst()));
        } catch (RuntimeException e) {
            logger.debug("CEL compile failed for '{}': {}", celExpression, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Translate the legacy simple syntax to CEL: word operators to symbolic
     * ones, bareword literals to quoted strings. Path tokens
     * ({@code subject.*}, {@code resource.*}, {@code context.*}) and numbers
     * pass through untouched.
     */
    String toCel(String expression) {
        StringBuilder out = new StringBuilder(expression.length() + 8);
        for (String token : expression.trim().split("\\s+")) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            switch (token) {
                case "AND", "and" -> out.append("&&");
                case "OR", "or" -> out.append("||");
                default -> out.append(quoteBareword(token));
            }
        }
        return out.toString();
    }

    /** CEL keywords/operators that must never be quoted as string literals. */
    private static final java.util.Set<String> CEL_KEYWORDS = java.util.Set.of(
            "in", "true", "false", "null", "size", "has", "int", "uint", "double", "string", "bytes", "type", "dyn");

    private String quoteBareword(String token) {
        boolean pathLike = token.startsWith("subject.")
                || token.startsWith("resource.")
                || token.startsWith("context.")
                || token.startsWith("resources.")
                || token.equals("subject")
                || token.equals("resource")
                || token.equals("resources")
                || token.equals("context")
                || token.equals("tenant");
        boolean alreadyLiteral = token.startsWith("'") || token.startsWith("\"");
        boolean numeric = token.matches("-?\\d+(\\.\\d+)?");

        if (pathLike || alreadyLiteral || numeric || CEL_KEYWORDS.contains(token)) {
            return token;
        }
        // bare identifier (e.g. USER, admin): the legacy DSL treated these as strings
        if (token.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            return "'" + token + "'";
        }
        return token; // operators, parentheses, macros etc: native CEL passes through
    }

    /**
     * Adapt the {@link RequestContext} into the CEL activation map. Values are
     * plain Java types (String/Number/List/Map) which CEL adapts natively.
     */
    Map<String, Object> toActivation(RequestContext context) {
        Map<String, Object> root = new HashMap<>();
        root.put("subject", subjectMap(context.getSubject()));

        List<Resource> resources = context.getResources();
        if (resources != null && !resources.isEmpty()) {
            root.put("resource", resourceMap(resources.getFirst()));
            root.put("resources", resources.stream().map(this::resourceMap).toList());
        }

        if (context.getContext() != null) {
            root.put("context", context.getContext());
        }
        root.put("tenant", context.getTenantId());
        return root;
    }

    private Map<String, Object> subjectMap(Subject subject) {
        Map<String, Object> map = new HashMap<>();
        if (subject == null) {
            return map;
        }
        if (subject.getAttributes() != null) {
            map.putAll(subject.getAttributes());
        }
        if (subject.getId() != null) {
            map.put("id", subject.getId());
        }
        if (subject.getType() != null) {
            map.put("type", subject.getType().name());
        }
        if (subject.getRoles() != null) {
            map.put("roles", List.copyOf(subject.getRoles()));
        }
        if (subject.getPermissions() != null) {
            map.put("permissions", List.copyOf(subject.getPermissions()));
        }
        return map;
    }

    private Map<String, Object> resourceMap(Resource resource) {
        Map<String, Object> map = new HashMap<>();

        Map<String, Object> attributes = null;
        String id = null;
        String type = null;
        if (resource instanceof Target target) {
            attributes = target.getAttributes();
            id = target.getId();
            type = target.getType();
        } else if (resource instanceof Intent intent) {
            attributes = intent.getAttributes();
            id = intent.getId();
            type = intent.getType();
        }

        if (attributes != null) {
            map.putAll(attributes);
        }
        if (id != null) {
            map.put("id", id);
        }
        if (type != null) {
            map.put("type", type);
        }
        return map;
    }
}
