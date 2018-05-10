package it.polito.mad.mad2018.chat;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;

import it.polito.mad.mad2018.data.UserProfile;

public class ChatIDService extends FirebaseInstanceIdService {

    private static final String TAG = "ChatIDService";

    @Override
    public void onTokenRefresh() {

        super.onTokenRefresh();
        Log.d(TAG, "onTokenRefresh");
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        if (refreshedToken != null && UserProfile.localInstance != null) {
            uploadToken(UserProfile.localInstance, refreshedToken);
            Log.d(TAG, "Refreshed token: " + refreshedToken);
        }
    }

    public static void uploadToken(UserProfile profile, String token) {
        FirebaseDatabase.getInstance().getReference().child("tokens")
                .child(profile.getUserId())
                .child(token).setValue(true);
    }
}