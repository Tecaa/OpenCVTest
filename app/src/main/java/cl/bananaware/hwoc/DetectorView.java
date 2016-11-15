package cl.bananaware.hwoc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;

import java.io.IOException;

import cl.bananaware.hwoc.ImageProcessing.PlateResult;

/**
 * Created by agall on 15-11-2016.
 */

public class DetectorView extends Activity implements TextureView.SurfaceTextureListener {

    private TextureView viewCont;
    private TextureView viewDisc;
    private Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detector_view);

        viewCont = (TextureView) findViewById(R.id.viewCont);
        viewDisc = (TextureView) findViewById(R.id.viewDisc);

        viewCont.setSurfaceTextureListener(this);
        //viewDisc.setSurfaceTextureListener(this);

    }
    Camera.PictureCallback picture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            Bitmap img = BitmapFactory.decodeByteArray(data, 0, data.length);
            PlateResult result =  MainActivity.plateProcessSystem.ProcessCapture(img);
            Log.d("RESULT", "Patente: " + result.Plate);
            camera.startPreview();

            camera.autoFocus(focus);

        }
    };

    Camera.AutoFocusCallback focus = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            if(success){
                camera.autoFocus(null);
                camera.takePicture(null, null, picture);
             }
        }
    };

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {


        camera = Camera.open();
        Camera.Parameters params = camera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(params);
        camera.setDisplayOrientation(90);
        try {
            camera.setPreviewTexture(surfaceTexture);
        }
        catch (IOException t) {
        }

        camera.startPreview();

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        camera.stopPreview();
        camera.release();
        return true;
    }

    Boolean p = false;
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        if(!p) {
            camera.autoFocus(focus);
            p = true;
        }

    }
}
