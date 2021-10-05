/*
 * Copyright (c) 2021, crashdump (<xxxx>@ioleak.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.ioleak.jnetcat;

import com.ioleak.jnetcat.service.JNetcatProcess;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;

import com.ioleak.jnetcat.common.FileWatcher;
import com.ioleak.jnetcat.common.Logging;
import com.ioleak.jnetcat.common.parsers.ArgumentsParser;
import com.ioleak.jnetcat.common.utils.JsonUtils;
import com.ioleak.jnetcat.server.console.KeyCharReader;

public class JNetcat {

  public static final String DESCRIPTION = "A useful network tool to debug for client/server";
  public static final String DEFAULT_CONFIG = "external/conf/AllOptionsInOne.json";

  private static Thread jnetcatThread;
  private static JNetcatProcess jnetcatRun;

  public static void main(String... args) {
    String version = JNetcat.class.getPackage().getImplementationVersion();
    ArgumentsParser argumentsParser = new ArgumentsParser(version, DESCRIPTION, getParametersMapping());
    int returnCode = 0;

    argumentsParser.setArguments(args);

    if (argumentsParser.switchPresent("-h")) {
      System.out.println(argumentsParser.getHelpMessage());
      System.exit(0);
    }

    String jsonParameters = argumentsParser.switchValue("-f", DEFAULT_CONFIG);
    Logging.getLogger().info(String.format("Using parameter file: %s", jsonParameters));
    URL url = JsonUtils.getRelativePath(jsonParameters, JNetcat.class);

    try {
      File jsonFile = new File(url.toURI());
      Logging.getLogger().info(String.format("Parameter file absolute path: %s", jsonFile.getAbsolutePath()));

      initJNetcatProcessRun(jsonFile);

      startThreadFileWatcher(jsonFile, 2500);
      readKeyboardCharConsole();

      keepRunningIndefinitely();
    } catch (URISyntaxException ex) {
      Logging.getLogger().error("Cannot start execution, invalid URI", ex);
    }

    System.exit(returnCode);
  }

  private static void startThreadFileWatcher(File jsonFile, int delayMs) {
    FileWatcher fileWatcher = new FileWatcher(jsonFile, JNetcat::paramsHotReload);
    Timer timer = new Timer();
    timer.schedule(fileWatcher, new Date(), delayMs);
  }

  private static void initJNetcatProcessRun(File jsonFile) {
    jnetcatRun = JNetcatProcess.JNETCATPROCESS;
    jnetcatRun.setJsonParamsFile(jsonFile);
  }

  private static void readKeyboardCharConsole() {
    KeyCharReader keyCharReader = new KeyCharReader(jnetcatRun::stopActiveExecution,
                                                    () -> {
                                                      boolean executionStopped = jnetcatRun.stopExecutions();
                                                      if (executionStopped) {
                                                        System.exit(0);
                                                      }

                                                      return executionStopped;
                                                    });
    keyCharReader.run();
  }

  private static void keepRunningIndefinitely() {
    final CountDownLatch latch = new CountDownLatch(1);

    try {
      latch.await();
    } catch (InterruptedException ex) {
      Logging.getLogger().error("Execution interrupted. Program will exit.", ex);
    }
  }

  private static void paramsHotReload(File jsonParamsFile) {
    Logging.getLogger().info(String.format("File (%s) modified since last check - reloading", jsonParamsFile.getAbsolutePath()));

    if (jnetcatThread != null) {
      jnetcatRun.stopExecutions();
      jnetcatThread.interrupt();

      try {
        jnetcatThread.join();
      } catch (InterruptedException ex) {
        Logging.getLogger().warn("A running thread was interrupted (and it was expected to stop: this message is informative only)");
      }

      Logging.getLogger().warn(String.format("Existing thread stopped with return code: %d", jnetcatRun.getResultExecution()));
    }

    jnetcatThread = new Thread(jnetcatRun);
    jnetcatThread.start();
  }

  private static Map<String, String> getParametersMapping() {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("-f", "Configuration file to use for default parameters");

    return parameters;
  }
}
