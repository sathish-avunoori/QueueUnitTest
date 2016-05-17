package com.kofax.processqueue.test.test;


import com.kofax.processqueue.PriorityQueue;
import com.kofax.processqueue.QueueModel;
import com.kofax.processqueue.exceptions.QueueException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
@RunWith(AndroidJUnit4.class)
public class PriorityQueueUnitTest {

    PriorityQueue<QueueModel> priorityQueue;

    @Test
    public void testBaseQueueInit(){
        priorityQueue = new PriorityQueue<QueueModel>();
        assertNotNull(priorityQueue);
        priorityQueue = null;
    }
    @Test
    public void testAddItemToqueue(){
        priorityQueue = new PriorityQueue<QueueModel>();
        QueueModel data = new QueueModel();
        priorityQueue.enQueue(data);
        assertEquals(1,priorityQueue.count());
        priorityQueue = null;
    }
    @Test//(expected = QueueException.class)
    public void setPriorityTest() throws QueueException {
        priorityQueue = new PriorityQueue<QueueModel>();
        QueueModel item = null;
        for(int i=0;i<10;i++){
            QueueModel data1 = new QueueModel();
            if(i==4){
                item = data1;
            }
            priorityQueue.enQueue(data1);

        }
        priorityQueue.setPriority(item,2);
        for(int j=0;j<10;j++){
            QueueModel q = (QueueModel) priorityQueue.deQueue();
            if(j == 2){
                assertEquals(item,q);
            }

        }
        priorityQueue = null;

    }

    @Test(expected = QueueException.class)
    public void setPriorityforUnknownItem() throws QueueException {
        priorityQueue = new PriorityQueue<QueueModel>();
        QueueModel item = null;
        for(int i=0;i<10;i++){
            QueueModel data1 = new QueueModel();
            data1.setInputFilePath(""+i);
            priorityQueue.enQueue(data1);

        }
        priorityQueue.setPriority(new QueueModel(),2);
        priorityQueue = null;

    }

    @Test(expected = QueueException.class)
    public void setUnknownPriorityforItem() throws QueueException {
        priorityQueue = new PriorityQueue<QueueModel>();
        QueueModel item = null;
        for(int i=0;i<10;i++){
            QueueModel data1 = new QueueModel();
            if(i==4){
                item = data1;
            }
            priorityQueue.enQueue(data1);

        }
        priorityQueue.setPriority(item,12);
        priorityQueue = null;

    }

    @Test(expected = QueueException.class)
    public void setNegativePriorityforItem() throws QueueException {
        priorityQueue = new PriorityQueue<QueueModel>();

        for(int i=0;i<10;i++){
            QueueModel data1 = new QueueModel();
            priorityQueue.enQueue(data1);
        }
        priorityQueue.setPriority(new QueueModel(),-1);
        priorityQueue = null;

    }

    @Test
    public void removeItemFromQueue(){
        priorityQueue = new PriorityQueue<QueueModel>();
        QueueModel item = null;
        for(int i=0;i<10;i++){
            QueueModel data1 = new QueueModel();
            if(i==4){
                item = data1;
            }
            priorityQueue.enQueue(data1);
        }
        try{
            assertFalse(priorityQueue.remove(null));
        }catch(Exception e){
            e.printStackTrace();
        }
        priorityQueue = null;
    }
    @Test
    public void removeUnknowItem(){
        priorityQueue = new PriorityQueue<QueueModel>();
        QueueModel item = null;
        for(int i=0;i<10;i++){
            QueueModel data1 = new QueueModel();
            data1.setInputFilePath(""+i);
            priorityQueue.enQueue(data1);
        }
        assertFalse(priorityQueue.remove(new QueueModel()));

        priorityQueue = null;
    }

}
