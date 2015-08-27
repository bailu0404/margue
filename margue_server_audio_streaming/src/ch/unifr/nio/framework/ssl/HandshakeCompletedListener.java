/*
 * HandshakeListener.java
 *
 * Created on 22. Januar 2007, 20:22
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

import javax.net.ssl.SSLSession;

/**
 * a customized version of HandshakeCompletedListener that works without a SSLSocket
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public interface HandshakeCompletedListener {

    /**
     * This method is invoked on registered objects when a SSL handshake is completed.
     * @param session the SSLSession this event is associated with
     */
    void handshakeCompleted(SSLSession session);
}
