package plc.project;

import java.io.PrintWriter;
import java.util.List;

import static java.sql.DriverManager.println;


public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        // Generate class header
        print("public class Main ");
        print("{");
        newline(indent); // Add newline after class header

        // Generate globals
        for (Ast.Global global : ast.getGlobals()) {
            newline(indent + 1); // Add indentation before each global
            visit(global);
            print(";"); // Add semicolon after each global
            newline(indent); // Add newline after each global
        }

        // Generate Java main method
        newline(indent + 1); // Add indentation before main method
        print("public static void main(String[] args) {");
        newline(indent + 2); // Add indentation before System.exit()
        print("System.exit(new Main().main());");
        newline(indent + 1); // Add newline after main method
        print("}");

        newline(indent); // Add newline between main method and main function

        // Generate main function
        newline(indent + 1); // Add indentation before main function
        print("int main() {");
        newline(indent + 2); // Add indentation before System.out.println() inside main function
        print("System.out.println(\"Hello, World!\");");
        newline(indent + 2); // Add indentation before return statement inside main function
        print("return 0;");
        newline(indent + 1); // Add newline after main function
        print("}");

        newline(indent); // Add newline before closing bracket of class
        print("}");
        newline(indent); // Add newline after closing bracket of class
        return null;
    }



    @Override
    public Void visit(Ast.Global ast) {
        //throw new UnsupportedOperationException(); //TODO
        print("public static ");
        if (ast.getMutable()) {
            print("var ");
        } else {
            print("final ");
        }
        print(ast.getTypeName() + " " + ast.getName());
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        return null;

    }

    @Override
    public Void visit(Ast.Function ast) {
        //throw new UnsupportedOperationException(); //TODO
        print("public static ");
        if (!ast.getReturnTypeName().orElse("").equals("Any")) {
            print(ast.getReturnTypeName().orElse("") + " ");
        }
        print(ast.getName() + "(");
        for (int i = 0; i < ast.getParameters().size(); i++) {
            if (i > 0) {
                print(", ");
            }
            print(ast.getParameterTypeNames().get(i) + " " + ast.getParameters().get(i));
        }
        print(") {");
        newline(indent + 1);
        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }
        newline(indent);
        print("}");
        return null;

    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        //throw new UnsupportedOperationException(); //TODO
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        //throw new UnsupportedOperationException(); //TODO
        if (ast.getTypeName().isPresent()) {
            String typeName = ast.getTypeName().get();
            if (typeName.equalsIgnoreCase("Integer")) {
                print("int ");
            } else if (typeName.equalsIgnoreCase("Decimal")) {
                print("double ");
            } // Add additional type mappings as needed
        } else {
            // Handle the case where no type is specified (e.g., "let name = 1.0;")
            // In this case, assume double as default
            print("double ");
        }

        print(ast.getName());

        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        // Ensure semicolon at the end
        println(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
       // throw new UnsupportedOperationException(); //TODO
        visit(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        //throw new UnsupportedOperationException(); //TODO
        print("if (");
        visit(ast.getCondition());
        print(") {");
        newline(indent + 1);

        // Generate thenStatements
        for (Ast.Statement statement : ast.getThenStatements()) {
            visit(statement);
            print(";"); // End each statement with a semicolon
        }

        newline(indent); // Decrease indentation before closing brace
        print("}");

        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            newline(indent + 1);

            // Generate elseStatements
            for (Ast.Statement statement : ast.getElseStatements()) {
                visit(statement);
                print(";"); // End each statement with a semicolon
            }

            newline(indent); // Decrease indentation before closing brace of else block
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
       // throw new UnsupportedOperationException(); //TODO
        print("switch (");
        visit(ast.getCondition());
        print(") {");
        newline(indent);
        for (Ast.Statement.Case switchCase : ast.getCases()) {
            visit(switchCase);
        }
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
       // throw new UnsupportedOperationException(); //TODO
        if (ast.getValue().isPresent()) {
            print("case ");
            visit(ast.getValue().get());
            print(":");
        } else {
            print("default:");
        }
        newline(indent + 1);
        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        //throw new UnsupportedOperationException(); //TODO
        print("while (");
        visit(ast.getCondition());
        print(") {");
        newline(indent + 1);
        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
       // throw new UnsupportedOperationException(); //TODO
        print("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        //throw new UnsupportedOperationException(); //TODO
        Object value;
        if (ast instanceof Ast.Expression.Literal) {
            value = ((Ast.Expression.Literal) ast).getLiteral();
        } else {
            throw new UnsupportedOperationException("Unsupported AST expression type");
        }

        if (value instanceof String) {
            print("\"" + value + "\""); // Enclose string literals in double quotes
        } else {
            print(value.toString());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        //throw new UnsupportedOperationException(); //TODO
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        //throw new UnsupportedOperationException(); //TODO
        if (ast.getOperator().equals("&&")) {
            visit(ast.getLeft());
            print(" " + ast.getOperator() + " ");
            visit(ast.getRight());
        } else if (ast.getOperator().equals("+")) {
            Ast.Expression left = ast.getLeft();
            Ast.Expression right = ast.getRight();
            boolean leftIsLiteralString = left instanceof Ast.Expression.Literal && ((Ast.Expression.Literal) left).getLiteral() instanceof String;
            boolean rightIsLiteralString = right instanceof Ast.Expression.Literal && ((Ast.Expression.Literal) right).getLiteral() instanceof String;
            if (leftIsLiteralString && rightIsLiteralString) {
                print(((Ast.Expression.Literal) left).getLiteral() + " + " + ((Ast.Expression.Literal) right).getLiteral());
            } else {
                visit(ast.getLeft());
                print(" + ");
                visit(ast.getRight());
            }
        } else if (ast.getOperator().equals("^")) {
            print("Math.pow(");
            visit(ast.getLeft());
            print(", ");
            visit(ast.getRight());
            print(")");
        } else {
            print("(");
            visit(ast.getLeft());
            print(" " + ast.getOperator() + " ");
            visit(ast.getRight());
            print(")");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
       // throw new UnsupportedOperationException(); //TODO
        if (ast.getOffset().isPresent()) {
            visit(ast.getOffset().get());
            print(".");
        }
        print(ast.getName());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        //throw new UnsupportedOperationException(); //TODO
        String functionName = ast.getName();
        List<Ast.Expression> arguments = ast.getArguments();

        // Handle the special case for the "print" function
        if (functionName.equals("print")) {
            print("System.out.println(");
            if (!arguments.isEmpty()) {
                visit(arguments.get(0)); // Print the first argument
                for (int i = 1; i < arguments.size(); i++) {
                    print(" + ");
                    visit(arguments.get(i)); // Print the remaining arguments
                }
            }
            print(")");
        } else {
            // For other functions, print the function name and arguments as usual
            print(functionName, "(");
            for (int i = 0; i < arguments.size(); i++) {
                visit(arguments.get(i));
                if (i < arguments.size() - 1) {
                    print(", ");
                }
            }
            print(")");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
       // throw new UnsupportedOperationException(); //TODO
        print("[");
        for (int i = 0; i < ast.getValues().size(); i++) {
            if (i > 0) {
                print(", ");
            }
            visit(ast.getValues().get(i));
        }
        print("]");
        return null;
    }

}
