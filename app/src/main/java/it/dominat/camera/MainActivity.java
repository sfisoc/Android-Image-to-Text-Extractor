package it.dominat.camera;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageView imageView;
    private Button camera, gallery;
    private TextRecognizer detector;
    private static final int REQUEST_CAMERA_PERMISSION = 22;
    private static final int REQUEST_CROP_PERMISSION = 23;
    private static final int REQUEST_WRITE_PERMISSION = 21;
    //keep track of camera capture intent
    static final int CAMERA_CAPTURE = 1;
    //keep track of cropping intent
    final int PIC_CROP = 3;
    //keep track of gallery intent
    final int PICK_IMAGE_REQUEST = 2;
    //captured picture uri
    private Uri picUri;
    private TextView scanResults;
    private  boolean allowAll;
    private TextDB textContent = new TextDB();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView =  findViewById(R.id.imageView);
        camera = findViewById(R.id.camera);
        gallery = findViewById(R.id.gallery);
        scanResults =  findViewById(R.id.results);
        camera.setOnClickListener(this);
        gallery.setOnClickListener(this);

        detector = new TextRecognizer.Builder(getApplicationContext()).build();

        ActivityCompat.requestPermissions(MainActivity.this, new
                String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},REQUEST_WRITE_PERMISSION);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.camera:
                try {


                   if(this.allowAll) {
                       this.takePicture();
                   }

                } catch (ActivityNotFoundException anfe) {
                    //display an error message
                    String errorMessage = "Whoops - your device doesn't support capturing images!";
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }

                break;

            case R.id.gallery:
                if(this.allowAll)
                {
                    this.gallerySelector();
                }
                break;

            default:
                break;
        }
    }

    private void takePicture()
    {
        try{



          //String imageFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/picture.jpg";
          // File imageFile = new File(imageFilePath);
         //  this.picUri = Uri.fromFile(getTempFile(this)); // convert path to Uri

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
           // takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, picUri);
            startActivityForResult(takePictureIntent, CAMERA_CAPTURE);



        } catch (ActivityNotFoundException anfe) {
            //display an error message
            String errorMessage = "Whoops - your device doesn't support capturing images!";
            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private File getTempFile(Context context) {
        final File path = new File(Environment.getExternalStorageDirectory(),
                context.getPackageName());
        if (!path.exists()) {
            path.mkdir();
        }
        return new File(path, "picture.jpg");
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private  void gallerySelector()
    {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_PERMISSION:{

                    this.allowAll = true;


            }
            case REQUEST_CAMERA_PERMISSION: {
                this.allowAll = true;
            }

        }
    }


        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            //user is returning from capturing an image using the camera
            if (requestCode == CAMERA_CAPTURE) {
                //get the Uri for the captured image
              //  this.picUri = data.getData();
                //carry out the crop operation
                Bitmap photo = (Bitmap) data.getExtras().get("data");

                this.picUri = getImageUri(getApplicationContext(), photo);
                performCrop();
                Log.d("picUri",  this.picUri.toString());

            } else if (requestCode == PICK_IMAGE_REQUEST) {
                picUri = data.getData();
                Log.d("uriGallery", picUri.toString());
                performCrop();
            }

            //user is returning from cropping the image
            else if (requestCode == PIC_CROP) {
                //get the returned data
                Bundle extras = data.getExtras();
                //get the cropped bitmap
                Bitmap thePic = (Bitmap) extras.get("data");
                //display the returned cropped image
                imageView.setImageBitmap(thePic);
                this.extraImage(thePic);
            }

        }
    }

    private void performCrop() {
        try {
            //call the standard crop action intent (the user device may not support it)
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            //indicate image type and Uri
            cropIntent.setDataAndType(picUri, "image/*");
            //set crop properties
            cropIntent.putExtra("crop", "true");
            //indicate aspect of desired crop
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            //indicate output X and Y
            cropIntent.putExtra("outputX", 256);
            cropIntent.putExtra("outputY", 256);
            //retrieve data on return
            cropIntent.putExtra("return-data", true);
            //start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, PIC_CROP);
        } catch (ActivityNotFoundException anfe) {
            //display an error message
            String errorMessage = "Whoops - your device doesn't support the crop action!";
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private  void extraImage(Bitmap bitmap )
    {
        try
        {
            if (detector.isOperational() && bitmap != null) {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<TextBlock> textBlocks = detector.detect(frame);
                String blocks = "";
                String lines = "";
                String words = "";
                this.textContent.clearDB();
                for (int index = 0; index < textBlocks.size(); index++) {
                    //extract scanned text blocks here
                    TextBlock tBlock = textBlocks.valueAt(index);
                    blocks = blocks + tBlock.getValue() + "\n" + "\n";
                    this.textContent.addBlock(tBlock.getValue());

                    for (com.google.android.gms.vision.text.Text line : tBlock.getComponents()) {
                        //extract scanned text lines here
                        lines = lines + line.getValue() + "\n";
                        this.textContent.addLine(line.getValue());

                        for (com.google.android.gms.vision.text.Text element : line.getComponents()) {
                            //extract scanned text words here
                            words = words + element.getValue() + ", ";
                            this.textContent.addWord(element.getValue());
                        }
                    }
                }

                scanResults.setText("");

                if (textBlocks.size() == 0) {
                    scanResults.setText("Scan Failed: Found nothing to scan");
                } else {
                    scanResults.setText(scanResults.getText() + "Blocks in Image: " + "\n");
                    scanResults.setText(scanResults.getText() + blocks + "\n");
                    scanResults.setText(scanResults.getText() + "---------" + "\n");
                    scanResults.setText(scanResults.getText() + "---------" + "\n");
                    scanResults.setText(scanResults.getText() + "Lines In Image: " + "\n");
                    scanResults.setText(scanResults.getText() + lines + "\n");
                    scanResults.setText(scanResults.getText() + "---------" + "\n");
                    scanResults.setText(scanResults.getText() + "---------" + "\n");
                    scanResults.setText(scanResults.getText() + "Words In Image: " + "\n");
                    scanResults.setText(scanResults.getText() + words + "\n");
                    scanResults.setText(scanResults.getText() + "---------" + "\n");

                    Log.i("Results", scanResults.getText().toString());
                }
            } else {
                scanResults.setText("Could not set up the detector!");
            }

        } catch (ActivityNotFoundException anfe)
        {

        }
    }
}
