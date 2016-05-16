package com.kofax.processqueue;


import com.kofax.kmc.ken.engines.data.BasicSettingsProfile;
import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.kmc.ken.engines.data.ImagePerfectionProfile;

public class QueueModel {

    private ImagePerfectionProfile imagePerfectionProfile;
    private BasicSettingsProfile basicSettingsProfile;
    private String inputFilePath;
    private String outputFilePath;
    private Image.ImageRep processedImageRepresentation;

    /**
     * This method returns the ImagePerfectionProfile which is set by its setter method.
     * @return
     */
    public ImagePerfectionProfile getImagePerfectionProfile() {
        return imagePerfectionProfile;
    }

    /**
     * this method is to set the ImagePerfectionProfile.
     * @param imagePerfectionProfile
     */
    public void setImagePerfectionProfile(ImagePerfectionProfile imagePerfectionProfile) {
        this.imagePerfectionProfile = imagePerfectionProfile;
    }

    /**
     * This method returns the BasicSettingsProfile which is set by its setter method.
     * @return
     */
    public BasicSettingsProfile getBasicSettingsProfile() {
        return basicSettingsProfile;
    }

    /**
     * This method is to set the BasicSettingsProfile.
     * @param basicSettingsProfile
     */
    public void setBasicSettingsProfile(BasicSettingsProfile basicSettingsProfile) {
        this.basicSettingsProfile = basicSettingsProfile;
    }

    /**
     * This method returns the input image file path which is set by its setter method.
     * @return
     */
    public String getInputFilePath() {
        return inputFilePath;
    }

    /**
     * This method is to set the input image file path.
     * @param inputFilePath
     */
    public void setInputFilePath(String inputFilePath) {
        this.inputFilePath = inputFilePath;
    }

    /**
     * This method returns the output image file path which is set by its setter method.
     * @return
     */
    public String getOutputFilePath() {
        return outputFilePath;
    }

    /**
     * This method is to set the output image file path.
     * @param outputFilePath
     */
    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    /**
     * This method returns the Processed Image Representation which is set by its setter method.
     * @return
     */
    public Image.ImageRep getProcessedImageRepresentation() {
        return processedImageRepresentation;
    }

    /**
     * This method is to set the Processed Image Representation.
     * @param processedImageRepresentation
     */
    public void setProcessedImageRepresentation(Image.ImageRep processedImageRepresentation) {
        this.processedImageRepresentation = processedImageRepresentation;
    }
}
