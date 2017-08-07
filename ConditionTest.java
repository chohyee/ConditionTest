package cn.wells.condition_advance;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ����Ϊ5�Ļ���,���object����֧�ֶ��̵߳Ķ�/д���塣
 * ����̲߳�����һ��BoundedBuffer����ʱ������ͨ��
 * ������lock�Ի�����items���л�����ʣ�����ͬһ��
 * BoundedBuffer�����µ�ȫ���̹߳��á�notFull����
 * ��notEmpty��������Condition��
 * @author clover
 *
 */
class BoundedBuffer {
    final Lock lock = new ReentrantLock();
    final Condition notFull  = lock.newCondition(); 
    final Condition notEmpty = lock.newCondition(); 

    final Object[] items = new Object[5];
    int putptr, takeptr, count;//count������������ ,ͬһ��Boundedbuffer,putptr,takeptr����

    public void put(Object x) throws InterruptedException {
        lock.lock();    //��ȡ��
        try {
            // �������������������ȴ���ֱ�������塱�������ģ��Ž�x��ӵ������С�
            while (count == items.length)
                notFull.await();//��notFull.await()�ȴ����߳̽��ᱻnotFull.signal()����
            					//���Ҹ��ݡ���ƽ�����򡰷ǹ�ƽ�������������������������
            // ��x��ӵ�������
            items[putptr] = x; 
            System.out.print("items["+putptr+"]--");
            // ����putͳ����putptr+1�������������������������putptrΪ0��
            if (++putptr == items.length) putptr = 0;
            // �������塱����+1
            ++count;
            // ����take�̣߳���Ϊtake�߳�ͨ��notEmpty.await()�ȴ�
            notEmpty.signal();//������û����������take()

            // ��ӡд�������
            System.out.println(Thread.currentThread().getName() + " put  "+ (Integer)x);
        } finally {
            lock.unlock();    // �ͷ���
        }
    }

    public Object take() throws InterruptedException {
        lock.lock();    //��ȡ��
        try {
            // ���������Ϊ�ա�����ȴ���ֱ�������塱��Ϊ�գ��Ž�x�ӻ�����ȡ����
            while (count == 0) 
                notEmpty.await();
            // ��x�ӻ�����ȡ��
            Object x = items[takeptr]; 
            // ����takeͳ����takeptr+1�������������Ϊ�ա�������takeptrΪ0��
            if (++takeptr == items.length) takeptr = 0;
            // �������塱����-1
            --count;
            // ����put�̣߳���Ϊput�߳�ͨ��notFull.await()�ȴ�
            notFull.signal();//������û����������put()

            // ��ӡȡ��������
            System.out.println(Thread.currentThread().getName() + " take "+ (Integer)x);
            return x;
        } finally {
            lock.unlock();    // �ͷ���
        }
    } 
}

public class ConditionTest {
    private static BoundedBuffer bb = new BoundedBuffer();

    public static void main(String[] args) {
        // ����10����д�̡߳�����BoundedBuffer�в��ϵ�д����(д��0-9)��
        // ����10�������̡߳�����BoundedBuffer�в��ϵĶ����ݡ�
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
                Thread.sleep(1);    // �߳�����1ms
                bb.put(num);        // ��BoundedBuffer��д������
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
                Thread.sleep(10);                    // �߳�����1ms
                bb.take();    // ��BoundedBuffer��ȡ������
            } catch (InterruptedException e) {
            }
        }
    }
}