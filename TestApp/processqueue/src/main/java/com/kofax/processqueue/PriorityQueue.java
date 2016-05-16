package com.kofax.processqueue;

import com.kofax.processqueue.exceptions.QueueErrorCodes;
import com.kofax.processqueue.exceptions.QueueException;

import java.util.LinkedList;


public class PriorityQueue<E> extends BaseQueue{

    public PriorityQueue(){
        super();
    }

    /**
     * this method is used to set the element priority in queue. the priority is assumed to be index position in the priority queue
     * @param element
     * @param position
     * @throws QueueException
     */
    public void setPriority(E element, int position) throws QueueException{
        if(this.queue == null || this.queue.isEmpty()){
            throw new QueueException(QueueErrorCodes.QUEUE_ISEMPTY);
        }

        if(position >= this.queue.size() ||  position<0) {

            throw new QueueException(QueueErrorCodes.QUEUE_INVALIDINDEX);
        }

        int elementIndex = ((LinkedList)this.queue).indexOf(element);
        if(elementIndex == -1){
            throw new QueueException(QueueErrorCodes.QUEUE_INVALIDELEMENT);
        }
        if(position != elementIndex){
            ((LinkedList)this.queue).remove(elementIndex);
            ((LinkedList)this.queue).add(position,element);
        }
    }

}
