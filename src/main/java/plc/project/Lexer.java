package plc.project;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {

        //throw new UnsupportedOperationException(); Comment this out?
        List<Token> tokens = new ArrayList<>();
        while (chars.has(0)) {
            char current = chars.get(0);
            if (Character.isWhitespace(current)) {
                chars.advance(); // Skip whitespace
                chars.skip();
            } else {
                tokens.add(lexToken());
            }
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        // Skip whitespace
        while (chars.has(0) && Character.isWhitespace(chars.get(0))) {
            chars.advance();
        }

        if (peek("[A-Za-z@_][A-Za-z0-9_-]*")) {
            return lexIdentifier();
        } else if (peek("-|\\d")) {
            return lexNumber();
        } else if (peek("\\'")) {
            return lexCharacter();
        } else if (peek("[\"]")) {
            return lexString();
        } else {
            return lexOperator();
        }
    }



    public Token lexIdentifier() {
        StringBuilder identifier = new StringBuilder(); // Initialize StringBuilder to store the identifier

        // Skip whitespace
        while (chars.has(0) && Character.isWhitespace(chars.get(0))) {
            chars.advance();
        }

        // Append characters to the identifier while they match the identifier pattern
        while (peek("[A-Za-z0-9_\\-]")) {
            identifier.append(chars.get(0)); // Append current character to the identifier
            chars.advance(); // Move to the next character
        }

        // Emit identifier token
        return chars.emit(Token.Type.IDENTIFIER);
    }





    public Token lexNumber() {
        boolean isNegative = false;
        boolean hasDigits = false;
        boolean hasDecimal = false;

        if (match("0")) {
            hasDigits = true; // If the number starts with '0', it has digits
            if (peek("\\.")) {
                hasDecimal = true; // If there's a '.', it's a decimal number
                match("\\."); // Consume the decimal point
                if (!match("[0-9]")) {
                    // If there are no digits following the decimal point, it's an integer
                    return chars.emit(Token.Type.INTEGER);
                }
                while (match("[0-9]")); // Consume remaining digits
            }
        } else if (peek("-")) {
            // Check if the negative sign is followed by digits
            chars.advance(); // Move past the negative sign
            isNegative = true;
            if (!match("[0-9]")) {
                // If the hyphen is not followed by a digit, it's an operator
                return chars.emit(Token.Type.OPERATOR);
            }
            // If the hyphen is followed by a digit, proceed to parse the negative integer
            hasDigits = true;
            while (match("[0-9]")); // Consume remaining digits
            if (peek("\\.")) {
                hasDecimal = true; // If there's a '.', it's a decimal number
                match("\\."); // Consume the decimal point
                if (!match("[0-9]")) {
                    // If there are no digits following the decimal point, it's an integer
                    return chars.emit(Token.Type.INTEGER);
                }
                while (match("[0-9]")); // Consume remaining digits
            }
        } else if (match("[1-9]")) {
            hasDigits = true; // If the number starts with a non-zero digit, it has digits
            while (match("[0-9]")); // Consume remaining digits
            if (peek("\\.")) {
                hasDecimal = true; // If there's a '.', it's a decimal number
                match("\\."); // Consume the decimal point
                if (!match("[0-9]")) {
                    // If there are no digits following the decimal point, it's an integer
                    return chars.emit(Token.Type.INTEGER);
                }
                while (match("[0-9]")); // Consume remaining digits
            }
        } else {
            // No valid number pattern matched, return an OPERATOR token
            return chars.emit(Token.Type.OPERATOR);
        }

        if (isNegative && !hasDigits && !hasDecimal) {
            // If it's a negative sign without any digits or decimal point, it's an operator
            return chars.emit(Token.Type.OPERATOR);
        } else if (hasDecimal) {
            return chars.emit(Token.Type.DECIMAL); // Decimal token
        } else if (isNegative) {
            // If it's a negative sign followed by digits, it's a negative integer
            return chars.emit(Token.Type.INTEGER);
        } else {
            return chars.emit(Token.Type.INTEGER); // Integer token
        }
    }







    public Token lexCharacter() throws ParseException {
        chars.advance(); // Move past the opening apostrophe

        if (!chars.has(0)) {
            throw new ParseException("Unterminated character literal", chars.index);
        }

        char currentChar = chars.get(0);

        if (currentChar == '\\') {
            chars.advance(); // Move past the backslash
            lexEscape(); // Handle escape sequence
        } else {
            chars.advance(); // Move past the character
        }

        if (!chars.has(0) || chars.get(0) != '\'') {
            throw new ParseException("Unterminated character literal", chars.index);
        }

        chars.advance(); // Move past the closing apostrophe

        return chars.emit(Token.Type.CHARACTER);
    }










    public Token lexString() throws ParseException {
        chars.advance(); // Move passed the opening double quote
        StringBuilder stringBuilder = new StringBuilder();

        while (chars.has(0)) {
            char currentChar = chars.get(0);

            if (currentChar == '\\') {
                chars.advance(); // Move passed the backslash
                if (!chars.has(0)) {
                    throw new ParseException("Unterminated escape sequence", chars.index - 1);
                }
                char escapedChar = chars.get(0);
                if (escapedChar == '"' || escapedChar == '\\' || escapedChar == 'b' || escapedChar == 'n'
                        || escapedChar == 'r' || escapedChar == 't') {
                    stringBuilder.append(escapedChar);
                    chars.advance(); // Move passed the escaped character
                } else {
                    throw new ParseException("Invalid escape sequence", chars.index - 1);
                }
            } else if (currentChar == '"') {
                chars.advance(); // Move passed the closing double quote
                return chars.emit(Token.Type.STRING);
            } else {
                stringBuilder.append(currentChar);
                chars.advance(); // Move to the next character
            }
        }

        // If we reached this point, it means the string is unterminated
        throw new ParseException("Unterminated string literal", chars.index);
    }



    public void lexEscape() throws ParseException {
        char escapeChar = chars.get(0);
        switch (escapeChar) {
            case 'b':
            case 'n':
            case 'r':
            case 't':
            case '\'':
            case '\"':
            case '\\':
                chars.advance(); // Advance passed the escape character
                break;
            default:
                throw new ParseException("Invalid escape sequence", chars.index);
        }
    }


    public Token lexOperator() {
        Map<String, Runnable> operatorActions = new HashMap<>();
        operatorActions.put("!=", () -> {});
        operatorActions.put("==", () -> {});
        operatorActions.put("&&", () -> {});
        operatorActions.put("||", () -> {});

        String[] singleOperators = new String[]{"!", "=", "&", "|"};

        for (String op : singleOperators) {
            if (match(op)) {
                if ("!".equals(op)) {
                    match("=");
                } else if ("=".equals(op)) {
                    match("=");
                } else if ("&".equals(op)) {
                    match("&");
                } else if ("|".equals(op)) {
                    match("|");
                }
                return chars.emit(Token.Type.OPERATOR);
            }
        }

        chars.advance();
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            // Check if there are enough characters in the stream for the pattern
            if (!chars.has(i)) {
                return false;
            }
            // Check if the character at index i matches the pattern
            if (!String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
        //throw new UnsupportedOperationException();
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }


    }

}