package org.jmxtrans.embedded.util.time;

import java.sql.Timestamp;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SystemClock {

    private final long period;
    private final AtomicLong now;

    private SystemClock(long period) {
        this.period = period;
        this.now = new AtomicLong(System.currentTimeMillis());
        scheduleClockUpdating();
    }

    /**
     * <p>Instance Holder.</p>
     *
     * @author <a href="https://github.com/loong10k">Loong Wan</a>
     * @since 1.0.0
     */
    private static class InstanceHolder {
        public static final SystemClock INSTANCE = new SystemClock(1);
    }
    /**
     * <p>Instance.</p>
     * @return the static  system clock
     */

    private static SystemClock instance() {
        return InstanceHolder.INSTANCE;
    }
    /**
     * <p>Schedule clock updating.</p>
     */

    private void scheduleClockUpdating() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            /**
             * <p>New thread.</p>
             * @param runnable the runnable
             * @return the thread
             */
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "System Clock");
                thread.setDaemon(true);
                return thread;
            }
        });
        scheduler.scheduleAtFixedRate(new Runnable() {
            /**
             * <p>Run.</p>
             */
            public void run() {
                now.set(System.currentTimeMillis());
            }
        }, period, period, TimeUnit.MILLISECONDS);
    }
    /**
     * <p>Current time millis.</p>
     * @return the long
     */

    private long currentTimeMillis() {
        return now.get();
    }
    /**
     * <p>Now.</p>
     * @return the static long
     */

    public static long now() {
        return instance().currentTimeMillis();
    }
	/**
	 * <p>Now date.</p>
	 * @return the static  string
	 */
    
	public static String nowDate() {
		return new Timestamp(instance().currentTimeMillis()).toString();
	}

}
