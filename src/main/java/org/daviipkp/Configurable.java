package org.daviipkp;

import java.io.File;

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
