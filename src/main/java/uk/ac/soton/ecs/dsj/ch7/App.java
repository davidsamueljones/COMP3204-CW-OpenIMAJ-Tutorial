package uk.ac.soton.ecs.dsj.ch7;

import java.net.URI;
import javax.swing.JFrame;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.video.Video;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.xuggle.XuggleVideo;

/**
 * Test application for OpenIMAJ-Tutorial Chapter 7 exercises.
 *
 * @author David Jones (dsj1n15@ecs.soton.ac.uk)
 */
public class App {

  public static void main(String[] args) {
    Video<MBFImage> video = null;
    Video<MBFImage> webcam = null;
    try {
      // !!! Load the tutorial video
      URI vidURI = new URI(App.class.getResource("/keyboardcat.flv").toString());
      video = new XuggleVideo(vidURI.getPath());
      // !!! Instead Use the webcam feed
      webcam = new VideoCapture(320, 240);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (video == null) {
        System.err.println("Unable to load video");
        return;
      }
    }

    // Process tutorial video and display
    FGaussianConvolve fgc = new FGaussianConvolve(3);
    JFrame frame = null;
    for (MBFImage mbfImage : video) {
      frame = DisplayUtilities.displayName(mbfImage.process(fgc), "videoFrames");
    }
    frame.setVisible(false);
    frame.dispose();

    // Use webcam source in place
    VideoDisplay<MBFImage> webcamDisplay = VideoDisplay.createVideoDisplay(webcam);
    webcamDisplay.addVideoListener(new VideoDisplayListener<MBFImage>() {
      @Override
      public void beforeUpdate(MBFImage frame) {
        frame.processInplace(new CannyEdgeDetector());
      }

      @Override
      public void afterUpdate(VideoDisplay<MBFImage> display) {}
    });

  }

}
