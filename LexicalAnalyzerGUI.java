import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LexicalAnalyzerGUI extends Application {

    private TextArea codeArea;   // TextArea to display the original C code
    private TextArea outputArea; // TextArea to display tokens and errors

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Lexical Analyzer");

        // Button to select a C file
        Button openFileButton = new Button("Open C File");
        openFileButton.setOnAction(e -> openFile(primaryStage));

        // Button to reanalyze the code
        Button reanalyzeButton = new Button("Reanalyze Code");
        reanalyzeButton.setOnAction(e -> reanalyzeCode());

        // Text area to display the uploaded C code
        codeArea = new TextArea();
        codeArea.setEditable(true); // Allow editing of the code
        codeArea.setWrapText(true);
        codeArea.setPromptText("Uploaded C code will appear here...");

        // Text area to display tokens and errors
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setPromptText("Lexical analysis results will appear here...");

        // Layout
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15, 15, 15, 15));
        layout.getChildren().addAll(openFileButton, reanalyzeButton, codeArea, outputArea);

        // Scene setup
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void openFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open C File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("C Files", "*.c"));
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                String inputCode = readFile(selectedFile.getAbsolutePath());
                codeArea.setText(inputCode); // Display the original C code in the codeArea
                List<String> tokens = lexicalAnalyzer(inputCode);
                displayTokensAndErrors(tokens);
            } catch (IOException e) {
                outputArea.appendText("Error reading the file: " + e.getMessage() + "\n");
            }
        }
    }

    private String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private void reanalyzeCode() {
        String inputCode = codeArea.getText(); // Get the modified code from the codeArea
        List<String> tokens = lexicalAnalyzer(inputCode);
        displayTokensAndErrors(tokens); // Reanalyze and display results
    }

    private List<String> lexicalAnalyzer(String inputCode) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        int i = 0;

        while (i < inputCode.length()) {
            char ch = inputCode.charAt(i);

            // Ignore whitespace
            if (Character.isWhitespace(ch)) {
                i++;
                continue;
            }

            // Handle single-line comments
            if (ch == '/' && i + 1 < inputCode.length() && inputCode.charAt(i + 1) == '/') {
                while (i < inputCode.length() && inputCode.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }

            // Handle multi-line comments
            if (ch == '/' && i + 1 < inputCode.length() && inputCode.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < inputCode.length() && !(inputCode.charAt(i) == '*' && inputCode.charAt(i + 1) == '/')) {
                    i++;
                }
                i += 2; // Skip over the closing */
                continue;
            }

            // Handle punctuation and operators as separate tokens
            if (isPunctuationOrOperator(ch)) {
                tokens.add(String.valueOf(ch));
                i++;
                continue;
            }

            // Check for valid token start (e.g., letters for identifiers)
            if (isValidTokenStart(ch)) {
                currentToken.append(ch);
                i++;

                // Continue forming the token
                while (i < inputCode.length() && isValidTokenChar(inputCode.charAt(i))) {
                    currentToken.append(inputCode.charAt(i));
                    i++;
                }

                // Verify if it is a valid token
                if (isValidToken(currentToken.toString())) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0); // Reset current token
                } else {
                    // Report error and try recovery
                    outputArea.appendText("Lexical error: Invalid token '" + currentToken + "' at position " + i + "\n");
                    int recoveryIndex = panicModeRecovery(inputCode, i);
                    i = recoveryIndex;
                    currentToken.setLength(0); // Reset current token
                }
            } else {
                // Report and recover from single-character error
                outputArea.appendText("Lexical error: Unexpected character '" + ch + "' at position " + i + "\n");
                i++; // Skip the erroneous character
            }
        }

        return tokens;
    }

    private boolean isValidTokenStart(char ch) {
        return Character.isLetter(ch) || ch == '_';
    }

    private boolean isValidTokenChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }

    private boolean isValidToken(String token) {
        String[] keywords = {"int", "float", "return", "main"};
        for (String keyword : keywords) {
            if (keyword.equals(token)) {
                return true;
            }
        }
        return isIdentifier(token);
    }

    private boolean isIdentifier(String token) {
        if (token.length() == 0 || Character.isDigit(token.charAt(0))) {
            return false;
        }
        for (char ch : token.toCharArray()) {
            if (!isValidTokenChar(ch)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPunctuationOrOperator(char ch) {
        // Define the set of punctuation and operators
        String punctuationAndOperators = "[]{}();,=+-*/<>!&|";
        return punctuationAndOperators.indexOf(ch) != -1;
    }

    private int panicModeRecovery(String inputCode, int currentIndex) {
        while (currentIndex < inputCode.length() && inputCode.charAt(currentIndex) != ' ' &&
                inputCode.charAt(currentIndex) != ';' && inputCode.charAt(currentIndex) != '\n') {
            currentIndex++;
        }
        return currentIndex + 1;
    }

    private void displayTokensAndErrors(List<String> tokens) {
        outputArea.clear();
        outputArea.appendText("Tokens:\n");

        for (String token : tokens) {
            outputArea.appendText(token + "\n"); // Each token on a new line
        }

        outputArea.appendText("\nLexical analysis completed.\n");
    }
}
