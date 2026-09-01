package org.daviipkp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;

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

    public static void handleFile(Exception e, File f) {
        if(e instanceof IOException) {
            throw new RuntimeException("Couldn't read file at '" + f.getAbsolutePath() + "'. The program having limited permissions might be causing this.");
        } else if(e instanceof FileNotFoundException) {
            throw new RuntimeException("Couldn't find the file at '" + f.getAbsolutePath() + "'. Check your path.", e);
        }
    }

    public static void handleReflection(Exception e, Field f) {
        if(e instanceof IllegalAccessException) {
            throw new RuntimeException("Couldn't access the field '" + f.getName() + "' in the class '" + f.getClass().getSimpleName() + "'. Check your access modifiers.", e);
        } else if(e instanceof IllegalArgumentException) {
            throw new RuntimeException("The value for the field '" + f.getName() + "' in the class '" + f.getClass().getSimpleName() + "' is of an incompatible type. Check your configuration file.", e);
        }

    }

    
    
}
