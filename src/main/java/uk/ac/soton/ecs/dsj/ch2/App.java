package uk.ac.soton.ecs.dsj.ch2;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JFrame;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.Image;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.shape.Ellipse;

/**
 * Test application for OpenIMAJ-Tutorial Chapter 2 exercises.
 *
 * @author David Jones (dsj1n15@ecs.soton.ac.uk)
 */
public class App {
  private static final String TEST_WINDOW_ID = "TEST_WINDOW";

  public static void main(String[] args) {
    Map<String, Image<?, ?>> images = new LinkedHashMap<>();

    // Download the tutorial image from the provided URL
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

    // Print the colour space, this will tell us the image is RGB
    System.out.println(String.format("Expected: RGB | Got: %s", image.colourSpace));
    // Track the loaded image so it can be displayed, alongside a version with just the red channel
    // data
    images.put("Normal", image);
    images.put("Red Channel", image.getBand(0));

    // Clone the image and remove the green and blue pixel data
    MBFImage imageNoGB = image.clone();
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        imageNoGB.getBand(1).pixels[y][x] = 0;
        imageNoGB.getBand(2).pixels[y][x] = 0;
      }
    }
    // ! SUGGESTED ALTERNATIVE
    // imageNoGB.getBand(1).fill(0f);
    // imageNoGB.getBand(2).fill(0f);
    images.put("Removed Green/Blue", imageNoGB);

    // Find edges, processing in-place
    MBFImage imageEdges = image.clone();
    imageEdges.processInplace(new CannyEdgeDetector());
    images.put("Detected Edges", imageEdges);
    // Draw some bordered speech bubbles
    MBFImage imageEdgesDrawings = imageEdges.clone();
    imageEdgesDrawings.drawShapeFilled(new Ellipse(700f, 450f, 20f, 10f, 0f), RGBColour.WHITE);
    imageEdgesDrawings.drawShape(new Ellipse(700f, 450f, 20f, 10f, 0f), RGBColour.RED);
    imageEdgesDrawings.drawShapeFilled(new Ellipse(650f, 425f, 25f, 12f, 0f), RGBColour.WHITE);
    imageEdgesDrawings.drawShape(new Ellipse(650f, 425f, 25f, 12f, 0f), RGBColour.RED);
    imageEdgesDrawings.drawShapeFilled(new Ellipse(600f, 380f, 30f, 15f, 0f), RGBColour.WHITE);
    imageEdgesDrawings.drawShape(new Ellipse(600f, 380f, 30f, 15f, 0f), RGBColour.RED);
    imageEdgesDrawings.drawShapeFilled(new Ellipse(500f, 300f, 100f, 70f, 0f), RGBColour.WHITE);
    imageEdgesDrawings.drawShape(new Ellipse(500f, 300f, 100f, 70f, 0f), RGBColour.RED);
    imageEdgesDrawings.drawText("OpenIMAJ is", 425, 300, HersheyFont.ASTROLOGY, 20, RGBColour.BLACK);
    imageEdgesDrawings.drawText("Awesome", 425, 330, HersheyFont.ASTROLOGY, 20, RGBColour.BLACK);
    images.put("Detected Edges + Speech Bubbles", imageEdgesDrawings);

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

}
