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

        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();

        while (peek("VAL") || peek( "VAR") || peek( "LIST")) {
            globals.add(parseGlobal());
        }

        while (peek( "FUN")) {
            functions.add(parseFunction());
        }

        return new Ast.Source(globals, functions);
    }
    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        if ( peek( "LIST"))
            return parseList();
        if( peek( "VAR"))
            return parseMutable();
        if (peek( "VAL")) 
            return parseImmutable();
        
        throw new ParseException("Expected global declaration", tokens.index);
    }
    
    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        if(match("LIST")){
            if(match(Token.Type.IDENTIFIER)){
                String name= previous().getLiteral();
                if(match(":")){
                    if(match(Token.Type.IDENTIFIER)) {
                        String typeName= previous().getLiteral();
                        if(match("=")){
                          if(match("[")){
                              List<Ast.Expression> listElements=new ArrayList<>();
                              listElements.add(parseExpression());
                              while(match(",")){
                                  listElements.add(parseExpression());
                              }
                              if(match("]")){
                                  Ast.Expression.PlcList list= new Ast.Expression.PlcList(listElements);
                                  return new Ast.Global(name, typeName, true, Optional.of(list));
                              }
                          }
                        }
                    }
                }
            }
        }
        throw new ParseException("Expected global declaration", tokens.index);

    }

    private Token previous() {
        return tokens.get(-1);
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        if(match("VAR")){
            if(match(Token.Type.IDENTIFIER)) {
                String name = previous().getLiteral();
                if(match(":")) {
                    if (match(Token.Type.IDENTIFIER)) {
                        String typeName = previous().getLiteral();
                        if(match("=")){
                            Ast.Expression expr=parseExpression();
                            return new Ast.Global(name, typeName, true, Optional.of(expr));
                        } else {
                            return new Ast.Global(name, typeName, true, Optional.empty());
                        }
                    }
                }
            }


        }
        throw new ParseException("Parser exception at ", tokens.index);

    }
    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        if(match("VAL")){
            if(match(Token.Type.IDENTIFIER)) {
                String name = previous().getLiteral();
                if(match(":")) {
                    if (match(Token.Type.IDENTIFIER)) {
                        String typeName = previous().getLiteral();
                        if(match("=")) {
                            Ast.Expression expr = parseExpression();
                            return new Ast.Global(name, typeName, false, Optional.of(expr));
                        }
                    }
                }
            }
        }
        throw new ParseException("Parser exception at ", tokens.index);

    }
    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        if (match("FUN")) {
            if (match(Token.Type.IDENTIFIER)) {
                String identifier = previous().getLiteral();
                if (match("(")) {
                    List<String> parameters = new ArrayList<>();
                    List<String> parameterTypeNames = new ArrayList<>();
                    if (match(Token.Type.IDENTIFIER)) {
                        parameters.add(previous().getLiteral());
                        if (match(":")) {
                            if (match(Token.Type.IDENTIFIER)) {
                                parameterTypeNames.add(previous().getLiteral());
                                while (match(",")) {
                                    if (match(Token.Type.IDENTIFIER)) {
                                        parameters.add(previous().getLiteral());
                                        if (match(":")) {
                                            if (match(Token.Type.IDENTIFIER)) {
                                                parameterTypeNames.add(previous().getLiteral());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!match(")"))
                        throw new ParseException("Expect ')' after parameters.", previous().getIndex());
                    if (match(":")) {
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
        throw new ParseException("Parser Exception at " , tokens.index);
    }
    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END") && !peek( "CASE") && !peek("DEFAULT") && !peek("ELSE"))
        {
            statements.add(parseStatement());
        }
        return statements;
    }
    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
// Part a
    public Ast.Statement parseStatement() throws ParseException {
        if (peek("WHILE")) {
            return parseWhileStatement();
        } else if (peek("RETURN")) {
            return parseReturnStatement();
        } else if (peek("SWITCH")) {
            return parseSwitchStatement();
        } else if (peek("IF")) {
            return parseIfStatement();
        } else if (peek("LET")) {
            return parseDeclarationStatement();
        } else {
            Ast.Expression expression = parseExpression(); // Parse the expression
// Check if there's an assignment
            if (match("=")) {
                Ast.Expression assignmentValue = parseExpression();
// Check for the terminal semicolon symbol
                if (!match(";")) {
                    throw new ParseException("Expected semicolon at end of statement", tokens.get(0).getIndex());
                }
                return new Ast.Statement.Assignment(expression, assignmentValue);
            } else {
// Check for the terminal semicolon symbol
                if (!match(";")) {
                    throw new ParseException("Expected semicolon at end of statement",tokens.get(0).getIndex());
                }
                return new Ast.Statement.Expression(expression);
            }
        }
    }
    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws
            ParseException {
        match("LET"); // Ensure the next token is 'LET'
        Token identifierToken = tokens.get(0);
        if (!match( Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier after 'LET'",
                    identifierToken.getIndex());
        }
        String identifier = identifierToken.getLiteral();
        Optional<Ast.Expression> initialization = Optional.empty(); // Initializeas empty by default
// call new match statement
// Check if there's an initialization expression
        if (match("=")) {
            initialization = Optional.of(parseExpression());
        }
        match(";"); // Ensure the statement ends with a semicolon
// Create and return the Declaration statement
        return new Ast.Statement.Declaration(identifier, initialization);
    }
    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {

        match("IF"); // Ensure the next token is 'IF'
        Ast.Expression condition = parseExpression();
        match("DO"); // Ensure the next token is 'DO'
        List<Ast.Statement> thenStatements = parseBlock();
        List<Ast.Statement> elseStatements = new ArrayList<>();
        if (match("ELSE")) {
            elseStatements = parseBlock();
        }
        match("END"); // Ensure the statement ends with 'END'
        match(";"); // Ensure semicolon at the end of the statement
        return new Ast.Statement.If(condition, thenStatements, elseStatements);
    }
    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {

        match("SWITCH"); // Ensure the next token is 'SWITCH'
        Ast.Expression condition = parseExpression();
        match("CASES"); // Ensure the next token is 'CASES'
        List<Ast.Statement.Case> cases = new ArrayList<>();
        while (peek("CASE")) {
            cases.add(parseCaseStatement());
        }
        match("END"); // Ensure the statement ends with 'END'
        return new Ast.Statement.Switch(condition, cases);
    }
    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {

        match("CASE"); // Ensure the next token is 'CASE'
        Optional<Ast.Expression> condition = Optional.of(parseExpression());
        match(":"); // Ensure the next token is ':'
        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("CASE", "DEFAULT", "END")) {
            statements.add(parseStatement());
        }
        return new Ast.Statement.Case(condition, statements);
    }
    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {

        match("WHILE"); // Ensure the next token is 'WHILE'
        Ast.Expression condition = parseExpression();
        match("DO"); // Ensure the next token is 'DO'
        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END")) {
            statements.add(parseStatement());
        }
        match("END"); // Ensure the next token is 'END'
        return new Ast.Statement.While(condition, statements);
    }
    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {

        match("RETURN"); // Ensure the next token is 'RETURN'
        Ast.Expression value = null;
        if (!peek(";")) {
            value = parseExpression();
        }
        match(";"); // Ensure the statement ends with a semicolon
        return new Ast.Statement.Return(value);
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
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-
                    1).getLiteral()));
        } else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-
                    1).getLiteral()));
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
            throw new ParseException("Invalid Primary Expression",
                    tokens.get(0).getIndex());
        }
    }
    private Ast.Expression parseCharacterLiteral(String literal) throws
            ParseException {
        char chars = literal.charAt(1); // Extracting the character from the literal
        if (chars == '\\') {
            chars = handleEscapeCharacters(literal.charAt(2));
        }
        return new Ast.Expression.Literal(chars);
    }
    private Ast.Expression parseStringLiteral(String literal) throws ParseException
    {
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
    private Ast.Expression parseFunctionCall(String parseParen) throws
            ParseException {
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
                throw new ParseException("Invalid escape sequence: \\" + x,
                        tokens.get(-1).getIndex());
        }
    }
    private void expect(String expected) throws ParseException {
        if (!match(expected)) {
            if (tokens.has(0)) {
                throw new ParseException("Expected '" + expected + "'",
                        tokens.get(0).getIndex());
            } else {
                throw new ParseException("Unexpected end of input. Expected '" +
                        expected + "'", -1);
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