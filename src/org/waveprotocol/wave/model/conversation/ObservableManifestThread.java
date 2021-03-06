/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.wave.model.conversation;

import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * Extends {@link ManifestThread} to provide events that can be listened to.
 *
 * @author zdwang@google.com (David Wang)
 */
interface ObservableManifestThread extends ManifestThread,
    SourcesEvents<ObservableManifestThread.Listener>, HasId {
  /**
   * Receives events on a {@link ManifestThread}.
   */
  public interface Listener {
    /**
     * Called when a blip is added to the thread.
     */
    void onBlipAdded(ObservableManifestBlip blip, ObservableManifestBlip previous,
        WaveletOperationContext opContext);

    /**
     * Called when a blip is going to be removed from the thread.
     */
    void onBeforeBlipRemoved(ObservableManifestBlip blip, WaveletOperationContext opContext);

    /**
     * Called when a blip is removed from the thread.
     */
    void onBlipRemoved(ObservableManifestBlip blip, WaveletOperationContext opContext);
  }

  /**
   * Removes all listeners from this thread.
   */
  void detachListeners();

  // Covariant specialisations.

  @Override
  ObservableManifestBlip appendBlip(String id);

  @Override
  ObservableManifestBlip insertBlip(int index, String id);

  @Override
  ObservableManifestBlip getBlip(int index);

  @Override
  Iterable<? extends ObservableManifestBlip> getBlips();
}
