package it.polito.mad.mad2018;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class OnboardingActivity extends AppCompatActivity {
    private static int SPLASH_TIME_OUT = 3500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        RelativeLayout linearLayout = findViewById(R.id.splash);
        ImageView imageView = findViewById(R.id.splash_image);
        TextView textView = findViewById(R.id.splash_text);
        Animation animation_image = AnimationUtils.loadAnimation(this, R.anim.from_top);
        Animation animation_text = AnimationUtils.loadAnimation(this, R.anim.from_bottom);

        imageView.setAnimation(animation_image);
        textView.setAnimation(animation_text);


        linearLayout.setOnClickListener(v -> {
            callMainActivity();
            finish();
        });

        new Handler().postDelayed(() -> {
            //callMainActivity();
            finish();
        }, SPLASH_TIME_OUT);

    }

    private void callMainActivity(){
        Intent callMainActivity = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(callMainActivity);
    }


}
