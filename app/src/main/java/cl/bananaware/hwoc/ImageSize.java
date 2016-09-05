package cl.bananaware.hwoc;

/**
 * Created by fergu on 07-08-2016.
 */
public enum ImageSize {
    PEQUEÑA (1.2f, 0),
    MEDIANA (2.9f, 1),
    GRANDE (5.5f, 2);

    public final float DilationAmplifier;
    public final int Index;

    ImageSize(float dilationAmp, int index)
    {
        /*if (!ImageViewer.GOOD_SIZE)
            dilationAmp /= 10;*/
        this.DilationAmplifier = dilationAmp;
        this.Index = index;
    }
}
