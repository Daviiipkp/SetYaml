package org.daviipkp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public class SetYaml {

    private List<BindedConfiguration<?>> binds;
    private Configuration config;
    private Yaml snake;

    private static SetYaml instance;

    public static SetYaml getInstance() {
        return (instance==null)?new SetYaml():instance;
    }

    public Yaml getSnake(){
        return snake;
    }

    private SetYaml() {
        config = new Configuration();

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);

        Representer representer = new Representer(options);

        snake = new Yaml(representer, options);

        if(config.canBind()) {
            binds = new ArrayList<>();
            if(config.getWatchType() == WatchType.POLLING) {
                //start polling thread
            }
        }
        

    }

    public <T> void createConfigurationFile(Class<T> clazz, T model, File file, boolean overwrite, boolean bind) {
        if(file.exists()) {
            if(!overwrite) {
                throw new RuntimeException("Cannot create the new file at '" + file.getAbsolutePath() + "' because it already exists and the overwrite flag is false.");  
            }
            try{
                file.delete();
                file.createNewFile();
            } catch(Exception e) {
                ErrorHandler.handleYaml(e);
            }
        }

        Field f = Utils.getNotInitializedField(model);
        if(f != null) {
            throw new RuntimeException("Can't create the configuration file at '" + file.getAbsolutePath() + "' because the field named '" + f.getName() + "'', has not been declared.");
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);
        Representer representer = new Representer(options);
        representer.addClassTag(model.getClass(), Tag.MAP);
        Yaml sn = new Yaml(representer, options);      
        
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            sn.dump(model, writer);
        }catch(Exception e) {
            ErrorHandler.handleYaml(e);
        }

        if(bind)createBind(model, file, clazz); 

    }


    public <T> T createConfigurationObject(Class<T> clazz, File file, boolean bind) {
        //CHECK IF AN OBJECT OF THAT CLASS ALREADY EXISTS!
        if(!file.exists()) {
            throw new RuntimeException("Impossible to create a configuration if the file doesn't exist. Please create the file at " + file.getAbsolutePath());
        }
        T obj;

        try{
            obj = clazz.getDeclaredConstructor().newInstance();
        }catch(ReflectiveOperationException e) {
            throw new RuntimeException("Couldn't create an instance of " + clazz.getSimpleName()+ ". Check your constructor.", e);
        }

        InputStream is;
        try {
            is = Files.newInputStream(file.toPath());
        } catch (IOException e) {
            ErrorHandler.handleYaml(e);
            return null;
        }
        Map<String, Object> things;
        
        try {
            things = snake.load(is);
        } catch (Exception e) {
            ErrorHandler.handleYaml(e);
            return null;
        }
        
        for(Field f : clazz.getDeclaredFields()) {
            if(Modifier.isTransient(f.getModifiers()) || Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            Object value = things.get(f.getName());
            if(value == null)throw new RuntimeException("The configuration file at '" + file.getAbsolutePath() + "'' doesn't have a value for the field named '" + f.getName() + "'', which makes it impossible to create the object of '" + clazz.getSimpleName() + "'");
            f.setAccessible(true);
            try {
                f.set(obj, value);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Field " + f.getName() + " expected a value with type of '" + f.getType().getSimpleName() + "' but received a value with type of '" + value.getClass().getSimpleName() + "', which makes it impossible to create the object of '" + clazz.getSimpleName()+ "'");
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Illegal access exception on field " + f.getName() + " of class with name '" + clazz.getSimpleName() + "'");
            } catch(Exception e) {
                e.printStackTrace();
            }
            
        }
        
        if(bind)createBind(obj, file, clazz);
        

        return obj;
    }

    private <T> void createBind(T obj, File file, Class<T> clazz) {
        if(config.getWatchType().equals(WatchType.WATCH_SERVICE)) {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
                Path path = Paths.get(file.toURI()); 
                path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            } catch (Exception e) {
                ErrorHandler.handleYaml(e);
            }
        }

        BindedConfiguration<T> bind = new BindedConfiguration<>(file.toPath(), obj, clazz);

        binds.add(bind); //REALLY SHOULD VERIFY THE BINDS LIST BEFORE ADDING!
    }

     public void declareFileChange(Path path) throws IOException {
        if(path == null) return;
        for(BindedConfiguration b : binds) {
            if (Files.isSameFile(b.getPath(), path)) {
                b.declareFileChange();
            }
        }
     }

}
