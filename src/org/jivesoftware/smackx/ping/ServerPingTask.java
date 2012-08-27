package org.jivesoftware.smackx.ping;

import java.lang.ref.WeakReference;
import java.util.Set;

import org.jivesoftware.smack.Connection;

class ServerPingTask implements Runnable {
    
    private WeakReference<Connection> weakConnection;
    private int pingIntervall;
    
    private int delta = 1000; // 1 seconds
    private int tries = 3; // 3 tries
    
    protected ServerPingTask(Connection connection, int pingIntervall) {
        this.weakConnection = new WeakReference<Connection>(connection);
        this.pingIntervall = pingIntervall;
    }
    
    protected void setDone() {
        this.pingIntervall = -1;
    }
    
    protected void setPingIntervall(int pingIntervall) {
        this.pingIntervall = pingIntervall;
    }
    
    protected int getIntIntervall() {
        return pingIntervall;
    }
    
    public void run() {            
        sleep(60000);
        
        outerLoop:
        while(pingIntervall > 0) {
            Connection connection = weakConnection.get();
            if (connection == null) {
                // connection has been collected by GC
                // which means we can stop the thread by breaking the loop
                break;
            }
            if (connection.isAuthenticated()) {
                PingManager pingManager = PingManager.getInstaceFor(connection);
                boolean res = false;
                
                for(int i = 0; i < tries; i++) {
                    if (i != 0) {
                        try {
                            Thread.sleep(delta);
                        } catch (InterruptedException e) {
                            // We received an interrupt
                            // This only happens if we should stop pinging
                            break outerLoop;
                        }
                    }
                    res = pingManager.pingMyServer();
                    // stop when we receive a pong back
                    if (res)
                        break;
                }
                if (!res) {
                    Set<PingFailedListener> pingFailedListeners = pingManager.getPingFailedListeners();
                    for (PingFailedListener l : pingFailedListeners) {
                        l.pingFailed();
                    }
                }
            }
            sleep();
        }
    }
    
    private void sleep(int extendedInterval) {
        if (pingIntervall + extendedInterval > 0) {
            try {
                Thread.sleep(pingIntervall + extendedInterval);
            } catch (InterruptedException e) {
                /* Ignore */
            }
        }
    }
    
    private void sleep() {
        sleep(0);
    }
}
