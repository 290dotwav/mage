package mage.server.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import mage.interfaces.MageServer;
import mage.server.Main;
import mage.utils.MageVersion;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * "call" frames: { kind: "call", method, gameId?, args[] } become the MageServer method of
 * the same name.
 * <p>
 * MageServer is compiled without parameter names, so the door carries its own copy of the
 * interface's signatures (mage-server-signatures.txt, one line per method, copied from
 * MageServer.java). At start-up each line is checked against the real interface by
 * reflection; a method whose signature changed upstream is refused with a clear error
 * instead of being called with shifted arguments.
 * <p>
 * Parameter filling: "sessionId" always comes from the socket (never from the browser),
 * "gameId" comes from the frame's gameId when present, every other parameter is taken from
 * args in order. A missing trailing arg is "" for String, the server's own version for
 * MageVersion, zero/false for primitives and null otherwise.
 */
final class Wire {

    private static final Logger logger = Logger.getLogger(Wire.class);
    private static final String SIGNATURES = "/mage/server/web/mage-server-signatures.txt";
    private static final JsonElement MISSING = null;

    static final class Param {
        final String name;
        final Class<?> type;
        final Type generic;

        Param(String name, Class<?> type, Type generic) {
            this.name = name;
            this.type = type;
            this.generic = generic;
        }
    }

    static final class Sig {
        final String text;
        final Method method;
        final Param[] params;

        Sig(String text, Method method, Param[] params) {
            this.text = text;
            this.method = method;
            this.params = params;
        }
    }

    private final MageServer server;
    private final Map<String, Sig> sigs = new TreeMap<>();

    Wire(MageServer server) {
        this.server = server;
        load();
    }

    Collection<String> methodNames() {
        return sigs.keySet();
    }

    private void load() {
        Map<String, Method> byName = new HashMap<>();
        for (Method m : MageServer.class.getMethods()) {
            byName.put(m.getName(), m); // MageServer has no overloads
        }
        Set<String> seen = new HashSet<>();
        try (InputStream in = Wire.class.getResourceAsStream(SIGNATURES)) {
            if (in == null) {
                throw new IllegalStateException("missing resource " + SIGNATURES);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                Sig sig = parse(line, byName);
                if (sig != null) {
                    sigs.put(sig.method.getName(), sig);
                    seen.add(sig.method.getName());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + SIGNATURES, e);
        }
        for (String name : byName.keySet()) {
            if (!seen.contains(name)) {
                logger.warn("Web door: MageServer." + name + " has no line in mage-server-signatures.txt, it is not callable from the web");
            }
        }
        logger.info("Web door: " + sigs.size() + " MageServer methods wired");
    }

    private static Sig parse(String line, Map<String, Method> byName) {
        int open = line.indexOf('('), close = line.lastIndexOf(')');
        if (open <= 0 || close < open) {
            logger.warn("Web door: bad signature line: " + line);
            return null;
        }
        String name = line.substring(0, open).trim();
        Method m = byName.get(name);
        if (m == null) {
            logger.warn("Web door: signature '" + line + "' has no MageServer method (removed upstream?), skipped");
            return null;
        }
        String inner = line.substring(open + 1, close).trim();
        List<String[]> declared = new ArrayList<>();
        if (!inner.isEmpty()) {
            for (String part : splitTopLevel(inner)) {
                String p = part.trim();
                int sp = p.lastIndexOf(' ');
                declared.add(new String[]{p.substring(0, sp).trim(), p.substring(sp + 1).trim()});
            }
        }
        Class<?>[] types = m.getParameterTypes();
        Type[] generics = m.getGenericParameterTypes();
        if (types.length != declared.size()) {
            logger.warn("Web door: signature '" + line + "' has " + declared.size() + " params, MageServer has " + types.length + ", method refused");
            return null;
        }
        Param[] params = new Param[types.length];
        for (int i = 0; i < types.length; i++) {
            String declaredType = declared.get(i)[0];
            int lt = declaredType.indexOf('<');
            String rawDeclared = lt > 0 ? declaredType.substring(0, lt) : declaredType;
            if (!rawDeclared.equals(types[i].getSimpleName())) {
                logger.warn("Web door: signature '" + line + "' param " + i + " is " + declaredType + " but MageServer has " + types[i].getSimpleName() + ", method refused");
                return null;
            }
            params[i] = new Param(declared.get(i)[1], types[i], generics[i]);
        }
        return new Sig(line, m, params);
    }

    /**
     * Split "A a, Map<K, V> b" on the commas that are not inside generics.
     */
    private static List<String> splitTopLevel(String s) {
        List<String> out = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out;
    }

    Sig signature(String method) {
        return sigs.get(method);
    }

    /**
     * Run one "call" frame. Returns the method's return value (null for void).
     */
    Object invoke(String method, JsonElement gameId, JsonArray args, String sessionId) throws Exception {
        Sig sig = sigs.get(method);
        if (sig == null) {
            throw new IllegalArgumentException("unknown MageServer method '" + method + "'; known: " + sigs.keySet());
        }
        Object[] values = new Object[sig.params.length];
        int ai = 0;
        for (int i = 0; i < sig.params.length; i++) {
            Param p = sig.params[i];
            if ("sessionId".equals(p.name)) {
                values[i] = sessionId;
            } else if ("gameId".equals(p.name) && gameId != null && !gameId.isJsonNull()) {
                values[i] = UUID.fromString(gameId.getAsString());
            } else {
                JsonElement e = ai < args.size() ? args.get(ai) : MISSING;
                ai++;
                try {
                    values[i] = convert(e, p, ai > args.size());
                } catch (RuntimeException ex) {
                    throw new IllegalArgumentException("arg " + (ai - 1) + " (" + p.name + ") of " + sig.text + ": " + ex.getMessage());
                }
            }
        }
        if (ai < args.size()) {
            throw new IllegalArgumentException("too many args (" + args.size() + ") for " + sig.text);
        }
        try {
            return sig.method.invoke(server, values);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw ex;
        }
    }

    static Object convert(JsonElement e, Param p, boolean missing) {
        Class<?> t = p.type;
        if (e == null || e.isJsonNull()) {
            if (t == String.class) {
                return missing ? "" : null;
            }
            if (t == MageVersion.class) {
                return Main.getVersion();
            }
            if (t == int.class || t == long.class || t == double.class) {
                return t == int.class ? (Object) 0 : t == long.class ? (Object) 0L : (Object) 0d;
            }
            if (t == boolean.class) {
                return false;
            }
            return null;
        }
        if (t == String.class) {
            return e.isJsonPrimitive() ? e.getAsString() : e.toString();
        }
        if (t == UUID.class) {
            return UUID.fromString(e.getAsString());
        }
        if (t == boolean.class || t == Boolean.class) {
            return e.getAsBoolean();
        }
        if (t == int.class || t == Integer.class) {
            return e.getAsInt();
        }
        if (t == long.class || t == Long.class) {
            return e.getAsLong();
        }
        if (t == double.class || t == Double.class) {
            return e.getAsDouble();
        }
        if (t.isEnum()) {
            return enumValue(t, e.getAsString());
        }
        if (t == MageVersion.class) {
            // a browser has no MageVersion of its own: the server's is used
            return Main.getVersion();
        }
        if (t == Object.class) {
            // sendPlayerAction's data: a UUID string, a string, an integer, a boolean or null
            if (e.isJsonPrimitive()) {
                JsonPrimitive prim = e.getAsJsonPrimitive();
                if (prim.isBoolean()) {
                    return prim.getAsBoolean();
                }
                if (prim.isNumber()) {
                    double d = prim.getAsDouble();
                    return d == Math.rint(d) ? (Object) (int) d : (Object) d;
                }
                String s = prim.getAsString();
                try {
                    return UUID.fromString(s);
                } catch (IllegalArgumentException ignore) {
                    return s;
                }
            }
            return Frames.GSON.fromJson(e, Object.class);
        }
        return Frames.GSON.fromJson(e, p.generic);
    }

    /**
     * Enum by name, or by its toString() (PlayerType "Computer - mad", for the config names).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object enumValue(Class<?> enumType, String text) {
        for (Object c : enumType.getEnumConstants()) {
            if (((Enum) c).name().equals(text) || c.toString().equals(text)) {
                return c;
            }
        }
        throw new IllegalArgumentException("'" + text + "' is not one of " + Arrays.toString(enumType.getEnumConstants()));
    }
}
