/*
 * BufferSizeListenerHandler.java
 *
 * Created on 02.06.2008, 16:30:13
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

package ch.unifr.nio.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that handles the details for serving a list of BufferSizeListeners
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class BufferSizeListenerHandler {

    private final static Logger logger =
            Logger.getLogger(BufferSizeListenerHandler.class.getName());
    private final Object source;
    private List<BufferSizeListener> bufferSizeListenerList;
    private int bufferFillLevel;

    /**
     * creates a new BufferSizeListenerHandler
     * @param source the source for the events that will be fired
     */
    public BufferSizeListenerHandler(Object source) {
        this.source = source;
    }

    /**
     * registers BufferSizeListener as event receiver.
     * @param listener the listener to be registered
     */
    public synchronized void addBufferSizeListener(
            BufferSizeListener listener) {
        if (bufferSizeListenerList == null) {
            bufferSizeListenerList =
                    new ArrayList<BufferSizeListener>();
        }
        bufferSizeListenerList.add(listener);
    }

    /**
     * removes a BufferSizeListener as event receiver.
     * @param listener the listener to be removed
     */
    public synchronized void removeBufferSizeListener(
            BufferSizeListener listener) {
        if (bufferSizeListenerList != null) {
            bufferSizeListenerList.remove(listener);
        }
    }

    /**
     * updates the current fill level and informs all listeners when necessary
     * @param newLevel
     */
    public synchronized void updateLevel(int newLevel) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "bufferFillLevel = " + bufferFillLevel +
                    ", newLevel = " + newLevel);

        }
        if (bufferFillLevel != newLevel) {
            /**
             * !!! IMPORTANT !!!
             * Must update value of bufferFillLevel before going through the 
             * bufferSizeListenerList. Otherwise we get an recursive loop
             * if some of the bufferSizeListeners do I/O.
             */
            bufferFillLevel = newLevel;
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                        "bufferFillLevel = " + bufferFillLevel);
            }
            if (bufferSizeListenerList == null) {
                logger.log(Level.FINEST, "bufferSizeListenerList == null");
            } else {
                for (int i = 0, size = bufferSizeListenerList.size();
                        i < size; i++) {
                    bufferSizeListenerList.get(i).
                            bufferSizeChanged(source, newLevel);
                }
            }
        }
    }
}
