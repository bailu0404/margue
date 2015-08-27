/*
 * TrafficShaper.java
 *
 * Created on 08.09.2008, 14:11:40
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

package ch.unifr.nio.framework.transform;

import java.io.IOException;

/**
 * An interface for transformers that do traffic shaping (i.e. can be
 * coordinated by the TrafficShaperCoordinator).
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public interface TrafficShaper {

    /**
     * sends a package with a given maximum package size
     * @param maxPackageSize the given maximum package size
     * @throws IOException if there is an I/O exception while sending the
     * package
     */
    void sendPackage(long maxPackageSize) throws IOException;
    
    /**
     * Gets called by the TrafficShaperCoordinator, when traffic shaping is 
     * switched on or off. This way TrafficShaper may reorganize some internal
     * structures when beeing switched on or off.
     * @param active if <code>true</code>, traffic shaping is turned on,
     * otherwise it is switched off
     */
    void setActive(boolean active);
}
