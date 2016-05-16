package com.kofax.processqueue.test.test;


import com.kofax.processqueue.BaseManager;
import com.kofax.processqueue.ImageProcessingQueueManager;
import com.kofax.processqueue.QueueModel;
import com.kofax.processqueue.exceptions.QueueException;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class QueueManagerUnitTest {
    ImageProcessingQueueManager queueManager = null;

    @Test
    public void initManager(){
        queueManager = ImageProcessingQueueManager.getInstance();
        assertNotNull(queueManager);

    }
    @Test
    public void addItem(){
        queueManager =  ImageProcessingQueueManager.getInstance();
        queueManager.addItem(new QueueModel());
        assertEquals(1,queueManager.count());
        queueManager.removeAll();
    }

    @Test
    public void removeItem(){
        queueManager =  ImageProcessingQueueManager.getInstance();
        QueueModel q = new QueueModel();
        queueManager.addItem(q);
        queueManager.removeItem(q);

        assertEquals(0,queueManager.count());
        queueManager.removeAll();
    }

    @Test
    public void removeUnknownItem(){
        queueManager =  ImageProcessingQueueManager.getInstance();
        QueueModel q = new QueueModel();
        queueManager.addItem(q);
        queueManager.removeItem(new QueueModel());

        assertEquals(1,queueManager.count());
        queueManager.removeAll();
    }

    @Test
    public void removeAll(){
        queueManager =  ImageProcessingQueueManager.getInstance();
        QueueModel q1 = new QueueModel();
        QueueModel q2 = new QueueModel();
        QueueModel q3 = new QueueModel();
        QueueModel q4 = new QueueModel();

        queueManager.addItem(q1);
        queueManager.addItem(q1);
        queueManager.addItem(q1);
        queueManager.addItem(q1);
        queueManager.removeAll();
        assertEquals(0,queueManager.count());

    }
    @Test//(expected = QueueException.class)
    public void startQueue() throws QueueException {
        queueManager =  ImageProcessingQueueManager.getInstance();

            queueManager.start(null);
            assertEquals(BaseManager.State.STARTED,queueManager.getStatus());

        queueManager.removeAll();

    }
    @Test
    public void stopQueue(){
        queueManager =  ImageProcessingQueueManager.getInstance();
        queueManager.stop();
        assertEquals(BaseManager.State.STOPPED,queueManager.getStatus());
        queueManager.removeAll();
    }
    @Test(expected = QueueException.class)
    public void restartQueue() throws QueueException {
        queueManager =  ImageProcessingQueueManager.getInstance();

            queueManager.start(null);
            queueManager.start(null);

        queueManager.removeAll();
    }
    @Test
    public void getCount(){
        queueManager =  ImageProcessingQueueManager.getInstance();
        QueueModel q1 = new QueueModel();
        QueueModel q2 = new QueueModel();
        QueueModel q3 = new QueueModel();
        QueueModel q4 = new QueueModel();

        queueManager.addItem(q1);
        queueManager.addItem(q2);
        queueManager.addItem(q3);
        queueManager.addItem(q4);

        assertEquals(4,queueManager.count());
        queueManager.removeItem(new QueueModel());
        assertEquals(4,queueManager.count());
        queueManager.removeAll();

    }

    @Test//(expected = QueueException.class)
    public void setPriorityTest() throws QueueException {
        queueManager =  ImageProcessingQueueManager.getInstance();
        QueueModel item = null;
        for(int i=0;i<10;i++){
            QueueModel data1 = new QueueModel();

            queueManager.addItem(data1);
            if(i==4){
                item = data1;
            }
            if(i==5){

                queueManager.setPriority(item,2);
            }
        }
        queueManager.removeAll();
    }


}
