package tech.ixor.calclox.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * A Java command-line tool that generate the AST classes (Expr) for the CalcLox language.
 */
public class GenerateAst {
    private static void defineVisitor(
            PrintWriter printWriter, String baseName, List<String> types
    ) {
        printWriter.println("    public interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            printWriter.println("        R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }

        printWriter.println("    }");
    }

    private static void defineType(
            PrintWriter writer, String baseName, String className, String fieldList
    ) {
        fieldList = fieldList.replace(",\n          ", ", ");

        writer.println("    //> " + baseName.toLowerCase() + "-" + className.toLowerCase());
        writer.println("    public static class " + className + " extends " + baseName + " {");

        //> Fields
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            writer.println("        public final " + field + ";");
        }
        //< Fields

        //> Constructor
        writer.println();
        if (fieldList.length() > 64) {
            fieldList = fieldList.replace(", ", ",\n          ");
        }
        writer.println("        public " + className + "(" + fieldList + ") {");

        //> Assign Fields
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }
        //< Assign Fields

        writer.println("        }");
        //< Constructor

        //> Visitor Pattern
        writer.println();
        writer.println("        @Override");
        writer.println("        public <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");
        //< Visitor Pattern

        writer.println("    }");
    }

    private static void defineAst(
            String outputDirectory, String baseName, List<String> types
    ) throws IOException {
        String outputFile = outputDirectory + File.separator + baseName + ".java";
        File file = new File(outputFile);
        PrintWriter printWriter = new PrintWriter(file, StandardCharsets.UTF_8);

        printWriter.println("//> Generated AST: " + baseName);
        printWriter.println("package tech.ixor.calclox.ast;");
        printWriter.println();
        printWriter.println("import tech.ixor.calclox.token.Token;");
        printWriter.println();
        printWriter.println("import java.util.List;");
        printWriter.println();
        printWriter.println("public abstract class " + baseName + " {");

        //> Define Visitor Interfaces
        defineVisitor(printWriter, baseName, types);
        //< Define Visitor Interfaces
        printWriter.println();

        printWriter.println("    // Nested" + baseName + " Classes");
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(printWriter, baseName, className, fields);
        }

        //> Base accept() Method
        printWriter.println();
        printWriter.println("    public abstract <R> R accept(Visitor<R> visitor);");
        //< Base accept() Method

        printWriter.println("}");
        printWriter.println("//< Generated AST: " + baseName);
        printWriter.close();
    }

    public static void main(String[] args) {
        if (args.length != 1 || args[0].equalsIgnoreCase("--help")) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }

        try {
            String outputDirectory = args[0];
            defineAst(outputDirectory, "Expr", Arrays.asList(
                    "Assign   : Token name, Expr value",
                    "Binary   : Expr left, Token operator, Expr right",
                    "Call     : Expr callee, Token paren, List<Expr> arguments",
                    "Grouping : Expr expression",
                    "Literal  : Object value",
                    "Logical  : Expr left, Token operator, Expr right",
                    "Unary    : Token operator, Expr right",
                    "Variable : Token name"
            ));
            defineAst(outputDirectory, "Statement", Arrays.asList(
                    "Block      : List<Statement> statements",
                    "Define     : Token name",
                    "Expression : Expr expression",
                    "Function   : Token name, List<Token> params, List<Statement> body",
                    "If         : Expr condition, Statement thenBranch, Statement elseBranch",
                    "Output     : Expr expression",
                    "Return     : Token keyword, Expr value",
                    "Var        : Token name, Expr initializer",
                    "While      : Expr condition, Statement body"
            )); // Add more statement types as needed
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
            System.exit(1);
        }
    }
}
