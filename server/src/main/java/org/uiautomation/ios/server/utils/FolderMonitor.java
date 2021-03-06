/*
 * Copyright 2012 ios-driver committers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.uiautomation.ios.server.utils;

import name.pachler.nio.file.*;
import org.uiautomation.ios.server.IOSServerConfiguration;
import org.uiautomation.ios.server.IOSServerManager;
import org.uiautomation.ios.server.application.APPIOSApplication;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;


public class FolderMonitor implements Runnable {
  private static final Logger log = Logger.getLogger(FolderMonitor.class.getName());
  private IOSServerManager iosServerManager;
  private IOSServerConfiguration iosServerConfiguration;
  private WatchService folderWatcher;
  private Path watchedFolder;
  private boolean stopped;

  public FolderMonitor(IOSServerConfiguration iosServerConfiguration, IOSServerManager iosServerManager) throws IOException {
    this.iosServerConfiguration = iosServerConfiguration;
    this.iosServerManager = iosServerManager;
    stopped = false;

    folderWatcher = FileSystems.getDefault().newWatchService();
    watchedFolder = Paths.get(iosServerConfiguration.getAppFolderToMonitor());
    WatchKey key = watchedFolder.register(folderWatcher, StandardWatchEventKind.ENTRY_CREATE, StandardWatchEventKind.ENTRY_DELETE);

    init();
  }

  private void init() {
    File[] listOfFiles = new File(iosServerConfiguration.getAppFolderToMonitor()).listFiles();
    for (File file : listOfFiles) {
      if (isApp(file)) {
        addApplication(file);
      }
      if (isZip(file)) {
        unzipToWatchedFolder(file);
      }
    }
  }

  @Override
  public void run() {
    while (!stopped) {
      checkForChanges();
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void checkForChanges() {
    WatchKey key = null;
    try {
      key = folderWatcher.poll();
    } catch (InterruptedException e) {
      log.warning("problem monitoring the folder, " + e.toString());
    }

    if (key != null) {
      List<WatchEvent<?>> list = key.pollEvents();
      key.reset();
      for (WatchEvent watchEvent : list) {
        final WatchEvent.Kind<?> kind = watchEvent.kind();
        final Path filename = (Path) watchEvent.context();

        log.fine(kind + " : " + filename.toString());
        handleFileChange(kind, new File(watchedFolder.toString() + "/" + filename.toString()));
      }

      boolean valid = key.reset();
      if (!valid) {
        log.warning("Can't monitor folder anymore, has it been deleted?");
        stop();
      }
    }
  }

  private void handleFileChange(WatchEvent.Kind kind, File filename) {

    if (kind.equals(StandardWatchEventKind.ENTRY_CREATE)) {
      if (isApp(filename)) {
        log.info("New app found! " + filename.getName());
        addApplication(filename);
      }
      if (isZip(filename)) {
        unzipToWatchedFolder(filename);
      }
    } else if (kind.equals(StandardWatchEventKind.ENTRY_MODIFY)) {
      log.info("App modified - no handler implemented!");
    } else if (kind.equals(StandardWatchEventKind.ENTRY_DELETE)) {
      log.info("App deleted - no handler implemented!");
    }
  }

  private void unzipToWatchedFolder(File filename) {
    log.info("Unzipping... " + filename.getName());
    try {
      ZipUtils.unzip(filename, new File(iosServerConfiguration.getAppFolderToMonitor()));
    } catch (IOException e) {
      log.warning("Problem unzipping " + filename.getName() + ", " + e.toString());
    }
  }

  private void addApplication(File filename) {
    if (isApp(filename)) {
      iosServerManager.addSupportedApplication(APPIOSApplication.createFrom(filename));
    } else {
      iosServerManager.addSupportedApplication(new APPIOSApplication(filename.getAbsolutePath()));
    }
  }

  private boolean isApp(File file) {
    if (file.getAbsolutePath().contains("ipa.unzipped")) {
      return false;
    }
    String ext = file.getName();
    return ext.endsWith(".app") || ext.endsWith(".ipa");
  }

  private boolean isZip(File file) {
    String ext = file.getName();
    return ext.endsWith(".zip");
  }

  public void stop() {
    stopped = true;
  }
}