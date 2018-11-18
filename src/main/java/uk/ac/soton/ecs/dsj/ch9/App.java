package uk.ac.soton.ecs.dsj.ch9;

import java.net.URI;
import org.openimaj.audio.SampleChunk;
import org.openimaj.audio.analysis.FourierTransform;
import org.openimaj.audio.filters.EQFilter;
import org.openimaj.audio.filters.EQFilter.EQType;
import org.openimaj.video.xuggle.XuggleAudio;
import org.openimaj.vis.audio.AudioSpectrogram;
import org.openimaj.vis.audio.AudioWaveform;

/**
 * Test application for OpenIMAJ-Tutorial Chapter 9 exercises.
 * (Not required)
 *
 * @author David Jones (dsj1n15@ecs.soton.ac.uk)
 */
public class App {

  public static void main(String[] args) {
    XuggleAudio xa = null;
    try {
      // Use local copy of audiocheck
      URI uri = new URI(App.class.getResource("/audiocheck.net_sweep20-20klin.wav").toString());
      xa = new XuggleAudio(uri.toString());
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (xa == null) {
        System.err.println("Unable to load video");
        return;
      }
    }
    SampleChunk sc = null;

    // Waveform visusalisation
    final AudioWaveform vis = new AudioWaveform(400, 400);
    vis.showWindow("Waveform");
    while ((sc = xa.nextSampleChunk()) != null) {
      vis.setData(sc.getSampleBuffer());
    }

    // Audio spectogram (\w LP)
    xa.reset();
    final AudioSpectrogram spectogram = new AudioSpectrogram(600, 400);
    spectogram.showWindow("FFT Spectogram");
    EQFilter eq = new EQFilter(xa, EQType.LPF, 5000);
    FourierTransform fft2 = new FourierTransform(eq);
    while ((sc = fft2.nextSampleChunk()) != null) {
      float[][] fftData = fft2.getNormalisedMagnitudes(1f / Integer.MAX_VALUE);
      spectogram.setData(fftData[0]);
    }

  }

}
