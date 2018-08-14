package com.mediatek.camera.tests.v3.observer;

import com.mediatek.camera.tests.v3.arch.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BackgroundObserver extends Observer {
    private ObserveThread mObserveThread;

    @Override
    protected void doBeginObserve(int index) {
        mObserveThread = new ObserveThread("ObserveThread-" + getClass().getSimpleName() +
                "-" + index, index);
        mObserveThread.start();
    }

    @Override
    protected void doEndObserve(int index) {
        mObserveThread.interrupt();
        mObserveThread.waitToStop();
        mObserveThread = null;
    }

    abstract protected void doObserveInBackground(int index);

    protected final boolean isObserveInterrupted() {
        if (mObserveThread == null) {
            return true;
        }
        return mObserveThread.isInterrupted();
    }

    class ObserveThread extends Thread {
        private AtomicBoolean mIsStopped = new AtomicBoolean(false);
        private int mIndex;

        public ObserveThread(String name, int index) {
            super(name);
            mIndex = index;
        }

        @Override
        public void run() {
            doObserveInBackground(mIndex);
            synchronized (mIsStopped) {
                mIsStopped.set(true);
                mIsStopped.notifyAll();
            }
        }

        public void waitToStop() {
            try {
                synchronized (mIsStopped) {
                    if (mIsStopped.get() == false) {
                        mIsStopped.wait();
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }
}
