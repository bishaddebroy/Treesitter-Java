package org.example;

import io.github.treesitter.jtreesitter.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


public class Main {
    public static void main(String[] args) {
        try {
            // Create a temporary directory for the native libraries
            Path tempDir = Files.createTempDirectory("tree-sitter-lib");
            System.out.println("Temporary directory created: " + tempDir);

            // Copy the Python library to the temporary directory
            Path pythonLibPath = copyLibrary(tempDir, "libtree-sitter-python.dll");

            // Load the symbols from the Python library
            System.out.println("Loading symbols from the Python library...");
            SymbolLookup symbols = SymbolLookup.libraryLookup(pythonLibPath, Arena.global());

            // Load the Python language
            System.out.println("Loading the Python language...");
            Language pythonLanguage = Language.load(symbols, "tree_sitter_python");

            // Create a parser
            System.out.println("Creating parser...");
            Parser parser = new Parser();
            parser.setLanguage(pythonLanguage);
            System.out.println("Parser created successfully!");


            // Test a sample Python code
            String pythonCode = "def main():\n    print('Hello, World!')";
            System.out.println("Parsing code: " + pythonCode);
//            try (Tree tree = parser.parse(pythonCode, InputEncoding.UTF_8).orElseThrow()) {
//                Node rootNode = tree.getRootNode();
//                System.out.println("Root node type: " + rootNode.getType());
//                System.out.println("Start column: " + rootNode.getStartPoint().column());
//                System.out.println("End column: " + rootNode.getEndPoint().column());
//            }
            // Generate AST
            try (Tree tree = parser.parse(pythonCode, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                System.out.println("\nAST Structure:");
                printASTWithCursor(rootNode);
            }

        } catch (Exception e) {
            System.err.println("An error occurred:");
            e.printStackTrace();
        }
    }

    private static Path copyLibrary(Path tempDir, String libraryName) throws IOException {
        Path libraryPath = tempDir.resolve(libraryName);
        try (InputStream is = Main.class.getResourceAsStream("/native/windows/" + libraryName)) {
            if (is == null) {
                throw new RuntimeException("Could not find " + libraryName + " in resources");
            }
            Files.copy(is, libraryPath, StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println(libraryName + " copied to: " + libraryPath);
        return libraryPath;
    }
    private static void printASTWithCursor(Node rootNode) {
        try (TreeCursor cursor = rootNode.walk()) {
            printNodeWithCursor(cursor, 0);
        }
    }

    private static void printNodeWithCursor(TreeCursor cursor, int depth) {
        String indent = "  ".repeat(depth);
        String nodeType = cursor.getCurrentNode().getType();
        String fieldName = cursor.getCurrentFieldName();
        String fieldInfo = fieldName != null ? " (" + fieldName + ")" : "";

        System.out.printf("%s%s%s\n", indent, nodeType, fieldInfo);

        if (cursor.gotoFirstChild()) {
            do {
                printNodeWithCursor(cursor, depth + 1);
            } while (cursor.gotoNextSibling());
            cursor.gotoParent();
        }
    }
}