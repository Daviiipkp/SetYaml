package org.daviipkp;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.daviipkp.annotations.Ignore;
import org.daviipkp.interfaces.Bindable;
import org.daviipkp.interfaces.Configurable;
import org.yaml.snakeyaml.Yaml;

public class Utils {

    public static Field getNotInitializedField(Object object) {
        Class<?> clazz = object.getClass();
        for(Field f : clazz.getDeclaredFields()) {
            if(Modifier.isTransient(f.getModifiers()) || Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            f.setAccessible(true);
            Object value = null;
            try {
                value = f.get(object);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Illegal access exception on field " + f.getName() + " of class with name '" + clazz.getSimpleName() + "'");
            }
            if(value==null)return f;
        }
        return null;
    }

    public static <T extends Bindable> T bindableFromFile(Class<T> clazz, File file, boolean replaceEmptyWithDefaults) {
        if(!replaceEmptyWithDefaults) {
            T obj;
            try{
                obj = clazz.getDeclaredConstructor().newInstance();
            }catch(ReflectiveOperationException e) {
                throw new RuntimeException("Couldn't create an instance of " + clazz.getSimpleName()+ ". Check your constructor.", e);
            }
            fillFromFile(obj, file, null);
            return obj;

        }else{
            T obj;
            T defaultObj;

            try{
                obj = clazz.getDeclaredConstructor().newInstance();
                defaultObj = clazz.getDeclaredConstructor().newInstance();
            }catch(ReflectiveOperationException e) {
                throw new RuntimeException("Couldn't create an instance of " + clazz.getSimpleName()+ ". Check your constructor.", e);
            }
            defaultObj.fillDefaults();
            fillFromFile(obj, file, defaultObj);
            
            return obj;
        }
    }

    public static <T extends Configurable> T configurableFromFile(Class<T> clazz, File file, boolean replaceEmptyWithDefaults) {
        if(!replaceEmptyWithDefaults) {
            T obj;
            try{
                obj = clazz.getDeclaredConstructor().newInstance();
            }catch(ReflectiveOperationException e) {
                throw new RuntimeException("Couldn't create an instance of " + clazz.getSimpleName()+ ". Check your constructor.", e);
            }
            fillFromFile(obj, file, null);
            return obj;

        }else{
            T obj;
            T defaultObj;

            try{
                obj = clazz.getDeclaredConstructor().newInstance();
                defaultObj = clazz.getDeclaredConstructor().newInstance();
            }catch(ReflectiveOperationException e) {
                throw new RuntimeException("Couldn't create an instance of " + clazz.getSimpleName()+ ". Check your constructor.", e);
            }
            defaultObj.fillDefaults();
            fillFromFile(obj, file, defaultObj);
            
            return obj;
        }
    }

    public static void fillFromFile(Object obj, File file, Object defaultToReplaceEmptyFields) {

        InputStream is;
        try {
            is = Files.newInputStream(file.toPath());
        } catch (IOException e) {
            ErrorHandler.handleYaml(e);
            return;
        }
        Map<String, Object> things;
        
        try {
            things = SetYaml.getSnake().load(is);
        } catch (Exception e) {
            ErrorHandler.handleYaml(e);
            return;
        }
        
        for(Field f : obj.getClass().getDeclaredFields()) {
            if(Modifier.isTransient(f.getModifiers()) || Modifier.isStatic(f.getModifiers()) || f.isAnnotationPresent(Ignore.class)) {
                continue;
            }
            Object value = things.get(f.getName());
            if(value == null){
                try {
                    f.setAccessible(true);
                    value = f.get(defaultToReplaceEmptyFields);
                    YamlUtils.unsafeFillValue(file, f.getName(), value);
                } catch (Exception e) {
                    throw new RuntimeException("The configuration file at '" + file.getAbsolutePath() + "'' doesn't have a value for the field named '" + f.getName() + "'', which makes it impossible to create the object of '" + obj.getClass().getSimpleName() + "'");
                }
            }
            f.setAccessible(true);
            try {
                if(f.getType().isEnum() && (value instanceof String)) {
                    f.set(obj, Enum.valueOf((Class<? extends Enum>)f.getType(), value.toString()));
                    continue;
                }
                f.set(obj, value);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Field " + f.getName() + " expected a value with type of '" + f.getType().getSimpleName() + "' but received a value with type of '" + value.getClass().getSimpleName() + "', which makes it impossible to create the object of '" + obj.getClass().getSimpleName()+ "'");
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Illegal access exception on field " + f.getName() + " of class with name '" + obj.getClass().getSimpleName() + "'");
            } catch(Exception e) {
                e.printStackTrace();
            }
            
        }
    }

    public static Path getRunningFolder() throws URISyntaxException {
        File f = new File(SetYaml.class.getProtectionDomain()
                                         .getCodeSource()
                                         .getLocation()
                                         .toURI());

        
        return f.toPath().getParent();
    }

    public static void createOrCrash(File f) {
        if (f.exists()) {
            return;
        }
        try {
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            f.createNewFile();
        } catch (Exception e) {
            ErrorHandler.handleYaml(e);
        }
    }

    public static void overwriteOrCrash(File f) {
        try{
            if(f.exists()) {
                f.delete(); 
            }
        }catch(Exception e) {
            ErrorHandler.handleYaml(e);
        }
        createOrCrash(f);
    }

    public static void deleteOrCrash(File f) {
        try{
            if(f.exists()) {
                f.delete(); 
            }
        }catch(Exception e) {
            ErrorHandler.handleYaml(e);
        }
    }

    public static Map<String, Object> loadOrCrash(Yaml snake, File f) {
        try (InputStream in = new FileInputStream(f)) {
            Map<String, Object> data = snake.load(in);
                
            return data;
        } catch (Exception e) {
            ErrorHandler.handleYaml(e);
            return null;
        }
    }

    public static Map<String, Object> loadOrCrash(Yaml snake, Path p) {
        try (InputStream in = new FileInputStream(p.toFile())) {
            Map<String, Object> data = snake.load(in);
            
            return data;
        } catch (Exception e) {
            ErrorHandler.handleYaml(e);
            return null;
        }
    }

    public static String parseField(Field f) {
        return f.getClass().getSimpleName() + "." + f.getName();
    }

}
