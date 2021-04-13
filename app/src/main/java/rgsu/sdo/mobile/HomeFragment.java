package rgsu.sdo.mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;
import static android.content.Context.DOWNLOAD_SERVICE;

public class HomeFragment extends Fragment implements OnBackPressedListener {

    private static String webview_url = "https://sdo.rgsu.net";    // web address or local file location you want to open in webview
    private static String file_type = "image/*";    // file types to be allowed for upload
    private boolean multiple_files = true;         // allowing multiple file upload

    private String[] listItems, listThemesUrl;
    private String chooseTheme, sdoLogin, sdoPassword;
    private Boolean isOpen = false, stateButton;

    private InterstitialAd mInterstitialAd;

    private DownloadManager mgr=null;
    private long lastDownload=-1L;

    private WebView webView;

    private String cam_file_data = null;        // for storing camera file information
    private ValueCallback<Uri> file_data;       // data/header received after file selection
    private ValueCallback<Uri[]> file_path;     // received file(s) temp. location
    private final static int file_req_code = 1;

    private SwipeRefreshLayout swipeRefreshLayout;
    private Animation fade_in, fade_out,fab_clock, fab_anticlock, fab_open, fab_close;
    private FloatingActionButton fab_main, fab1_theme, fab2_donate, fab3_autologin;
    private TextView textview_mail, textview_donate, textview_autologin;

    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private long mStartTimeInMillis;
    private long mTimeLeftInMillis;
    private long mEndTime;
    private String timeLeftFormatted;

    private long downloadID;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.home_fragment, container, false);

        webView = (WebView) view.findViewById(R.id.webView);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_to_refresh);
        final RelativeLayout rootView = (RelativeLayout) view.findViewById(R.id.addresses_confirm_root_view);

        fab_main = (FloatingActionButton) view.findViewById(R.id.fab);
        fab1_theme = (FloatingActionButton) view.findViewById(R.id.fab1);
        fab2_donate = (FloatingActionButton) view.findViewById(R.id.fab2);
        fab3_autologin = (FloatingActionButton) view.findViewById(R.id.fab3);

        fade_in = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        fade_out = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
        fab_close = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close);
        fab_open = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fab_clock = AnimationUtils.loadAnimation(getContext(), R.anim.fab_rotate_clock);
        fab_anticlock = AnimationUtils.loadAnimation(getContext(), R.anim.fab_rotate_anticlock);

        textview_mail = (TextView) view.findViewById(R.id.textview_mail);
        textview_donate = (TextView) view.findViewById(R.id.textview_donate);
        textview_autologin = (TextView) view.findViewById(R.id.textview_autologin);

        listItems = getResources().getStringArray(R.array.shopping_item);
        listThemesUrl = getResources().getStringArray(R.array.themes_url);

//            mgr=(DownloadManager) Objects.requireNonNull(getActivity()).getSystemService(DOWNLOAD_SERVICE);
//            getActivity().registerReceiver(onComplete,
//                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
//            getActivity().registerReceiver(onNotificationClick,
//                    new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(true);
                        updateWatchInterface();
                        webView.reload();
                    }
                }, 1000);
            }
        });

        final SharedPreferences prefs1 = PreferenceManager.getDefaultSharedPreferences(getContext());

        chooseTheme = prefs1.getString("chooseTheme", "main");
        stateButton = prefs1.getBoolean("StateButton", false);

        fab_main.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isOpen) {
                        textview_mail.setVisibility(View.GONE);
                        textview_donate.setVisibility(View.GONE);
                        textview_autologin.setVisibility(View.GONE);

                        fab3_autologin.startAnimation(fab_close);
                        fab2_donate.startAnimation(fab_close);
                        fab1_theme.startAnimation(fab_close);
                        fab_main.startAnimation(fab_anticlock);

                        fab3_autologin.setClickable(false);
                        fab2_donate.setClickable(false);
                        fab1_theme.setClickable(false);

                        fab1_theme.hide();
                        fab2_donate.hide();
                        fab3_autologin.hide();
                        isOpen = false;
                    } else {
                        textview_mail.setVisibility(View.VISIBLE);
                        textview_donate.setVisibility(View.VISIBLE);
                        textview_autologin.setVisibility(View.VISIBLE);

                        fab3_autologin.startAnimation(fab_open);
                        fab2_donate.startAnimation(fab_open);
                        fab1_theme.startAnimation(fab_open);
                        fab_main.startAnimation(fab_clock);

                        fab3_autologin.setClickable(true);
                        fab2_donate.setClickable(true);
                        fab1_theme.setClickable(true);

                        fab1_theme.show();
                        fab2_donate.show();
                        fab3_autologin.show();
                        isOpen = true;
                    }

                }
        });

        getThemeInfo();

        fab1_theme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(Objects.requireNonNull(getContext()));
                mBuilder.setTitle("Темы");
                mBuilder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        SharedPreferences.Editor editor = prefs1.edit();
                        editor.putString("chooseTheme", listThemesUrl[i]);
                        editor.apply();

                        SharedPreferences prefs_1 = PreferenceManager.getDefaultSharedPreferences(getContext());
                        chooseTheme = prefs_1.getString("chooseTheme", "main");

                        getThemeInfo();

                        dialogInterface.dismiss();
                        Toast.makeText(getContext(), "Тема успешно изменена", Toast.LENGTH_LONG).show();
                    }
                });

                AlertDialog mDialog = mBuilder.create();
                mDialog.show();
            }
        });

        fab2_donate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTimeLeftInMillis <= 2000 || mTimeLeftInMillis == 900000) {
                        resetTimer();
                        startTimer();

                        Intent intent = new Intent(getActivity(), DonateActivity.class);
                        startActivity(intent);
                    } else {
                    Toast.makeText(v.getContext(),"Потворите через " + timeLeftFormatted, Toast.LENGTH_SHORT).show();
                }
                }
        });

        fab3_autologin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AutologinActivity.class);
                startActivity(intent);
            }
        });

        updateWatchInterface();

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDomStorageEnabled(true);

        //ca-app-pub-3940256099942544/1033173712
        mInterstitialAd = new InterstitialAd(Objects.requireNonNull(getContext()));
        mInterstitialAd.setAdUnitId("ca-app-pub-3904312256009713/4764949123"); //ca-app-pub-3904312256009713/4764949123
        mInterstitialAd.loadAd(new AdRequest.Builder().build());


        webView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY > oldScrollY){
                    fab_main.setClickable(false);
                    if (isOpen){
                        fab3_autologin.setClickable(false);
                        fab2_donate.setClickable(false);
                        fab1_theme.setClickable(false);
                    }
                    fab_main.hide();
                    fab1_theme.hide();
                    fab2_donate.hide();
                    fab3_autologin.hide();
                    textview_mail.setVisibility(View.GONE);
                    textview_donate.setVisibility(View.GONE);
                    textview_autologin.setVisibility(View.GONE);
                } else if (scrollY < oldScrollY){
                    fab_main.setClickable(true);
                    fab_main.show();
                    if (isOpen){
                        fab3_autologin.setClickable(true);
                        fab2_donate.setClickable(true);
                        fab1_theme.setClickable(true);
                        textview_mail.setVisibility(View.VISIBLE);
                        textview_donate.setVisibility(View.VISIBLE);
                        textview_autologin.setVisibility(View.VISIBLE);
                        fab1_theme.show();
                        fab2_donate.show();
                        fab3_autologin.show();
                    }
                }
            }
        });


        webView.setWebViewClient(new Callback());
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {

                if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getContext(), "Для скачивания, нам нужно Ваше разрешение!" , Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                } else {
                    final String fileName = contentDisposition.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
                    if (mimeType.equals("application/zip")){
                        if (fileExtension(fileName).equals("docx")){
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                            request.setMimeType(mimeType);
                            //------------------------COOKIE!!------------------------
                            String cookies = CookieManager.getInstance().getCookie(url);
                            request.addRequestHeader("cookie", cookies);
                            //------------------------COOKIE!!------------------------
                            request.addRequestHeader("User-Agent", userAgent);
                            request.setDescription("Загрузка файла...");
                            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                            request.allowScanningByMediaScanner();
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                            DownloadManager dm = (DownloadManager) Objects.requireNonNull(getActivity()).getSystemService(DOWNLOAD_SERVICE);
                            dm.enqueue(request);

                            Toast.makeText(getContext(), "Загрузка файла " + fileName, Toast.LENGTH_SHORT).show();
                        } else if(fileExtension(fileName).equals("doc")){
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                            mimeType = "application/msword";
                            request.setMimeType(mimeType);
                            //------------------------COOKIE!!------------------------
                            String cookies = CookieManager.getInstance().getCookie(url);
                            request.addRequestHeader("cookie", cookies);
                            //------------------------COOKIE!!------------------------
                            request.addRequestHeader("User-Agent", userAgent);
                            request.setDescription("Загрузка файла...");
                            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                            request.allowScanningByMediaScanner();
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                            DownloadManager dm = (DownloadManager) Objects.requireNonNull(getActivity()).getSystemService(DOWNLOAD_SERVICE);
                            dm.enqueue(request);

                            Toast.makeText(getContext(), "Загрузка файла " + fileName, Toast.LENGTH_SHORT).show();
                        } else if(fileExtension(fileName).equals("pptx")){
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                            mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                            request.setMimeType(mimeType);
                            //------------------------COOKIE!!------------------------
                            String cookies = CookieManager.getInstance().getCookie(url);
                            request.addRequestHeader("cookie", cookies);
                            //------------------------COOKIE!!------------------------
                            request.addRequestHeader("User-Agent", userAgent);
                            request.setDescription("Загрузка файла...");
                            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                            request.allowScanningByMediaScanner();
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                            DownloadManager dm = (DownloadManager) Objects.requireNonNull(getActivity()).getSystemService(DOWNLOAD_SERVICE);
                            dm.enqueue(request);

                            Toast.makeText(getContext(), "Загрузка файла " + fileName, Toast.LENGTH_SHORT).show();
                        } else {
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                            request.setMimeType(mimeType);
                            //------------------------COOKIE!!------------------------
                            String cookies = CookieManager.getInstance().getCookie(url);
                            request.addRequestHeader("cookie", cookies);
                            //------------------------COOKIE!!------------------------
                            request.addRequestHeader("User-Agent", userAgent);
                            request.setDescription("Загрузка файла...");
                            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                            request.allowScanningByMediaScanner();
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                            DownloadManager dm = (DownloadManager) Objects.requireNonNull(getActivity()).getSystemService(DOWNLOAD_SERVICE);
                            dm.enqueue(request);

                            Toast.makeText(getContext(), "Загрузка файла " + fileName, Toast.LENGTH_SHORT).show();
                        }
                    }  else if(mimeType.equals("application/octet-stream")){
                        if (fileExtension(fileName).equals("xls")){
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                            mimeType = "application/vnd.ms-excel";
                            request.setMimeType(mimeType);
                            //------------------------COOKIE!!------------------------
                            String cookies = CookieManager.getInstance().getCookie(url);
                            request.addRequestHeader("cookie", cookies);
                            //------------------------COOKIE!!------------------------
                            request.addRequestHeader("User-Agent", userAgent);
                            request.setDescription("Загрузка файла...");
                            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                            request.allowScanningByMediaScanner();
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                            DownloadManager dm = (DownloadManager) Objects.requireNonNull(getActivity()).getSystemService(DOWNLOAD_SERVICE);
                            dm.enqueue(request);

                            Toast.makeText(getContext(), "Загрузка файла " + fileName, Toast.LENGTH_SHORT).show();
                        } else {
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                            request.setMimeType(mimeType);
                            //------------------------COOKIE!!------------------------
                            String cookies = CookieManager.getInstance().getCookie(url);
                            request.addRequestHeader("cookie", cookies);
                            //------------------------COOKIE!!------------------------
                            request.addRequestHeader("User-Agent", userAgent);
                            request.setDescription("Загрузка файла...");
                            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                            request.allowScanningByMediaScanner();
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                            DownloadManager dm = (DownloadManager) Objects.requireNonNull(getActivity()).getSystemService(DOWNLOAD_SERVICE);
                            dm.enqueue(request);

                            Toast.makeText(getContext(), "Загрузка файла " + fileName, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        request.setMimeType(mimeType);
                        //------------------------COOKIE!!------------------------
                        String cookies = CookieManager.getInstance().getCookie(url);
                        request.addRequestHeader("cookie", cookies);
                        //------------------------COOKIE!!------------------------
                        request.addRequestHeader("User-Agent", userAgent);
                        request.setDescription("Загрузка файла...");
                        request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                        DownloadManager dm = (DownloadManager) Objects.requireNonNull(getActivity()).getSystemService(DOWNLOAD_SERVICE);
                        dm.enqueue(request);

                        Toast.makeText(getContext(), "Загрузка файла " + fileName, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            public void onProgressChanged(WebView view, int progress) {
                if(progress < 100 && !swipeRefreshLayout.isRefreshing()){
                    swipeRefreshLayout.setRefreshing(true);
                } else if(progress == 100) {
//                    injectCSS();
//                    webView.loadUrl("javascript:(function() {" +
//                                    "document.querySelector('.login').click();" +
//                            "})()");
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            webView.loadUrl("javascript:(function() {" +
//                                    "document.getElementById('login').value = 'VenisiatckSN0201611';" +
//                                    "document.getElementById('password').value = 'xI5iohoK';" +
//                                    "document.forms[0].appendChild(document.createElement('button')).click();" +
//                                    "})()");
//                        }
//                    }, 1000);

//                    rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//                        @Override
//                        public void onGlobalLayout() {
//                            int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
//
//                            if (heightDiff > 100) {
//                                Log.e("MyActivity", "keyboard opened");
//                                fab.setVisibility(View.GONE);
//                            } else {
//                                Log.e("MyActivity", "keyboard closed");
//                                new Handler().postDelayed(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        fab.setVisibility(View.VISIBLE);
//                                    }
//                                }, 50);
//                            }
//                        }
//                    });

                    if (mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                    }

                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            /*--
            openFileChooser is not a public Android API and has never been part of the SDK.
            handling input[type="file"] requests for android API 16+; I've removed support below API 21 as it was failing to work along with latest APIs.
            --*/
        /*    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                file_data = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType(file_type);
                if (multiple_files) {
                    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                startActivityForResult(Intent.createChooser(i, "File Chooser"), file_req_code);
            }
        */
            /*-- handling input[type="file"] requests for android API 21+ --*/

            @RequiresApi(api = Build.VERSION_CODES.N)
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

                if (file_permission()) {
                    file_path = filePathCallback;
                    Intent takePictureIntent = null;
                    Intent takeVideoIntent = null;

                    boolean includeVideo = false;
                    boolean includePhoto = false;

                    /*-- checking the accept parameter to determine which intent(s) to include --*/
                    paramCheck:
                    for (String acceptTypes : fileChooserParams.getAcceptTypes()) {
                        String[] splitTypes = acceptTypes.split(", ?+"); // although it's an array, it still seems to be the whole value; split it out into chunks so that we can detect multiple values
                        for (String acceptType : splitTypes) {
                            switch (acceptType) {
                                case "*/*":
                                    includePhoto = true;
                                    includeVideo = true;
                                    break paramCheck;
                                case "image/*":
                                    includePhoto = true;
                                    break;
                                case "video/*":
                                    includeVideo = true;
                                    break;
                            }
                        }
                    }

                    if (fileChooserParams.getAcceptTypes().length == 0) {   //no `accept` parameter was specified, allow both photo and video
                        includePhoto = true;
                        includeVideo = true;
                    }

                    if (includePhoto) {
                        takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(Objects.requireNonNull(getActivity()).getPackageManager()) != null) {
                            File photoFile = null;
                            try {
                                photoFile = create_image();
                                takePictureIntent.putExtra("PhotoPath", cam_file_data);
                            } catch (IOException ex) {
                                Log.e(TAG, "Image file creation failed", ex);
                            }
                            if (photoFile != null) {
                                cam_file_data = "file:" + photoFile.getAbsolutePath();
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                            } else {
                                cam_file_data = null;
                                takePictureIntent = null;
                            }
                        }
                    }

                    if (includeVideo) {
                        takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                        if (takeVideoIntent.resolveActivity(Objects.requireNonNull(getActivity()).getPackageManager()) != null) {
                            File videoFile = null;
                            try {
                                videoFile = create_video();
                            } catch (IOException ex) {
                                Log.e(TAG, "Video file creation failed", ex);
                            }
                            if (videoFile != null) {
                                cam_file_data = "file:" + videoFile.getAbsolutePath();
                                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(videoFile));
                            } else {
                                cam_file_data = null;
                                takeVideoIntent = null;
                            }
                        }
                    }

                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.setType(file_type);
                    if (multiple_files) {
                        contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }

                    Intent[] intentArray;
                    if (takePictureIntent != null && takeVideoIntent != null) {
                        intentArray = new Intent[]{takePictureIntent, takeVideoIntent};
                    } else if (takePictureIntent != null) {
                        intentArray = new Intent[]{takePictureIntent};
                    } else if (takeVideoIntent != null) {
                        intentArray = new Intent[]{takeVideoIntent};
                    } else {
                        intentArray = new Intent[0];
                    }

                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, "File chooser");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                    startActivityForResult(chooserIntent, file_req_code);
                    return true;
                } else {
                    return false;
                }
            }
        });

        webView.loadUrl(webview_url);

        if(savedInstanceState==null){
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.loadUrl(webview_url);
                }
            });
        }

        return view;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        Uri[] results = null;

        /*-- if file request cancelled; exited camera. we need to send null value to make future attempts workable --*/
        if (resultCode == Activity.RESULT_CANCELED) {
            if (requestCode == file_req_code) {
                file_path.onReceiveValue(null);
                return;
            }
        }

        /*-- continue if response is positive --*/
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == file_req_code) {
                if (null == file_path) {
                    return;
                }

                ClipData clipData;
                String stringData;
                try {
                    clipData = intent.getClipData();
                    stringData = intent.getDataString();
                } catch (Exception e) {
                    clipData = null;
                    stringData = null;
                }

                if (clipData == null && stringData == null && cam_file_data != null) {
                    results = new Uri[]{Uri.parse(cam_file_data)};
                } else {
                    if (clipData != null) { // checking if multiple files selected or not
                        final int numSelectedFiles = clipData.getItemCount();
                        results = new Uri[numSelectedFiles];
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            results[i] = clipData.getItemAt(i).getUri();
                        }
                    } else {
                        results = new Uri[]{Uri.parse(stringData)};
                    }
                }
            }
        }
        file_path.onReceiveValue(results);
        file_path = null;
    }
    public class Callback extends WebViewClient {

//        @Override
//        public boolean shouldOverrideUrlLoading(WebView view, String urlNewString) {
//
//            return false;
//        }

        @Override
        public void onPageFinished(WebView view, String url) {

//            Toast.makeText(getContext(), url, Toast.LENGTH_LONG).show();

            if (!url.equals("https://portfolio.rgsu.net/")){
                injectCSS();
            }

            getValueStateAutoLogin();

            if (stateButton){
                if (url.equals("https://sdo.rgsu.net/")){
                    webView.loadUrl("javascript:(function() {" +
                            "document.querySelector('.login').click();" +
                            "})()");
                    getValueForAuth();
                    if(!sdoLogin.equals("") && !sdoPassword.equals("")){
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadUrl("javascript:(function() {" +
                                        "document.getElementById('login').value = '" + sdoLogin + "';" +
                                        "document.getElementById('password').value = '" + sdoPassword + "';" +
                                        "document.forms[0].appendChild(document.createElement('button')).click();" +
                                        "})()");
                            }
                        }, 2000);
                    } else {
                        Toast.makeText(getContext(), "Функция автологин не доступна!", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(getContext(), "Функция автологин не включена!", Toast.LENGTH_LONG).show();
            }

            swipeRefreshLayout.setRefreshing(false);

            super.onPageFinished(view, url);
        }

    }

    /*-- checking and asking for required file permissions --*/
    private boolean file_permission() {
        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
            return false;
        } else {
            return true;
        }
    }

    /*-- creating new image file here --*/
    @RequiresApi(api = Build.VERSION_CODES.N)
    private File create_image() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    /*-- creating new video file here --*/
    @RequiresApi(api = Build.VERSION_CODES.N)
    private File create_video() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String file_name = new SimpleDateFormat("yyyy_mm_ss").format(new Date());
        String new_name = "file_" + file_name + "_";
        File sd_directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(new_name, ".3gp", sd_directory);
    }

    // Inject CSS method: read style.css from assets folder
// Append stylesheet to document head
    private void injectCSS() {
        try {
            InputStream inputStream = Objects.requireNonNull(getActivity()).getAssets().open(chooseTheme + ".css");
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            webView.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style);" +
                    "})()");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//
//        Objects.requireNonNull(getActivity()).unregisterReceiver(onComplete);
//        getActivity().unregisterReceiver(onNotificationClick);
//    }

//    protected void openFile(String fileName) {
//        Intent install = new Intent(Intent.ACTION_VIEW);
//        install.setDataAndType(Uri.fromFile(new File(fileName)),
//                "MIME-TYPE");
//        startActivity(install);
//    }

//    private BroadcastReceiver onComplete = new BroadcastReceiver() {
//        public void onReceive(Context ctxt, Intent intent) {
//            swipeRefreshLayout.setRefreshing(false);
//        }
//    };
//
//    private BroadcastReceiver onNotificationClick = new BroadcastReceiver() {
//        public void onReceive(Context ctxt, Intent intent) {
//            Toast.makeText(ctxt, "Ummmm...hi!", Toast.LENGTH_LONG).show();
//        }
//    };

    @Override
    public void onBackPressed(){
        if(webView.canGoBack()){
            webView.goBack();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getContext()));
            builder.setCancelable(false);
            builder.setTitle(R.string.title_alert_exit);
            builder.setMessage(R.string.subtitle_alert_exit);
            builder.setPositiveButton(R.string.pos_button_alert_exit, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Objects.requireNonNull(getActivity()).finish();
                }
            });

            builder.setNegativeButton(R.string.button_close, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            positiveButton.setTextColor(Color.parseColor("#FF5252"));
            negativeButton.setTextColor(Color.parseColor("#9E9E9E"));
        }
    }

    private void resetTimer() {
        mTimeLeftInMillis = mStartTimeInMillis;
        updateCountDownText();
        updateWatchInterface();
    }

    private void startTimer() {
        mEndTime = System.currentTimeMillis() + mTimeLeftInMillis;

        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                updateWatchInterface();
            }
        }.start();

        mTimerRunning = true;
    }

    @SuppressLint("SetTextI18n")
    private void updateCountDownText() {
        int hours = (int) (mTimeLeftInMillis / 1000) / 3600;
        int minutes = (int) ((mTimeLeftInMillis / 1000) % 3600) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;

        if (hours > 0) {
            timeLeftFormatted = String.format(Locale.getDefault(),
                    "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeLeftFormatted = String.format(Locale.getDefault(),
                    "%02d:%02d", minutes, seconds);
        }

        textview_donate.setText("" + timeLeftFormatted);
    }

    @SuppressLint("SetTextI18n")
    private void updateWatchInterface() {
        if (mTimeLeftInMillis <= 2000 || mTimeLeftInMillis == 900000) {
            textview_donate.setText(R.string.donate_fab);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        SharedPreferences prefs = Objects.requireNonNull(this.getActivity()).getSharedPreferences("donate_time", 0);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putLong("startTimeInMillis", mStartTimeInMillis);
        editor.putLong("millisLeft", mTimeLeftInMillis);
        editor.putBoolean("timerRunning", mTimerRunning);
        editor.putLong("endTime", mEndTime);

        editor.apply();

        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        SharedPreferences prefs = Objects.requireNonNull(this.getActivity()).getSharedPreferences("donate_time",0);

        mStartTimeInMillis = prefs.getLong("startTimeInMillis", 900000);
        mTimeLeftInMillis = prefs.getLong("millisLeft", mStartTimeInMillis);
        mTimerRunning = prefs.getBoolean("timerRunning", false);

        updateCountDownText();
        updateWatchInterface();

        if (mTimerRunning) {
            mEndTime = prefs.getLong("endTime", 0);
            mTimeLeftInMillis = mEndTime - System.currentTimeMillis();

            if (mTimeLeftInMillis < 0) {
                mTimeLeftInMillis = 0;
                mTimerRunning = false;
                updateCountDownText();
            } else {
                startTimer();
            }
        }
    }

    private void getThemeInfo(){
        webView.reload();

        if (chooseTheme.equals("black_theme")){
            Window window = getActivity().getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.colorPrimaryDark_BlackTheme));

            swipeRefreshLayout.setColorSchemeResources(
                    R.color.refresh_progress_1_black,
                    R.color.refresh_progress_2_black,
                    R.color.refresh_progress_3_black);

            int myColor = Color.parseColor("#444446");
            swipeRefreshLayout.setProgressBackgroundColorSchemeColor(myColor);
        } else {
            Window window = getActivity().getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.colorPrimaryDark));

            swipeRefreshLayout.setColorSchemeResources(
                    R.color.refresh_progress_1,
                    R.color.refresh_progress_2,
                    R.color.refresh_progress_3);

            int myColor = Color.parseColor("#ffffff");
            swipeRefreshLayout.setProgressBackgroundColorSchemeColor(myColor);
        }
    }

    private void getValueForAuth(){
        final SharedPreferences prefs1 = PreferenceManager.getDefaultSharedPreferences(getContext());
        sdoLogin = prefs1.getString("LoginSDO", "");
        sdoPassword = prefs1.getString("PasswordSDO", "");
    }

    private void getValueStateAutoLogin(){
        final SharedPreferences prefs1 = PreferenceManager.getDefaultSharedPreferences(getContext());
        stateButton = prefs1.getBoolean("StateButton", false);
    }

    private static String fileExtension(String name) {
        if(name.lastIndexOf(".") != -1 && name.lastIndexOf(".") != 0)
            return name.substring(name.lastIndexOf(".") + 1);
        else
            return "";
    }

}
