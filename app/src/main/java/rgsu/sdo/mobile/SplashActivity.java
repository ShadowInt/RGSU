package rgsu.sdo.mobile;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SplashActivity extends AppCompatActivity {

    TextView textView;
    ImageView imageView;
    ProgressBar progressBar;

    RequestQueue mQueue;

    int versionCodeAndroid = BuildConfig.VERSION_CODE;
    int versionCode;

    String url_to_download;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_v2);
        imageView = (ImageView) findViewById(R.id.logotype);
        textView = (TextView) findViewById(R.id.madeWithLove);
        progressBar = (ProgressBar) findViewById(R.id.my_progressBar);

        mQueue = Volley.newRequestQueue(SplashActivity.this);

        getVersionCode();

//        textView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://vk.com/mrvenik"));
//                startActivity(intent);
//            }
//        });
    }

    private void getVersionCode() {
        String url = "https://api.npoint.io/67e82eaa19799af8d039";

        @SuppressLint({"SetTextI18n", "DefaultLocale"}) final JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("result");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject hit = jsonArray.getJSONObject(i);
                                versionCode = hit.getInt("version_code");
                                url_to_download = hit.getString("url");
                            }

                            if (versionCode != versionCodeAndroid) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(SplashActivity.this);
                                builder.setCancelable(false);
                                builder.setTitle(R.string.title_alert_error_version);
                                builder.setMessage(R.string.subtitle_alert_error_version);

                                builder.setPositiveButton(R.string.download_last_version, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url_to_download));
                                        SplashActivity.this.startActivity(intent);
                                    }
                                });

                                builder.setNegativeButton(R.string.button_close, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        finish();
                                        startActivity(intent);
                                    }
                                });

                                AlertDialog dialog = builder.create();
                                dialog.show();

                                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                                positiveButton.setTextColor(Color.parseColor("#00669B"));
                                negativeButton.setTextColor(Color.parseColor("#9E9E9E"));
                            } else {
                                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } catch (JSONException e) {
                            versionCode = 0;
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Ошибка загрузки метода getVersionCode " + error);
                Snackbar.make(findViewById(android.R.id.content).getRootView(), "Ошибка загрузки данных о последней версии", Snackbar.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        finish();
                        startActivity(intent);
                    }
                }, 2000);
            }
        });

        mQueue.add(request);
    }
}
