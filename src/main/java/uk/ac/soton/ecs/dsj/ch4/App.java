package uk.ac.soton.ecs.dsj.ch4;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import org.apache.commons.lang3.ArrayUtils;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.Image;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.pixel.statistics.HistogramModel;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.math.statistics.distribution.MultidimensionalHistogram;
import org.openimaj.video.Video;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.VideoDisplayStateListener;
import org.openimaj.video.VideoPositionListener;
import org.openimaj.video.VideoDisplay.Mode;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.xuggle.XuggleVideo;

/**
 * Test application for OpenIMAJ-Tutorial Chapter 4 exercises.
 *
 * @author David Jones (dsj1n15@ecs.soton.ac.uk)
 */
public class App {
  private static final String TEST_WINDOW_ID = "TEST_WINDOW";

  public static void main(String[] args) {
    // Generate the input URLs
    URL[] imageURLs;
    try {
      imageURLs = new URL[] {new URL("http://openimaj.org/tutorial/figs/hist1.jpg"),
          new URL("http://openimaj.org/tutorial/figs/hist2.jpg"),
          new URL("http://openimaj.org/tutorial/figs/hist3.jpg")};
    } catch (MalformedURLException e) {
      e.printStackTrace();
      return;
    }

    // Create histograms for each image (with 64 bins)
    List<MultidimensionalHistogram> histograms = new ArrayList<MultidimensionalHistogram>();
    HistogramModel model = new HistogramModel(4, 4, 4);
    for (URL u : imageURLs) {
      try {
        model.estimateModel(ImageUtilities.readMBF(u));
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
      histograms.add(model.histogram.clone());
    }

    // A score of 1 is the most different an image can be
    double score = 1;
    Integer idxImg1 = null;
    Integer idxImg2 = null;

    // Calculate the distances
    for (int i = 0; i < histograms.size(); i++) {
      for (int j = i; j < histograms.size(); j++) {
        double euclidean = histograms.get(i).compare(histograms.get(j), DoubleFVComparison.EUCLIDEAN);
        double intersection = histograms.get(i).compare(histograms.get(j), DoubleFVComparison.INTERSECTION);
        double other = histograms.get(i).compare(histograms.get(j), DoubleFVComparison.COSINE_SIM);
        System.out.println(String.format("H%d-H%d [E: %f, I: %f, O: %f]", i, j, euclidean, intersection, other));
        // Use euclidean for finding differences (ignore if identical)
        if (euclidean != 0 && euclidean < score) {
          idxImg1 = i;
          idxImg2 = j;
          score = euclidean;
        }
      }
    }

    // Display the most similar images
    if (idxImg1 == null || idxImg2 == null) {
      System.err.println("No different images found");
    } else {
      System.out.println(String.format("\nSimilar Images: %d & %d\n", idxImg1, idxImg2));
      // Downloading again here for display but doesn't matter too much
      try {
        DisplayUtilities.displayLinked("Similar Images", 2, ImageUtilities.readMBF(imageURLs[idxImg1]),
            ImageUtilities.readMBF(imageURLs[idxImg2]));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
