package org.daviipkp;

import java.io.IOException;

import org.yaml.snakeyaml.composer.ComposerException;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.reader.ReaderException;
import org.yaml.snakeyaml.scanner.ScannerException;

public class ErrorHandler {

    public static void handleYaml(Exception e) {
        if (e instanceof ScannerException) {
            throw new RuntimeException("Lexical or syntax error in YAML file: " + e.getMessage(), e);
        } else if (e instanceof ParserException) {
            throw new RuntimeException("Structural parsing error in YAML file: " + e.getMessage(), e);
        } else if (e instanceof ComposerException) {
            throw new RuntimeException("YAML structure composed incorrectly or unresolved anchor: " + e.getMessage(), e);
        } else if (e instanceof ConstructorException) {
            throw new RuntimeException("Failed to construct POJO from YAML: " + e.getMessage(), e);
        } else if (e instanceof ReaderException) {
            throw new RuntimeException("Invalid character encoding or stream reading error: " + e.getMessage(), e);
        } else if (e instanceof YAMLException) {
            throw new RuntimeException("General SnakeYAML processing error: " + e.getMessage(), e);
        } else if (e instanceof IOException) {
            throw new RuntimeException("Failed to read configuration file from disk: " + e.getMessage(), e);
        } else {
            throw new RuntimeException("Unexpected error during configuration loading: " + e.getMessage(), e);
        }
    }

    
    
}
