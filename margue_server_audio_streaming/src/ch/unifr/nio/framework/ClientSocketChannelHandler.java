/*
 * ClientSocketChannelHandler.java
 *
 * Created on 15. September 2007, 14:50
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

import java.io.IOException;

/**
 * A ChannelHandler for SocketChannels on client sides that uses the NIO
 * framework to resolve the host name of the target system and to establish the
 * connection to the target system.
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public interface ClientSocketChannelHandler extends ChannelHandler {

    /**
     * called by the framework if resolving the host name failed
     */
    void resolveFailed();

    /**
     * Called by the framework if connecting to the given host succeeded.
     * WARNING: Do not use blocking calls within this method or handling of the
     * connection by the NIO Framework will be stalled.
     */
    void connectSucceeded();

    /**
     * called by the framework if connecting to the given host failed
     * @param exception the exception that occured when the connection failed
     */
    void connectFailed(IOException exception);
}
