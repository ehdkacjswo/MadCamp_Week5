package com.example.q.week5;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity2 extends AppCompatActivity implements View.OnClickListener {
    private static final int INPUT_SIZE = 299;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128;
    private static final String INPUT_NAME = "Mul";
    private static final String OUTPUT_NAME = "final_result";

    private static final String MODEL_FILE = "file:///android_asset/optimized_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/output_labels.txt";

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();

    BottomNavigationView navigationView;
    private Button btn1, btn2;
    private TextView iv;
    private ImageView image;
    private static final int CAMERA_CODE = 1111;
    private static final int GALLERY_CODE = 1112;

    HashMap<String, String> name = new HashMap<String, String>() {
        {
            put("ahin", "유아인");
            put("chaewon", "문채원");
            put("dongwon", "강동원");
            put("gongyou", "공유");
            put("hyegyo", "송혜교");
            put("hyesun", "구혜선");
            put("hyoju", "한효주");
            put("jiwon", "김지원");
            put("joonggi", "송중기");
            put("onebin", "원빈");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        navigationView = (BottomNavigationView) findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        btn1 = (Button) findViewById(R.id.btn_camera);
        btn2 = (Button) findViewById(R.id.btn_gallery);
        iv = (TextView) findViewById(R.id.iv);
        image = (ImageView) findViewById(R.id.image);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);

        initTensorFlowAndLoadModel();
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    private void recognize_bitmap(Bitmap bitmap) {

        // 비트맵을 처음에 정의된 INPUT SIZE에 맞춰 스케일링 (상의 왜곡이 일어날수 있는데, 이건 나중에 따로 설명할게요)
        bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

// classifier 의 recognizeImage 부분이 실제 inference 를 호출해서 인식작업을 하는 부분입니다.
        final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
        // 결과값은 Classifier.Recognition 구조로 리턴되는데, 원래는 여기서 결과값을 배열로 추출가능하지만,
        // 여기서는 간단하게 그냥 통째로 txtResult에 뿌려줍니다.
        // imgResult에는 분석에 사용된 비트맵을 뿌려줍니다.
        //imgResult.setImageBitmap(bitmap);

        iv.setText(name.get(results.get(0).getTitle())+ "\n" + String.format("(%.1f%%) ", results.get(0).getConfidence() * 100.0f));
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_camera:
                Intent iCam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(iCam, CAMERA_CODE);
                break;
            case R.id.btn_gallery:
                Intent iGal = new Intent(Intent.ACTION_PICK);
                iGal.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                iGal.setType("image/*");
                startActivityForResult(iGal, GALLERY_CODE);
                break;
        }
    }

    // onActivityResult로 intent의 결과값을 처리하고, data.getData로 사진URI를 가져온다
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAMERA_CODE :
                    Toast.makeText(this, "사진을 선택하세요 !", Toast.LENGTH_SHORT).show();
                    //촬영한 사진 저장코드 추가
                    break;
                case GALLERY_CODE :
                    // Image URI check @@@@@@@@@@
                    String uriStr = data.getData().toString();
                    String path = data.getData().getPath();
                    String realPath = getRealPathFromUri(this, data.getData());
                    Log.d("@@@@ img URI: ", uriStr);
                    Log.d("@@@@ img URI path: ", path);
                    Log.d("@@@@ img URI realPath: ", realPath);

                    try{
                        Bitmap bm = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                        image.setImageBitmap(bm);
                        recognize_bitmap(bm);
                    }catch (IOException e) {}

                    break;
                default :
                    break;
            }
        }
    }

    public static String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_cele1:
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    overridePendingTransition(R.anim.hold, R.anim.fade_in);
                    finish();
                    return true;
                case R.id.navigation_cele2:
                    startActivity(new Intent(getApplicationContext(), MainActivity2.class));
                    overridePendingTransition(R.anim.hold, R.anim.fade_in);
                    finish();
                    return true;
                case R.id.navigation_age:
                    startActivity(new Intent(getApplicationContext(), MainActivity3.class));
                    overridePendingTransition(R.anim.hold, R.anim.fade_in);
                    finish();
                    return true;
                case R.id.navigation_same:
                    startActivity(new Intent(getApplicationContext(), MainActivity4.class));
                    overridePendingTransition(R.anim.hold, R.anim.fade_in);
                    finish();
                    return true;
            }
            return false;
        }
    };
    @Override
    protected void onStart() {
        super.onStart();
        updateNavigationBarState();
    }
    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }
    private void updateNavigationBarState(){
        int actionId = getNavigationMenuItemId();
        selectBottomNavigationBarItem(actionId);
    }
    void selectBottomNavigationBarItem(int itemId) {
        MenuItem item = navigationView.getMenu().findItem(itemId);
        item.setChecked(true);
    }
    int getNavigationMenuItemId() {
        return R.id.navigation_cele2;
    }

}
