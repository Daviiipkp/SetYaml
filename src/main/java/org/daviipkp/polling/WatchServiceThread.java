package org.daviipkp.polling;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;

import org.daviipkp.SetYaml;

public class WatchServiceThread extends Thread {

    private Path folder;
    private List<String> files;
    private File dynamic_file;


    private boolean dynamic_paused;
    private static List<WatchServiceThread> threads;
    private static WatchServiceThread dynamic_thread;

    @Override
    public void run() {
        try(WatchService ws = FileSystems.getDefault().newWatchService()){
            folder.register(ws, StandardWatchEventKinds.ENTRY_MODIFY);
            while(!Thread.currentThread().isInterrupted()) {
                WatchKey key = ws.take();
                for(WatchEvent<?> event : key.pollEvents()) {
                    if(event.kind() == StandardWatchEventKinds.OVERFLOW)continue;
                    if(dynamic_file != null) {
                        if(!dynamic_paused) {
                            if(Files.isSameFile(folder.resolve((Path) event.context()), dynamic_file.toPath())) {
                                SetYaml.getInstance().declareDynamicFileChange(dynamic_file);
                            }
                        }
                    }
                    else{
                        Path file = (Path)event.context();
                        for(String f : files) {
                            if (file.toString().equals(f)) {
                                SetYaml.getInstance().declareFileChange(folder.resolve((Path) event.context()));
                            }
                        }
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    System.out.println("cant access " + folder);
                    break;
                }
            }

            

        }catch(InterruptedException e) {
            e.printStackTrace(); //change this
        }catch(Exception e) {
            e.printStackTrace(); //change this
        }
        
    }

    public static void watch(File file) {
        if(threads == null) {
            threads = new ArrayList<>();
        }
        WatchServiceThread trr = parentFolderWatched(file);
        if(trr != null) {
            trr.files.add(file.getName());
            return;
        }
        WatchServiceThread tr = new WatchServiceThread();
        tr.folder = file.toPath().getParent();
        tr.files = new ArrayList<>();
        tr.files.add(file.getName());
        tr.setDaemon(true);
        tr.start();

        
        threads.add(tr);
    }

    public static void setDynamicFile(File f) {
        if(threads == null) {
            threads = new ArrayList<>();
        }
        WatchServiceThread trr = parentFolderWatched(f);
        if(trr != null) {
            trr.files.add(f.getName());
            return;
        }
        WatchServiceThread tr = new WatchServiceThread();
        tr.folder = f.toPath().getParent();
        tr.dynamic_file = f;
        tr.dynamic_paused = false;
        tr.setDaemon(true);
        tr.start();
        
        dynamic_thread = tr;
    }

    public static List<Path> getWatchedFolders() {
        if(threads == null || threads.isEmpty()) {
            return new ArrayList<>();
        }
        List<Path> l = new ArrayList<>();
        for(WatchServiceThread t : threads) {
            l.add(t.folder);
        }
        return l;
    }

    public static WatchServiceThread parentFolderWatched(File f) {
        if(threads == null || threads.isEmpty()) {
            return null;
        }
        Path parent = f.toPath().getParent();
        for(WatchServiceThread t : threads) {
            if (t.folder == parent) {
                return t;
            }
        }
        return null;
    }

    public static void pauseDynamic() {
        dynamic_thread.dynamic_paused = true;
    }

    public static void resumeDynamic() {
        dynamic_thread.dynamic_paused = false;
    }

    
}
