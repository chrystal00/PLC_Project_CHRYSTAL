package plc.project;

import java.util.ArrayList;
import java.util.List;

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
        if (peek("@?[A-Za-z_][A-Za-z0-9_-]*")) {
            return lexIdentifier();
        }
        else if (match("-?[1-9][0-9]*")) {
            // Non-zero integer
            return chars.emit(Token.Type.INTEGER);
        }
        else if (match("0")) {
            // Zero
            return chars.emit(Token.Type.INTEGER);
        }
        else if (match("-?[1-9][0-9]*\\.[0-9]+")) {
            // Decimal
            return chars.emit(Token.Type.DECIMAL);
        }
        else if (match("-?0\\.[0-9]+")) {
            // Decimal starting with zero
            return chars.emit(Token.Type.DECIMAL);
        }
        else if (peek("'[^\\\\\\n\\r]'")) {
            return lexCharacter();
        }
        else if (peek("\"([^\\\\\"\\n\\r]|\\\\[bnrt'\"\\\\.])*\"")) {
            return lexString();
        }
        else if (peek("[^\\s]")) {
            return lexOperator();
        }
        else {
            throw new UnsupportedOperationException("Invalid token at index " + chars.index);
        }
    }

    public Token lexIdentifier() {
        if (match("@?[A-Za-z_][A-Za-z0-9_-]*")) {
            return chars.emit(Token.Type.IDENTIFIER);
        }
        else
        {
            throw new UnsupportedOperationException("Invalid identifier at index " + chars.index);
        }
    }

    public Token lexNumber() {
        if (match("-?[1-9][0-9]+")) {
            // Non-zero integer with multiple digits
            return chars.emit(Token.Type.INTEGER);
        }
        else if (match("0")) {
            // Zero
            return chars.emit(Token.Type.INTEGER);
        }
        else if (match("-?[1-9][0-9]*\\.[0-9]+")) {
            // Decimal
            return chars.emit(Token.Type.DECIMAL);
        }
        else if (match("-?0\\.[0-9]+")) {
            // Decimal starting with zero
            return chars.emit(Token.Type.DECIMAL);
        }
        else {
            throw new UnsupportedOperationException("Invalid number at index " + chars.index);
        }

    }

    public Token lexCharacter() {
        if (match("'.'")) {
            // Single character
            return chars.emit(Token.Type.CHARACTER);
        }
        else if (peek("'\\\\n'")) {
            // Newline escape
            return chars.emit(Token.Type.CHARACTER);
        }
        else if (match("'[^\\\\\\n\\r]'")) {
            return chars.emit(Token.Type.CHARACTER);
        }
        else if (match("'\\\\[nrt]'")) {
            lexEscape(); // Handle escape sequences like '\n', '\r', '\t'
            return chars.emit(Token.Type.CHARACTER);
        }
        else {
            throw new UnsupportedOperationException("Invalid character at index " + chars.index);
        }
    }

    public Token lexString() {
        if (match("\"([^\\\\\"\\n\\r]|\\\\[bnrt'\"\\\\.])*\"")) {
            return chars.emit(Token.Type.STRING);
        }
        else {
            throw new UnsupportedOperationException("Invalid string at index " + chars.index);
        }
    }

    public void lexEscape() {
        if (match("\\\\[bnrt'\"\\\\]")) {
            // Valid escape sequence
        }
        else {
            throw new UnsupportedOperationException("Invalid escape sequence at index " + chars.index);
        }
    }

    public Token lexOperator() {
        if (match("[^\\s]")) {
            return chars.emit(Token.Type.OPERATOR);
        }
        else {
            throw new UnsupportedOperationException("Invalid operator at index " + chars.index);
        }
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i <patterns.length; i++) {
            if ( !chars.has(i) ||
            !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
        //throw new UnsupportedOperationException();
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
