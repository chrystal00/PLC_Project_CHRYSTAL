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

        // Check if the value is present
        if (value.isPresent()) {
            Ast.Expression expression = value.get();

            // Check if the expression is a Literal
            if (expression instanceof Ast.Expression.Literal) {
                Ast.Expression.Literal literal = (Ast.Expression.Literal) expression;
                Object literalValue = literal.getLiteral();

                // Check if the literal value is a Boolean
                if (literalValue instanceof Boolean) {
                    boolean initialValue = (Boolean) literalValue;
                    // Check if the global variable type is BOOLEAN
                    if ("Boolean".equals(typeName)) {
                        // Define the variable in the current scope
                        Environment.PlcObject plcObject = new Environment.PlcObject(new Scope(null), initialValue);
                        Environment.Variable variable = scope.defineVariable(name, typeName, Environment.Type.BOOLEAN, false, plcObject);
                        // Set the variable in the AST
                        ast.setVariable(variable);
                    } else {
                        throw new RuntimeException("Global variable '" + name + "' must have type Boolean");
                    }
                } else {
                    throw new RuntimeException("Value of type " + literalValue.getClass().getSimpleName() +
                            " is not assignable to global variable '" + name + "' of type Boolean");
                }
            } else {
                throw new RuntimeException("Unsupported expression type for global variable initialization");
            }
        } else {
            // If the value is not present, throw an exception or handle accordingly
            throw new RuntimeException("Value is required for global variable initialization");
        }

        return null;
    }



    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException();  // TODO

    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
       // throw new UnsupportedOperationException();  // TODO
        // Visit the expression
        if (ast.getExpression() instanceof Ast.Expression.Literal) {
            visit((Ast.Expression.Literal) ast.getExpression());
        } else if (ast.getExpression() instanceof Ast.Expression.Group) {
            visit((Ast.Expression.Group) ast.getExpression());
        } else if (ast.getExpression() instanceof Ast.Expression.Binary) {
            visit((Ast.Expression.Binary) ast.getExpression());
        } else if (ast.getExpression() instanceof Ast.Expression.Access) {
            visit((Ast.Expression.Access) ast.getExpression());
        } else if (ast.getExpression() instanceof Ast.Expression.Function) {
            visit((Ast.Expression.Function) ast.getExpression());
        } else if (ast.getExpression() instanceof Ast.Expression.PlcList) {
            visit((Ast.Expression.PlcList) ast.getExpression());
        } else {
            throw new AssertionError("Unimplemented AST type: " +
                    ast.getExpression().getClass().getName() + ".");
        }
        return null; // Since this is a void method

    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        //throw new UnsupportedOperationException();  // TODO
        // Visit the optional expression if present
        ast.getValue().ifPresent(expression -> visit(expression));
        return null; // Since this is a void method
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        //throw new UnsupportedOperationException();  // TODO
        // Visit the receiver and value expressions
        visit(ast.getReceiver());
        visit(ast.getValue());
        return null; // Since this is a void method
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
       // throw new UnsupportedOperationException();  // TODO
        // Visit the condition expression
        visit(ast.getCondition());

        // Visit the then statements
        for (Ast.Statement statement : ast.getThenStatements()) {
            visit(statement);
        }

        // Visit the else statements
        for (Ast.Statement statement : ast.getElseStatements()) {
            visit(statement);
        }

        return null; // Since this is a void method
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        //throw new UnsupportedOperationException();  // TODO
        // Visit the switch condition
        visit(ast.getCondition());

        // Visit each case block
        for (Ast.Statement.Case caseBlock : ast.getCases()) {
            visit(caseBlock);
        }

        return null; // Since this is a void method
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
       // throw new UnsupportedOperationException();  // TODO
        // Visit the case value (if present)
        ast.getValue().ifPresent(this::visit);

        // Visit each statement in the case block
        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }

        return null; // Since this is a void method
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
       // throw new UnsupportedOperationException();  // TODO
        // Visit the condition expression
        if (ast.getCondition() instanceof Ast.Expression.Literal) {
            visit((Ast.Expression.Literal) ast.getCondition());
        } else if (ast.getCondition() instanceof Ast.Expression.Group) {
            visit((Ast.Expression.Group) ast.getCondition());
        } else if (ast.getCondition() instanceof Ast.Expression.Binary) {
            visit((Ast.Expression.Binary) ast.getCondition());
        } else if (ast.getCondition() instanceof Ast.Expression.Access) {
            visit((Ast.Expression.Access) ast.getCondition());
        } else if (ast.getCondition() instanceof Ast.Expression.Function) {
            visit((Ast.Expression.Function) ast.getCondition());
        } else if (ast.getCondition() instanceof Ast.Expression.PlcList) {
            visit((Ast.Expression.PlcList) ast.getCondition());
        }

        // Visit each statement in the loop block
        for (Ast.Statement statement : ast.getStatements()) {
            if (statement instanceof Ast.Statement.Expression) {
                visit((Ast.Statement.Expression) statement);
            } else if (statement instanceof Ast.Statement.Declaration) {
                visit((Ast.Statement.Declaration) statement);
            } else if (statement instanceof Ast.Statement.Assignment) {
                visit((Ast.Statement.Assignment) statement);
            } else if (statement instanceof Ast.Statement.If) {
                visit((Ast.Statement.If) statement);
            } else if (statement instanceof Ast.Statement.Switch) {
                visit((Ast.Statement.Switch) statement);
            } else if (statement instanceof Ast.Statement.Case) {
                visit((Ast.Statement.Case) statement);
            } else if (statement instanceof Ast.Statement.While) {
                visit((Ast.Statement.While) statement);
            } else if (statement instanceof Ast.Statement.Return) {
                visit((Ast.Statement.Return) statement);
            }
        }

        return null; // Since this is a void method
    }

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

        return null; // Since this is a void method
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
       // throw new UnsupportedOperationException();  // TODO
        // Set the type of the literal if it's not already set
        if (ast.getType() == null) {
            // Assuming it's a literal of type ANY
            ast.setType(Environment.Type.ANY);
        }

        return null; // Since this is a void method
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        //throw new UnsupportedOperationException();  // TODO
        // Manually call the visit method for the inner expression
        if (ast.getExpression() instanceof Ast.Expression.Literal) {
            visit((Ast.Expression.Literal) ast.getExpression());
        } else if (ast.getExpression() instanceof Ast.Expression.Group) {
            visit((Ast.Expression.Group) ast.getExpression());
        } else if (ast.getExpression() instanceof Ast.Expression.Binary) {
            visit((Ast.Expression.Binary) ast.getExpression());
        } else if (ast.getExpression() instanceof Ast.Expression.Access) {
            visit((Ast.Expression.Access) ast.getExpression());
        } else if (ast.getExpression() instanceof Ast.Expression.Function) {
            visit((Ast.Expression.Function) ast.getExpression());
        } else if (ast.getExpression() instanceof Ast.Expression.PlcList) {
            visit((Ast.Expression.PlcList) ast.getExpression());
        } else {
            throw new IllegalArgumentException("Unsupported expression type: " + ast.getExpression().getClass());
        }

        // You might perform additional operations here if needed

        return null; // Since this is a void method
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        //throw new UnsupportedOperationException();  // TODO
        // Manually call the visit method for the left and right expressions
        if (ast.getLeft() instanceof Ast.Expression.Literal) {
            visit((Ast.Expression.Literal) ast.getLeft());
        } else if (ast.getLeft() instanceof Ast.Expression.Group) {
            visit((Ast.Expression.Group) ast.getLeft());
        } else if (ast.getLeft() instanceof Ast.Expression.Binary) {
            visit((Ast.Expression.Binary) ast.getLeft());
        } else if (ast.getLeft() instanceof Ast.Expression.Access) {
            visit((Ast.Expression.Access) ast.getLeft());
        } else if (ast.getLeft() instanceof Ast.Expression.Function) {
            visit((Ast.Expression.Function) ast.getLeft());
        } else if (ast.getLeft() instanceof Ast.Expression.PlcList) {
            visit((Ast.Expression.PlcList) ast.getLeft());
        } else {
            throw new IllegalArgumentException("Unsupported expression type: " + ast.getLeft().getClass());
        }

        if (ast.getRight() instanceof Ast.Expression.Literal) {
            visit((Ast.Expression.Literal) ast.getRight());
        } else if (ast.getRight() instanceof Ast.Expression.Group) {
            visit((Ast.Expression.Group) ast.getRight());
        } else if (ast.getRight() instanceof Ast.Expression.Binary) {
            visit((Ast.Expression.Binary) ast.getRight());
        } else if (ast.getRight() instanceof Ast.Expression.Access) {
            visit((Ast.Expression.Access) ast.getRight());
        } else if (ast.getRight() instanceof Ast.Expression.Function) {
            visit((Ast.Expression.Function) ast.getRight());
        } else if (ast.getRight() instanceof Ast.Expression.PlcList) {
            visit((Ast.Expression.PlcList) ast.getRight());
        } else {
            throw new IllegalArgumentException("Unsupported expression type: " + ast.getRight().getClass());
        }

        // You might perform additional operations here if needed

        return null; // Since this is a void method
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
       // throw new UnsupportedOperationException();  // TODO
        // Manually handle the access expression

        // Visit the offset expression if present
        if (ast.getOffset().isPresent()) {
            Ast.Expression offset = ast.getOffset().get();
            if (offset instanceof Ast.Expression.Literal) {
                visit((Ast.Expression.Literal) offset);
            } else if (offset instanceof Ast.Expression.Group) {
                visit((Ast.Expression.Group) offset);
            } else if (offset instanceof Ast.Expression.Binary) {
                visit((Ast.Expression.Binary) offset);
            } else if (offset instanceof Ast.Expression.Access) {
                visit((Ast.Expression.Access) offset);
            } else if (offset instanceof Ast.Expression.Function) {
                visit((Ast.Expression.Function) offset);
            } else if (offset instanceof Ast.Expression.PlcList) {
                visit((Ast.Expression.PlcList) offset);
            } else {
                throw new IllegalArgumentException("Unsupported expression type: " + offset.getClass());
            }
        }

        // You might perform additional operations here if needed

        return null; // Since this is a void method
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
       // throw new UnsupportedOperationException();  // TODO
        // Manually handle the function expression

        // Visit the function arguments
        for (Ast.Expression argument : ast.getArguments()) {
            if (argument instanceof Ast.Expression.Literal) {
                visit((Ast.Expression.Literal) argument);
            } else if (argument instanceof Ast.Expression.Group) {
                visit((Ast.Expression.Group) argument);
            } else if (argument instanceof Ast.Expression.Binary) {
                visit((Ast.Expression.Binary) argument);
            } else if (argument instanceof Ast.Expression.Access) {
                visit((Ast.Expression.Access) argument);
            } else if (argument instanceof Ast.Expression.Function) {
                visit((Ast.Expression.Function) argument);
            } else if (argument instanceof Ast.Expression.PlcList) {
                visit((Ast.Expression.PlcList) argument);
            } else {
                throw new IllegalArgumentException("Unsupported expression type: " + argument.getClass());
            }
        }

        // You might perform additional operations here if needed

        return null; // Since this is a void method
    }

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

        // You might perform additional operations here if needed

        return null; // Since this is a void method
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
       // throw new UnsupportedOperationException();  // TODO
        Scope targetTypeScope = target.getScope();
        Scope typeScope = type.getScope();

        // Check if the target's scope is the same as the type's scope or is its ancestor
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
