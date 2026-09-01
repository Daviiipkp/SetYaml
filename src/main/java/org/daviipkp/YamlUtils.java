package org.daviipkp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class YamlUtils {

    public static String parseField(Field f) {
        StringBuilder sb = new StringBuilder();
        sb.append(f.getDeclaringClass().getPackageName().replace(".", "-"));
        sb.append(".");
        sb.append(f.getDeclaringClass().getSimpleName());
        sb.append(".");
        sb.append(f.getName());

        return sb.toString();
    
    }

    @SuppressWarnings("unchecked")
    public static void unsafeFillValue(File f, String path, Object value) {
        Yaml snake = SetYaml.getSnake();
        Map<String, Object> map;
        if(f.exists() && f.length() > 0) {
            try {
                InputStream in = new FileInputStream(f);
                map = snake.load(in);
            
            } catch (Exception e) {
                ErrorHandler.handleFile(e, f);
                return;
            }
        
        }else{
            map = new LinkedHashMap<>();
        }
        String[] keys = path.split("\\.");
        Map<String, Object> cMap = map;        
        
        for(int i = 0; i < keys.length - 1; i++) {
            String k = keys[i];
            Object child = cMap.get(k);
            if(child == null || !(child instanceof Map)) {
                Map<String, Object> a = new LinkedHashMap<>();
                cMap.put(k, a);
                cMap = a;
            }else{
                cMap = (Map<String, Object>) child;
            }
        }
        cMap.put(keys[keys.length - 1], value);
        
        try {
            snake.dump(cMap, new FileWriter(f));
        }catch(Exception e) {
            ErrorHandler.handleYaml(e);
        }

    }

    @SuppressWarnings("unchecked")
    public static void unsafeFillValue(File f, String path, Field field) {
        Object value;

        try{
            value = field.get(null);
        }
        catch(Exception e) {
            ErrorHandler.handleReflection(e, field);
            return;
        }

        Yaml snake = SetYaml.getSnake();
        Map<String, Object> map;
        if(f.exists() && f.length() > 0) {
            try {
                InputStream in = new FileInputStream(f);
                map = snake.load(in);
            
            } catch (Exception e) {
                ErrorHandler.handleFile(e, f);
                return;
            }
        
        }else{
            map = new LinkedHashMap<>();
        }
        String[] keys = path.split("\\.");
        Map<String, Object> cMap = map;        
        
        for(int i = 0; i < keys.length - 1; i++) {
            String k = keys[i];
            Object child = cMap.get(k);
            if(child == null || !(child instanceof Map)) {
                Map<String, Object> a = new LinkedHashMap<>();
                cMap.put(k, a);
                cMap = a;
            }else{
                cMap = (Map<String, Object>) child;
            }
        }
        cMap.put(keys[keys.length - 1], value);
        
        try {
            snake.dump(map, new FileWriter(f));
        }catch(Exception e) {
            ErrorHandler.handleYaml(e);
        }

    }

}
