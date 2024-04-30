package plc.project;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 * <p>
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 * <p>
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */

/*
Parser for following grammar:


source ::= global* function*

global ::= ( list | mutable | immutable ) ';'
list ::= 'LIST' identifier '=' '[' expression (',' expression)* ']'
mutable ::= 'VAR' identifier ('=' expression)?
immutable ::= 'VAL' identifier '=' expression

function ::= 'FUN' identifier '(' (identifier (',' identifier)* )? ')' 'DO' block 'END'

block ::= statement*

statement ::=
    'LET' identifier ('=' expression)? ';' |
    'SWITCH' expression ('CASE' expression ':' block)* 'DEFAULT' block 'END' |
    'IF' expression 'DO' block ('ELSE' block)? 'END' |
    'WHILE' expression 'DO' block 'END' |
    'RETURN' expression ';' |
    expression ('=' expression)? ';'

expression ::= logical_expression

logical_expression ::= comparison_expression (('&&' | '||') comparison_expression)*
comparison_expression ::= additive_expression (('' | '==' | '!=') additive_expression)*
additive_expression ::= multiplicative_expression (('+' | '-') multiplicative_expression)*
multiplicative_expression ::= primary_expression (('*' | '/' | '^') primary_expression)*

primary_expression ::=
    'NIL' | 'TRUE' | 'FALSE' |
    integer | decimal | character | string |
    '(' expression ')' |
    identifier ('(' (expression (',' expression)*)? ')')? |
    identifier '[' expression ']'

identifier ::= ( '@' | [A-Za-z] ) [A-Za-z0-9_-]*
integer ::= '0' | '-'? [1-9] [0-9]*
decimal ::= '-'? ('0' | [1-9] [0-9]*) '.' [0-9]+
character ::= ['] ([^'\n\r\\] | escape) [']
string ::= '"' ([^"\n\r\\] | escape)* '"'
escape ::= '\' [bnrt'"\\]
operator ::= [!=] '='? | '&&' | '||' | 'any character'

whitespace ::= [ \b\n\r\t]
 */


public final class Parser {

    private static final String NIL = "NIL";
    private static final String FALSE = "FALSE";
    private static final String TRUE = "TRUE";
    private static final String LEFT_PAREN = "(";
    private static final String RIGHT_PAREN = ")";
    private static final String LEFT_BRACKET = "[";
    private static final String RIGHT_BRACKET = "]";
    private static final String SLASH = "/";
    private static final String STAR = "*";
    private static final String MINUS = "-";
    private static final String PLUS = "+";
    private static final Object GREATER = ">";
    private static final Object EQUALS = "==";
    private static final Object LESS = "<";
    private static final Object NOT_EQUAL = "!=";
    private static final Object AND = "&&";
    private static final Object OR = "||";
    public static final String SEMICOLON = ";";
    public static final String ASSIGNMENT = "=";
    public static final String COLON = ":";
    public static final String COMMA = ",";


    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Global> globals = new ArrayList<>();
        while (peek("LIST", "VAR", "VAL")) {
            globals.add(parseGlobal());
        }
        List<Ast.Function> functions = new ArrayList<>();
        while (peek("FUN")) {
            functions.add(parseFunction());
        }
        if (tokens.has(0))
            throw new ParseException("Parse exception ", tokens.get(0).getIndex()); //TODO
        return new Ast.Source(globals, functions);
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        if (peek("LIST"))
            return parseList();
        if (peek("VAR"))
            return parseMutable();
        if (peek("VAL"))
            return parseImmutable();
        throw new ParseException("Parse exception ", tokens.get(0).getIndex()); //TODO

    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        List<Ast.Expression> listElements = new ArrayList<>();
        Ast.Expression.PlcList list = new Ast.Expression.PlcList(listElements);
        if (match("LIST")) {
            if (match(Token.Type.IDENTIFIER)) {
                String identifier = previous().getLiteral();
                if (match(COLON)) {
                    if (match(Token.Type.IDENTIFIER)) {
                        String typeName = previous().getLiteral();
                        list.setType(Environment.getType(typeName));
                        if (match(ASSIGNMENT)) {
                            if (match(LEFT_BRACKET)) {
                                listElements.add(parseExpression());
                                while (match(COMMA))
                                    listElements.add(parseExpression());
                            }
                            if (!match(RIGHT_BRACKET))
                                throw new ParseException("Expect ']' after list listElements.", previous().getIndex());

                            if (match(SEMICOLON)) {

                                return new Ast.Global(identifier, typeName, true, Optional.of(list));
                            }
                        }
                    }
                }
            }
        }
        throw new ParseException("Parse exception ", tokens.get(0).getIndex());


    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        if (match("VAR")) {
            if (match(Token.Type.IDENTIFIER)) {
                String identifier = previous().getLiteral();
                if (match(COLON)) {
                    if (match(Token.Type.IDENTIFIER)) {
                        String typeName = previous().getLiteral();
                        if (match(ASSIGNMENT)) {
                            Ast.Expression expression = parseExpression();
                            if (match(SEMICOLON))
                                return new Ast.Global(identifier, typeName, true, Optional.of(expression));
                        }
                        if (match(SEMICOLON)) return new Ast.Global(identifier, typeName, true, Optional.empty());
                    }
                }
            }
        }
        throw new ParseException("Parse exception ", tokens.get(0).getIndex()); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        if (match("VAL")) {
            if (match(Token.Type.IDENTIFIER)) {
                String identifier = previous().getLiteral();
                // Parse type
                if (match(COLON)) {
                    if (match(Token.Type.IDENTIFIER)) {
                        String typeName = previous().getLiteral();

                        if (match(ASSIGNMENT)) {
                            Ast.Expression expression = parseExpression();
                            if (match(SEMICOLON))
                                return new Ast.Global(identifier, typeName, false, Optional.of(expression));
                        }
                    }
                }
            }
        }
        throw new ParseException("Parse exception ", tokens.get(0).getIndex()); //TODO
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        if (match("FUN")) {
            if (match(Token.Type.IDENTIFIER)) {
                String identifier = previous().getLiteral();
                if (match(LEFT_PAREN)) {
                    List<String> parameters = new ArrayList<>();
                    List<String> parameterTypeNames = new ArrayList<>();
                    if (match(Token.Type.IDENTIFIER)) {
                        parameters.add(previous().getLiteral());
                        if (match(COLON)) {
                            if (match(Token.Type.IDENTIFIER)) {
                                parameterTypeNames.add(previous().getLiteral());
                                while (match(COMMA)) {
                                    if (match(Token.Type.IDENTIFIER)) {
                                        parameters.add(previous().getLiteral());
                                        if (match(COLON)) {
                                            if (match(Token.Type.IDENTIFIER)) {
                                                parameterTypeNames.add(previous().getLiteral());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!match(RIGHT_PAREN))
                        throw new ParseException("Expect ')' after parameters.", previous().getIndex());
                    if (match(COLON)) {
                        if (match(Token.Type.IDENTIFIER)) {
                            String returnType = previous().getLiteral();
                            if (match("DO")) {
                                List<Ast.Statement> block = parseBlock();
                                if (match("END"))
                                    return new Ast.Function(identifier, parameters, parameterTypeNames, Optional.of(returnType), block);
                            }
                        }
                    }
                    //consume(RIGHT_PAREN, "Expect ')' after parameters.");
                    if (match("DO")) {
                        List<Ast.Statement> block = parseBlock();
                        if (match("END"))
                            return new Ast.Function(identifier, parameters, parameterTypeNames, Optional.empty(), block);
                    }
                }
            }
        }
        throw new ParseException("Parse exception ", tokens.get(0).getIndex()); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END", "ELSE", "CASE", "DEFAULT")) {
            statements.add(parseStatement());
        }
        return statements;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (peek("LET"))
            return parseDeclarationStatement();
        if (peek("IF"))
            return parseIfStatement();
        if (peek("SWITCH"))
            return parseSwitchStatement();
        if (peek("WHILE"))
            return parseWhileStatement();
        if (peek("RETURN"))
            return parseReturnStatement();

        Ast.Expression expression = parseExpression();

        if (match(SEMICOLON)) return new Ast.Statement.Expression(expression);

        if (match(ASSIGNMENT)) {
            Ast.Expression right = parseExpression();
            if (match(";")) return new Ast.Statement.Assignment(expression, right);
        }
        throw new ParseException("Unable to parse statement @", tokens.get(0).getIndex()); //TODO
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        if (match("LET")) {
            if (match(Token.Type.IDENTIFIER)) {
                String identifier = previous().getLiteral();
                if (match(COLON)) {
                    if (match(Token.Type.IDENTIFIER)) {
                        String typeName = previous().getLiteral();
                        if (match(ASSIGNMENT)) {
                            Ast.Expression expression = parseExpression();
                            if (match(SEMICOLON))
                                return new Ast.Statement.Declaration(identifier, Optional.of(typeName), Optional.of(expression));
                        }
                        if (match(SEMICOLON))
                            return new Ast.Statement.Declaration(identifier, Optional.of(typeName), Optional.empty());
                    }
                }
                if (match(ASSIGNMENT)) {
                    Ast.Expression expression = parseExpression();
                    if (match(SEMICOLON))
                        return new Ast.Statement.Declaration(identifier, Optional.empty(), Optional.of(expression));
                }
                if (match(SEMICOLON))
                    return new Ast.Statement.Declaration(identifier, Optional.empty(), Optional.empty());
            }
        }
        throw new ParseException("Parse exception ", tokens.get(0).getIndex()); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        if (match("IF")) {
            Ast.Expression condition = parseExpression();
            if (match("DO")) {
                List<Ast.Statement> block = parseBlock();
                if (match("ELSE")) {
                    List<Ast.Statement> elseBlock = parseBlock();
                    if (match("END")) return new Ast.Statement.If(condition, block, elseBlock);
                } else {
                    if (match("END")) return new Ast.Statement.If(condition, block, new ArrayList<>());
                }
            }
        }
        throw new ParseException("Parse exception ", tokens.get(0).getIndex()); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        if (match("SWITCH")) {
            Ast.Expression condition = parseExpression();
            List<Ast.Statement.Case> cases = new ArrayList<>();
            while (peek("CASE", "DEFAULT")) {
                cases.add(parseCaseStatement());
            }
            if (match("END")) return new Ast.Statement.Switch(condition, cases);
        }
        throw new ParseException("Parse exception ", tokens.get(0).getIndex()); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        if (match("CASE")) {
            Ast.Expression condition = parseExpression();
            if (match(COLON)) {
                List<Ast.Statement> block = parseBlock();
                return new Ast.Statement.Case(Optional.of(condition), block);
            }
        }
        if (match("DEFAULT")) {

            List<Ast.Statement> block = parseBlock();
            return new Ast.Statement.Case(Optional.empty(), block);

        }
        throw new ParseException("Parse exception ", tokens.get(0).getIndex()); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        if (match("WHILE")) {
            Ast.Expression condition = parseExpression();
            if (match("DO")) {
                List<Ast.Statement> block = parseBlock();
                if (match("END")) return new Ast.Statement.While(condition, block);
            }
        }
        throw new ParseException("Parse exception ", tokens.get(0).getIndex()); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        if (match("RETURN")) {
            Ast.Expression expression = parseExpression();
            if (match(SEMICOLON)) return new Ast.Statement.Return(expression);
        }
        throw new ParseException("Parse exception ", tokens.get(0).getIndex()); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {

        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression left = parseComparisonExpression();

        //while it is a comparison operator GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, parse the right side of the expression and create a binary expression
        while (match(AND, OR)) {
            String operator = previous().getLiteral();
            Ast.Expression right = parseComparisonExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression left = parseAdditiveExpression();

        //while it is a comparison operator GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, parse the right side of the expression and create a binary expression
        while (match(GREATER, EQUALS, LESS, NOT_EQUAL)) {
            String operator = previous().getLiteral();
            Ast.Expression right = parseAdditiveExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression left = parseMultiplicativeExpression();

        while (match(MINUS, PLUS)) {
            String operator = previous().getLiteral();
            Ast.Expression right = parseMultiplicativeExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }

        return left;

    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {

        Ast.Expression left = parsePrimaryExpression();

        while (match(SLASH, STAR)) {
            String operator = previous().getLiteral();
            Ast.Expression right = parsePrimaryExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }

        return left;

    }


    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (match(NIL)) return new Ast.Expression.Literal(null);
        if (match(TRUE)) return new Ast.Expression.Literal(Boolean.TRUE);
        if (match(FALSE)) return new Ast.Expression.Literal(Boolean.FALSE);
        String literal = tokens.get(0).getLiteral();
        if (match(Token.Type.INTEGER)) return new Ast.Expression.Literal(new BigInteger(literal));
        if (match(Token.Type.DECIMAL)) return new Ast.Expression.Literal(new BigDecimal(literal));
        if (match(Token.Type.CHARACTER)) return new Ast.Expression.Literal(literal.charAt(1));
        //TODO parse the string literal with escape characters into unescaped string
        if (match(Token.Type.STRING))
            return new Ast.Expression.Literal(unescapeString(literal.substring(1, literal.length() - 1)));//TODO what about escaped characters?

        if (match(LEFT_PAREN)) {
            Ast.Expression expr = parseExpression();
            if (!match(RIGHT_PAREN))
                throw new ParseException("Expect ')' after expression.", previous().getIndex());
            //consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Ast.Expression.Group(expr);
        }

        if (match(Token.Type.IDENTIFIER)) {
            if (match(LEFT_PAREN)) {
                List<Ast.Expression> arguments = parseArguments();
                if (!match(RIGHT_PAREN))
                    throw new ParseException("Expect ')' after arguments.", previous().getIndex());
                //consume(RIGHT_PAREN, "Expect ')' after arguments.");
                return new Ast.Expression.Function(literal, arguments);
            }
            if (match(LEFT_BRACKET)) {
                Ast.Expression index = parseExpression();
                if (!match(RIGHT_BRACKET))
                    throw new ParseException("Expect ']' after index.", previous().getIndex());
                //consume(RIGHT_BRACKET, "Expect ']' after index.");
                return new Ast.Expression.Access(Optional.ofNullable(index), literal);
            }
            // TODO Return a variable expression--it will be handled in the analyzer P4
            //throw new UnsupportedOperationException("Variable expressions are not yet supported.");
            return new Ast.Expression.Access(Optional.empty(), literal);
        }
        throw new ParseException("Parse exception", tokens.get(0).getIndex()); //TODO
    }

    private String unescapeString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
                c = s.charAt(i);
                switch (c) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case '\'':
                        sb.append('\'');
                        break;
                    case '\"':
                        sb.append('\"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    default:
                        throw new ParseException("Invalid escape character: \\" + c, i);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Token previous() {
        return tokens.get(-1);
    }

    private List<Ast.Expression> parseArguments() throws ParseException {
        List<Ast.Expression> arguments = new ArrayList<>();

        if (peek(RIGHT_PAREN))
            return arguments;

        arguments.add(parseExpression());

        while (match(COMMA)) {
            arguments.add(parseExpression());
        }
        //TODO should this be here or in the calling function?
        /*if(!peek(RIGHT_PAREN))
            throw new ParseException("Expect ')' after arguments.", tokens.get(-1).getIndex());*/
        return arguments;
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     * <p>
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        if (!tokens.has(0)) {
            return false;
        }
        Token token = tokens.get(0);
        for (int i = 0; i < patterns.length; i++) {
            // Check if the next token matches the pattern
            if (matchToken(token, patterns[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean matchToken(Token token, Object tokenPattern) {
        if (tokenPattern instanceof String)
            return ((String) tokenPattern).equals(token.getLiteral());
        if (tokenPattern instanceof Token.Type)
            return ((Token.Type) tokenPattern).equals(token.getType());
        //if pattern is not a String and not a token type(ambiguous wording in the comment for this function description), then it must be a Token, compare to see if their types are equal
        return ((Token) tokenPattern).getType().equals(token.getType());
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek)
            tokens.advance();
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