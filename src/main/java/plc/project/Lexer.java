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

        //may need to change
        Token token;
        if (peek("[A-Za-z@_][A-Za-z0-9_-]*")) {
            token= lexIdentifier();}
        else if (peek("-?[1-9][0-9]*|0")) {
            token= lexNumber();
        }
        else if (peek("'.'")) {
           token = lexCharacter();
        }
        else if (peek("\"([^\\\\\"\\n\\r]|\\\\[bnrt'\"\\\\.])*\"")) {
           token = lexString();
        }
        else {
             token= lexOperator();
        }
        return token;
    }


    public Token lexIdentifier() {
        chars.advance(); // Advance past the first character
        while (peek("[A-Za-z0-9_\\-]")) {
            chars.advance();
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }



    public Token lexNumber() {
        // Regular expression to match integers and decimals
        String numberPattern = "-?(0|[1-9][0-9]*)(\\.[0-9]+)?";

        // Check if the input matches the number pattern
        if (peek(numberPattern)) {
            // If the input matches the pattern, get the matched number
            String matchedNumber = String.valueOf(match(numberPattern));

            // Check if the matched number contains a decimal point
            boolean isDecimal = matchedNumber.contains(".");

            // Emit the appropriate token type based on whether the number is decimal or integer
            if (isDecimal) {
                return chars.emit(Token.Type.DECIMAL);
            } else {
                return chars.emit(Token.Type.INTEGER);
            }
        }

        // If the input does not match the number pattern, throw an exception
        throw new UnsupportedOperationException("Invalid number at index " + chars.index);
    }







    public Token lexCharacter() {
        if (match("'.'")) {
            // Single character
            return chars.emit(Token.Type.CHARACTER);
        } else if (peek("'\\\\n'")) {
            // Newline escape
            return lexString();
        } else if (peek("'\\\\[nrt]'")) {
            // Handle escape sequences like '\n', '\r', '\t'
            lexEscape();
            return lexString();
        } else if (peek("'\\\\'")) {
            // Match literal backslash: '\\'
            return lexString();
        } else if (peek("'[^\\\\]'")) {
            // Match single character except backslash: '[^\\]'
            return lexString();
        }
        else if (match("'[^']'")) {
            // Single character
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
        if (match("!=")) {
            if (peek("=")) { // Check if the next character is '='
                match("="); // Match the second '='
            }
            return chars.emit(Token.Type.OPERATOR);
        } else if (match("==")) {
            return chars.emit(Token.Type.OPERATOR);
        } else if (match("&&")) {
            return chars.emit(Token.Type.OPERATOR);
        } else if (match("||")) {
            return chars.emit(Token.Type.OPERATOR);
        } else if (match("=")) {
            return chars.emit(Token.Type.OPERATOR);
        } else {
            chars.advance();
            return chars.emit(Token.Type.OPERATOR);
        }
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
