package org.daviipkp.interfaces;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.daviipkp.Utils;

public interface Configurable {

    

    void fillDefaults();
    void handleUpdate(List<Field> changedFields);

    default <T extends Configurable> void fillFromFileOrDefaults(File f, boolean replaceEmptyFieldsWithDefaults){
        if (!f.exists()) {
            fillDefaults();
            return;
        }
        if(replaceEmptyFieldsWithDefaults) {
            try{
                T def = (T) this.getClass().getDeclaredConstructor().newInstance();
                def.fillDefaults();
                Utils.fillFromFile(this, f, def);
            }catch(Exception e) {
                throw new RuntimeException("Couldn't create an instance of " + this.getClass().getSimpleName()+ ". Check your constructor.", e);
            } 
        }else{
            Utils.fillFromFile(this, f, null);
     
        }
    }

    default <T extends Configurable> void declareFileChange(T newObj) {
        List<Field> changed = new ArrayList<>();
        for(Field f : newObj.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            try{
                Object oldF = f.get(this);
                Object newF = f.get(newObj);
                if(!oldF.equals(newF)) {
                    changed.add(f);
                    f.set(this, newF);
                }
            }catch(Exception e) {
                throw new RuntimeException("Change declared with wrong class type.");
            }

        }
        handleUpdate(changed);
    }

}
