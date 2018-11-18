package uk.ac.soton.ecs.dsj.ch12;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openimaj.data.DataSource;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.experiment.dataset.sampling.GroupSampler;
import org.openimaj.experiment.dataset.sampling.GroupedUniformRandomisedSampler;
import org.openimaj.experiment.dataset.split.GroupedRandomSplitter;
import org.openimaj.experiment.evaluation.classification.ClassificationEvaluator;
import org.openimaj.experiment.evaluation.classification.ClassificationResult;
import org.openimaj.experiment.evaluation.classification.analysers.confusionmatrix.CMAnalyser;
import org.openimaj.experiment.evaluation.classification.analysers.confusionmatrix.CMResult;
import org.openimaj.feature.DiskCachingFeatureExtractor;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.SparseIntFV;
import org.openimaj.feature.local.data.LocalFeatureListDataSource;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101.Record;
import org.openimaj.image.feature.dense.gradient.dsift.ByteDSIFTKeypoint;
import org.openimaj.image.feature.dense.gradient.dsift.DenseSIFT;
import org.openimaj.image.feature.dense.gradient.dsift.PyramidDenseSIFT;
import org.openimaj.image.feature.local.aggregate.BagOfVisualWords;
import org.openimaj.image.feature.local.aggregate.PyramidSpatialAggregator;
import org.openimaj.io.IOUtils;
import org.openimaj.ml.annotation.linear.LiblinearAnnotator;
import org.openimaj.ml.annotation.linear.LiblinearAnnotator.Mode;
import org.openimaj.ml.clustering.ByteCentroidsResult;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.kmeans.ByteKMeans;
import org.openimaj.ml.kernel.HomogeneousKernelMap;
import org.openimaj.ml.kernel.HomogeneousKernelMap.KernelType;
import org.openimaj.ml.kernel.HomogeneousKernelMap.WindowType;
import org.openimaj.util.pair.IntFloatPair;
import de.bwaldvogel.liblinear.SolverType;

/**
 * Test application for OpenIMAJ-Tutorial Chapter 12 exercises.
 *
 * @author David Jones (dsj1n15@ecs.soton.ac.uk)
 */
public class App {

  public static void main(String[] args) {
    GroupedDataset<String, VFSListDataset<Record<FImage>>, Record<FImage>> allData = null;
    try {
      allData = Caltech101.getData(ImageUtilities.FIMAGE_READER);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (allData == null) {
        return;
      }
    }

    // Get caching path
    String cachingPath = "/Users/David/Desktop/Test";


    // Sample a subset of the data
    GroupedDataset<String, ListDataset<Record<FImage>>, Record<FImage>> data =
        GroupSampler.sample(allData, 5, false);
    // Split into training and testing set
    GroupedRandomSplitter<String, Record<FImage>> splits =
        new GroupedRandomSplitter<String, Record<FImage>>(data, 15, 0, 15);

    // Feature extractors
    int[] bins = {4, 6, 8, 10};
    DenseSIFT dsift = new DenseSIFT(3, 7);
    PyramidDenseSIFT<FImage> pdsift = new PyramidDenseSIFT<FImage>(dsift, 6f, bins);
    
    // Fetch assigner or train a new one
    HardAssigner<byte[], float[], IntFloatPair> assigner = null;
    File pathAssigner = new File(cachingPath + "/assigner");
    if (Files.exists(Paths.get(pathAssigner.toString()))) {
      try {
        assigner = IOUtils.readFromFile(pathAssigner);
        System.out.println("Using cached assigner");
      } catch (IOException e) {
        e.printStackTrace();
        System.err.println("Unable to use cached assigner");
      }
    }
    if (assigner == null) {
      assigner = trainQuantiser(
          GroupedUniformRandomisedSampler.sample(splits.getTrainingDataset(), 30), pdsift);
      try {
        IOUtils.writeToFile(assigner, pathAssigner);
        System.out.println("Cached assigner");
      } catch (IOException e) {
        e.printStackTrace();
        System.err.println("Unable to cache assigner");
      }
    }
    // Create extractor that uses disk caching
    FeatureExtractor<DoubleFV, Record<FImage>> extractor = new PHOWExtractor(pdsift, assigner);
    HomogeneousKernelMap hkm =
        new HomogeneousKernelMap(KernelType.Chi2, 1, 1, -1, WindowType.Rectangular);
    extractor = hkm.createWrappedExtractor(extractor);
    DiskCachingFeatureExtractor<DoubleFV, Record<FImage>> cachedExtractor =
        new DiskCachingFeatureExtractor<>(new File(cachingPath), extractor);
    LiblinearAnnotator<Record<FImage>, String> ann = new LiblinearAnnotator<>(cachedExtractor,
        Mode.MULTICLASS, SolverType.L2R_L2LOSS_SVC, 1.0, 0.00001);
    ann.train(splits.getTrainingDataset());
    ClassificationEvaluator<CMResult<String>, String, Record<FImage>> eval =
        new ClassificationEvaluator<>(ann, splits.getTestDataset(),
            new CMAnalyser<Record<FImage>, String>(CMAnalyser.Strategy.SINGLE));

    Map<Record<FImage>, ClassificationResult<String>> guesses = eval.evaluate();
    CMResult<String> result = eval.analyse(guesses);
    System.out.println(result.getSummaryReport());

    // ACCURACY RESULTS
    // Basic: 0.680
    // Homogeneous: 0.773
    // PyramidSpatialAggregator + Reduced Step + Extra Bins: 0.800
  }


  static HardAssigner<byte[], float[], IntFloatPair> trainQuantiser(Dataset<Record<FImage>> sample,
      PyramidDenseSIFT<FImage> pdsift) {

    // Find image features
    List<LocalFeatureList<ByteDSIFTKeypoint>> allkeys = new ArrayList<>();
    for (Record<FImage> rec : sample) {
      FImage img = rec.getImage();
      pdsift.analyseImage(img);
      allkeys.add(pdsift.getByteKeypoints(0.005f));
    }
    // Truncate the feature list
    if (allkeys.size() > 10000) {
      allkeys = allkeys.subList(0, 10000);
    }

    // Cluster using k-means
    ByteKMeans km = ByteKMeans.createKDTreeEnsemble(300);
    DataSource<byte[]> datasource = new LocalFeatureListDataSource<>(allkeys);
    ByteCentroidsResult result = km.cluster(datasource);

    return result.defaultHardAssigner();
  }

  static class PHOWExtractor implements FeatureExtractor<DoubleFV, Record<FImage>> {
    PyramidDenseSIFT<FImage> pdsift;
    HardAssigner<byte[], float[], IntFloatPair> assigner;

    public PHOWExtractor(PyramidDenseSIFT<FImage> pdsift,
        HardAssigner<byte[], float[], IntFloatPair> assigner) {
      this.pdsift = pdsift;
      this.assigner = assigner;
    }

    @Override
    public DoubleFV extractFeature(Record<FImage> object) {
      FImage image = object.getImage();
      pdsift.analyseImage(image);

      BagOfVisualWords<byte[]> bovw = new BagOfVisualWords<>(assigner);
      PyramidSpatialAggregator<byte[], SparseIntFV> spatial =
          new PyramidSpatialAggregator<>(bovw, 2, 4);

      return spatial.aggregate(pdsift.getByteKeypoints(0.015f), image.getBounds()).normaliseFV();
    }
  }
}
