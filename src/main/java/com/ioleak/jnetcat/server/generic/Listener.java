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
package com.ioleak.jnetcat.server.generic;

import java.util.List;

import com.ioleak.jnetcat.common.property.ObjectProperty;
import com.ioleak.jnetcat.server.console.KeyCharReader;

public abstract class Listener<T, S> {

  private T serverType;
  private int port;

  private final ObjectProperty<List<S>> objectProperty = new ObjectProperty<>();
  private final Thread keyCharReaderThread = new Thread(new KeyCharReader(this::stopServer));

  public abstract void startServer();

  public abstract boolean stopServer();

  public Listener(T serverType, int port) {
    setServerType(serverType);
    setPort(port);
  }

  public final void startCharReaderThread() {
    keyCharReaderThread.start();
  }

  public final void stopCharReaderThread() {
    keyCharReaderThread.interrupt();
  }

  public final void setServerType(T serverType) {
    this.serverType = serverType;
  }

  public final T getServerType() {
    return serverType;
  }

  public final void setPort(int port) {
    this.port = port;
  }

  public final int getPort() {
    return port;
  }

  protected final ObjectProperty<List<S>> getObjectProperty() {
    return objectProperty;
  }
}