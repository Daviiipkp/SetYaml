package org.daviipkp;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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
    
}
