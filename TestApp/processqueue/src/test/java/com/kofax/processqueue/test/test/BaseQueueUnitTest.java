package com.kofax.processqueue.test.test;

import com.kofax.processqueue.BaseQueue;
import com.kofax.processqueue.QueueModel;
import com.kofax.processqueue.exceptions.QueueException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class BaseQueueUnitTest {
    BaseQueue<QueueModel> baseQueue;

    @Test
    public void testBaseQueueInit(){
        baseQueue = new BaseQueue<QueueModel>();
        assertNotNull(baseQueue);
        baseQueue = null;
    }
    @Test
    public void testAddItemToBqueue(){
        baseQueue = new BaseQueue<QueueModel>();
        QueueModel data = new QueueModel();

        baseQueue.enQueue(data);
        assertEquals(1,baseQueue.count());
        baseQueue = null;
    }
    @Test(expected = NullPointerException.class)
    public void addNullToBqueue(){
        baseQueue = new BaseQueue<QueueModel>();
        baseQueue.enQueue(null);

        baseQueue = null;
    }

    @Test
    public void dequeueItemFromQueue() throws QueueException {
        baseQueue = new BaseQueue<QueueModel>();
        for(int i=0;i<10;i++){
            QueueModel data1 = new QueueModel();
            baseQueue.enQueue(data1);
        }
        for(int j=0;j<10;j++){
            QueueModel q = baseQueue.deQueue();
            assertNotNull(q);
            //assertEquals();
        }
        assertEquals(0,baseQueue.count());
        baseQueue = null;

    }




}
