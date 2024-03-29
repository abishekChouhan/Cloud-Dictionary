package com.cloudDictionary.cloudCam;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudDictionry.cloudCam.PreviewImage.PreviewImageGridview;
import com.cloudDictionry.cloudCam.Utils.BitmapUtils;
import com.cloudDictionry.cloudCam.Utils.MediaFileUtils;
import com.cloudDictionry.cloudCam.Utils.OrientationUtils;
import com.cloudDictionry.cloudCam.bucket.BucketFiles;
import com.cloudDictionry.cloudCam.drawer.NavDrawerItem;
import com.cloudDictionry.cloudCam.drawer.NavDrawerListAdapter;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * καλητερη υλοποιηση της MainActivity
 * προβλημα οτι ειναι God/parent class.
 * Και εχει γινει μεγαλη
 */

public class main extends Activity {

    private Thread takePictureThread;

    private Timer vidTimer;

    //--boolean--------------------------
    public static boolean isRecordingon = false;
    public static boolean frontcamswitch;
    public static boolean rearcamswitch;
    //-----------------------------------

    private Camera camera;
    private CameraPreview camPrev;
    private MediaRecorder mediaRec;
    private Camera.PictureCallback pic;
    private int mobOrientation = -1;

    //static final int REQUEST_VIDEO_CAPTURE = 1;

    DrawerLayout mobDrawerLayout;
    ListView mobDrawerList;
    ActionBarDrawerToggle mobDrawerToggle;

    // nav drawer title
    private CharSequence mobDrawerTitle;
    private TypedArray navigationMenuIcons;

    // used to store app title
    private CharSequence mobTitle;

    // slide menu items
    private String[] navigationMenuTitles;
    //TypedArray navigationMenuIcons;

    private ArrayList<NavDrawerItem> navDrawerItems;
    private NavDrawerListAdapter adapter;

    private OrientationEventListener orientationEventListener;


    private static ProgressDialog progressDialog;

    //init create the proper folders if not exists at app startup
    {
        MediaFileUtils.initializeFolder();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressbar);
        progressBar.setVisibility(View.GONE);
        // Create an instance of Camera
        camera = getCameraInstance();


        if(frontcamswitch == true || rearcamswitch == true) {
            CameraFeatures.params = camera.getParameters();
            frontcamswitch = false;
            rearcamswitch = false;

        } else {
            //get/set camera parameters
            if (CameraFeatures.params == null) {
                CameraFeatures.params = camera.getParameters();
            } else {

            }
            try {

                // Nexus 5 is giving preview which is too dark without this
                if (Build.MODEL.contains("Nexus 5"))
                {
                    CameraFeatures.params.setPreviewFpsRange(7000, 30000);
                }


                camera.setParameters(CameraFeatures.params);
            } catch (Exception ex) {
            }

        }


        //set device hardware support
        if(camera != null) {
            PackageManager pm = getPackageManager();
            CameraFeatures.setDevicehardwareSupport(CameraFeatures.params,pm);
            //CameraFeatures.params.setPictureSize(1024,768);
            camera.setParameters(CameraFeatures.params);
        }


        // Create our Preview view and set it as the content of our activity
        camPrev = new camPrev(this,camera);

        FrameLayout preview = (FrameLayout)findViewById(R.id.camPrev);

        preview.addView(camPrev);

        bringlayoutsToFront();
        picturecallbackSetup();


        orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                //Log.v("MainActivity","orientation changed to: "+orientation);
                mobOrientation = orientation;

                //for Debug only
                //--
                //TextView text = (TextView)findViewById(R.id.txt);
                TextView text2 = (TextView)findViewById(R.id.txt2);

                //Display dis = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

                //String rot = String.valueOf(dis.getRotation());
                String rot2 = "Rota" + String.valueOf(orientation);
                //text.setText(rot);
                text2.setText(rot2);
                //--
            }
        };

        if(orientationEventListener.canDetectOrientation() == true) {

            Log.v("MainActivity","Can detect Orientation ");
            orientationEventListener.enable();

        } else {

            Log.v("MainActivity", "Cannot detect Orientation ");
            orientationEventListener.disable();

        }

        CameraFeatures.isTakingpicture = false;

        ImageView img = (ImageView)findViewById(R.id.imageview);

        if(Constants.bitmap != null) {

            BitmapDrawable bitmap = new BitmapDrawable(Constants.bitmap);
            img.setImageDrawable(bitmap);
            img.refreshDrawableState();
            Constants.bitmap = null;

        }

        setUpDefaultDrawer();


    }//>-------------------------END onCreate-------------------







    //camera settings is default drawer
    @SuppressWarnings("ResourceType")
    private void setUpDefaultDrawer() {
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressbar);
        progressBar.setVisibility(View.GONE);
        //set up drawer menu
        mobTitle = mobDrawerTitle = getTitle();

        String values[] = getResources().getStringArray(R.array.drawervalues);
        // load slide menu items
        navigationMenuTitles = new String[6];

        navigationMenuTitles[0] = values[0];
        //navigationMenuTitles[1] = values[1];
        //navigationMenuTitles[2] = values[2];
        navigationMenuTitles[3] = "Focus";

        // nav drawer icons from resources
        mobDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mobDrawerList = (ListView) findViewById(R.id.list_slidermenu);

        navigationMenuIcons = getResources().obtainTypedArray(R.array.nav_drawer_icons);

        navDrawerItems = new ArrayList<NavDrawerItem>();

        navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[0],navigationMenuIcons.getResourceId(0,-1)));
        //navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[1],navigationMenuIcons.getResourceId(1,-1)));
        //navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[2], navigationMenuIcons.getResourceId(2, -1)));
        navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[3], navigationMenuIcons.getResourceId(10, -1)));

        View header = getLayoutInflater().inflate(R.layout.drawer_drawerheader,null);

        mobDrawerList.addHeaderView(header);
        // setting the nav drawer list adapter
        adapter = new NavDrawerListAdapter(getApplicationContext(),
                navDrawerItems);
        mobDrawerList.setAdapter(adapter);



        mobDrawerToggle = new ActionBarDrawerToggle(this, mobDrawerLayout,
                R.drawable.ic_launcher, //nav menu toggle icon
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        )

        {
            public void onDrawerClosed(View view) {

                setUpDefaultDrawerAdapter();
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {

                invalidateOptionsMenu();
            }
        };

        mobDrawerLayout.setDrawerListener(mobDrawerToggle);
        mobDrawerList.setOnItemClickListener(new DrawerDefaultItemClicklistener());


    }


    @SuppressWarnings("ResourceType")
    public  void setUpDefaultDrawerAdapter() {

        //set up drawer menu
        mobTitle = mobDrawerTitle = getTitle();

        String values[] = getResources().getStringArray(R.array.drawervalues);
        // load slide menu items
        navigationMenuTitles = new String[6];

        navigationMenuTitles[0] = values[0];
//        navigationMenuTitles[1] = values[1];
        //     navigationMenuTitles[2] = values[2];
        navigationMenuTitles[3] = "Focus";

        // nav drawer icons from resources
        mobDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mobDrawerList = (ListView) findViewById(R.id.list_slidermenu);

        navigationMenuIcons = getResources().obtainTypedArray(R.array.nav_drawer_icons);

        navDrawerItems = new ArrayList<NavDrawerItem>();

        navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[0], navigationMenuIcons.getResourceId(0, -1)));
        //navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[1], navigationMenuIcons.getResourceId(1, -1)));
        //navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[2], navigationMenuIcons.getResourceId(2, -1)));
        navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[3], navigationMenuIcons.getResourceId(10, -1)));

        // setting the nav drawer list adapter
        adapter = new NavDrawerListAdapter(getApplicationContext(),
                navDrawerItems);
        mobDrawerList.setAdapter(adapter);



        mobDrawerToggle = new ActionBarDrawerToggle(this, mobDrawerLayout,
                R.drawable.ic_launcher, //nav menu toggle icon
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        )
        {
            public void onDrawerClosed(View view) {

                setUpDefaultDrawerAdapter();
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {

                invalidateOptionsMenu();
            }
        };

        mobDrawerLayout.setDrawerListener(mobDrawerToggle);
        mobDrawerList.setOnItemClickListener(new DrawerDefaultItemClicklistener());

    }


    private class DrawerResolutionClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int pos = --position;
            if(pos >= 0 && pos <= navDrawerItems.size()) {
                final CharSequence[] camerasizes = CameraFeatures.getSupportedPictureSizes();
                final CharSequence[] focus = CameraFeatures.getSupportedFocusModes();
                for(int j=0;j<camerasizes.length;j++)
                {
                    System.out.println(camerasizes[j]);
                }
                for(int j=0;j<focus.length;j++)
                {
                    System.out.println(focus[j]);
                }
                if(pos >= 0 && pos < camerasizes.length) {

                    System.out.println(camerasizes[pos].toString());
                    String[] camsizes = camerasizes[pos].toString().split("x");

                    int w = Integer.valueOf(camsizes[0]);
                    int h = Integer.valueOf(camsizes[1]);

                    CameraFeatures.params.setPictureSize(w, h);
                    camera.setParameters(CameraFeatures.params);


                    mobDrawerLayout.closeDrawer(Gravity.LEFT);
                    setUpDefaultDrawerAdapter();
                }
            }
        }
    }


    /*
    private class DrawerEffectsClicklistener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int pos = --position;
            if(pos >= 0 && pos <= navDrawerItems.size()) {

                final CharSequence effects[] = CameraFeatures.getSupportedCameraEffects();

                if(pos >= 0 && pos < effects.length) {


                    mobDrawerList.setOnItemClickListener(null);


                    if (effects[pos].equals("aqua")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraColorEffectsChange().execute(Camera.Parameters.EFFECT_AQUA);
                        }
                    } else if (effects[pos].equals("blackboard")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraColorEffectsChange().execute(Camera.Parameters.EFFECT_BLACKBOARD);
                        }
                    } else if (effects[pos].equals("negative")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraColorEffectsChange().execute(Camera.Parameters.EFFECT_NEGATIVE);
                        }
                    } else if (effects[pos].equals("mono")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraColorEffectsChange().execute(Camera.Parameters.EFFECT_MONO);
                        }
                    } else if (effects[pos].equals("none")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraColorEffectsChange().execute(Camera.Parameters.EFFECT_NONE);
                        }
                    } else if (effects[pos].equals("posterize")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraColorEffectsChange().execute(Camera.Parameters.EFFECT_POSTERIZE);
                        }
                    } else if (effects[pos].equals("sepia")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraColorEffectsChange().execute(Camera.Parameters.EFFECT_SEPIA);
                        }
                    } else if (effects[pos].equals("solarize")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraColorEffectsChange().execute(Camera.Parameters.EFFECT_SOLARIZE);
                        }
                    } else if (effects[pos].equals("whiteboard")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraColorEffectsChange().execute(Camera.Parameters.EFFECT_WHITEBOARD);
                        }
                    }


                }
            }
        }

    }

    /*
    private class DrawerScenesItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent,View view, int position, long id) {

            int pos = --position;
            if(pos >= 0 && pos <= navDrawerItems.size()) {

               // final CharSequence scenes[] = CameraFeatures.getSupportedCameraScenes();

                if(pos >= 0 && pos < scenes.length) {


                    mobDrawerList.setOnItemClickListener(null);


                    if (scenes[pos].equals("action")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_ACTION);
                            //old way
                            //CameraFeatures.params.setSceneMode(Camera.Parameters.SCENE_MODE_ACTION);
                            //startActivity(new Intent(MainActivity.this, MainActivity.class));
                            //finish();
                        }
                    } else if (scenes[pos].equals("auto")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_AUTO);
                        }
                    } else if (scenes[pos].equals("beach")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_BEACH);
                        }
                    } else if (scenes[pos].equals("candlelight")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_CANDLELIGHT);
                        }
                    } else if (scenes[pos].equals("fireworks")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_FIREWORKS);
                        }
                    } else if (scenes[pos].equals("HDR")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_HDR);
                        }
                    } else if (scenes[pos].equals("landscape")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_LANDSCAPE);
                        }
                    } else if (scenes[pos].equals("night")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_NIGHT);
                        }
                    } else if (scenes[pos].equals("night portrait")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_NIGHT_PORTRAIT);
                        }
                    } else if (scenes[pos].equals("party")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_PARTY);
                        }
                    } else if (scenes[pos].equals("portrait")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_PORTRAIT);
                        }
                    } else if (scenes[pos].equals("snow")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_SNOW);
                        }
                    } else if (scenes[pos].equals("sports")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_SPORTS);
                        }
                    } else if (scenes[pos].equals("steady photo")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_STEADYPHOTO);
                        }
                    } else if (scenes[pos].equals("sunset")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_SUNSET);
                        }
                    } else if (scenes[pos].equals("theatre")) {
                        releaseCamera();
                        if (camera == null) {
                            new CameraSceneChange().execute(Camera.Parameters.SCENE_MODE_THEATRE);
                        }
                    }


                }

            }

        }
    }


    */
    private class DrawerFocusModesClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            int pos = --position;

            if(pos >= 0 && pos <= navDrawerItems.size()) {

                final CharSequence[] focusModes = CameraFeatures.getSupportedFocusModes();

                if(pos >= 0 && pos < focusModes.length) {

                    if(focusModes[pos].equals("focus auto")) {
                        if(CameraFeatures.hasAutofocus) {
                            if(camera != null) {
                                CameraFeatures.params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                                camera.setParameters(CameraFeatures.params);
                            }
                        }
                    } else if(focusModes[pos].equals("Extended depth of field")) {
                        if(camera != null) {
                            CameraFeatures.params.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
                            camera.setParameters(CameraFeatures.params);
                        }
                    } else if(focusModes[pos].equals("fixed focus")) {
                        if(camera != null) {
                            CameraFeatures.params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                            camera.setParameters(CameraFeatures.params);
                        }
                    } else if(focusModes[pos].equals("infinity focus")) {
                        if(camera != null) {
                            CameraFeatures.params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                            camera.setParameters(CameraFeatures.params);
                        }
                    } else if(focusModes[pos].equals("macro(close-up) focus")) {
                        if(camera != null) {
                            CameraFeatures.params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                            camera.setParameters(CameraFeatures.params);
                        }
                    }

                    mobDrawerLayout.closeDrawer(Gravity.LEFT);
                    setUpDefaultDrawerAdapter();

                }
            }

        }

    }


    private class DrawerFlashModesClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            int pos = --position;

            if(pos >= 0 && pos <= navDrawerItems.size()) {

                final CharSequence flashmodes[] = CameraFeatures.getSupportedFlashoptions();

                if (pos >= 0 && pos < flashmodes.length) {

                    if (flashmodes[pos].equals("flash auto")) {
                        if (CameraFeatures.hasFlash) {
                            if (camera != null) {
                                CameraFeatures.params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                                camera.setParameters(CameraFeatures.params);
                            }
                        }
                    } else if (flashmodes[pos].equals("flash off")) {
                        if (CameraFeatures.hasFlash) {
                            if (camera != null) {
                                CameraFeatures.params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                                camera.setParameters(CameraFeatures.params);
                            }
                        }
                    } else if (flashmodes[pos].equals("flash on")) {
                        if (CameraFeatures.hasFlash) {
                            if (camera != null) {
                                CameraFeatures.params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                                camera.setParameters(CameraFeatures.params);
                            }
                        }
                    } else if (flashmodes[pos].equals("flash red-eye reduction")) {
                        if (CameraFeatures.hasFlash) {
                            if (camera != null) {
                                CameraFeatures.params.setFlashMode(Camera.Parameters.FLASH_MODE_RED_EYE);
                                camera.setParameters(CameraFeatures.params);
                            }
                        }
                    } else if (flashmodes[pos].equals("flashlight")) {
                        if (CameraFeatures.hasFlash) {
                            if (camera != null) {
                                CameraFeatures.params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                                camera.setParameters(CameraFeatures.params);
                            }
                        }
                    }

                    mobDrawerLayout.closeDrawer(Gravity.LEFT);
                    setUpDefaultDrawerAdapter();
                }
            }

        }

    }


    @SuppressWarnings("ResourceType")
    private class DrawerDefaultItemClicklistener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(!isRecordingon) {

                int pos = --position;
                if (pos >= 0 && pos <= navDrawerItems.size()) {
                    if (pos == 0)
                    {

                        final CharSequence[] camerasizes = CameraFeatures.getSupportedPictureSizes();
                        if (camerasizes != null) {
                            // load slide menu itemsf
                            navigationMenuTitles = new String[camerasizes.length];

                            for (int i = 0; i < navigationMenuTitles.length; i++) {
                                navigationMenuTitles[i] = camerasizes[i].toString();
                            }

                            navDrawerItems = new ArrayList<NavDrawerItem>();

                            for (int i = 0; i < navigationMenuTitles.length; i++) {
                                String[] camsizes = navigationMenuTitles[i].toString().split("x");
                                int w = Integer.valueOf(camsizes[0]);
                                int h = Integer.valueOf(camsizes[1]);
                                String sizeInMp = CameraFeatures.getMegaPixels(w, h);
                                navDrawerItems.add(new NavDrawerItem(sizeInMp + "  " + navigationMenuTitles[i], navigationMenuIcons.getResourceId(0, -1)));
                            }

                            adapter = new NavDrawerListAdapter(getApplicationContext(),
                                    navDrawerItems);
                            mobDrawerList.setAdapter(adapter);

                            mobDrawerList.setOnItemClickListener(new DrawerResolutionClickListener());

                        }

                    }
                    //else if (pos == 1) {

                        /*final CharSequence effects[] = CameraFeatures.getSupportedCameraEffects();
                        if (effects != null) {

                            navigationMenuTitles = new String[effects.length];

                            for (int i = 0; i < navigationMenuTitles.length; i++) {
                                navigationMenuTitles[i] = effects[i].toString();
                            }

                            navDrawerItems = new ArrayList<NavDrawerItem>();

                            for (int i = 0; i < navigationMenuTitles.length; i++) {
                                navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[i], navigationMenuIcons.getResourceId(1, -1)));
                            }

                            adapter = new NavDrawerListAdapter(getApplicationContext(),
                                    navDrawerItems);
                            mobDrawerList.setAdapter(adapter);

                            mobDrawerList.setOnItemClickListener(new DrawerEffectsClicklistener());


                        } else {
                            Context context = getApplicationContext();
                            int duration = Toast.LENGTH_SHORT;
                            Toast.makeText(context, "no effects support", duration).show();
                        }

                    } else if (pos == 2) {

                        /*final CharSequence scenes[] = CameraFeatures.getSupportedCameraScenes();

                        if (scenes != null) {

                            navigationMenuTitles = new String[scenes.length];

                            for (int i = 0; i < navigationMenuTitles.length; i++) {
                                navigationMenuTitles[i] = scenes[i].toString();
                            }

                            navDrawerItems = new ArrayList<NavDrawerItem>();

                            for (int i = 0; i < navigationMenuTitles.length; i++) {
                                navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[i], navigationMenuIcons.getResourceId(2, -1)));
                            }

                            adapter = new NavDrawerListAdapter(getApplicationContext(),
                                    navDrawerItems);
                            mobDrawerList.setAdapter(adapter);

                            mobDrawerList.setOnItemClickListener(new DrawerScenesItemClickListener());

                        } else {
                            Context context = getApplicationContext();
                            int duration = Toast.LENGTH_SHORT;
                            Toast.makeText(context, "no scenes support", duration).show();
                        }
                    */
                    //}
                    else if (pos == 1) {

                        final CharSequence focusModes[] = CameraFeatures.getSupportedFocusModes();

                        if(focusModes != null) {

                            navigationMenuTitles = new String[focusModes.length];

                            for(int i =0; i < navigationMenuTitles.length; i++) {
                                navigationMenuTitles[i] = focusModes[i].toString();
                            }

                            navDrawerItems = new ArrayList<NavDrawerItem>();

                            for(int i =0; i < navigationMenuTitles.length; i++) {
                                navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[i],navigationMenuIcons.getResourceId(10,-1)));
                            }

                            adapter = new NavDrawerListAdapter(getApplicationContext(),navDrawerItems);

                            mobDrawerList.setAdapter(adapter);

                            mobDrawerList.setOnItemClickListener(new DrawerFocusModesClickListener());

                        } else {

                            Toast.makeText(getApplicationContext(),"no focus support",Toast.LENGTH_LONG).show();

                        }

                    }
                }
            }
        }
    }



    public void imagecapture(View view) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressbar);
                        progressBar.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();


        CameraFeatures.isTakingpicture = true;
        View imagebutton = (View) findViewById(R.id.capturebutton);
        //View recbutton = (View) findViewById(R.id.recbutton);

        try {

            imagebutton.setEnabled(false);
//                recbutton.setEnabled(false);

            camera.setPreviewCallback(null);

            if(CameraFeatures.hasAutofocus && camera.getParameters().getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO)) {

                camera.autoFocus(new Camera.AutoFocusCallback() {

                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {

                        try {

                            camera.takePicture(new Camera.ShutterCallback() {
                                @Override
                                public void onShutter() {

                                }

                            }, null, picture);

                        } catch (Throwable th) {

                        }

                    }
                });

            } else {

                try {

                    camera.takePicture(new Camera.ShutterCallback() {
                        @Override
                        public void onShutter() {

                        }

                    }, null, pic);

                } catch (Throwable th) {

                }

            }


        } catch (Exception ex) {
            ex.printStackTrace();
            releaseCamera();
            camera = null;
            startActivity(new Intent(main.this, main.class));
            finish();
        } finally {
            //imagebutton.setEnabled(true);
            //recbutton.setEnabled(true);
        }

    }


    public  Camera getCameraInstance() {
        Camera c = null;
        try {
            if(Camera.getNumberOfCameras() == 1) {
                c = Camera.open();
                //// TODO:  check if no back camera exists open front
                if(c == null) {
                    c = Camera.open(1);
                }
            } else {
                try {
                    c = Camera.open(CameraFeatures.camId);
                } catch (Exception ex) {
                    c = Camera.open();
                }
            }
            Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            OrientationUtils.setCameraDisplayOrientation(CameraFeatures.camId,c,display);

        } catch(Exception ex) {
            ex.printStackTrace();
            releaseMediaRecorder();
            releaseCamera();
        }

        return c;
    }


    private void picturecallbackSetup() {

        pic = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] data,Camera camera) {

                if (MediaFileUtils.isFreeSpaceAvailable(data)) {

                    try {

                        final File picturefile = MediaFileUtils.getOutputMediaFile(MediaFileUtils.IMAGE_TYPE);

                        if (picturefile == null) {
                            return;
                        }
                        //set picture rotation
                        //camid == 0 => back camera in use
                        final int rotation = OrientationUtils.getRotationForPictureTakeCallBack(mobOrientation);
                        final Context context = getApplicationContext();

                        try {
                            Constants.bitmap = BitmapUtils.decodeSampledBitmapFromByteArray(data, 50, 50);
                        } catch (Throwable th) {
                            if(th instanceof OutOfMemoryError) {
                                System.gc();
                            }
                        }

                        takePictureThread = new Thread(new Runnable() {
                            @Override
                            public void run() {

                                try {

                                    MediaFileUtils.jnirotateImageAndWriteToFile(rotation, data, picturefile, context);

                                } catch (Throwable th) {

                                    if(th instanceof OutOfMemoryError) {
                                        System.gc();
                                    } else {
                                    }
                                    //throwable will be thrown only if cpp/so library fails to load or outofmemory
                                    //in that case process image with java
                                    try {

                                        MediaFileUtils.rotateImageAndWriteToFile(rotation, data, picturefile, context);

                                    } catch (Throwable throwable) {
                                        //in that case abort this picture capture
                                    }

                                }
                            }
                        });

                        takePictureThread.setPriority(Thread.MAX_PRIORITY - 1);
                        takePictureThread.start();

                    } finally{
                        //releashing camera
                        camera.release();
                        startActivity(new Intent(main.this, main.class));
                        //finishing this activity and starting a new one
                        finish();
                    }
                } else {

                    Context context = getApplicationContext();
                    int duration = Toast.LENGTH_LONG;
                    Toast.makeText(context, "no free space", duration).show();

                    //releashing camera
                    camera.release();
                    startActivity(new Intent(main.this, main.class));
                    //finishing this activity and starting a new one
                    finish();

                }
            }
        };
    }




    private void releaseCamera() {
        if(camera != null) {
            camPrev.getHolder().removeCallback(camPrev);
            camera.release();
            camera = null;
        }
    }

    private void releaseMediaRecorder() {
        if(mediaRec != null) {
            mediaRec.reset();
            mediaRec.release();
            mediaRec = null;
            camera.lock();
        }
    }


    //---Begin Override Methods--------------------------------------------

    /***
     * Called when invalidateOptionsMenu() is triggered
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // if nav drawer is opened, hide the action items
        boolean drawerOpen = mobDrawerLayout.isDrawerOpen(mobDrawerList);
        menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }



    @Override
    public void setTitle(CharSequence title) {
        mobTitle = title;
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mobDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mobDrawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaRecorder();
        releaseCamera();
        orientationEventListener.disable();

    }


    @Override
    protected void onPause() {
        super.onPause();
        if(CameraFeatures.params != null && CameraFeatures.hasFlash) {
            if(CameraFeatures.params.getFlashMode() != null) {
                if (CameraFeatures.params.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                    CameraFeatures.params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    if (camera != null) {
                        camera.setParameters(CameraFeatures.params);
                    }
                }
            }
        }

        releaseCamera();
    }


    @Override
    protected void onResume() {
        super.onResume();

        if(camera == null) {

            camera = getCameraInstance();
            setContentView(R.layout.activity_main);

            camPrev = new camPrev(this,camera);
            FrameLayout preview = (FrameLayout)findViewById(R.id.camPrev);
            preview.addView(camPrev);

            bringlayoutsToFront();
            setUpDefaultDrawer();

        }

        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressbar);
        progressBar.setVisibility(View.GONE);

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public void onBackPressed() {
       /* if (mobDrawerLayout.isDrawerOpen(GravityCompat.START)) {

            mobDrawerLayout.closeDrawer(GravityCompat.START);

        } else {
            super.onBackPressed();
        }*/
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);

    }

    //|-----END OVERRIDE METHODS--------------------------------


    /**
     * bring Linear layout tha contains buttons to front to be visible in surfaceview-Framelayout
     */
    public void bringlayoutsToFront() {

        //this linearlayout contains take picture and record video button...at tha time
        LinearLayout ll = (LinearLayout)findViewById(R.id.linearbottom);
        ll.bringToFront();

        //...contains option button
        LinearLayout l2 = (LinearLayout)findViewById(R.id.optionsbuttonlayout);
        l2.bringToFront();

        //bottom right
        LinearLayout l4 = (LinearLayout)findViewById(R.id.linearimagepreview);
        l4.bringToFront();

        //recstatus layout botton left
        // LinearLayout recstat = (LinearLayout)findViewById(R.id.recstatus);
        // recstat.bringToFront();

    }


    public void optionclick(View v) {
        if(!isRecordingon) {
            mobDrawerLayout.openDrawer(GravityCompat.START);
        }
    }



    public void imagepreview(View v) {


        if(!isRecordingon) {
            if(!CameraFeatures.isTakingpicture) {

                BucketFiles.initializeAllPicturesToBucket();

                if(BucketFiles.getPictureFileSize() > 0) {

                    final ImageView imageView = (ImageView) findViewById(R.id.imageview);
                    imageView.setEnabled(false);
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    imageView.setEnabled(true);
                                }
                            });
                        }
                    }, 1500);

                    startActivity(new Intent(main.this, PreviewImageGridview.class).setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP));
                }
            }
        }


    }




    @SuppressWarnings("ResourceType")
    public void showFlashOptions(View v) {


        if (CameraFeatures.hasFlash == true) {
            if (CameraFeatures.params.getFlashMode() != null) {

                final CharSequence flashmodes[] = CameraFeatures.getSupportedFlashoptions();

                if (flashmodes != null) {
                    navigationMenuTitles = new String[flashmodes.length];

                    for(int i =0; i< navigationMenuTitles.length; i++) {
                        navigationMenuTitles[i] = flashmodes[i].toString();
                    }

                    navDrawerItems = new ArrayList<NavDrawerItem>();

                    for(int i =0; i < navigationMenuTitles.length; i++) {
                        if(navigationMenuTitles[i].equals("flash auto")) {
                            navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[i], navigationMenuIcons.getResourceId(3, -1)));
                        } else if(navigationMenuTitles[i].equals("flash off")) {
                            navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[i], navigationMenuIcons.getResourceId(4, -1)));
                        } else if(navigationMenuTitles[i].equals("flash on")) {
                            navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[i], navigationMenuIcons.getResourceId(5, -1)));
                        } else if(navigationMenuTitles[i].equals("flashlight")) {
                            navDrawerItems.add(new NavDrawerItem(navigationMenuTitles[i], navigationMenuIcons.getResourceId(6, -1)));
                        }
                    }

                    adapter = new NavDrawerListAdapter(getApplicationContext(),
                            navDrawerItems);
                    mobDrawerList.setAdapter(adapter);

                    mobDrawerList.setOnItemClickListener(new DrawerFlashModesClickListener());

                    mobDrawerLayout.openDrawer(GravityCompat.START);

                }

            } else {

                Context context = getApplicationContext();
                int duration = Toast.LENGTH_LONG;
                Toast.makeText(context, "no flash support!maybe front camera is in use?", duration).show();

            }
        } else {

            Context context = getApplicationContext();
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(context, "no flash", duration).show();

        }

    }

    public void changeCamera(View v) {

        final ImageView imageView = (ImageView)findViewById(R.id.changecamerabutton);
        imageView.setEnabled(false);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setEnabled(true);
                    }
                });
            }
        }, 1500);

        if(CameraFeatures.frontcam && CameraFeatures.rearcam) {

            if (CameraFeatures.camId == 0) {
                CameraFeatures.camId = 1;
                releaseCamera();
                if(camera == null) {
                    frontcamswitch = true;
                    startActivity(new Intent(main.this, main.class));
                    finish();
                }
            } else if (CameraFeatures.camId == 1) {
                CameraFeatures.camId = 0;
                releaseCamera();
                if(camera == null) {
                    rearcamswitch = true;
                    startActivity(new Intent(main.this,main.class));
                    finish();
                }
            }

        } else {

            Context context = getApplicationContext();
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(context, "only one camera", duration).show();

        }


    }



}
