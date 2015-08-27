/*
 * HandshakeNotifier.java
 *
 * Created on 14. Oktober 2006, 20:35
 *
 * This file is part of the NIO Framework.
 *
 * The NIO Framework is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * The NIO Framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.unifr.nio.framework.ssl;

import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLSession;

/**
 * notifies listeners about completed handshakes
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class HandshakeNotifier {

    /**
     * Utility field holding list of HandshakeCompletedListeners.
     */
    private List<HandshakeCompletedListener> listenerList;

    /**
     * Registers HandshakeCompletedListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addHandshakeCompletedListener(
            HandshakeCompletedListener listener) {
        if (listenerList == null) {
            listenerList = new ArrayList<HandshakeCompletedListener>();
        }
        listenerList.add(listener);
    }

    /**
     * Removes HandshakeCompletedListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeHandshakeCompletedListener(
            HandshakeCompletedListener listener) {
        if (listenerList != null) {
            listenerList.remove(listener);
        }
    }

    /**
     * Notifies all registered listeners about the event.
     * @param sslSession the SSLSession where the handshake completed
     */
    public synchronized void fireHandshakeCompleted(SSLSession sslSession) {
        if (listenerList != null) {
            for (int i = 0, size = listenerList.size(); i < size; i++) {
                HandshakeCompletedListener listener = listenerList.get(i);
                listener.handshakeCompleted(sslSession);
            }
        }
    }
}
