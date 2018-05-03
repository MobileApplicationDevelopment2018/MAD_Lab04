package it.polito.mad.mad2018;

import android.app.Application;
import android.content.Context;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

public class MAD2018Application extends Application {

    public static Context applicationContext;
    private static final long UPLOAD_RETRY_TIMEOUT = 15000;


    @Override
    public void onCreate() {
        super.onCreate();
        MAD2018Application.applicationContext = getApplicationContext();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        FirebaseStorage.getInstance().setMaxUploadRetryTimeMillis(UPLOAD_RETRY_TIMEOUT);
    }
}
