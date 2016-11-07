package cl.bananaware.hwoc.ImageProcessing;

/**
 * Created by fergu on 26-10-2016.
 */
public class PlateResult {
    public String Plate;
    public int Confidence;
    public PlateResult() {
        Plate = "";
        Confidence = 0;
    }
    public PlateResult(String plate, int confidence) {
        SetPlate(plate, confidence);
    }

    public void SetPlate(String plate, int confidence) {
        Plate = plate;
        Confidence = confidence;
    }
}
