package  com.kofax.processqueue;

import com.kofax.processqueue.exceptions.QueueErrorCodes;
import com.kofax.processqueue.exceptions.QueueException;

import java.util.LinkedList;
import java.util.Queue;

public class BaseQueue<E>{

    public Queue<E> queue;

    public BaseQueue(){
        this.queue = new LinkedList<E>();
    }

    /**
     * This method is used to add the item to queue
     * @param item
     * @throws NullPointerException
     */
    public synchronized void enQueue(E item) throws NullPointerException{
        if(null == item){
            throw new NullPointerException(QueueErrorCodes.QUEUE_INVALIDELEMENT);
        }
        queue.add(item);
    }

    /**
     * This method is used to remove from the queue
     * @return
     * @throws QueueException
     */
    public synchronized E deQueue(){

        return queue.poll();

    }

    /**
     * This method is used to remove specified element from the queue
     * @param element
     * @return true/false
     */
    public boolean remove(E element){
        if(this.queue != null && this.queue.contains(element)){
            return this.queue.remove(element);
        }else{
            return false;
        }
    }

    /**
     * This method is used to remove all elements from the queue
     */
    public void removeAll(){
        if(this.queue != null && !this.queue.isEmpty()){
            this.queue.clear();
        }
    }

    /**
     * This method is used to return the elements count in the queue
     * @return
     */
    public int count(){
        if(queue != null){
            return queue.size();
        }else{
            return 0;
        }

    }

}