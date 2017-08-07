package cn.wells.condition_advance;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 容量为5的缓冲,存放object对象支持多线程的读/写缓冲。
 * 多个线程操作“一个BoundedBuffer对象”时，它们通过
 * 互斥锁lock对缓冲区items进行互斥访问；而且同一个
 * BoundedBuffer对象下的全部线程共用“notFull”和
 * “notEmpty”这两个Condition。
 * @author clover
 *
 */
class BoundedBuffer {
    final Lock lock = new ReentrantLock();
    final Condition notFull  = lock.newCondition(); 
    final Condition notEmpty = lock.newCondition(); 

    final Object[] items = new Object[5];
    int putptr, takeptr, count;//count缓冲区的容量 ,同一个Boundedbuffer,putptr,takeptr共享

    public void put(Object x) throws InterruptedException {
        lock.lock();    //获取锁
        try {
            // 如果“缓冲已满”，则等待；直到“缓冲”不是满的，才将x添加到缓冲中。
            while (count == items.length)
                notFull.await();//被notFull.await()等待的线程将会被notFull.signal()唤醒
            					//并且根据“公平锁”或“非公平锁”机制抢夺这个“互斥锁”
            // 将x添加到缓冲中
            items[putptr] = x; 
            System.out.print("items["+putptr+"]--");
            // 将“put统计数putptr+1”；如果“缓冲已满”，则设putptr为0。
            if (++putptr == items.length) putptr = 0;
            // 将“缓冲”数量+1
            ++count;
            // 唤醒take线程，因为take线程通过notEmpty.await()等待
            notEmpty.signal();//不管有没有满都唤醒take()

            // 打印写入的数据
            System.out.println(Thread.currentThread().getName() + " put  "+ (Integer)x);
        } finally {
            lock.unlock();    // 释放锁
        }
    }

    public Object take() throws InterruptedException {
        lock.lock();    //获取锁
        try {
            // 如果“缓冲为空”，则等待；直到“缓冲”不为空，才将x从缓冲中取出。
            while (count == 0) 
                notEmpty.await();
            // 将x从缓冲中取出
            Object x = items[takeptr]; 
            // 将“take统计数takeptr+1”；如果“缓冲为空”，则设takeptr为0。
            if (++takeptr == items.length) takeptr = 0;
            // 将“缓冲”数量-1
            --count;
            // 唤醒put线程，因为put线程通过notFull.await()等待
            notFull.signal();//不管有没有满都唤醒put()

            // 打印取出的数据
            System.out.println(Thread.currentThread().getName() + " take "+ (Integer)x);
            return x;
        } finally {
            lock.unlock();    // 释放锁
        }
    } 
}

public class ConditionTest {
    private static BoundedBuffer bb = new BoundedBuffer();

    public static void main(String[] args) {
        // 启动10个“写线程”，向BoundedBuffer中不断的写数据(写入0-9)；
        // 启动10个“读线程”，从BoundedBuffer中不断的读数据。
        for (int i=0; i<10; i++) {
            new PutThread("p"+i, i).start();
            new TakeThread("t"+i).start();
        }
    }

    static class PutThread extends Thread {
        private int num;
        public PutThread(String name, int num) {
            super(name);
            this.num = num;
        }
        public void run() {
            try {
                Thread.sleep(1);    // 线程休眠1ms
                bb.put(num);        // 向BoundedBuffer中写入数据
            } catch (InterruptedException e) {
            }
        }
    }

    static class TakeThread extends Thread {
        public TakeThread(String name) {
            super(name);
        }
        public void run() {
            try {
                Thread.sleep(10);                    // 线程休眠1ms
                bb.take();    // 从BoundedBuffer中取出数据
            } catch (InterruptedException e) {
            }
        }
    }
}