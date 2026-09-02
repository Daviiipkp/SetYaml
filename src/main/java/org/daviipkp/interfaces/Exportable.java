package org.daviipkp.interfaces;

import java.lang.reflect.Field;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public interface Exportable {

    void exportUpdate();

    default String classAsJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    default String fieldAsJson(Field f) {
        return null; //implement this

    }

}
