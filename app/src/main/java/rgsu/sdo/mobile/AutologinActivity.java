package rgsu.sdo.mobile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class AutologinActivity extends AppCompatActivity {

    TextInputEditText sdoLoginEdit, sdoPasswoedEdit;
    Button buttonSave, buttonTurnOn, buttonTurnOff;
    CardView alertAuth;
    Animation slide_down;

    String valueLogin, valueLogin1;
    String valuePassword, valuePassword1;
    Boolean valueStateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autologin);

        sdoLoginEdit = (TextInputEditText) findViewById(R.id.sdo_login_edit);
        sdoPasswoedEdit = (TextInputEditText) findViewById(R.id.sdo_password_edit);
        buttonSave = (Button) findViewById(R.id.button_save);
        buttonTurnOff = (Button) findViewById(R.id.button_turn_off);
        buttonTurnOn = (Button) findViewById(R.id.button_turn_on);
        alertAuth = (CardView) findViewById(R.id.card_item);

        slide_down = AnimationUtils.loadAnimation(AutologinActivity.this, R.anim.slide_down);

        sdoPasswoedEdit.setTransformationMethod(new PasswordTransformationMethod());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        valueStateButton = prefs.getBoolean("StateButton", false);

        valueLogin = prefs.getString("LoginSDO", "");
        sdoLoginEdit.setText(valueLogin);

        valuePassword = prefs.getString("PasswordSDO", "");
        sdoPasswoedEdit.setText(valuePassword);

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!sdoLoginEdit.getText().toString().isEmpty() && !sdoPasswoedEdit.getText().toString().isEmpty()){
                    valueLogin1 = Objects.requireNonNull(sdoLoginEdit.getText()).toString();
                    valuePassword1 = Objects.requireNonNull(sdoPasswoedEdit.getText()).toString();

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AutologinActivity.this);

                    SharedPreferences.Editor editor1 = prefs.edit();
                    editor1.putString("LoginSDO", valueLogin1);
                    editor1.putBoolean("StateButton", true);
                    editor1.apply();

                    SharedPreferences.Editor editor2 = prefs.edit();
                    editor2.putString("PasswordSDO", valuePassword1);
                    editor2.apply();

                    Toast.makeText(getApplicationContext(),"Успешно сохранено", Toast.LENGTH_SHORT).show();
                    alertAuth.setVisibility(View.VISIBLE);
                    alertAuth.startAnimation(slide_down);
                    buttonTurnOn.setVisibility(View.GONE);
                    buttonTurnOff.setVisibility(View.VISIBLE);
                    buttonTurnOn.setEnabled(true);
                    buttonTurnOff.setEnabled(true);
                } else {
                    Toast.makeText(getApplicationContext(),"Заполните все поля!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AutologinActivity.this);

                SharedPreferences.Editor editor1 = prefs.edit();
                editor1.putBoolean("StateButton", false);
                editor1.apply();

                Toast.makeText(getApplicationContext(),"Функция автологин выключена", Toast.LENGTH_SHORT).show();
                buttonTurnOff.setVisibility(View.GONE);
                buttonTurnOn.setVisibility(View.VISIBLE);
            }
        });

        buttonTurnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AutologinActivity.this);

                SharedPreferences.Editor editor1 = prefs.edit();
                editor1.putBoolean("StateButton", true);
                editor1.apply();

                Toast.makeText(getApplicationContext(),"Функция автологин включена", Toast.LENGTH_SHORT).show();
                buttonTurnOn.setVisibility(View.GONE);
                buttonTurnOff.setVisibility(View.VISIBLE);
            }
        });

        if (valueStateButton){
            buttonTurnOff.setVisibility(View.VISIBLE);
        } else {
            buttonTurnOn.setVisibility(View.VISIBLE);
        }

        if (valueLogin.isEmpty() || valuePassword.isEmpty()){
            buttonTurnOn.setEnabled(false);
            buttonTurnOff.setEnabled(false);
        }
    }

}
