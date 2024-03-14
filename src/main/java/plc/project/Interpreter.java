package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
// New Logarithm Function from Lecture
        scope.defineFunction ("logarithm", 1, args -> {
            if (! (args.get(0).getValue() instanceof BigDecimal )) {
                throw new RuntimeException("expected type BigDecimal. Received, " + args.get(0).getValue().getClass().getName());
            }
            BigDecimal bd1 = (BigDecimal) args.get(0).getValue();

            BigDecimal bd2 = requireType(
                    BigDecimal.class,
                    Environment.create(args.get(0).getValue())
            );
            BigDecimal result = BigDecimal.valueOf(Math.log(bd2.doubleValue()));
            return Environment.create(result);
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        //throw new UnsupportedOperationException(); //TODO
        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        for (Ast.Function function : ast.getFunctions()) {
            visit(function);
        }
        return scope.lookupFunction("main", 0).invoke(new ArrayList<>());
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        // throw new UnsupportedOperationException(); //TODO
        if (ast.getValue().isPresent()) {
            Environment.PlcObject value = visit(ast.getValue().get());
            scope.defineVariable(ast.getName(), true, value); // Assuming all global variables are mutable
        } else {
            scope.defineVariable(ast.getName(), true, Environment.NIL); // Assuming all global variables are mutable
        }
        return Environment.create(true);

    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        // throw new UnsupportedOperationException(); //TODO
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope functionScope = new Scope(scope); // Create a new scope for the function
            for (int i = 0; i < ast.getParameters().size(); i++) {
                functionScope.defineVariable(ast.getParameters().get(i), true, args.get(i));
            }
            try {
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
                return Environment.NIL;
            } catch (Return returnValue) {
                return returnValue.value;
            }
        });
        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        // throw new UnsupportedOperationException(); //TODO
        // Evaluate the expression
        Environment.PlcObject result = visit(ast.getExpression());

        // Return Environment.NIL
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        // throw new UnsupportedOperationException(); //TODO (in lecture)
        Optional optional = ast.getValue();
        Boolean present = optional.isPresent();

        if (present) {
            Ast.Expression expr = (Ast.Expression) optional.get();
            scope.defineVariable(ast.getName(), true, visit(expr));
        } else {
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
       // throw new UnsupportedOperationException(); //

        Environment.PlcObject value = visit(ast.getValue());

        // Retrieve the name of the variable from the left-hand side expression
        String variableName;
        if (ast.getReceiver() instanceof Ast.Expression.Access) {
            variableName = ((Ast.Expression.Access) ast.getReceiver()).getName();
        } else {
            // Handle other types of expressions for the left-hand side (if any)
            throw new RuntimeException("Invalid left-hand side expression for assignment.");
        }

        // Retrieve the variable from the scope
        Environment.Variable variable = scope.lookupVariable(variableName);

        // Check if the variable is a list
        if (variable.getValue().getValue() instanceof List) {
            // Update the list value with the new element
            List<Object> list = (List<Object>) variable.getValue().getValue();
            Optional<Ast.Expression> offset = ((Ast.Expression.Access) ast.getReceiver()).getOffset();
            if (offset.isPresent() && offset.get() instanceof Ast.Expression.Literal) {
                int index = ((BigInteger) ((Ast.Expression.Literal) offset.get()).getLiteral()).intValue();
                list.set(index, value.getValue());
            } else {
                throw new RuntimeException("Invalid offset expression for list assignment.");
            }
        } else {
            // If it's not a list, update the variable value directly
            variable.setValue(value);
        }

        // Return Environment.NIL
        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        // throw new UnsupportedOperationException(); //TODO
        Environment.PlcObject conditionResult = visit(ast.getCondition());

        if (conditionResult.getValue() instanceof Boolean) {
            boolean conditionValue = (Boolean) conditionResult.getValue();
            if (conditionValue) {
                // Evaluate thenStatements
                for (Ast.Statement statement : ast.getThenStatements()) {
                    visit(statement);
                }
            } else if (!ast.getElseStatements().isEmpty()) {
                // Evaluate elseStatements if condition is false and there are elseStatements
                for (Ast.Statement statement : ast.getElseStatements()) {
                    visit(statement);
                }
            }
        } else {
            throw new RuntimeException("Condition expression must evaluate to a boolean value");
        }

        // Return null as If statement doesn't return a value
        return null;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        //throw new UnsupportedOperationException(); //TODO
        Environment.PlcObject conditionValue = visit(ast.getCondition());

        for (Ast.Statement.Case switchCase : ast.getCases()) {
            // If there is no value associated with the case, it's a default case
            if (!switchCase.getValue().isPresent()) {
                for (Ast.Statement statement : switchCase.getStatements()) {
                    visit(statement);
                }
                return Environment.NIL;
            }

            // Evaluate the case value
            Environment.PlcObject caseValue = visit(switchCase.getValue().get());

            // Check if the case value matches the condition value
            if (Objects.equals(caseValue.getValue(), conditionValue.getValue())) {
                for (Ast.Statement statement : switchCase.getStatements()) {
                    visit(statement);
                }
                return Environment.NIL;
            }
        }

        // If no case matches the condition, return NIL
        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        // throw new UnsupportedOperationException(); //TODO
        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
      //  throw new UnsupportedOperationException(); //TODO (in lecture)
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                // added is this right?
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            } finally {
                // added is this right?
                scope = scope.getParent();

            }
        }

return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        // throw new UnsupportedOperationException(); //TODO
        Environment.PlcObject value = visit(ast.getValue());

        // Throw a Return exception containing the value to be returned
        throw new Return(value);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        //throw new UnsupportedOperationException(); //TODO
        // Check if the literal value is null
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        } else {
            // If not null, create an environment object with the literal value
            return Environment.create(ast.getLiteral());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        // throw new UnsupportedOperationException(); //TODO
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        // throw new UnsupportedOperationException(); //TODO
        Environment.PlcObject left = visit(ast.getLeft());
        Environment.PlcObject right = visit(ast.getRight());

        switch (ast.getOperator()) {
            case "+":
                if (left.getValue() instanceof String || right.getValue() instanceof String) {
                    return Environment.create(left.getValue().toString() + right.getValue().toString());
                }
                if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger ) {
                    return Environment.create(requireType(BigInteger.class, left).add(requireType(BigInteger.class, right)));
                }
                else if (left.getValue() instanceof BigDecimal && right.getValue () instanceof BigInteger) {
                    return Environment.create(requireType(BigDecimal.class, left).add(requireType(BigDecimal.class, right)));
                }
                else throw new RuntimeException("Operator Not Defined For Input Provided" + ast.getOperator());
            case "-":
                return Environment.create(requireType(Number.class, left).doubleValue() - requireType(Number.class, right).doubleValue());
            case "*":
                return Environment.create(requireType(Number.class, left).doubleValue() * requireType(Number.class, right).doubleValue());
            case "/":
                if (requireType(Number.class, right).doubleValue() == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                return Environment.create(requireType(Number.class, left).doubleValue() / requireType(Number.class, right).doubleValue());
            case "==":
                return Environment.create(Objects.equals(left.getValue(), right.getValue()));
            case "!=":
                return Environment.create(!Objects.equals(left.getValue(), right.getValue()));
            case "<":
                requireType(left.getValue().getClass(), right);
                return Environment.create(requireType(Comparable.class, left).compareTo(requireType(Comparable.class, right)) < 0);
            case ">":
                return Environment.create(requireType(Number.class, left).doubleValue() > requireType(Number.class, right).doubleValue());
            case "&&":
                return Environment.create(requireType(Boolean.class, left) && requireType(Boolean.class, right));
            case "||":
                return Environment.create(requireType(Boolean.class, left) || requireType(Boolean.class, right));
            case "^":
                BigInteger base = requireType(BigInteger.class, left);
                BigInteger exponent = requireType(BigInteger.class, right);
                return Environment.create(base.pow(exponent.intValue()));
            default:
                throw new RuntimeException("Unknown operator: " + ast.getOperator());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        //throw new UnsupportedOperationException(); //TODO
        String name = ast.getName();
        Optional<Ast.Expression> offset = ast.getOffset();

        if (offset.isPresent()) {
            throw new UnsupportedOperationException("Offset access is not supported in this implementation");
        }

        Environment.Variable variable = scope.lookupVariable(name);
        return variable.getValue();

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        //throw new UnsupportedOperationException(); //TODO
        List<Environment.PlcObject> arguments = ast.getArguments().stream().map(this::visit).collect(Collectors.toList());
        Environment.Function function = scope.lookupFunction(ast.getName(), arguments.size());
        return function.invoke(arguments);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        // throw new UnsupportedOperationException(); //TODO
        List<Environment.PlcObject> values = ast.getValues().stream().map(this::visit).collect(Collectors.toList());
        return Environment.create(values);
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
