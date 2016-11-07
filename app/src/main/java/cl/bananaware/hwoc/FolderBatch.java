package cl.bananaware.hwoc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cl.bananaware.hwoc.ImageProcessing.PlateResult;

/**
 * Created by fergu on 29-10-2016.
 */
public class FolderBatch {

    List<File> files;
    PlateProcessSystem plateProcessSystem;

    public FolderBatch(Context context)
    {


        files = new ArrayList<File>();
        plateProcessSystem = new PlateProcessSystem(context);
    }

    public void Process(String path)
    {
        ImageViewer.SHOW_PROCESS_DEBUG = false;
        GetFilesFromFolder(path);
        DoBatch();
    }

    private void GetFilesFromFolder(String path)
    {
        files = GetFilesFromFolderRec(new File(path));
    }

    private List<File> GetFilesFromFolderRec (File parentDir)
    {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(GetFilesFromFolderRec(file));
            } else {
                //if(file.getName().endsWith(".csv")){
                    inFiles.add(file);
                //}
            }
        }

        return inFiles;
    }

    private void DoBatch()
    {
        for(int i=0; i< files.size(); ++i)
        {
            TimeProfiler.ResetCheckPoints();
            TimeProfiler.CheckPoint(0);
            PlateResult plateResult = this.ProcessImage(files.get(i));
            if (plateResult == null)
                plateResult = new PlateResult();
            Log.d("RESULT", files.get(i).getName() + ";" + plateResult.Plate + ";"
                    + plateResult.Confidence + ";" + TimeProfiler.GetTotalTime());
        }
    }

    private PlateResult ProcessImage(File f)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(), options);
        return plateProcessSystem.ProcessCapture(bitmap, true);
    }
}