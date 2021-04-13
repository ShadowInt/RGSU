package rgsu.sdo.mobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;

public class DonateActivity extends AppCompatActivity {

    ProgressBar progressBar;
    TextView textView;
    Button button;

    String rewardVideoID = "ca-app-pub-3904312256009713/1406693007",
            interstitialID = "ca-app-pub-3904312256009713/6418097090",
            interstitialTEST = "ca-app-pub-3940256099942544/1033173712";

    private RewardedVideoAd mRewardedVideoAd;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.donate_activity);
        progressBar = (ProgressBar) findViewById(R.id.donate_progress);
        textView = (TextView) findViewById(R.id.text_donate_loading);
        button = (Button) findViewById(R.id.try_again_button);

        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(DonateActivity.this);
        mRewardedVideoAd.setRewardedVideoAdListener(rewardedVideoAdListener);
        mRewardedVideoAd.loadAd(rewardVideoID, new AdRequest.Builder().build());

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadRewardedVideoAd();
            }
        }, 5000);

        mInterstitialAd = new InterstitialAd(DonateActivity.this);
        mInterstitialAd.setAdUnitId(interstitialID);
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }

    private void loadRewardedVideoAd() {
        if (!mRewardedVideoAd.isLoaded()){
            mRewardedVideoAd.loadAd(rewardVideoID, new AdRequest.Builder().build());
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(DonateActivity.this, "Попробуйте ещё раз!", Toast.LENGTH_SHORT).show();
                    loadRewardedVideoAd();
                }
            });
        } else {
            mRewardedVideoAd.show();
        }
    }

    RewardedVideoAdListener rewardedVideoAdListener = new RewardedVideoAdListener() {
        @Override
        public void onRewardedVideoAdLoaded() {

        }

        @Override
        public void onRewardedVideoAdOpened() {

        }

        @Override
        public void onRewardedVideoStarted() {

        }

        @Override
        public void onRewardedVideoAdClosed() {
            finish();
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            }
            Toast.makeText(DonateActivity.this, "Спасибо! Это поможет нам стать лучше :)", Toast.LENGTH_LONG).show();
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void onRewarded(RewardItem rewardItem) {

        }

        @Override
        public void onRewardedVideoAdLeftApplication() {

        }

        @Override
        public void onRewardedVideoAdFailedToLoad(int i) {

        }

        @Override
        public void onRewardedVideoCompleted() {

        }
    };
}
