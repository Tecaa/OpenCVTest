package cl.bananaware.hwoc;

/**
 * Created by fergu on 07-08-2016.
 */
public enum ImageSize {
    PEQUEÑA (2f),
    MEDIANA (2.9f),
    GRANDE (5.5f);

    public final float DilationAmplifier;

    ImageSize(float dilationAmp)
    {
        this.DilationAmplifier = dilationAmp;
    }
}
