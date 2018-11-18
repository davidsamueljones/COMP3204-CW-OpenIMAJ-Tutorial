package uk.ac.soton.ecs.dsj.ch8;

import java.util.List;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.FaceDetector;
import org.openimaj.image.processing.face.detection.keypoints.FKEFaceDetector;
import org.openimaj.image.processing.face.detection.keypoints.FacialKeypoint;
import org.openimaj.image.processing.face.detection.keypoints.FacialKeypoint.FacialKeypointType;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Ellipse;
import org.openimaj.video.Video;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.VideoCapture;

/**
 * Test application for OpenIMAJ-Tutorial Chapter 8 exercises.
 * (Not required)
 *
 * @author David Jones (dsj1n15@ecs.soton.ac.uk)
 */
public class App {

  public static void main(String[] args) {
    Video<MBFImage> vc = null;
    try {
      // Use the webcam feed
      vc = new VideoCapture(320, 240);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (vc == null) {
        System.err.println("Unable to load video");
        return;
      }
    }

    // Do face detection on input stream
    VideoDisplay<MBFImage> vd = VideoDisplay.createVideoDisplay(vc);
    vd.addVideoListener(new VideoDisplayListener<MBFImage>() {
      @Override
      public void beforeUpdate(MBFImage frame) {
        FaceDetector<KEDetectedFace, FImage> fd = new FKEFaceDetector();
        // Draw faces
        List<KEDetectedFace> faces = fd.detectFaces(Transforms.calculateIntensity(frame));
        // Draw points
        for (KEDetectedFace face : faces) {
          frame.drawShape(face.getBounds(), RGBColour.RED);
          for (FacialKeypoint point : face.getKeypoints()) {
            Point2dImpl pos = point.position.clone();
            pos.translate(face.getBounds().getTopLeft());
            frame.drawPoint(pos, RGBColour.GREEN, 3);
          }
        }
        // Draw speech bubbles
        for (KEDetectedFace face : faces) {
          Point2dImpl pos = face.getKeypoint(FacialKeypointType.MOUTH_LEFT).position.clone();
          pos.translate(face.getBounds().getTopLeft());
          pos.translate(-5, -5);
          frame.drawShapeFilled(new Ellipse(pos.x, pos.y, 10, 5, 0), RGBColour.WHITE);
          pos.translate(-20, -20);
          frame.drawShapeFilled(new Ellipse(pos.x, pos.y, 20, 10, 0), RGBColour.WHITE);
          pos.translate(-50, -50);
          frame.drawShapeFilled(new Ellipse(pos.x, pos.y, 70, 30, 0), RGBColour.WHITE);
          pos.translate(-20, 5);
          frame.drawText("OpenIMAJ!", (int) pos.x, (int) pos.y, HersheyFont.ASTROLOGY, 10, RGBColour.BLACK);
        }
      }

      @Override
      public void afterUpdate(VideoDisplay<MBFImage> display) {}
    });

  }

}
