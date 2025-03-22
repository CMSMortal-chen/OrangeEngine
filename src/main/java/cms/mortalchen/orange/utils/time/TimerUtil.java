
package cms.mortalchen.orange.utils.time;

public class TimerUtil {
    public long lastMs;
    public TimerUtil(){
        this.lastMs = System.currentTimeMillis();
    }

    public boolean isDelayComplete(long delay) {
        return System.currentTimeMillis() - this.lastMs > delay;
    }

    public long getCurrentMS() {
        return System.currentTimeMillis();
    }

    public void reset() {
        this.lastMs = System.currentTimeMillis();
    }

    public long getLastMs() {
        return this.lastMs;
    }

    public void setLastMs(int i) {
        this.lastMs = System.currentTimeMillis() + (long)i;
    }

    public boolean hasReached(long timeLeft) {
        return (this.getCurrentMS() - this.lastMs) >= timeLeft;
    }

    public boolean hasReached(float timeLeft) {
        return (this.getCurrentMS() - this.lastMs) >= timeLeft;
    }

    public boolean hasReached(double timeLeft) {
        return (this.getCurrentMS() - this.lastMs) >= timeLeft;
    }

    public boolean delay(double nextDelay) {
        return (double)(System.currentTimeMillis() - this.lastMs) >= nextDelay;
    }
}
