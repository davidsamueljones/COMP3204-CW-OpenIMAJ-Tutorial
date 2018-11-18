package uk.ac.soton.ecs.dsj.ch14;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.experiment.dataset.sampling.GroupSampler;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.resize.ResizeProcessor;
import org.openimaj.time.Timer;
import org.openimaj.util.function.Operation;
import org.openimaj.util.parallel.Parallel;
import org.openimaj.util.parallel.partition.RangePartitioner;

/**
 * Test application for OpenIMAJ-Tutorial Chapter 14 exercises.
 *
 * @author David Jones (dsj1n15@ecs.soton.ac.uk)
 */
public class App {

  public static void main(String[] args) {
    Parallel.forIndex(0, 10, 1, new Operation<Integer>() {
      @Override
      public void perform(Integer i) {
        System.out.println(i);
      }
    });

    VFSGroupDataset<MBFImage> allImages = null;
    try {
      allImages = Caltech101.getImages(ImageUtilities.MBFIMAGE_READER);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (allImages == null) {
        return;
      }
    }
    GroupedDataset<String, ListDataset<MBFImage>, MBFImage> images =
        GroupSampler.sample(allImages, 8, false);

    System.out.println("Hardware Processors: " + Runtime.getRuntime().availableProcessors());
    final List<MBFImage> output = new ArrayList<MBFImage>();
    final ResizeProcessor resize = new ResizeProcessor(200);
    final Timer t1 = Timer.timer();
    Parallel.forEachPartitioned(new RangePartitioner<ListDataset<MBFImage>>(images.values()),
        new Operation<Iterator<ListDataset<MBFImage>>>() {

          @Override
          public void perform(Iterator<ListDataset<MBFImage>> it) {
            while (it.hasNext()) {
              ListDataset<MBFImage> clzImages = it.next();

              MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);
              for (MBFImage i : clzImages) {
                MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);
                tmp.fill(RGBColour.WHITE);

                MBFImage small = i.process(resize).normalise();
                int x = (200 - small.getWidth()) / 2;
                int y = (200 - small.getHeight()) / 2;
                tmp.drawImage(small, x, y);

                current.addInplace(tmp);
              }
              current.divideInplace((float) clzImages.size());
              System.out.println("Time: " + t1.duration() + "ms");
              synchronized (output) {
                output.add(current);
              }
            }

          }
        });



    // !!! Exercise 1:
    //
    // Pros:
    // - A group being processed will finish before another group is started making it more
    // predictable
    // - Synchronisation will occur less frequently as all processing for a single output image is
    // done on a single thread
    // - Ideal if processing only occurs within group (as per point above)

    // Cons:
    // - If one group takes longer than the rest, free threads will not be able to take
    // on work for this group. For this set, 6 images take approx 3500ms, two take approx 11000ms.
    // - Full hardware capability cannot be used if datasets have less groups than there are
    // available cores (/physical threads)
    // - Not ideal if processing has to occur between groups (due to extra syncs required)

    // Original: 19457ms
    // Parallel Inner Loop:
    // - 8852ms [Each Image]
    // - 8190ms [Range-Partitioned]
    // Parallel Outer Loop:
    // - 11605ms

    DisplayUtilities.display("Images", output);
  }

}
