package uk.ac.soton.ecs.dsj.ch13;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.experiment.dataset.split.GroupedRandomSplitter;
import org.openimaj.experiment.dataset.util.DatasetAdaptors;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.model.EigenImages;

/**
 * Test application for OpenIMAJ-Tutorial Chapter 13 exercises.
 *
 * @author David Jones (dsj1n15@ecs.soton.ac.uk)
 */
public class App {

  public static void main(String[] args) {
    VFSGroupDataset<FImage> dataset = null;
    try {
      URI facesURI = new URI(App.class.getResource("/att_faces.zip").toString());
      dataset =
          new VFSGroupDataset<FImage>("zip:" + facesURI.toString(), ImageUtilities.FIMAGE_READER);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (dataset == null) {
        return;
      }
    }

    int nTraining = 6;
    int nTesting = 5;
    GroupedRandomSplitter<String, FImage> splits =
        new GroupedRandomSplitter<>(dataset, nTraining, 0, nTesting);
    GroupedDataset<String, ListDataset<FImage>, FImage> training = splits.getTrainingDataset();
    GroupedDataset<String, ListDataset<FImage>, FImage> testing = splits.getTestDataset();
    // Project features
    List<FImage> basisImages = DatasetAdaptors.asList(training);
    int nEigenvectors = 239;
    EigenImages eigen = new EigenImages(nEigenvectors);
    eigen.train(basisImages);

    List<FImage> eigenFaces = new ArrayList<FImage>();
    for (int i = 0; i < eigen.getNumComponents(); i++) {
      eigenFaces.add(eigen.visualisePC(i));
    }
    // DisplayUtilities.display("EigenFaces", eigenFaces);

    // Extract features
    Map<String, DoubleFV[]> features = new HashMap<String, DoubleFV[]>();
    for (final String person : training.getGroups()) {
      final DoubleFV[] fvs = new DoubleFV[nTraining];

      for (int i = 0; i < nTraining; i++) {
        final FImage face = training.get(person).get(i);
        fvs[i] = eigen.extractFeature(face);
      }
      features.put(person, fvs);
    }

    double correct = 0, incorrect = 0, unknown = 0;
    Map<String, FImage> testReconstructed = new HashMap<>();
    // Test each face for each person in test set
    for (String truePerson : testing.getGroups()) {
      for (FImage face : testing.get(truePerson)) {
        // Extract test image features
        DoubleFV testFeature = eigen.extractFeature(face);
        // Reconstruct test straight after
        if (!testReconstructed.containsKey(truePerson)) {
          FImage eigenface = eigen.reconstruct(testFeature);
          testReconstructed.put(truePerson, eigenface.normalise());
        }

        String bestPerson = null;
        double distance = -1;
        double minDistance = Double.MAX_VALUE;
        // Compare trained features against test image features
        for (final String person : features.keySet()) {
          for (final DoubleFV fv : features.get(person)) {
            distance = fv.compare(testFeature, DoubleFVComparison.EUCLIDEAN);
            if (distance < minDistance) {
              minDistance = distance;
              bestPerson = person;
            }
          }
        }
        boolean equal = truePerson.equals(bestPerson);
        if (equal)
          correct++;
        else
          incorrect++;

        System.out.println((equal ? "C" : "W") + " Actual: " + truePerson + "\tGuess: " + bestPerson
            + "\tDistance: " + minDistance);
        if (minDistance > 12) {
          bestPerson = "Unknown";
          unknown++;
        }
      }
    }
    System.out.println("Accuracy: " + (correct / (correct + incorrect)));
    System.out.println("Correct: " + correct + " Incorrect: " + incorrect + " Unknown: " + unknown);
    // Reconstruct trained
    List<FImage> trainedReconconstructed = new ArrayList<>();
    for (final String person : features.keySet()) {
      for (final DoubleFV fv : features.get(person)) {
        FImage eigenface = eigen.reconstruct(fv);
        eigenface.normalise();
        trainedReconconstructed.add(eigenface);
        // Only do one face per person
        break;
      }
    }
    DisplayUtilities.display("Test Reconstructed", testReconstructed.values());
    DisplayUtilities.display("Trained Reconstructed", trainedReconconstructed);
  }

}
