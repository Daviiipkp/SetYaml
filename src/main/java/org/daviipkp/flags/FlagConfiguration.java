package org.daviipkp.flags;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.daviipkp.Utils;
import org.daviipkp.interfaces.Bindable;
import org.daviipkp.interfaces.Configurable;
import org.daviipkp.types.WatchType;

public class FlagConfiguration implements Configurable, Bindable {

    private String dynamic_file;

    private String working_folder;

    private boolean support_bind;
    private boolean support_dynamic;
    private boolean bind_itself;

    private WatchType watch_type;

    private long watch_service_delay;
    private long polling_delay;

    public FlagConfiguration() {

    }

    public void enableBind() {
        support_bind = true;
    }

    public void disableBind() {
        support_bind = false;
    }

    public boolean canBind() {
        return support_bind;
    }

    public void setWatchType(WatchType arg0) {
        watch_type = arg0;
    }

    public WatchType getWatchType() {
        return watch_type;
    }

    public long getWatchServiceDelay() {
        return watch_service_delay;
    }

    public void setWatchWatchServiceDelay(long watch_service_delay) {
        this.watch_service_delay = watch_service_delay;
    }

    public long getPollingDelay() {
        return polling_delay;
    }

    public void setPollingDelay(long polling_delay) {
        this.polling_delay = polling_delay;
    }

    public boolean shouldBindItself(){
        return bind_itself;
    }

    public boolean canDynamic() {
        return support_dynamic;
    }

    public String getDynamicFile() {
        return dynamic_file;
    }

    public String getWorkingFolder() {
        return working_folder;
    }

    @Override
    public void fillDefaults() {
        dynamic_file = "dynamic.yml";
        working_folder = ".";

        support_bind = true;
        support_dynamic = true;
        bind_itself = false;

        watch_type = WatchType.WATCH_SERVICE;

        watch_service_delay = 1000;
        polling_delay = 10000;
    }

    @Override
    public Path getFile() {
        try {
            return new File(Utils.getRunningFolder().toFile(), "config.yml").toPath();
        } catch (URISyntaxException e) {
            
            e.printStackTrace();
            return null;
        }

    }

}
