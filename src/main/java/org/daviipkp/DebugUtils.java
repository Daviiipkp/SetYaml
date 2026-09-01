package org.daviipkp;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class DebugUtils {

    private String separator =  " ";

    private static DebugUtils instance;

    private static DebugUtils getInstance() {
        if(instance == null) {
            instance = new DebugUtils();
        }
        return instance;
    }

    private static String useSeparator() {
        String toReturn = getInstance().separator;
        getInstance().separator = " ";
        return toReturn;
    }

    public static void debug(Object... args) {
        try{
            if(SetYaml.getInstance().getFlagConfiguration().canDebug()) {
                StringBuilder sb = new StringBuilder();
                String ss = useSeparator();
                for(Object arg : args) {
                    sb.append(ss).append(arg.toString());

                }
                System.out.println(sb.toString().replaceFirst(ss, ""));
            }
        }catch(Exception e) {

        }

    }

    public static Map<String, Object> getDynamicFields() {
        Map<String, Object> toReturn = new HashMap<>();
        try{
            if(SetYaml.getInstance().getFlagConfiguration().canDynamic()) {
                for(Field f : SetYaml.getInstance().getDynamicFields()) {
                    f.setAccessible(true);
                    toReturn.put(YamlUtils.parseField(f), f.get(null));
                }
            }
        }catch(Exception e) {
        }
        return toReturn;
    } 

    public static void debugDynamicFields() {
        try{
            if(SetYaml.getInstance().getFlagConfiguration().canDynamic()) {
                printSeparator();
                for(Field f : SetYaml.getInstance().getDynamicFields()) {
                    f.setAccessible(true);
                    DebugUtils.withSeparator(" = ").debug(YamlUtils.parseField(f), f.get(null));
                }
                printSeparator();
            }
        }catch(Exception e) {
        }
    }

    public static DebugUtils withSeparator(String arg0) {
        DebugUtils a = getInstance();
        a.separator = arg0;
        return a;
    }

    public static void printSeparator() {
        debug("-----------------------------");
    }
    
}
