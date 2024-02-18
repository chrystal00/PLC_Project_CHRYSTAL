package plc.project;

import java.util.List;
import java.util.ArrayList;
import java.math.BigInteger; // added
import java.math.BigDecimal; // added
import java.util.Optional;



/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;


    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO

    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO

    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {

        throw new UnsupportedOperationException(); //TODO

    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO


    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    // Part a
    public Ast.Statement parseStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        Ast.Expression expression = parseExpression(); // Parse the expression

        // Check if there's an assignment
        if (match("=")) {
            Ast.Expression assignmentValue = parseExpression();
            return new Ast.Statement.Assignment(expression, assignmentValue);
        } else {
            return new Ast.Statement.Expression(expression);
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression side1 = parseComparisonExpression();

        while (true) {
            if (match("&&")) {
                String exp = tokens.get(-1).getLiteral();
                Ast.Expression side2 = parseComparisonExpression();
                side1 = new Ast.Expression.Binary(exp, side1, side2);
            } else if (match("||")) {
                String exp = tokens.get(-1).getLiteral();
                Ast.Expression side2 = parseComparisonExpression();
                side1 = new Ast.Expression.Binary(exp, side1, side2);
            } else {
                break; // no more expressions, exit loop
            }
        }

        return side1;
    }

    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression side1 = parseAdditiveExpression();

        while (true) {
            if (match("<")) {
                String exp = tokens.get(-1).getLiteral();
                Ast.Expression side2 = parseAdditiveExpression();
                side1= new Ast.Expression.Binary(exp, side1, side2);
            } else if (match(">")) {
                String exp = tokens.get(-1).getLiteral();
                Ast.Expression side2 = parseAdditiveExpression();
                side1 = new Ast.Expression.Binary(exp, side1, side2);
            } else if (match("==")) {
                String exp = tokens.get(-1).getLiteral();
                Ast.Expression side2 = parseAdditiveExpression();
                side1 = new Ast.Expression.Binary(exp, side1, side2);
            } else if (match("!=")) {
                String exp = tokens.get(-1).getLiteral();
                Ast.Expression side2 = parseAdditiveExpression();
                side1= new Ast.Expression.Binary(exp, side1, side2);
            } else {
                break; //no more expressions, exit loop
            }
        }

        return side1;
    }


    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression side1 = parseMultiplicativeExpression();

        while (true) {
            if (match("+")) {
                String exp = tokens.get(-1).getLiteral();
                Ast.Expression side2 = parseMultiplicativeExpression();
                side1 = new Ast.Expression.Binary(exp, side1, side2);
            } else if (match("-")) {
                String exp = tokens.get(-1).getLiteral();
                Ast.Expression side2 = parseMultiplicativeExpression();
                side1= new Ast.Expression.Binary(exp, side1, side2);
            } else {
                break; //no more expressions, exit the loop
            }
        }

        return side1;
    }


    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression side1 = parsePrimaryExpression();

        while (true) {
            if (match("*")) {
                String exp = tokens.get(-1).getLiteral();
                Ast.Expression side2 = parsePrimaryExpression();
                side1= new Ast.Expression.Binary(exp, side1, side2);
            } else if (match("/")) {
                String exp = tokens.get(-1).getLiteral();
                Ast.Expression side2 = parsePrimaryExpression();
                side1 = new Ast.Expression.Binary(exp,side1, side2);
            } else if (match("^")) {
                String exp = tokens.get(-1).getLiteral();
                Ast.Expression side2 = parsePrimaryExpression();
                side1 = new Ast.Expression.Binary(exp, side1, side2);
            } else {
                break; //no more expressions, exit the loop
            }
        }

        return side1;
    }


    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (match("NIL")) {
            return new Ast.Expression.Literal(null);
        } else if (match("TRUE")) {
            return new Ast.Expression.Literal(true);
        } else if (match("FALSE")) {
            return new Ast.Expression.Literal(false);
        } else if (match(Token.Type.INTEGER)) {
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.CHARACTER)) {
            return parseCharacterLiteral(tokens.get(-1).getLiteral());
        } else if (match(Token.Type.STRING)) {
            return parseStringLiteral(tokens.get(-1).getLiteral());
        } else if (match("(")) {
            Ast.Expression expr = parseExpression();
            expect(")");
            return new Ast.Expression.Group(expr);
        } else if (match(Token.Type.IDENTIFIER)) {
            return parseIdentifierExpression();
        } else {
            throw new ParseException("Invalid Primary Expression", tokens.get(0).getIndex());
        }
    }

    private Ast.Expression parseCharacterLiteral(String literal) throws ParseException {
        char chars = literal.charAt(1); // Extracting the character from the literal
        if (chars == '\\') {
            chars = handleEscapeCharacters(literal.charAt(2));
        }
        return new Ast.Expression.Literal(chars);
    }

    private Ast.Expression parseStringLiteral(String literal) throws ParseException {
        String value = literal.substring(1, literal.length() - 1); // Removing quotes
        value = value.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\b", "\b")
                .replace("\\r", "\r")
                .replace("\\'", "\'")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
        return new Ast.Expression.Literal(value);
    }

    private Ast.Expression parseGroupExpression() throws ParseException {
        Ast.Expression expr = parseExpression();
        expect(")");
        return new Ast.Expression.Group(expr);
    }

    private Ast.Expression parseIdentifierExpression() throws ParseException {
        String identity = tokens.get(-1).getLiteral();
        if (match("(")) {
            return parseFunctionCall(identity);
        } else if (match("[")) {
            Ast.Expression indexExpr = parseExpression();
            expect("]");
            return new Ast.Expression.Access(Optional.of(indexExpr), identity);
        } else {
            return new Ast.Expression.Access(Optional.empty(), identity);
        }
    }



    private Ast.Expression parseFunctionCall(String parseParen) throws ParseException {
        List<Ast.Expression> arguments = new ArrayList<>();
        if (!match(")")) {
            arguments = parseExpressionList();
        }


        return new Ast.Expression.Function(parseParen, arguments);
    }





    private List<Ast.Expression> parseExpressionList() throws ParseException {
        List<Ast.Expression> expressions = new ArrayList<>();
        expressions.add(parseExpression());
        while (match(",")) {
            expressions.add(parseExpression());
        }
        return expressions;
    }

    private char handleEscapeCharacters(char x) throws ParseException {
        switch (x) {
            case 'n':
                return '\n';
            case 'b':
                return '\b';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            case '\'':
            case '\"':
            case '\\':
                return x;
            default:
                throw new ParseException("Invalid escape sequence: \\" + x, tokens.get(-1).getIndex());
        }
    }

    private void expect(String expected) throws ParseException {
        if (!match(expected)) {
            if (tokens.has(0)) {
                throw new ParseException("Expected '" + expected + "'", tokens.get(0).getIndex());
            } else {
                throw new ParseException("Unexpected end of input. Expected '" + expected + "'", -1);
            }
        }
    }




    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern Object: " + patterns[i]);
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
