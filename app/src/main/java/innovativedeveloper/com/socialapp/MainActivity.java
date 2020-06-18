package innovativedeveloper.com.socialapp;
import android.*;
import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import innovativedeveloper.com.socialapp.adapter.BadgeCounter;
import innovativedeveloper.com.socialapp.adapter.PageFragmentAdapter;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.AppHelper;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.Feed;
import innovativedeveloper.com.socialapp.fragment.FriendRequests;
import innovativedeveloper.com.socialapp.fragment.NewsFeed;
import innovativedeveloper.com.socialapp.fragment.Notifications;
import innovativedeveloper.com.socialapp.fragment.Profile;
import innovativedeveloper.com.socialapp.messaging.ChatActivity;
import innovativedeveloper.com.socialapp.messaging.Messages;
import innovativedeveloper.com.socialapp.preferences.SettingsActivity;
import innovativedeveloper.com.socialapp.services.AppService;

public class MainActivity extends AppCompatActivity implements AppService.OnServiceChanged {

    TabLayout tabLayout;
    ViewPager viewPager;
    ActionBar actionbar;
    Toolbar toolbar;
    View mainActivity;
    PageFragmentAdapter adapter;

    FriendRequests fragmentRequests;
    NewsFeed fragmentNewsFeed;
    Notifications fragmentNotification;
    Profile fragmentProfile;
    BroadcastReceiver broadcastReceiver;
    int notificationCount, messagesCount, requestsCount;
    FloatingActionButton floatingActionButton;

    List<Fragment> fragmentList;
    public static void startActivity(Activity startingActivity) {
        Intent intent = new Intent(startingActivity, MainActivity.class);
        startingActivity.startActivity(intent);
    }

    public void setFabVisibility(int v) {
        if (floatingActionButton != null) {
            floatingActionButton.setVisibility(v);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        fragmentList = new ArrayList<>();
        setSupportActionBar(toolbar);
        actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(false);
        mainActivity = findViewById(R.id.main_content);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(4);

        setupViewPager(viewPager);
        floatingActionButton = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, UpdatePost.class));
            }
        });
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (getSupportActionBar() != null) {
                    if (tab.getPosition() == 0) {
                        getSupportActionBar().setTitle("News Feed");
                    } else if (tab.getPosition() == 1) {
                        getSupportActionBar().setTitle("Notifications");
                    } else if (tab.getPosition() == 2) {
                        getSupportActionBar().setTitle("Friend Requests");
                    } else if (tab.getPosition() == 3) {
                        getSupportActionBar().setTitle("Settings");
                        ((Profile)getSupportFragmentManager().getFragments().get(3)).loadAccount(AppHandler.getInstance().getUser());
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        setupTabIcons();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                if (intent.getAction().equals("notification")) {
                    if (intent.getStringExtra("action").equals(String.valueOf(Config.PUSH_TYPE_NOTIFICATION))) {
                        if (fragmentNotification != null) {
                            fragmentNotification.refreshNotifications();
                        }
                        updateNotification(AppHandler.getInstance().getDBHandler().getNotificationsCount());
                        Snackbar.make(mainActivity, intent.getStringExtra("messageData"), Snackbar.LENGTH_LONG).setAction("View", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (intent.getBooleanExtra("isCustom", false)) {
                                    viewPager.setCurrentItem(1);
                                    return;
                                }
                                if (!intent.getStringExtra("commentId").equals("0")) {
                                    final Intent commentIntent = new Intent(MainActivity.this, Comments.class);
                                    commentIntent.putExtra("postId", intent.getStringExtra("postId"));
                                    startActivity(commentIntent);
                                    overridePendingTransition(0, 0);
                                    return;
                                }
                                if (intent.hasExtra("postId")) {
                                    ActivityPost.startActivityPost(MainActivity.this, intent.getStringExtra("username"), intent.getStringExtra("username"), intent.getStringExtra("postId"));
                                    return;
                                }
                                UserProfile.startUserProfile(MainActivity.this, intent.getStringExtra("username"), intent.getStringExtra("username"));
                            }
                        }).show();
                    } else if (intent.getStringExtra("action").equals(String.valueOf(Config.PUSH_TYPE_REQUESTS))) {
                        if (fragmentRequests != null) {
                            fragmentRequests.refreshRequests();
                        }
                        Snackbar.make(mainActivity, intent.getStringExtra("messageData"), Snackbar.LENGTH_LONG).setAction("View Requests", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                viewPager.setCurrentItem(2);
                            }
                        }).show();
                    } else if (intent.getStringExtra("action").equals(String.valueOf(Config.PUSH_TYPE_MESSAGE))) {
                        updateMessages(AppHandler.getInstance().getDBHandler().getMessagesCount());
                        Snackbar.make(mainActivity, intent.getStringExtra("messageData"), Snackbar.LENGTH_LONG).setAction("Open", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (intent.getBooleanExtra("isCustom", false)) {
                                    startActivity(new Intent(MainActivity.this, Messages.class));
                                } else {
                                    ChatActivity.startActivity(MainActivity.this, intent.getStringExtra("username"), intent.getStringExtra("name"));
                                }
                            }
                        }).show();
                    } else if (intent.getStringExtra("action").equals(String.valueOf(Config.PUSH_TYPE_REPLY))) {
                        if (fragmentNotification != null) {
                            fragmentNotification.refreshNotifications();
                        }
                        updateNotification(AppHandler.getInstance().getDBHandler().getNotificationsCount());
                        Snackbar.make(mainActivity, intent.getStringExtra("messageData"), Snackbar.LENGTH_LONG).setAction("View", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (intent.getBooleanExtra("isCustom", false)) {
                                    viewPager.setCurrentItem(1);
                                    return;
                                }
                                final Intent commentIntent = new Intent(MainActivity.this, Comments.class);
                                commentIntent.putExtra("postId", intent.getStringExtra("postId"));
                                commentIntent.putExtra("commentId", intent.getStringExtra("commentId"));
                                commentIntent.putExtra("isDisabled", false);
                                commentIntent.putExtra("isOwnPost", false);
                                commentIntent.putExtra("isReply", true);
                                startActivity(commentIntent);
                                overridePendingTransition(0, 0);
                            }
                        }).show();
                    }
                }
            }
        };


        if (getIntent().getBooleanExtra("isNotification", false)) {
            viewPager.setCurrentItem(1);
        }
        authorize();
        MobileAds.initialize(getApplicationContext(), getResources().getString(R.string.ad_app_id));
    }

    public void updateMessages(int count) {
        this.messagesCount = count;
        invalidateOptionsMenu();
    }

    public void updateNotification(int count) {
        this.notificationCount = count;
        BadgeCounter.updateTab(this, viewPager, tabLayout.getTabAt(1), R.drawable.tab_notification, BadgeCounter.BadgeColor.RED, String.valueOf(notificationCount));
    }

    public void updateRequests(int count) {
        this.requestsCount = count;
        BadgeCounter.updateTab(this, viewPager, tabLayout.getTabAt(2), R.drawable.tab_friend, BadgeCounter.BadgeColor.RED, String.valueOf(requestsCount));
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        BadgeCounter.update(this, menu.findItem(R.id.action_messages), R.drawable.ic_messages, BadgeCounter.BadgeColor.BLUE, messagesCount);
        return super.onPrepareOptionsMenu(menu);
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new PageFragmentAdapter(getSupportFragmentManager());
        fragmentNewsFeed = new NewsFeed();
        fragmentRequests = new FriendRequests();
        fragmentNotification = new Notifications();
        fragmentProfile = new Profile();
        adapter.addFragment(fragmentNewsFeed, getString(R.string.tab_feed));
        adapter.addFragment(fragmentNotification, getString(R.string.tab_notifications));
        adapter.addFragment(fragmentRequests, getString(R.string.tab_requests));
        adapter.addFragment(fragmentProfile, getString(R.string.tab_profile));
        viewPager.setAdapter(adapter);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("News Feed");
    }

    private void setupTabIcons() {
        tabLayout.getTabAt(0).setIcon(R.drawable.tab_feed);
        tabLayout.getTabAt(1).setIcon(R.drawable.tab_notification);
        tabLayout.getTabAt(2).setIcon(R.drawable.tab_friend);
        tabLayout.getTabAt(3).setIcon(R.drawable.tab_profile);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMessages(AppHandler.getInstance().getDBHandler().getMessagesCount());
        updateNotification(AppHandler.getInstance().getDBHandler().getNotificationsCount());
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("notification"));
        AppHandler.getInstance().getAppService().setOnServiceChanged(this);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            super.onBackPressed();
        } else {
            viewPager.setCurrentItem(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            startActivity(new Intent(this, Search.class));
        } else if (item.getItemId() == R.id.action_messages) {
            startActivity(new Intent(this, Messages.class));
        } else if (item.getItemId() == R.id.action_trending) {
            startActivity(new Intent(this, Trending.class));
        } else if (item.getItemId() == R.id.action_profile) {
            UserProfile.startUserProfile(this, AppHandler.getInstance().getUser().getUsername(), AppHandler.getInstance().getUser().getName());
        } else if (item.getItemId() == R.id.action_logout) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout from this account?")
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AppHandler.getInstance().getDBHandler().resetDatabase();
                            AppHandler.getInstance().getDataManager().clear();
                            startActivity(new Intent(getApplicationContext(), AppHelper.class));
                            Toast.makeText(MainActivity.this, "Session expired.", Toast.LENGTH_SHORT).show();
                            try {
                                FirebaseInstanceId.getInstance().deleteInstanceId();
                            } catch (IOException ex) {
                                Log.e("MainActivity", "Unable to delete instance id.");
                            }
                        }
                    }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            })
                    .show();

        } else if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAuthorizedStatusChanged(JSONObject obj) {
        try {
            if (!obj.getBoolean("error")) {
                ((NewsFeed)getSupportFragmentManager().getFragments().get(0)).loadFeed();
                getLocation();
            } else {
                int code = obj.getInt("code");
                if (code == Config.ACCOUNT_DISABLED) {
                    AppHandler.getInstance().getDBHandler().resetDatabase();
                    AppHandler.getInstance().getDataManager().clear();
                    new AlertDialog.Builder(MainActivity.this, R.style.AppTheme_Dark_Dialog)
                            .setTitle("Account Disabled")
                            .setMessage("Your account is disabled/suspended for %reason. You\'ll be redirected at login screen.".replace("%reason", obj.getString("reason")))
                            .setPositiveButton("OKAY", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent loginIntent = new Intent(getApplicationContext(), AppHelper.class);
                                    loginIntent.putExtra("isLogin", true);
                                    startActivity(loginIntent);
                                    dialog.dismiss();
                                }
                            })
                            .show();
                } else if (code == Config.SESSION_EXPIRED) {
                    AppHandler.getInstance().getDBHandler().resetDatabase();
                    AppHandler.getInstance().getDataManager().clear();
                    startActivity(new Intent(getApplicationContext(), AppHelper.class));
                    Toast.makeText(MainActivity.this, "Session expired.", Toast.LENGTH_SHORT).show();
                    try {
                        FirebaseInstanceId.getInstance().deleteInstanceId();
                    } catch (IOException ex) {
                        Log.e("MainActivity", "Unable to delete instance id.");
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Unable to connect to the server.", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (JSONException ex) {
            Log.e("Authorization", "Unexpected error: " + ex.getMessage());
        }
    }

    private void authorize() {
        class Authorize extends AsyncTask<Void, Void, String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
            }

            @Override
            protected String doInBackground(Void... params) {
                AppHandler.getInstance().getAppService().Authorize();
                return "";
            }
        }
        Log.e("InstanceId", "Token updated:" + FirebaseInstanceId.getInstance().getToken());
        Authorize auth = new Authorize();
        auth.execute();
    }

    public void actionClick(View view){
        fragmentProfile.actionClick(view);
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.INTERNET,
                android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (isGPSEnabled) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, Config.MIN_TIME_BW_UPDATES, Config.MIN_DISTANCE_CHANGE_FOR_UPDATES, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (location != null) {
                            double longitude = location.getLongitude();
                            double latitude = location.getLatitude();
                            updateLocation(longitude, latitude);
                        }
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {

                    }

                    @Override
                    public void onProviderEnabled(String s) {

                    }

                    @Override
                    public void onProviderDisabled(String s) {

                    }
                });
                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    double longitude = location.getLongitude();
                    double latitude = location.getLatitude();
                    updateLocation(longitude, latitude);
                }
            }
        } else {
            requestPermissions();
        }
    }

    private void updateLocation(final double longitude, final double latitude) {
        StringRequest request = new StringRequest(Request.Method.POST, Config.UPDATE_SETTINGS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {}
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("MainActivity", "Error: " + error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("longitude", String.valueOf(longitude));
                params.put("latitude", String.valueOf(latitude));
                return params;
            }
        };
        int socketTimeout = 0;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        request.setRetryPolicy(policy);
        AppHandler.getInstance().addToRequestQueue(request);
    }
}
