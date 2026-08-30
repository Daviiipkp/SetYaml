package org.daviipkp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class BindedConfiguration<T> {

    private Path configurationFile;
    private T configurationObject;
    private Class<T> configurationClass;


    public BindedConfiguration(Path configurationFile, T configurationObject, Class<T> configurationClass) {
        this.configurationFile = configurationFile;
        this.configurationObject = configurationObject;
        this.configurationClass = configurationClass;
    }

    public Path getPath() {
        return configurationFile;
    }

    public void declareFileChange() {
        InputStream is;
        try {
            is = Files.newInputStream(configurationFile);
        } catch (IOException e) {
            ErrorHandler.handleYaml(e);
            return;
        }
        configurationObject = SetYaml.getInstance().getSnake().loadAs(is, configurationClass);
    }

    

}
