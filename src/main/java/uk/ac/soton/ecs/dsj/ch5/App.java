package uk.ac.soton.ecs.dsj.ch5;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JFrame;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.matcher.BasicMatcher;
import org.openimaj.feature.local.matcher.BasicTwoWayMatcher;
import org.openimaj.feature.local.matcher.FastBasicKeypointMatcher;
import org.openimaj.feature.local.matcher.LocalFeatureMatcher;
import org.openimaj.feature.local.matcher.MatchingUtilities;
import org.openimaj.feature.local.matcher.VotingKeypointMatcher;
import org.openimaj.feature.local.matcher.consistent.ConsistentLocalFeatureMatcher2d;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.Image;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.math.geometry.transforms.HomographyRefinement;
import org.openimaj.math.geometry.transforms.estimation.RobustAffineTransformEstimator;
import org.openimaj.math.geometry.transforms.estimation.RobustHomographyEstimator;
import org.openimaj.math.model.fit.RANSAC;
import org.openimaj.math.model.fit.RANSAC.StoppingCondition;

/**
 * Test application for OpenIMAJ-Tutorial Chapter 5 exercises.
 *
 * @author David Jones (dsj1n15@ecs.soton.ac.uk)
 */
public class App {
  private static final String TEST_WINDOW_ID = "TEST_WINDOW";

  public static void main(String[] args) {
    Map<String, Image<?, ?>> images = new LinkedHashMap<>();

    MBFImage query = null;
    MBFImage target = null;
    try {
      query =
          ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/query.jpg"));
      target =
          ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/target.jpg"));
      MBFImage beforeMatching = MatchingUtilities.drawMatches(query, target, null, RGBColour.RED);
      images.put("Before", beforeMatching);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (query == null || target == null) {
        return;
      }
    }

    // Find features
    DoGSIFTEngine engine = new DoGSIFTEngine();
    LocalFeatureList<Keypoint> queryKeypoints = engine.findFeatures(query.flatten());
    LocalFeatureList<Keypoint> targetKeypoints = engine.findFeatures(target.flatten());

    // Basic matching
    LocalFeatureMatcher<Keypoint> matcher = new BasicMatcher<>(1000);
    matcher.setModelFeatures(queryKeypoints);
    matcher.findMatches(targetKeypoints);
    MBFImage basicMatches =
        MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.RED);
    images.put("Basic", basicMatches);

    // Basic two way matching
    matcher = new BasicTwoWayMatcher<>();
    matcher.setModelFeatures(queryKeypoints);
    matcher.findMatches(targetKeypoints);
    MBFImage basicTwoWayMatches =
        MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.RED);
    images.put("Basic2Way", basicTwoWayMatches);

    // Voting keypoint matcher
    matcher = new VotingKeypointMatcher<>(5);
    matcher.setModelFeatures(queryKeypoints);
    matcher.findMatches(targetKeypoints);
    MBFImage votingKeypointMatches =
        MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.RED);
    images.put("VotingKeypoint", votingKeypointMatches);
    
    // Consistent matching (RANSAC)
    LocalFeatureMatcher<Keypoint> internalMatcher = new FastBasicKeypointMatcher<>(8);
    StoppingCondition sc = new RANSAC.PercentageInliersStoppingCondition(0.5);
    RobustAffineTransformEstimator ratFitter = new RobustAffineTransformEstimator(5.0, 1500, sc);
    matcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(internalMatcher, ratFitter);
    matcher.setModelFeatures(queryKeypoints);
    matcher.findMatches(targetKeypoints);
    MBFImage ransacMatches =
        MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.BLUE);
    images.put("Consistent RANSAC", ransacMatches);

    // Consistent matching (Homography)
    RobustHomographyEstimator homoFitter =
        new RobustHomographyEstimator(5.0, 1500, sc, HomographyRefinement.SYMMETRIC_TRANSFER);
    matcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(internalMatcher, homoFitter);
    matcher.setModelFeatures(queryKeypoints);
    matcher.findMatches(targetKeypoints);
    MBFImage homoMatches =
        MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.GREEN);
    images.put("Consistent Homo", homoMatches);

    // Find shape
    target.drawShape(query.getBounds().transform(ratFitter.getModel().getTransform().inverse()), 3,
        RGBColour.BLUE);
    target.drawShape(query.getBounds().transform(homoFitter.getModel().getTransform().inverse()), 3,
        RGBColour.GREEN);
    DisplayUtilities.display(target);

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
