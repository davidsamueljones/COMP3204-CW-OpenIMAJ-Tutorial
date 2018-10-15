package uk.ac.soton.ecs.dsj.ch7;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
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
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.video.Video;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.VideoDisplayStateListener;
import org.openimaj.video.VideoPositionListener;
import org.openimaj.video.VideoDisplay.Mode;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.xuggle.XuggleVideo;

/**
 * Test application for OpenIMAJ-Tutorial Chapter 7 exercises.
 *
 * @author David Jones (dsj1n15@ecs.soton.ac.uk)
 */
public class App {
  private static final String TEST_WINDOW_ID = "TEST_WINDOW";

  @SuppressWarnings("resource")
  public static void main(String[] args) {
    Video<MBFImage> video = null;

    // !!! Load the tutorial video
    try {
      // !!! Load the tutorial video
      URI vidURI = new URI(App.class.getResource("/keyboardcat.flv").toString());
      video = new XuggleVideo(vidURI.getPath());
      // !!! Use the webcam feed
      video = new VideoCapture(320, 240);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      // Validate inputs
      if (video == null) {
        System.err.println("Unable to load video");
        return;
      }
    }

    VideoDisplay<MBFImage> display = VideoDisplay.createVideoDisplay(video);
    display.addVideoListener(
      new VideoDisplayListener<MBFImage>() {
        public void beforeUpdate(MBFImage frame) {
            frame.processInplace(new CannyEdgeDetector());
        }

        public void afterUpdate(VideoDisplay<MBFImage> display) {
        }
      });
  }

}
