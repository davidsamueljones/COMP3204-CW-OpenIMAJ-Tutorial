package uk.ac.soton.ecs.dsj.ch6;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;


/**
 * Test application for OpenIMAJ-Tutorial Chapter 6 exercises.
 *
 * @author David Jones (dsj1n15@ecs.soton.ac.uk)
 */
public class App {
  private static final String TEST_WINDOW_ID = "TEST_WINDOW";

  public static void main(String[] args) {
    VFSGroupDataset<FImage> groupedFaces = null;
    try {
      URI facesURI = new URI(App.class.getResource("/att_faces.zip").toString());
      groupedFaces =
          new VFSGroupDataset<FImage>("zip:" + facesURI.toString(), ImageUtilities.FIMAGE_READER);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (groupedFaces == null) {
        return;
      }
    }

    // --- Exercise 1
    int count = 0;
    List<FImage> randomFaces = new ArrayList<>();
    for (final Entry<String, VFSListDataset<FImage>> entry : groupedFaces.entrySet()) {
      randomFaces.add(entry.getValue().getRandomInstance());
      count += entry.getValue().size();
    }
    System.out.println("Faces: " + count);
    DisplayUtilities.display("Faces", randomFaces);

    // --- Exercise 2
    // Other Sources: https://commons.apache.org/proper/commons-vfs/filesystems.html

    // --- Exercise 3
    // -- UNABLE TO GET API KEY
    // BingAPIToken bingToken = DefaultTokenFactory.get(BingAPIToken.class);
    // BingImageDataset<FImage> cats = null;
    // try {
    // cats = BingImageDataset.create(ImageUtilities.FIMAGE_READER, bingToken, "cat", 10);
    // DisplayUtilities.display("Cats", cats);
    // } catch (Exception e) {
    // e.printStackTrace();
    // } finally {
    // if (people == null) {
    // return;
    // }
    // }

    // --- Exercise 4
    // -- Unable to use Bing so used grouped faces
    MapBackedDataset<String, VFSListDataset<FImage>, FImage> mbd = new MapBackedDataset<>();
    for (Entry<String, VFSListDataset<FImage>> entry : groupedFaces.entrySet()) {
      mbd.add(entry.getKey(), entry.getValue());
    }
    System.out.println("Groups: " + mbd.size());
  }

}
