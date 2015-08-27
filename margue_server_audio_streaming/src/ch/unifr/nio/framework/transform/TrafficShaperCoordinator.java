/*
 * TrafficShaperCoordinator.java
 *
 * Created on 28.07.2008, 12:25:27
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

import ch.unifr.nio.framework.ChannelHandler;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class for coordinating traffic shapers.
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
//@ThreadSafe
public class TrafficShaperCoordinator implements Runnable {

    private final static Logger logger =
            Logger.getLogger(TrafficShaperCoordinator.class.getName());
    private final ScheduledExecutorService executor;
    private final String debugName;
    private static int counter;
    //@GuardedBy("this")
    private long packageSize;
    //@GuardedBy("this")
    private long delay;
    private Map<ChannelHandler, TrafficShaper> trafficShapers;
    //@GuardedBy("this")
    private Future future;
    //@GuardedBy("this")
    private boolean started;

    /**
     * creates a new instance of TrafficShaperCoordinator
     * @param executor the ScheduledExecutorService for scheduling the executios
     * of this TrafficShaperCoordinator
     * @param packageSize the size of the packages to be sent
     * @param delay the time delay between sending two packages (in ms)
     */
    public TrafficShaperCoordinator(
            ScheduledExecutorService executor, long packageSize, long delay) {
        this(executor, packageSize, delay,
                "TrafficShaperCoordinator " + (++counter));
    }

    /**
     * creates a new instance of TrafficShaperCoordinator
     * @param executor the ScheduledExecutorService for scheduling the executios
     * of this TrafficShaperCoordinator
     * @param debugName a name for this TrafficShaperCoordinator that appears in
     * log files
     * @param packageSize the size of the packages to be sent
     * @param delay the time delay between sending two packages (in ms)
     */
    public TrafficShaperCoordinator(ScheduledExecutorService executor,
            long packageSize, long delay, String debugName) {
        this.executor = executor;
        this.debugName = debugName;
        this.packageSize = packageSize;
        this.delay = delay;
    }

    @Override
    public void run() {
        synchronized (this) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest(debugName + " is calling " +
                        trafficShapers.size() +
                        " trafficShapers to send their package");
            }
            for (TrafficShaper trafficShaper : trafficShapers.values()) {
                try {
                    trafficShaper.sendPackage(packageSize);
                } catch (IOException ex) {
                    handleException(trafficShaper, ex);
                }
            }
        }
    }

    /**
     * starts the coordination process
     */
    public synchronized void start() {
        if (trafficShapers == null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("TrafficShaperCoordinator \"" + debugName +
                        "\": no traffic shapers");
            }
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("TrafficShaperCoordinator \"" + debugName +
                        "\": activating " + trafficShapers.size() +
                        " trafficShapers");
            }
            for (TrafficShaper trafficShaper : trafficShapers.values()) {
                trafficShaper.setActive(true);
            }
            schedule();
        }
        started = true;
    }

    /**
     * stops the coordination process
     */
    public synchronized void stop() {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(
                    "stopping TrafficShaperCoordinator \"" + debugName + "\"");
        }
        if (future != null) {
            future.cancel(false);
        }
        if (trafficShapers != null) {
            for (TrafficShaper trafficShaper : trafficShapers.values()) {
                trafficShaper.setActive(false);
            }
        }
        started = false;
    }

    /**
     * returns the size of the packages to be sent
     * @return the size of the packages to be sent
     */
    public synchronized long getPackageSize() {
        return packageSize;
    }

    /**
     * sets the size of the packages to be sent
     * @param packageSize the size of the packages to be sent
     */
    public synchronized void setPackageSize(long packageSize) {
        this.packageSize = packageSize;
    }

    /**
     * returns the time delay between sending two packages (in ms)
     * @return the time delay between sending two packages (in ms)
     */
    public synchronized long getDelay() {
        return delay;
    }

    /**
     * sets the time delay between sending two packages (in ms)
     * @param delay the time delay between sending two packages (in ms)
     */
    public synchronized void setDelay(long delay) {
        this.delay = delay;
        if ((future != null) && !future.isCancelled()) {
            // reschedule
            future.cancel(false);
            schedule();
        }
    }

    /**
     * registers a TrafficShaper and activates it
     * @param channelHandler the ChannelHandler of the TrafficShaper
     * @param trafficShaper the TrafficShaper to register
     */
    public synchronized void addTrafficShaper(
            ChannelHandler channelHandler, TrafficShaper trafficShaper) {
        if (trafficShapers == null) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest(
                        debugName + ": must create new map of trafficShapers");
            }
            trafficShapers =
                    new ConcurrentHashMap<ChannelHandler, TrafficShaper>();
        } else {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest(debugName + ": put trafficShaper into existing " +
                        "map of trafficShapers");
            }
        }
        trafficShapers.put(channelHandler, trafficShaper);
        trafficShaper.setActive(true);

        if (started && (trafficShapers.size() == 1)) {
            // Scheduling was cancelled because there were no traffic shapers.
            // Now we must reschedule.
            schedule();
        }
    }

    /**
     * removes a TrafficShaper and deactivates it
     * @param channelHandler the ChannelHandler of the TrafficShaper to remove
     */
    public synchronized void removeTrafficShaper(ChannelHandler channelHandler) {
        if (trafficShapers == null) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        debugName + ": no map of trafficShapers!");
            }
        } else {
            TrafficShaper trafficShaper = trafficShapers.remove(channelHandler);
            if (trafficShaper == null) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING, debugName +
                            ": trafficShaper of " +
                            channelHandler.getClass().getName() +
                            " is not in map of trafficShapers!");
                }
            } else {
                trafficShaper.setActive(false);
            }
            if (started && (trafficShapers.size() == 0)) {
                // There is no traffic shaper left. Execution must be cancelled.
                future.cancel(false);
            }
        }
    }

    private void handleException(
            TrafficShaper trafficShaper, Exception exception) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.log(Level.SEVERE, debugName + ": sending package failed, " +
                    "removing trafficShaper...", exception);
        }
        // we have to find the ChannelHandler of of the
        // TrafficShaper in the map
        Set<ChannelHandler> keySet = trafficShapers.keySet();
        for (ChannelHandler channelHandler : keySet) {
            TrafficShaper tmpTrafficShaper = trafficShapers.get(channelHandler);
            if (tmpTrafficShaper.equals(trafficShaper)) {
                removeTrafficShaper(channelHandler);
                channelHandler.channelException(exception);
                return;
            }
        }
    }

    private void schedule() {
        future = executor.scheduleAtFixedRate(
                this, delay, delay, TimeUnit.MILLISECONDS);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("TrafficShaperCoordinator \"" + debugName +
                    "\": successfully scheduled");
        }
    }
}
