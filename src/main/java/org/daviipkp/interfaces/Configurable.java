package org.daviipkp.interfaces;

import java.io.File;

import org.daviipkp.Utils;

public interface Configurable {

    void fillDefaults();

    default void fillFromFileOrDefaults(File f){
        if (!f.exists()) {
            fillDefaults();
            return;
        }
        Utils.fillFromFile(this, f);
    }

}
