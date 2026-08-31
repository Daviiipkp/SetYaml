package org.daviipkp;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.Map;

import org.daviipkp.annotations.Ignore;

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

    public static <T extends Bindable> T bindableFromFile(Class<T> clazz, File file) {
        T obj;

        try{
            obj = clazz.getDeclaredConstructor().newInstance();
        }catch(ReflectiveOperationException e) {
            throw new RuntimeException("Couldn't create an instance of " + clazz.getSimpleName()+ ". Check your constructor.", e);
        }

        fillFromFile(obj, file);
        
        return obj;
    }

    public static void fillFromFile(Object obj, File file) {

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
            if(value == null)throw new RuntimeException("The configuration file at '" + file.getAbsolutePath() + "'' doesn't have a value for the field named '" + f.getName() + "'', which makes it impossible to create the object of '" + obj.getClass().getSimpleName() + "'");
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
    
}
