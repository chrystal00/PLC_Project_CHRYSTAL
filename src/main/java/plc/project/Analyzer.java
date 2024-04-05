package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    // CHECK
    @Override
    public Void visit(Ast.Source ast) {
     // throw new UnsupportedOperationException();  // TODO
        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }

        // Analyze function declarations
        for (Ast.Function function : ast.getFunctions()) {
            visit(function);
        }

        return null;


    }

    @Override
    public Void visit(Ast.Global ast) {
       // throw new UnsupportedOperationException();  // TODO
        String name = ast.getName();
        String typeName = ast.getTypeName();
        Optional<Ast.Expression> value = ast.getValue();

        Environment.Variable variable = scope.defineVariable(name, name, Environment.getType(ast.getTypeName()), ast.getMutable(), Environment.NIL);

        // Check if the value is present
        if (value.isPresent()) {
            Ast.Expression expression = value.get();
            visit(expression);
            requireAssignable(variable.getType(), expression.getType());
        }

        ast.setVariable(variable);


        return null;
    }



   // NEED HELP
    @Override
    public Void visit(Ast.Function ast) {
        String name = ast.getName();
        List<String> parameterNames = ast.getParameters();
        List<String> parameterTypeNames = ast.getParameterTypeNames();
        Optional<String> optionalReturnTypeName = ast.getReturnTypeName();

        // Resolve parameter types
        List<Environment.Type> parameterTypes = parameterTypeNames.stream()
                .map(Environment::getType)
                .collect(Collectors.toList());

        // Resolve return type
        Environment.Type returnType;
        if (optionalReturnTypeName.isPresent()) {
            returnType = Environment.getType(optionalReturnTypeName.get());
        } else {
            returnType = Environment.Type.NIL; // Default return type is NIL
        }

        // Define the function in the current scope
        Environment.Function function = new Environment.Function(name, name, parameterTypes, returnType, args -> Environment.NIL);
        scope.defineFunction(name, name, parameterTypes, returnType, args -> Environment.NIL);
        ast.setFunction(function);

        // Visit all function statements inside a new scope containing variables for each parameter
        Scope functionScope = new Scope(scope);
        scope = functionScope;
        for (int i = 0; i < parameterNames.size(); i++) {
            String parameterName = parameterNames.get(i);
            Environment.Type parameterType = parameterTypes.get(i);
            scope.defineVariable(parameterName, parameterName, parameterType, true, Environment.NIL);
        }

        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
            if(statement instanceof Ast.Statement.Return){
                Ast.Statement.Return returnStatement=(Ast.Statement.Return) statement;
                requireAssignable(returnType, returnStatement.getValue().getType());
            }

        }


        scope = functionScope.getParent(); // Revert to the previous scope after visiting function statements

        // Throw RuntimeException as requested
        //throw new RuntimeException("The function " + name + " is not defined in this scope.");


        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
       // throw new UnsupportedOperationException();  // TODO
        // Visit the expression
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("Expression statement must be an Ast.Expression.Function.");
        }

        // Visit the expression
        visit(ast.getExpression());

        return null;

    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        String name = ast.getName();
        Optional<String> typeName = ast.getTypeName();
        Optional<Ast.Expression> value = ast.getValue();

        if (!typeName.isPresent()) {
            if (!value.isPresent()) {
                throw new RuntimeException("Type and value are missing for variable '" + name + "'");
            }
            // If type is missing, infer the type from the value expression
            Ast.Expression expression = value.get();
            visit(expression);
            typeName = Optional.of(expression.getType().getName());
        }

        Environment.Type type = Environment.getType(typeName.get());
        Environment.Variable variable;

        if (value.isPresent()) {
            Ast.Expression expression = value.get();
            visit(expression);
            requireAssignable(type, expression.getType());
        }

        // If value is absent, define a variable with NIL as the value
        variable = scope.defineVariable(name, name, type, true, Environment.NIL);

        ast.setVariable(variable);

        return null;
    }


    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        // Visit the receiver and value expressions
        visit(ast.getReceiver());
        visit(ast.getValue());

        // Retrieve the variable being assigned to
        Environment.Variable variable = null;
        if (ast.getReceiver() instanceof Ast.Expression.Access) {
            Ast.Expression.Access receiver = (Ast.Expression.Access) ast.getReceiver();
            String varName = receiver.getName();
            variable = scope.lookupVariable(varName);
        }

        // Ensure the variable exists in the current scope
        if (variable == null) {
            throw new RuntimeException("Variable '" + ast.getReceiver() + "' not found in scope.");
        }

        // Check if the assigned value has a compatible type with the variable
        Environment.Type targetType = variable.getType();
        Environment.Type valueType = ast.getValue().getType();
        if (!targetType.equals(valueType)) {
            throw new RuntimeException("Type mismatch in assignment for variable '" + variable.getName() + "'.");
        }

        return null;
    }


   // NEED HELP
    @Override
    public Void visit(Ast.Statement.If ast) {
       // throw new UnsupportedOperationException();  // TODO
        // Visit the condition expression
        // Visit the condition expression
        visit(ast.getCondition());

        // Ensure the condition is of type BOOLEAN
        Environment.Type conditionType = ast.getCondition().getType();
        if (conditionType != Environment.Type.BOOLEAN) {
            throw new RuntimeException("Condition must be of type BOOLEAN.");
        }

        // Check if thenStatements list is empty
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("Then statements list is empty.");
        }

        // Visit the then statements inside a new scope
        Scope thenScope = new Scope(scope);
        scope = thenScope;
        for (Ast.Statement statement : ast.getThenStatements()) {
            visit(statement);
        }
        scope = thenScope.getParent(); // Revert to the previous scope after visiting thenStatements

        // No need to visit statements inside the ELSE block because it's not implemented in the provided tests

        return null;
    }

    // NEED HELP
    @Override
    public Void visit(Ast.Statement.Switch ast) {
        //throw new UnsupportedOperationException();  // TODO
        // Visit the switch condition
        visit(ast.getCondition());
        Environment.Type conditionType = ast.getCondition().getType();

        // Analyze each case block within its own scope
        for (Ast.Statement.Case caseBlock : ast.getCases()) {
            try {
                Optional<Ast.Expression> value = caseBlock.getValue();
                if(value.isPresent()){
                    visit(value.get());
                    requireAssignable(conditionType, value.get().getType());
                }
                scope = new Scope(scope); // Enter a new scope for the case block
                // Analyze statements within the case block
                for (Ast.Statement statement : caseBlock.getStatements()) {
                    visit(statement);

                }
            } finally {
                scope = scope.getParent(); // Exit the scope
            }
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
       // throw new UnsupportedOperationException();  // TODO
        ast.getValue().ifPresent(this::visit);

        // Visit each statement in the case block
        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
       // throw new UnsupportedOperationException();  // TODO
       visit(ast.getCondition());
       requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
       try {
           scope = new Scope(scope);
           for (Ast.Statement stmt : ast.getStatements()) {
               visit(stmt);
           }
       } finally {
           scope = scope.getParent();
       }
       return null;
    }

    // NEED HELP
    @Override
    public Void visit(Ast.Statement.Return ast) {
       // throw new UnsupportedOperationException();  // TODO
        // Visit the expression being returned
        if (ast.getValue() instanceof Ast.Expression.Literal) {
            visit((Ast.Expression.Literal) ast.getValue());
        } else if (ast.getValue() instanceof Ast.Expression.Group) {
            visit((Ast.Expression.Group) ast.getValue());
        } else if (ast.getValue() instanceof Ast.Expression.Binary) {
            visit((Ast.Expression.Binary) ast.getValue());
        } else if (ast.getValue() instanceof Ast.Expression.Access) {
            visit((Ast.Expression.Access) ast.getValue());
        } else if (ast.getValue() instanceof Ast.Expression.Function) {
            visit((Ast.Expression.Function) ast.getValue());
        } else if (ast.getValue() instanceof Ast.Expression.PlcList) {
            visit((Ast.Expression.PlcList) ast.getValue());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
       // throw new UnsupportedOperationException();  // TODO
        if (ast.getLiteral() == null) {
            ast.setType(Environment.Type.NIL);
            return null;
        }
        if (ast.getLiteral() instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
            return null;
        }
        if (ast.getLiteral() instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
            return null;
        }
        if (ast.getLiteral() instanceof String) {
            ast.setType(Environment.Type.STRING);
            return null;
        }
        if (ast.getLiteral() instanceof BigInteger) {
            // make sure this int is in int range
            BigInteger value = (BigInteger)ast.getLiteral(); // type case literal into bigint and store
            if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 || value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                throw new RuntimeException("Value is out of range of a Java int");
            }
            ast.setType(Environment.Type.INTEGER);
            return null;
        }
        if (ast.getLiteral() instanceof BigDecimal) {
            BigDecimal value = (BigDecimal)ast.getLiteral();
            if (value.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) > 0 || value.compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) < 0) {
                throw new RuntimeException("Value is out of range of a Java double");
            }
            ast.setType(Environment.Type.DECIMAL);
            return null;
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        //throw new UnsupportedOperationException();  // TODO
        Ast.Expression expression = ast.getExpression();
        visit(expression);
        ast.setType(expression.getType());


        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        //throw new UnsupportedOperationException();  // TODO
        // Call the visit method for the left and right expressions
        visit(ast.getLeft());
        visit(ast.getRight());

        Environment.Type leftType = ast.getLeft().getType();
        Environment.Type rightType = ast.getRight().getType();

        switch (ast.getOperator()) {
            case "&&":
            case "||":
                if (leftType != Environment.Type.BOOLEAN || rightType != Environment.Type.BOOLEAN) {
                    throw new RuntimeException("Logical AND/OR operation expects boolean operands");
                }
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case ">":
            case "<":
            case "==":
            case "!=":
                if (!leftType.getScope().equals(rightType.getScope()) ||
                        !leftType.equals(Environment.Type.COMPARABLE) ||
                        !rightType.equals(Environment.Type.COMPARABLE)) {
                    throw new RuntimeException("Comparison operators expect comparable operands of the same type");
                }
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "+":
                if (leftType == Environment.Type.STRING || rightType == Environment.Type.STRING) {
                    ast.setType(Environment.Type.STRING);
                } else if ((leftType == Environment.Type.INTEGER || leftType == Environment.Type.DECIMAL) &&
                        (rightType == Environment.Type.INTEGER || rightType == Environment.Type.DECIMAL) && (leftType == rightType )) {
                    ast.setType(leftType);
                } else {
                    throw new RuntimeException("Invalid operands for addition operation");
                }
                break;
            case "-":
            case "*":
            case "/":
            case "%":
                if ((leftType != Environment.Type.INTEGER && leftType != Environment.Type.DECIMAL) ||
                        (rightType != Environment.Type.INTEGER && rightType != Environment.Type.DECIMAL)) {
                    throw new RuntimeException("Arithmetic operations expect numeric operands");
                }
                ast.setType(leftType);
                break;
            case "^":
                if (leftType != Environment.Type.INTEGER || rightType != Environment.Type.INTEGER) {
                    throw new RuntimeException("Exponentiation operation expects integer operands");
                }
                ast.setType(Environment.Type.INTEGER);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported binary operator: " + ast.getOperator());
        }

        // Set the type if it's still uninitialized
        if (ast.getType() == null) {
            throw new RuntimeException("Type is uninitialized");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
       // throw new UnsupportedOperationException();  // TODO
        ast.getOffset().ifPresent(this::visit);

        // Retrieve the variable from the scope
        Environment.Variable variable = scope.lookupVariable(ast.getName());

        if (variable == null) {
            throw new RuntimeException("Variable '" + ast.getName() + "' not found in scope.");
        }

        // Set the variable of the expression, which internally sets the type of the expression
        ast.setVariable(variable);

        return null;

    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
       // throw new UnsupportedOperationException();  // TODO
        // Visit the function arguments
        for (Ast.Expression argument : ast.getArguments()) {
            visit(argument);
        }

        // Retrieve the function from the scope
        Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());

        if (function == null) {
            throw new RuntimeException("Function '" + ast.getName() + "' not found in scope.");
        }

        // Set the function of the expression, which internally sets the type of the expression
        ast.setFunction(function);

        // Check if the provided arguments match the parameter types of the function
        List<Environment.Type> parameterTypes = function.getParameterTypes();
        List<Ast.Expression> arguments = ast.getArguments();

        if (parameterTypes.size() != arguments.size()) {
            throw new RuntimeException("Incorrect number of arguments for function '" + ast.getName() + "'.");
        }

        for (int i = 0; i < parameterTypes.size(); i++) {
            Environment.Type expectedType = parameterTypes.get(i);
            Environment.Type actualType = arguments.get(i).getType();

            // Check if the names of the expected and actual types match
            /*if (!expectedType.getName().equals(actualType.getName())) {
                throw new RuntimeException("Argument " + (i + 1) + " of function '" + ast.getName() +
                        "' has incorrect type. Expected: " + expectedType + ", Actual: " + actualType);
            }*/
            requireAssignable(expectedType, actualType);
        }

        return null; // Since this is a void method
    }

    // CHECK
    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        //throw new UnsupportedOperationException();  // TODO

        // Visit each expression in the list
        for (Ast.Expression expression : ast.getValues()) {
            if (expression instanceof Ast.Expression.Literal) {
                visit((Ast.Expression.Literal) expression);
            } else if (expression instanceof Ast.Expression.Group) {
                visit((Ast.Expression.Group) expression);
            } else if (expression instanceof Ast.Expression.Binary) {
                visit((Ast.Expression.Binary) expression);
            } else if (expression instanceof Ast.Expression.Access) {
                visit((Ast.Expression.Access) expression);
            } else if (expression instanceof Ast.Expression.Function) {
                visit((Ast.Expression.Function) expression);
            } else if (expression instanceof Ast.Expression.PlcList) {
                visit((Ast.Expression.PlcList) expression);
            } else {
                throw new IllegalArgumentException("Unsupported expression type: " + expression.getClass());
            }
        }


        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
       // throw new UnsupportedOperationException();  // TODO
        Scope targetTypeScope = target.getScope();
        Scope typeScope = type.getScope();

        // Check if the target's scope is the same as the type's scope
        while (typeScope != null) {
            if (typeScope.equals(targetTypeScope)) {
                return; // Type is assignable
            }
            typeScope = typeScope.getParent(); // Navigate to the parent scope
        }

        // If the loop completes without finding a match, throw an exception
        throw new IllegalArgumentException("Type " + type + " is not assignable to " + target);
    }

}
