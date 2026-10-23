package noesis.core.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A deliberately small, dependency-free JSON reader and writer.
 *
 * <p>It only supports the subset of JSON that this framework needs: objects,
 * arrays, strings, numbers, booleans and null. It is intentionally compact so
 * the whole project stays free of third-party libraries, while still being
 * strict enough to round-trip the payloads exchanged with LLM HTTP endpoints.</p>
 */
public final class Json {

    private Json() {}

    // ----------------------------------------------------------------- parsing

    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipWhitespace();
        Object value = p.parseValue();
        p.skipWhitespace();
        if (!p.atEnd()) {
            throw new JsonException("Trailing characters at index " + p.index);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (!(v instanceof Map)) {
            throw new JsonException("Expected JSON object");
        }
        return (Map<String, Object>) v;
    }

    // ----------------------------------------------------------------- writing

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    // --------------------------------------------------------------- builders

    /** Start building a JSON object. */
    public static ObjectBuilder object() {
        return new ObjectBuilder();
    }

    /** Start building a JSON array. */
    public static ArrayBuilder array() {
        return new ArrayBuilder();
    }

    // --------------------------------------------------------------- helpers

    @SuppressWarnings("unchecked")
    public static String getString(Map<String, Object> obj, String key) {
        Object v = obj.get(key);
        return v == null ? null : String.valueOf(v);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getObject(Map<String, Object> obj, String key) {
        Object v = obj.get(key);
        if (v == null) return null;
        if (!(v instanceof Map)) throw new JsonException("Key '" + key + "' is not an object");
        return (Map<String, Object>) v;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getArray(Map<String, Object> obj, String key) {
        Object v = obj.get(key);
        if (v == null) return null;
        if (!(v instanceof List)) throw new JsonException("Key '" + key + "' is not an array");
        return (List<Object>) v;
    }

    public static double getDouble(Map<String, Object> obj, String key) {
        Object v = obj.get(key);
        if (v == null) throw new JsonException("Missing number key '" + key + "'");
        if (v instanceof Number) return ((Number) v).doubleValue();
        return Double.parseDouble(String.valueOf(v));
    }

    public static boolean getBool(Map<String, Object> obj, String key, boolean dflt) {
        Object v = obj.get(key);
        return v == null ? dflt : Boolean.parseBoolean(String.valueOf(v));
    }

    // --------------------------------------------------------- writing engine

    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Map) {
            writeObject(sb, (Map<?, ?>) value);
        } else if (value instanceof List) {
            writeArray(sb, (List<?>) value);
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(String.valueOf(value));
        } else {
            writeString(sb, String.valueOf(value));
        }
    }

    private static void writeObject(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(sb, String.valueOf(e.getKey()));
            sb.append(':');
            writeValue(sb, e.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, List<?> list) {
        sb.append('[');
        boolean first = true;
        for (Object e : list) {
            if (!first) sb.append(',');
            first = false;
            writeValue(sb, e);
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------- builders

    public static final class ObjectBuilder {
        private final Map<String, Object> map = new LinkedHashMap<>();

        public ObjectBuilder put(String key, Object value) {
            map.put(key, value);
            return this;
        }

        public ObjectBuilder put(String key, String value) {
            map.put(key, value);
            return this;
        }

        public ObjectBuilder put(String key, Number value) {
            map.put(key, value);
            return this;
        }

        public ObjectBuilder put(String key, Boolean value) {
            map.put(key, value);
            return this;
        }

        public Map<String, Object> build() {
            return map;
        }
    }

    public static final class ArrayBuilder {
        private final List<Object> list = new ArrayList<>();

        public ArrayBuilder add(Object value) {
            list.add(value);
            return this;
        }

        public List<Object> build() {
            return list;
        }
    }

    // ---------------------------------------------------------------- parser

    private static final class Parser {
        private final String s;
        private int index;

        Parser(String s) {
            this.s = s;
            this.index = 0;
        }

        boolean atEnd() {
            return index >= s.length();
        }

        void skipWhitespace() {
            while (index < s.length() && Character.isWhitespace(s.charAt(index))) index++;
        }

        Object parseValue() {
            skipWhitespace();
            if (atEnd()) throw new JsonException("Unexpected end of input");
            char c = s.charAt(index);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') { index++; return map; }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                char c = next();
                if (c == '}') break;
                if (c != ',') throw new JsonException("Expected ',' or '}' at " + (index - 1));
            }
            return map;
        }

        List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') { index++; return list; }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                char c = next();
                if (c == ']') break;
                if (c != ',') throw new JsonException("Expected ',' or ']' at " + (index - 1));
            }
            return list;
        }

        String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) throw new JsonException("Unterminated string");
                char c = s.charAt(index++);
                if (c == '"') break;
                if (c == '\\') {
                    if (atEnd()) throw new JsonException("Unterminated escape");
                    char e = s.charAt(index++);
                    sb.append(switch (e) {
                        case '"' -> '"';
                        case '\\' -> '\\';
                        case '/' -> '/';
                        case 'b' -> '\b';
                        case 'f' -> '\f';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        case 'u' -> parseUnicodeEscape();
                        default -> throw new JsonException("Invalid escape \\" + e);
                    });
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        char parseUnicodeEscape() {
            if (index + 4 > s.length()) throw new JsonException("Invalid unicode escape");
            String hex = s.substring(index, index + 4);
            index += 4;
            return (char) Integer.parseInt(hex, 16);
        }

        Object parseNumber() {
            int start = index;
            while (index < s.length() && "+-eE.0123456789".indexOf(s.charAt(index)) >= 0) index++;
            String num = s.substring(start, index);
            if (num.isEmpty()) throw new JsonException("Invalid number at " + start);
            try {
                if (num.contains(".") || num.contains("e") || num.contains("E")) {
                    return Double.parseDouble(num);
                }
                long l = Long.parseLong(num);
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
                return l;
            } catch (NumberFormatException ex) {
                return Double.parseDouble(num);
            }
        }

        Object parseBoolean() {
            if (s.startsWith("true", index)) { index += 4; return Boolean.TRUE; }
            if (s.startsWith("false", index)) { index += 5; return Boolean.FALSE; }
            throw new JsonException("Invalid literal at " + index);
        }

        Object parseNull() {
            if (s.startsWith("null", index)) { index += 4; return null; }
            throw new JsonException("Invalid literal at " + index);
        }

        char peek() {
            skipWhitespace();
            return atEnd() ? '\0' : s.charAt(index);
        }

        char next() {
            if (atEnd()) throw new JsonException("Unexpected end of input");
            return s.charAt(index++);
        }

        void expect(char c) {
            skipWhitespace();
            if (atEnd() || s.charAt(index) != c) {
                throw new JsonException("Expected '" + c + "' at " + index);
            }
            index++;
        }
    }

    /** Thrown when JSON cannot be parsed or is structurally invalid. */
    public static final class JsonException extends RuntimeException {
        public JsonException(String message) {
            super(message);
        }
    }
}
