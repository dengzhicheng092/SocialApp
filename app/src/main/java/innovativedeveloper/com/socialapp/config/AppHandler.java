package innovativedeveloper.com.socialapp.config;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.dataset.User;
import innovativedeveloper.com.socialapp.services.AppService;

public class AppHandler extends Application {
    public static final String TAG = AppHandler.class.getSimpleName();
    static AppHandler mInstance;
    RequestQueue mRequestQueue;
    DataStorage dStorage;
    DatabaseHandler dbHandler;
    AppService appService;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    public static synchronized AppHandler getInstance() {
        return mInstance;
    }

    public AppService getAppService() {
        if (appService == null) {
            appService = new AppService();
        }
        return appService;
    }

    public DataStorage getDataManager() {
        if (dStorage == null) {
            dStorage = new DataStorage(this);
        }
        return dStorage;
    }

    public DatabaseHandler getDBHandler() {
        if (dbHandler == null) {
            dbHandler = new DatabaseHandler(this);
        }
        return dbHandler;
    }

    public User getUser() {
        User u = new User();
        u.setId(getDataManager().getString("id", ""));
        u.setName(getDataManager().getString("name", ""));
        u.setUsername(getDataManager().getString("username", ""));
        u.setLocation(getDataManager().getString("location", ""));
        u.setEmail(getDataManager().getString("email", ""));
        u.setProfilePhoto(getDataManager().getString("profilePhoto", ""));
        u.setDescription(getDataManager().getString("description", ""));
        u.setRelationship(getDataManager().getInt("relationship", 0));
        u.setGender(getDataManager().getInt("gender", 0));
        u.totalPosts = getDataManager().getInt("totalPosts", 0);
        u.totalPhotos = getDataManager().getInt("totalPhotos", 0);
        u.totalFriends = getDataManager().getInt("totalFriends", 0);
        u.totalVideos = getDataManager().getInt("totalVideos", 0);
        return u;
    }

    public void updateUser(User u) {
        getDataManager().setString("name", u.getName());
        getDataManager().setString("username", u.getUsername());
        getDataManager().setString("location", u.getLocation());
        getDataManager().setString("email", u.getEmail());
        getDataManager().setString("profilePhoto", u.getProfilePhoto());
        getDataManager().setInt("totalPosts", u.totalPosts);
        getDataManager().setInt("totalPhotos", u.totalPhotos);
        getDataManager().setInt("totalFriends", u.totalFriends);
        getDataManager().setInt("totalVideos", u.totalVideos);
    }

    public Map<String, String> getAuthorization() {
        if (dStorage == null) {
            getDataManager();
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", dStorage.getString("api", "null"));
        return headers;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        return mRequestQueue;
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }

    public <T> void addToRequestQueue(String tag, Request<T> req) {
        req.setTag(tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void playNotificationSound() {
        MediaPlayer mp = MediaPlayer.create(this, R.raw.notification);
        if (mp.isPlaying()) {
            return;
        }
        mp.start();
    }

    public static boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }
        return isInBackground;
    }

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    public static String getTimestamp(String stamp) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date past = format.parse(stamp);
            Date now = new Date();
            final long cr = now.getTime() - past.getTime();
            if (cr < MINUTE_MILLIS) {
                return "Just a second ago";
            } else if (cr < 2 * MINUTE_MILLIS) {
                return "Just a minute ago";
            } else if (cr < 50 * MINUTE_MILLIS) {
                return cr / MINUTE_MILLIS + " minute ago";
            } else if (cr < 90 * MINUTE_MILLIS) {
                return "1 hour ago";
            } else if (cr < 24 * HOUR_MILLIS) {
                return cr / HOUR_MILLIS + " hour ago";
            } else if (cr < 48 * HOUR_MILLIS) {
                return "1 day ago";
            } else {
                return cr / DAY_MILLIS + " days ago";
            }
        } catch (Exception j) {
            return null;
        }
    }

    public static String getTimestampShort(String stamp) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date past = format.parse(stamp);
            Date now = new Date();
            final long cr = now.getTime() - past.getTime();
            if (cr < MINUTE_MILLIS) {
                return "1s ago";
            } else if (cr < 2 * MINUTE_MILLIS) {
                return "1m ago";
            } else if (cr < 50 * MINUTE_MILLIS) {
                return cr / MINUTE_MILLIS + "m ago";
            } else if (cr < 90 * MINUTE_MILLIS) {
                return "1h ago";
            } else if (cr < 24 * HOUR_MILLIS) {
                return cr / HOUR_MILLIS + "h ago";
            } else if (cr < 48 * HOUR_MILLIS) {
                return "1d ago";
            } else {
                return cr / DAY_MILLIS + "d ago";
            }
        } catch (Exception j) {
            return null;
        }
    }

}
