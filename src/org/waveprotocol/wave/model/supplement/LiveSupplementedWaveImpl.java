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

package org.waveprotocol.wave.model.supplement;

import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.conversation.quasi.ObservableQuasiConversation;
import org.waveprotocol.wave.model.conversation.quasi.ObservableQuasiConversationView;
import org.waveprotocol.wave.model.conversation.quasi.QuasiConversationImpl;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.supplement.SupplementedWaveImpl.DefaultFollow;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.Wavelet;
import org.waveprotocol.wave.model.wave.WaveletListener;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;
import org.waveprotocol.wave.model.wave.opbased.WaveletListenerImpl;

/**
 * Extension of {@link SupplementedWaveImpl} that is also observable.
 *
 */
public final class LiveSupplementedWaveImpl extends SupplementedWaveWrapper<SupplementedWave>
    implements ObservableSupplementedWave {

  /** Wave view, used to broadcast events in semantic types. */
  private final ObservableWaveView wave;

  /** Conversation model view */
  private final ObservableConversationView conversationView;

  /** Listeners for supplement events. */
  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  private final ObservablePrimitiveSupplement.Listener primitiveListener =
      new ObservablePrimitiveSupplement.Listener() {
        
    @Override
    public void onArchiveVersionChanged(WaveletId wid, long oldVersion, long newVersion) {
      triggerOnMaybeInboxStateChanged();
    }

    @Override
    public void onArchiveClearChanged(boolean oldValue, boolean newValue) {
      triggerOnMaybeInboxStateChanged();
    }

    @Override
    public void onFolderAdded(int newFolder) {
      triggerOnFolderAdded(newFolder);
    }

    @Override
    public void onFolderRemoved(int oldFolder) {
      triggerOnFolderRemoved(oldFolder);
    }

    @Override
    public void onLastReadBlipVersionChanged(WaveletId wid, String bid, long oldVersion,
        long newVersion) {
      triggerOnMaybeBlipReadChanged(wid, bid);
    }

    @Override
    public void onFirstLookBlipVersionSet(WaveletId wid, String bid, long newVersion) {
      triggerOnMaybeBlipLookChanged(wid, bid);
    }
    
    @Override
    public void onLastReadParticipantsVersionChanged(WaveletId wid, long oldVersion,
        long newVersion) {
      Wavelet wavelet = wave.getWavelet(wid);
      if (wavelet != null) {
        triggerOnMaybeParticipantsReadChanged(wavelet);
      }
    }

    @Override
    public void onLastReadTagsVersionChanged(WaveletId wid, long oldVersion, long newVersion) {
      Wavelet wavelet = wave.getWavelet(wid);
      if (wavelet != null) {
        triggerOnMaybeTagsReadChanged(wavelet);
      }
    }

    @Override
    public void onLastReadWaveletVersionChanged(WaveletId wid, long oldVersion, long newVersion) {
      // Fire maybe change for everything.
      Wavelet wavelet = wave.getWavelet(wid);
      if (wavelet != null) {
        triggerOnMaybeWaveletReadChanged(wid);
      }
    }

    @Override
    public void onFirstLookWaveletVersionSet(WaveletId wid, long newVersion) {
      // Fire maybe change for everything.
      Wavelet wavelet = wave.getWavelet(wid);
      if (wavelet != null) {
        triggerOnMaybeWaveletFirstLookVersionChanged(wid);
      }
    }    
    
    @Override
    public void onFollowed() {
      triggerOnMaybeFollowStateChanged();
    }

    @Override
    public void onUnfollowed() {
      triggerOnMaybeFollowStateChanged();
    }

    @Override
    public void onFollowCleared() {
      triggerOnMaybeFollowStateChanged();
    }

    @Override
    public void onWantedEvaluationsChanged(WaveletId wid) {
      triggerOnWantedEvaluationsChanged(wid);
    }

    @Override
    public void onThreadStateChanged(WaveletId waveletId, String threadId,
        ThreadState oldState, ThreadState newState) {
      ObservableConversation conversation = conversationView.getConversation(
          ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveletId));
      if (conversation != null) {
        ObservableConversationThread thread = conversation.getThread(threadId);
        if (thread != null) {
          triggerOnThreadStateChanged(thread);
        }
      }
    }

    @Override
    public void onGadgetStateChanged(String gadgetId, String key, String oldValue,
        String newValue) {
      triggerOnMaybeGadgetStateChanged(gadgetId);
    }

    @Override
    public void onFocusedBlipIdChanged(
        WaveletId wid, String oldFocusedBlipId, String newFocusedBlipId) {
    }

    @Override
    public void onScreenPositionChanged(
        WaveletId wid, ScreenPosition oldScreenPosition, ScreenPosition newScreenPosition) {
    }
  };

  private final WaveletListener waveletListener = new WaveletListenerImpl() {
    
    @Override
    public void onBlipVersionModified(
        ObservableWavelet wavelet, Blip blip, Long oldVersion, Long newVersion) {
      // TODO(hearnden/anorth): Move this to conversation listener when LMVs live there.
      triggerOnMaybeBlipReadChanged(wavelet.getId(), blip.getId());
    }

    @Override
    public void onVersionChanged(ObservableWavelet wavelet, long oldVersion, long newVersion) {
      triggerOnMaybeInboxStateChanged();
      triggerOnMaybeTagsReadChanged(wavelet);
      triggerOnMaybeParticipantsReadChanged(wavelet);
    }
  };

  public LiveSupplementedWaveImpl(ObservablePrimitiveSupplement supplement,
      ObservableWaveView wave, ParticipantId viewer, DefaultFollow followPolicy,
      ObservableQuasiConversationView conversationView) {
    super(SupplementedWaveImpl.create(supplement, conversationView, viewer, followPolicy));
    
    this.wave = wave;
    this.conversationView = conversationView;
    supplement.addListener(primitiveListener);

    ObservableQuasiConversationView.Listener viewListener =
        new ObservableQuasiConversationView.Listener() {

      @Override
      public void onConversationAdded(ObservableQuasiConversation conversation) {
        // TODO(user): Once bug 2820511 is fixed, stop listening to the wavelet.
        ((QuasiConversationImpl) conversation).getBaseConversation().getWavelet().addListener(
            waveletListener);
      }

      @Override
      public void onConversationRemoved(ObservableQuasiConversation conversation) {
        // TODO(user): Once bug 2820511 is fixed, stop listening to the wavelet.
        ((QuasiConversationImpl) conversation).getBaseConversation().getWavelet().removeListener(
            waveletListener);
      }
    };

    conversationView.addListener(viewListener);
    for (ObservableQuasiConversation conversation : conversationView.getConversations()) {
      viewListener.onConversationAdded(conversation);
    }
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  // Private methods

  private void triggerOnMaybeWaveletReadChanged(WaveletId wid) {
    String conversationId = WaveletBasedConversation.idFor(wid);
    ObservableConversation c = conversationView.getConversation(conversationId);
    if (c != null) {
      triggerOnMaybeWaveletReadChanged(c);
    }
  }
  
  private void triggerOnMaybeWaveletReadChanged(ObservableConversation conversation) {
    for (Listener listener : listeners) {
      listener.onMaybeWaveletReadChanged(conversation);
    }
  }

  private void triggerOnMaybeWaveletFirstLookVersionChanged(WaveletId wid) {
    String conversationId = WaveletBasedConversation.idFor(wid);
    ObservableConversation c = conversationView.getConversation(conversationId);
    if (c != null) {
      triggerOnMaybeWaveletFirstLookVersionChanged(c);
    }
  }

  private void triggerOnMaybeWaveletFirstLookVersionChanged(ObservableConversation conversation) {
    for (Listener listener : listeners) {
      listener.onMaybeWaveletFirstLookVersionChanged(conversation);
    }
  }
  
  private void triggerOnMaybeParticipantsReadChanged(Wavelet wavelet) {
    for (Listener listener : listeners) {
      listener.onMaybeParticipantsReadChanged(wavelet);
    }
  }

  private void triggerOnMaybeTagsReadChanged(Wavelet wavelet) {
    for (Listener listener : listeners) {
      listener.onMaybeTagsReadChanged(wavelet);
    }
  }

  private void triggerOnMaybeInboxStateChanged() {
    for (Listener listener : listeners) {
      listener.onMaybeInboxStateChanged();
    }
  }

  private void triggerOnMaybeFollowStateChanged() {
    for (Listener listener : listeners) {
      listener.onMaybeFollowStateChanged();
    }
  }

  private void triggerOnFolderAdded(int newFolder) {
    for (Listener listener : listeners) {
      listener.onFolderAdded(newFolder);
    }
  }

  private void triggerOnFolderRemoved(int oldFolder) {
    for (Listener listener : listeners) {
      listener.onFolderRemoved(oldFolder);
    }
  }

  private void triggerOnWantedEvaluationsChanged(WaveletId waveletId) {
    for (Listener listener : listeners) {
      listener.onWantedEvaluationsChanged(waveletId);
    }
  }

  private void triggerOnThreadStateChanged(ObservableConversationThread thread) {
    for (Listener listener : listeners) {
      listener.onThreadStateChanged(thread);
    }
  }

  private void triggerOnMaybeGadgetStateChanged(String gadgetId) {
    for (Listener listener : listeners) {
      listener.onMaybeGadgetStateChanged(gadgetId);
    }
  }

  //
  // Converters.
  //

  private ObservableConversationBlip getBlip(WaveletId wid, String bid) {
    String conversationId = WaveletBasedConversation.idFor(wid);
    ObservableConversation c = conversationView.getConversation(conversationId);
    return c != null ? c.getBlip(bid) : null;
  }

  private void triggerOnMaybeBlipReadChanged(WaveletId wid, String bid) {
    ObservableConversationBlip blip = getBlip(wid, bid);
    if (blip != null) {
      triggerOnMaybeBlipReadChanged(blip);
    }
  }

  private void triggerOnMaybeBlipReadChanged(ObservableConversationBlip blip) {
    // TODO(user): P0: make these definite events.
    for (Listener listener : listeners) {
      listener.onMaybeBlipReadChanged(blip);
    }
  }

  private void triggerOnMaybeBlipLookChanged(WaveletId wid, String bid) {
    ObservableConversationBlip blip = getBlip(wid, bid);
    if (blip != null) {
      triggerOnMaybeBlipLookChanged(blip);
    }
  }

  private void triggerOnMaybeBlipLookChanged(ObservableConversationBlip blip) {
    // TODO(user): P0: make these definite events.
    for (Listener listener : listeners) {
      listener.onMaybeBlipLookChanged(blip);
    }
  }
}

