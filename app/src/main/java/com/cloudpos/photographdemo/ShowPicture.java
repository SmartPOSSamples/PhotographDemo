package com.cloudpos.photographdemo;

import android.app.Activity;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.io.FileNotFoundException;

/**
 * Created by gecx on 16-9-8.
 */
public class ShowPicture extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_pic);

        ImageView iv_show = (ImageView) findViewById(R.id.iv_show);

        Uri uri = this.getIntent() != null ? this.getIntent().getData() : null;
        if (uri != null) {
            Log.e(MainActivity.TAG, uri.toString());
            ContentResolver cr = this.getContentResolver();
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                if (iv_show != null) {
                    iv_show.setImageBitmap(bitmap);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

    }
}
