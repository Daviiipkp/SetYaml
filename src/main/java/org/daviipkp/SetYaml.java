package org.daviipkp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.daviipkp.annotations.Ignore;
import org.daviipkp.flags.FlagConfiguration;
import org.daviipkp.polling.WatchServiceThread;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public class SetYaml {

    private List<BindedConfiguration<?>> binds = new ArrayList<>();
    private static Yaml snake;
    private FlagConfiguration config;

    private static SetYaml instance;

    public static SetYaml getInstance() {
        if(instance==null)instance = new SetYaml();
        return instance;
    }

    public static Yaml getSnake(){
        return snake;
    }

    
    public static void main(String[] args) throws InterruptedException {
        instance = new SetYaml();
        while(true){
            System.out.println(instance.getFlagConfiguration().getPollingDelay());
            Thread.sleep(1000);
        }
    }


    private SetYaml() {

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);
        Representer representer = new Representer(options){
            @Override
            protected Set<Property> getProperties(Class<?> type) {
                return super.getProperties(type).stream()
                    .filter(prop -> prop.getAnnotation(Ignore.class) == null)
                    .collect(Collectors.toSet());
            }
            @Override
            protected MappingNode representJavaBean(Set<Property> properties, Object javaBean) {
                MappingNode node = super.representJavaBean(properties, javaBean);
                node.setTag(Tag.MAP);
                return node;
            }
        };
        representer.getPropertyUtils().setBeanAccess(BeanAccess.FIELD);

        snake = new Yaml(representer, options);


        setupConfig();


        if(getFlagConfiguration().canBind()) {
            if(getFlagConfiguration().getWatchType() == WatchType.POLLING) {
                //start polling thread
            }
        }
        

    }

    public <T extends Configurable> void createConfigurationFile(Class<T> clazz, T model, File file, boolean overwrite) {
        if(file.exists()) {
            if(!overwrite) {
                System.out.println("Cannot create the new file at '" + file.getAbsolutePath() + "' because it already exists and the overwrite flag is false.");  
                return;
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
        
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            getSnake().dump(model, writer);
        }catch(Exception e) {
            ErrorHandler.handleYaml(e);
        }

    }

    public <T extends Bindable> void createAndBindConfigurationFile(Class<T> clazz, T model, File file, boolean overwrite) {
        if(file.exists()) {
            if(!overwrite) {
                System.out.println("Cannot create the new file at '" + file.getAbsolutePath() + "' because it already exists and the overwrite flag is false.");  
                createBind(model, file, clazz); 
                return;
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
        
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            getSnake().dump(model, writer);
        }catch(Exception e) {
            ErrorHandler.handleYaml(e);
        }

        createBind(model, file, clazz); 

    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T extends Configurable> T createConfigurationObject(Class<T> clazz, File file) {
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
            if(Modifier.isTransient(f.getModifiers()) || Modifier.isStatic(f.getModifiers()) || f.isAnnotationPresent(Ignore.class)) {
                continue;
            }
            Object value = things.get(f.getName());
            if(value == null)throw new RuntimeException("The configuration file at '" + file.getAbsolutePath() + "'' doesn't have a value for the field named '" + f.getName() + "'', which makes it impossible to create the object of '" + clazz.getSimpleName() + "'");
            f.setAccessible(true);
            try {
                if(f.getType().isEnum() && (value instanceof String)) {
                    f.set(obj, Enum.valueOf((Class<? extends Enum>)f.getType(), value.toString()));
                    continue;
                }
                f.set(obj, value);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Field " + f.getName() + " expected a value with type of '" + f.getType().getSimpleName() + "' but received a value with type of '" + value.getClass().getSimpleName() + "', which makes it impossible to create the object of '" + clazz.getSimpleName()+ "'");
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Illegal access exception on field " + f.getName() + " of class with name '" + clazz.getSimpleName() + "'");
            } catch(Exception e) {
                e.printStackTrace();
            }
            
        }

        return obj;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T extends Bindable> T createAndBindConfigurationObject(Class<T> clazz, File file) {
        //CHECK IF AN OBJECT OF THAT CLASS ALREADY EXISTS!
        if(!file.exists()) {
            throw new RuntimeException("Impossible to create a configuration if the file doesn't exist. Please create the file at " + file.getAbsolutePath());
        }
        T obj = Utils.bindableFromFile(clazz, file);

        createBind(obj, file, clazz);

        return obj;
    }

    private <T extends Bindable> void createBind(T obj, File file, Class<T> clazz) {
        if(getFlagConfiguration().getWatchType().equals(WatchType.WATCH_SERVICE)) {
            WatchServiceThread.watch(file);
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

     private void setupConfig() {
        config = new FlagConfiguration();
        config.fillFromFileOrDefaults(config.getFile().toFile());
        if(config.shouldBindItself()) {
            this.createAndBindConfigurationFile(FlagConfiguration.class, this.getFlagConfiguration(), this.getFlagConfiguration().getFile().toFile(), false);
            config = null;
        }
     }

     public static Path getRunningFolder() throws URISyntaxException {
        File f = new File(SetYaml.class.getProtectionDomain()
                                         .getCodeSource()
                                         .getLocation()
                                         .toURI());

        
        return f.toPath().getParent();
     }

     public FlagConfiguration getFlagConfiguration() {
        if (config != null) return config;
        for(BindedConfiguration<?> b : binds) {
            if (b.getObject() instanceof FlagConfiguration) {
                return (FlagConfiguration)b.getObject();
            }
        }
        return null;
     }

}
