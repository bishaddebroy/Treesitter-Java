package org.example;

import io.github.treesitter.jtreesitter.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter language (python/cpp/javascript): ");
        String languageInput = scanner.nextLine().toLowerCase();

        try {
            // Create a temporary directory for the native libraries
            Path tempDir = Files.createTempDirectory("tree-sitter-lib");
            System.out.println("Temporary directory created: " + tempDir);

            // Load the selected language library
            Path libPath;
            Language language;

            switch (languageInput) {
                case "cpp":
                    libPath = copyLibrary(tempDir, "libtree-sitter-cpp.dll");
                    language = loadLanguage(libPath, "tree_sitter_cpp");
                    break;
                case "javascript":
                    libPath = copyLibrary(tempDir, "libtree-sitter-javascript.dll");
                    language = loadLanguage(libPath, "tree_sitter_javascript");
                    break;
                case "python":
                default:
                    libPath = copyLibrary(tempDir, "libtree-sitter-python.dll");
                    language = loadLanguage(libPath, "tree_sitter_python");
                    break;
            }

            // Create and set up parser
            System.out.println("Creating parser for " + languageInput + "...");
            Parser parser = new Parser();
            parser.setLanguage(language);

            // Test code snippet
            String code = getSampleCode(languageInput);
            System.out.println("Parsing code: " + code);

            // Generate AST
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
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

    private static Language loadLanguage(Path libPath, String treeSitterName) throws IOException {
        System.out.println("Loading symbols from library...");
        SymbolLookup symbols = SymbolLookup.libraryLookup(libPath, Arena.global());
        System.out.println("Loading language: " + treeSitterName);
        return Language.load(symbols, treeSitterName);
    }

    private static String getSampleCode(String language) {
        switch (language) {
            case "cpp":
                return "int main() {\n  std::cout << \"Hello, C++!\" << std::endl;\n  return 0;\n}";
            case "javascript":
                return "function hello() {\n  console.log('Hello, JavaScript!');\n}";
            case "python":
            default:
                return "def main():\n    print('Hello, Python!')";
        }
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
