package io.github.wycst.wast.clients.redis.data.future;

import java.util.concurrent.*;

/**
 * @Author: wangy
 * @Date: 2020/5/18 17:25
 * @Description:
 */
public abstract class RedisFuture<E> implements Future<E> {

    private LockResult lockResult = new LockResult();
    private boolean done = false;
    private boolean waitingDone = false;

    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return done;
    }

//    public boolean isWaitingDone() {
//        return waitingDone;
//    }

    public void set(Object result) {
        lockResult.set(result);
        lockResult.unlock();
        waitingDone = false;
        done = true;
    }

    public void sync() throws InterruptedException {
        waitingDone = true;
        lockResult.sync();
    }

    public E get() throws InterruptedException, ExecutionException {
        return (E) lockResult.get();
    }

    public E get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return (E) lockResult.get(timeout, unit);
    }

    public abstract boolean isKeepAlive();

    public E getResult() {
        try {
            return get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * @Author: wangy
     * @Date: 2020/5/18 17:21
     * @Description:
     */
    private class LockResult {

        private CountDownLatch countDownLatch = new CountDownLatch(1);
        private Object result;

        public void unlock() {
            countDownLatch.countDown();
        }

        public void set(Object result) {
            this.result = result;
        }

        public Object get() throws InterruptedException {
            countDownLatch.await();
            return result;
        }

        public Object get(long timeout, TimeUnit unit) throws InterruptedException {
            countDownLatch.await(timeout, unit);
            return result;
        }

        public void sync() throws InterruptedException {
            countDownLatch.await();
        }
    }

}
