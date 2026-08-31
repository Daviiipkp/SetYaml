package org.daviipkp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.daviipkp.annotations.Dynamic;
import org.daviipkp.annotations.Ignore;
import org.daviipkp.flags.FlagConfiguration;
import org.daviipkp.interfaces.Bindable;
import org.daviipkp.interfaces.Configurable;
import org.daviipkp.polling.WatchServiceThread;
import org.daviipkp.types.WatchType;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public final class SetYaml {

    private List<BindedConfiguration<?>> binds = new ArrayList<>();
    private List<Field> dynamic = new ArrayList<>();
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

        setupSnake();

        setupConfig();


        if(getFlagConfiguration().canBind() && getFlagConfiguration().getWatchType() == WatchType.POLLING) {
            setupPolling();
        }

        if(getFlagConfiguration().canDynamic()) {
            setupDynamicSystem();
        }
        

    }

    public void registerDynamicClass(Class<?> clazz) {
        if (!getFlagConfiguration().canDynamic()) {
            return;
        }
        for(Field f : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.isAnnotationPresent(Dynamic.class)) {
                f.setAccessible(true);
                dynamic.add(f);
                //gotta dump each field into the dynamic.yml file. 
            }
        }
    }

    public <T extends Configurable> void createConfigurationFile(Class<T> clazz, T model, File file, boolean overwrite) {
        if(file.exists()) {
            if(!overwrite) {
                System.out.println("Cannot create the new file at '" + file.getAbsolutePath() + "' because it already exists and the overwrite flag is false.");  
                return;
            }
            Utils.deleteOrCrash(file);
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
            Utils.deleteOrCrash(file);
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


    public <T extends Configurable> T createConfigurationObject(Class<T> clazz, File file) {
        //CHECK IF AN OBJECT OF THAT CLASS ALREADY EXISTS!
        if(!file.exists()) {
            throw new RuntimeException("Impossible to create a configuration if the file doesn't exist. Please create the file at " + file.getAbsolutePath());
        }
        return Utils.configurableFromFile(clazz, file);
    }

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
        if (Files.isSameFile(new File(getFlagConfiguration().getWorkingFolder(), getFlagConfiguration().getDynamicFile()).toPath(), path)) {
            Map<String, Object> map = Utils.loadOrCrash(snake, path);
            if (map != null) {
                map.forEach((key, value) -> {
                    if (value instanceof Map<?, ?> innerMap) {
                        innerMap.forEach((innerKey, innerValue) -> {
                            for(Field f : dynamic) {
                                if (Utils.parseField(f).equals(key + "." + innerKey)) {
                                    try {
                                        f.set(null, innerValue);
                                    } catch (IllegalArgumentException e) {
                                        e.printStackTrace();
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
                });
            }
            return;
        }
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

     public FlagConfiguration getFlagConfiguration() {
        if (config != null) return config;
        for(BindedConfiguration<?> b : binds) {
            if (b.getObject() instanceof FlagConfiguration) {
                return (FlagConfiguration)b.getObject();
            }
        }
        return null;
     }

     private void setupSnake() {
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
     }

     private void setupPolling() {

     }
     
     private void setupDynamicSystem() {
        File f = new File(getFlagConfiguration().getWorkingFolder(), getFlagConfiguration().getDynamicFile());
        Utils.overwriteOrCrash(f);
        WatchServiceThread.watch(f);

     }
}
