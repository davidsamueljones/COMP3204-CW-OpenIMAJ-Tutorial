package uk.ac.soton.ecs.dsj.ch3;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import org.apache.commons.lang3.ArrayUtils;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.Image;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.connectedcomponent.GreyscaleConnectedComponentLabeler;
import org.openimaj.image.pixel.ConnectedComponent;
import org.openimaj.image.processor.PixelProcessor;
import org.openimaj.image.segmentation.FelzenszwalbHuttenlocherSegmenter;
import org.openimaj.image.segmentation.SegmentationUtilities;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.ml.clustering.FloatCentroidsResult;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.kmeans.FloatKMeans;

/**
 * Test application for OpenIMAJ-Tutorial Chapter 3 exercises.
 *
 * @author David Jones (dsj1n15@ecs.soton.ac.uk)
 */
public class App {
  private static final String TEST_WINDOW_ID = "TEST_WINDOW";

  public static void main(String[] args) {
    Map<String, Image<?, ?>> images = new LinkedHashMap<>();

    // Download the tutorial image from the provided URL (from Chapter 2)
    MBFImage image = null;
    try {
      System.out.println("Attempting to download test image...");
      image = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/sinaface.jpg"));
      System.out.println("Downloaded test image");
    } catch (IOException e) {
      System.err.println("Could not load image from URL");
    }
    // Validate inputs, exit on any failure
    if (image == null) {
      return;
    }

    // Generate images for different K means, labelling component groups
    for (int i = 2; i <= 5; i++) {
      System.out.println("Processing " + i + " class image...");
      System.out.println("Doing K-Means...");
      MBFImage workingImage = doKMeans(image, i);
      System.out.println("Labelling components...");
      inplaceLabelComponents(workingImage, 1000);
      images.put(String.valueOf(i) + "A", workingImage);
      workingImage = workingImage.clone();
      System.out.println("Segmenting image...");
      inplaceSegmentComponents(workingImage, 10000);
      images.put(String.valueOf(i) + "B", workingImage);
    }
    System.out.println("Processing finished!");

    // Create the named window to update with generated images
    JFrame window = DisplayUtilities.createNamedWindow(TEST_WINDOW_ID, "Slideshow Window", true);
    window.setVisible(true);

    // Refresh the window with the next image in the slideshow every second
    while (true) {
      for (Map.Entry<String, Image<?, ?>> entry : images.entrySet()) {
        DisplayUtilities.updateNamed(TEST_WINDOW_ID, entry.getValue(), entry.getKey());
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          // Interrupted, safely exit
          return;
        }
      }
    }

  }

  /**
   * Group together similar colours in the provided image using the K-means clustering algorithm.
   * 
   * @param image Image to process, this input will not be affected
   * @param classes Number of classes to create
   * @return The generated image
   */
  private static MBFImage doKMeans(MBFImage image, int classes) {
    // Get image in Lab colour space for human accurate Euclidean Distance calculations
    image = ColourSpace.convert(image, ColourSpace.CIE_Lab);
    // Construct the k means algorithm (use the exact form with n number of classes)
    FloatKMeans cluster = FloatKMeans.createExact(classes);
    // Prepare the image data and run the clustering algorithm on it
    float[][] imageData = image.getPixelVectorNative(new float[image.getWidth() * image.getHeight()][3]);
    FloatCentroidsResult result = cluster.cluster(imageData);
    // Print the average point of all points for each class (the centroid)
    final float[][] centroids = result.centroids;
    for (float[] fs : centroids) {
      System.out.println(Arrays.toString(fs));
    }

    // Create an assigner and classify each pixel to a centroid
    final HardAssigner<float[], ?, ?> assigner = result.defaultHardAssigner();
    image.processInplace(new PixelProcessor<Float[]>() {
      public Float[] processPixel(Float[] pixel) {
        // A primitive float array is expected by assigner so we must convert it
        float[] primPixel = ArrayUtils.toPrimitive(pixel);
        primPixel = centroids[assigner.assign(primPixel)];
        // We must now convert back to a float
        return ArrayUtils.toObject(primPixel);
      }
    });

    // !!! Non PixelProcessor Method
    // for (int y = 0; y < image.getHeight(); y++) {
    // for (int x = 0; x < image.getWidth(); x++) {
    // float[] pixel = image.getPixelNative(x, y);
    // int centroid = assigner.assign(pixel);
    // image.setPixelNative(x, y, centroids[centroid]);
    // }
    // }

    /****************************************************************************************
     * EXERCISE 1: PixelProcessor Advantages: 
     * - Works no matter how many dimensions there are in the
     * image (e.g. 2D or 3D) 
     * PixelProcessor Disadvantages: 
     * - In this case, assigner function requires primitive float arrays for inputs, 
     * this requires a conversion on input and return from the PixelProcessor input. 
     * - Anonymous class makes use of external objects, these references must be
     * final to satisfy the JDK which can make code appear too strict.
     ****************************************************************************************/

    // Convert back to RGB (from Lab)
    image = ColourSpace.convert(image, ColourSpace.RGB);
    return image;
  }

  /**
   * Find any connected components and label them if they satisfy the minimum area constraint
   * 
   * @param image Image to process
   * @param minArea Minimum area to justify labelling
   * @return Reference to the input image
   */
  private static MBFImage inplaceLabelComponents(MBFImage image, int minArea) {
    GreyscaleConnectedComponentLabeler labeler = new GreyscaleConnectedComponentLabeler();
    List<ConnectedComponent> components = labeler.findComponents(image.flatten());

    int x = 0;
    // Check all discovered components, labelling on the image if area constraint is satisfied
    for (ConnectedComponent comp : components) {
      if (comp.calculateArea() < minArea) {
        continue;
      }
      image.drawText(String.valueOf(x++), comp.calculateCentroidPixel(), HersheyFont.TIMES_MEDIUM, 20);
    }
    return image;
  }

  /**
   * Segment the image components using a minimum area constraint. Overlay the segments with a low opacity.
   * 
   * @param image Image to process
   * @param minArea Minimum areas to classify as segments
   * @return Reference to the input image
   */
  private static MBFImage inplaceSegmentComponents(MBFImage image, int minArea) {
    FelzenszwalbHuttenlocherSegmenter<MBFImage> segmenter = new FelzenszwalbHuttenlocherSegmenter<>(0.5f, 500f / 255f, minArea);
    List<ConnectedComponent> components = segmenter.segment(image);
    MBFImage renderedSegments = new MBFImage(image.getWidth(), image.getHeight());
    
    // Render transulucent layer displaying segments
    SegmentationUtilities.renderSegments(renderedSegments, components);
    renderedSegments.multiplyInplace(0.2f);
    // Add segment layer to image
    image.addInplace(renderedSegments);
    
    return image;
  }
  
}
