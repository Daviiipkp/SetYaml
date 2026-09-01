package org.daviipkp.interfaces;

import java.io.File;

import org.daviipkp.Utils;

public interface Configurable {

    void fillDefaults();

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

}
