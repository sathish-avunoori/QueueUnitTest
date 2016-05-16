package com.kofax.processqueue;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.kofax.kmc.ken.engines.ImageProcessor;
import com.kofax.kmc.ken.engines.ImageProcessor.ImageOutListener;
import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.processqueue.exceptions.QueueErrorCodes;
import com.kofax.processqueue.exceptions.QueueException;


public class ImageProcessingQueueManager extends BaseManager {

    private ImageProcessor imageProcessor;
    private PriorityQueue<QueueModel> priorityQueue;
    private HandlerThread handlerThread = null;
    private Handler handler = null;
    private Runnable runnable = null;
    private Handler callBackHandler = null;
    private static ImageProcessingQueueManager instance = null;

    private ImageProcessingQueueManager(){
        if(priorityQueue == null){
            priorityQueue = new PriorityQueue<QueueModel>();
        }
        if(handlerThread == null){
            handlerThread = new HandlerThread(QueueConstants.PROCESS_QUEUE);
        }
    }

    public synchronized static ImageProcessingQueueManager getInstance(){
        if(instance == null){
            instance = new ImageProcessingQueueManager();
        }
        return instance;
    }

    /**
     * This method is used to add the item to queue
     * @param queueItem
     */
    public void addItem(QueueModel queueItem){
        priorityQueue.enQueue(queueItem);

    }

    /**
     * This method is used to remove from the queue
     * @param queueModel
     */
    public boolean removeItem(QueueModel queueModel){
        return this.priorityQueue.remove(queueModel);
    }

    /**
     * This method is used to remove all elements from the queue
     */
    public void removeAll(){
        this.priorityQueue.removeAll();
    }

    /**
     * This method is used to return the count of the queue
     * @return
     */
    public int count(){
        return this.priorityQueue.count();
    }

    /**
     * this method is used to set the element priority in queue. the priority is assumed to be index position in the priority queue
     * @param queueModel
     * @param position
     * @throws QueueException
     */
    public void setPriority(QueueModel queueModel,int position) throws QueueException {
        this.priorityQueue.setPriority(queueModel,position);
    }
    /**
     * this method is to start the ImageProcessingQueueManager to start processing in background
     * @param callBackHandler
     */
    public void start(Handler callBackHandler) throws QueueException {

        this.callBackHandler = callBackHandler;

        if(this.getStatus().equals(State.STARTED)){
            throw new QueueException(QueueErrorCodes.QUEUE_ALREADY_STARTED);
        }
        this.setStatus(State.STARTED);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

            }
        };

        if(runnable == null){
            runnable = new QueueConsumerThread();
        }
        handler.post(runnable);
}


    /**
     * this method is to stop the ImageProcessingQueueManager to stop the background processing
     */
    public void stop(){

        this.setStatus(State.STOPPED);
        if(handlerThread != null){
            handlerThread.quit();
        }
        if(priorityQueue != null){
            priorityQueue.removeAll();
        }

    }

    /**
     * this method is used internal to process the image based on provided data object
     * @param data
     * @throws KmcException
     * @throws QueueException
     */
    private void processImage(QueueModel data) throws KmcException, QueueException {
        if(null != data){
            imageProcessor = new ImageProcessor();
            Image mImage = null;

            if (data.getImagePerfectionProfile() != null) {
                imageProcessor.setImagePerfectionProfile(data.getImagePerfectionProfile());
            } else {
                imageProcessor.setBasicSettingsProfile(data.getBasicSettingsProfile());
            }
            imageProcessor.setProcessedImageRepresentation(Image.ImageRep.IMAGE_REP_BOTH);
            imageProcessor.setProcessedImageFilePath(data.getOutputFilePath());
            imageProcessor.setProcessedImageMimeType(Image.ImageMimeType.MIMETYPE_TIFF);


            mImage = getImageWithExtension(data.getInputFilePath());

            if(mImage.getImageMimeType().equals(Image.ImageMimeType.MIMETYPE_JPEG)){
                imageProcessor.setProcessedImageJpegQuality(QueueConstants.JPEG_QUALITY);
            }

            mImage.imageReadFromFile();

            imageProcessor.addImageOutEventListener(new ImageOutListener() {
                @SuppressLint("LongLogTag")
                @Override
                public void imageOut(ImageProcessor.ImageOutEvent imageOutEvent) {
                    imageProcessor.removeImageOutEventListener(this);
                    imageProcessor = null;
                    if(callBackHandler != null){
                        Message processMessage = new Message();
                        processMessage.obj = imageOutEvent;
                        callBackHandler.sendMessage(processMessage);
                    }

                }
            });
            imageProcessor.processImage(mImage);

        }else{
            throw new QueueException(QueueErrorCodes.QUEUE_INVALIDELEMENT);
        }
    }

    /**
     * This method is to set return the new image based on input image extension.
     * @param imagePath
     * @return
     */
    public Image getImageWithExtension(String imagePath){
        Image mImage = null;

        String[] splitPath = imagePath.split("\\.");

        switch(splitPath[1]){
            case QueueConstants.PNG_EXTENSION:
                mImage = new Image(imagePath,Image.ImageMimeType.MIMETYPE_PNG);
                break;
            case QueueConstants.TIFF_EXTENSION:
                mImage = new Image(imagePath,Image.ImageMimeType.MIMETYPE_TIFF);
                break;
            default:
                mImage = new Image(imagePath,Image.ImageMimeType.MIMETYPE_JPEG);
                break;

        }

        return mImage;
    }
    /**
     * this is a background thread which consume the item from queue and send it for process
     */
    private class QueueConsumerThread implements Runnable{

        @Override
        public void run() {
            if(priorityQueue.count()>0){
                try {
                    processImage((QueueModel) priorityQueue.deQueue());

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            handler.postDelayed(runnable,500);
        }
    }

    /**
     * this method is to cleanup the ImageProcessingQueue Manager to cleanup and release the memory used by ImagePricessingQueueManger
     */
    public void cleanUp(){
        imageProcessor = null;
        if(priorityQueue != null){
            priorityQueue.removeAll();
            priorityQueue = null;
        }

        if(handlerThread != null){
            if(handlerThread.isAlive()){
                handlerThread.quit();
            }
            handlerThread = null;
        }
        if(handler != null){
            handler.removeCallbacks(runnable);
            handler = null;
        }
        if(runnable != null){
            runnable = null;
        }
        if(callBackHandler != null){
            callBackHandler = null;
        }
    }
}
