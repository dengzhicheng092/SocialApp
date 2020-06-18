package innovativedeveloper.com.socialapp.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import innovativedeveloper.com.socialapp.ActivityPost;
import innovativedeveloper.com.socialapp.Comments;
import innovativedeveloper.com.socialapp.MainActivity;
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.UserProfile;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.Message;
import innovativedeveloper.com.socialapp.dataset.Notification;
import innovativedeveloper.com.socialapp.dataset.User;
import innovativedeveloper.com.socialapp.messaging.ChatActivity;
import innovativedeveloper.com.socialapp.messaging.Messages;

import static innovativedeveloper.com.socialapp.config.Config.PUSH_TYPE_MESSAGE;
import static innovativedeveloper.com.socialapp.config.Config.PUSH_TYPE_NOTIFICATION;
import static innovativedeveloper.com.socialapp.config.Config.PUSH_TYPE_REPLY;
import static innovativedeveloper.com.socialapp.config.Config.PUSH_TYPE_REQUESTS;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("MessagingService", "Message received.");
        Map data = remoteMessage.getData();
        String id = data.get("id").toString();
        String title = data.get("title").toString();
        String type = data.get("type").toString();
        String strData = data.get("data").toString();
        boolean isBackground = Boolean.valueOf(data.get("isBackground").toString());
        boolean isCustom = Boolean.valueOf(data.get("isCustom").toString());
        if (type.equals(String.valueOf(PUSH_TYPE_NOTIFICATION)) || type.equals(String.valueOf(PUSH_TYPE_REPLY))) {
            updateNotification(strData, title, id, isCustom, isBackground);
        } else if (type.equals(String.valueOf(PUSH_TYPE_MESSAGE))) {
            updateMessage(strData, title, id, isCustom, isBackground);
        } else if (type.equals(String.valueOf(PUSH_TYPE_REQUESTS))) {
            updateRequests(strData, title, id, isCustom);
        }
    }

    private void updateMessage(String Data, String title, String id, boolean isCustom, boolean isBackground) {
        try {
            JSONObject data = new JSONObject(Data);
            JSONObject sender = data.getJSONObject("sender");
            Notification n = new Notification();
            Message m = new Message();
            if (!isCustom) {
                // Notification set
                n.setId(id);
                n.setUser_id(sender.getString("id"));
                n.setName(sender.getString("name"));
                n.setUsername(sender.getString("username"));
                n.setAction(data.getString("action"));
                n.setIcon(sender.getString("icon"));
                n.setData(sender.getString("name") + " sent you a message.");
                n.setCreation(data.getString("creation"));

                // Message set
                m.setUsername(n.getUsername());
                m.setMessage(data.getString("message"));
                m.setCreation(data.getString("creation"));
                m.setType(data.getInt("type"));
                m.setChecked(0);
                m.setSent(1);
                m.setIsOwn(0);

                if (!AppHandler.getInstance().getDBHandler().isUserExists(n.getUsername())) {
                    User u = new User();
                    u.setUsername(sender.getString("username"));
                    u.setName(sender.getString("name"));
                    u.setEmail(sender.getString("email"));
                    u.setProfilePhoto(sender.getString("icon"));
                    AppHandler.getInstance().getDBHandler().addUser(u);
                }
                AppHandler.getInstance().getDBHandler().addMessage(m);

                if (!isBackground) {
                    if (!AppHandler.isAppIsInBackground(getApplicationContext())) {
                        AppHandler.getInstance().playNotificationSound();
                    }
                    broadcastNotification(n, false, title);
                }
                updateMessage(id);
            } else {
                n.setAction("3");
                n.setData(data.getString("messageData"));
                broadcastNotification(n, true, title);
            }
        } catch (JSONException ex) {
            Log.e("ListenerService", ex.getMessage());
        }
    }

    private void updateRequests(String Data, String title, String id, boolean isCustom) {
        try {
            JSONObject data = new JSONObject(Data);
            Notification n = new Notification();
            n.setId(id);
            n.setUser_id(data.getString("userId"));
            n.setUsername(data.getString("username"));
            n.setAction(data.getString("action"));
            n.setData(data.getString("messageData"));
            if (AppHandler.isAppIsInBackground(getApplicationContext())) {

            } else {

            }
            updateNotification(n.getId());
        } catch (JSONException ex) {
            Log.e("ListenerService", ex.getMessage());
        }
    }

    private void updateNotification(String Data, String title, String id, boolean isCustom, boolean isBackground) {
        try {
            JSONObject data = new JSONObject(Data);
            Notification n = new Notification();
            if (!isCustom) {
                n.setId(id);
                n.setUser_id(data.getString("userId"));
                n.setPostId(data.getString("postId"));
                n.setCommentId(data.getString("commentId"));
                n.setUsername(data.getString("username"));
                n.setAction(data.getString("action"));
                n.setIcon(data.getString("icon"));
                n.setData(data.getString("messageData"));
                n.setCreation(data.getString("creation"));
                AppHandler.getInstance().getDBHandler().addNotification(n);

                if (!isBackground) {
                    if (!AppHandler.isAppIsInBackground(getApplicationContext())) {
                        AppHandler.getInstance().playNotificationSound();
                    }
                    broadcastNotification(n, false, title);
                }
                updateNotification(n.getId());
            } else {
                n.setAction("1");
                n.setData(data.getString("messageData"));
                broadcastNotification(n, true, title);
            }
        } catch (JSONException ex) {
            Log.e("ListenerService", ex.getMessage());
        }
    }

    private void updateNotification(String id) {
        final StringRequest request = new StringRequest(Request.Method.POST, Config.UPDATE_NOTIFICATION.replace(":id", id), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        Log.d("NotificationService", "Notification marked as read from server-side.");
                    } else {}
                }
                catch (JSONException ex) {
                    Log.e("FirebaseService", "Unexpected error: " + ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("FirebaseService", "Unexpected error: " + error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }
        };
        int socketTimeout = 0;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        request.setRetryPolicy(policy);
        AppHandler.getInstance().addToRequestQueue(request);
    }

    private void updateMessage(String id) {
        final StringRequest request = new StringRequest(Request.Method.POST, Config.UPDATE_MESSAGE.replace(":id", id), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        Log.d("MessagingService", "Message marked as read from server-side.");
                    } else {}
                }
                catch (JSONException ex) {
                    Log.e("FirebaseService", "Unexpected error: " + ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("FirebaseService", "Unexpected error: " + error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }
        };
        int socketTimeout = 0;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        request.setRetryPolicy(policy);
        AppHandler.getInstance().addToRequestQueue(request);
    }

    private void broadcastNotification(Notification n, boolean isCustom, String title) {
        if (AppHandler.isAppIsInBackground(getApplicationContext())) {
            Bitmap bitmap = getBitmapFromURL(n.getIcon());

            Intent intent = new Intent();

            if (n.getAction().equals(String.valueOf(PUSH_TYPE_MESSAGE))) {
                if (isCustom) {
                    intent.setClass(getApplicationContext(), Messages.class);
                } else {
                    intent.putExtra("username", n.getUsername());
                    intent.putExtra("name", n.getName());
                    intent.setClass(getApplicationContext(), ChatActivity.class);
                }
            }

            if (n.getAction().equals(String.valueOf(PUSH_TYPE_NOTIFICATION))) {
                if (isCustom) {
                    intent.putExtra("isNotification", true);
                    intent.setClass(getApplicationContext(), MainActivity.class);
                } else {
                    if (!n.getPostId().equals("0")) {
                        intent.putExtra("username", n.getUsername());
                        intent.putExtra("name", n.getUsername());
                        intent.putExtra("postId", n.getPostId());
                        intent.setClass(getApplicationContext(), ActivityPost.class);
                    } else {
                        intent.putExtra("username", n.getUsername());
                        intent.putExtra("name", n.getUsername());
                        intent.setClass(getApplicationContext(), UserProfile.class);
                    }
                }
            }

            if (n.getAction().equals(String.valueOf(PUSH_TYPE_REPLY))) {
                if (isCustom) {
                    intent.putExtra("isNotification", true);
                    intent.setClass(getApplicationContext(), MainActivity.class);
                } else {
                    Log.d("Messaging", "comment id: " + n.getPostId());
                    intent.putExtra("postId", n.getPostId());
                    intent.putExtra("commentId", n.getCommentId());
                    intent.putExtra("isDisabled", false);
                    intent.putExtra("isOwnPost", false);
                    intent.putExtra("isReply", true);
                    intent.setClass(getApplicationContext(), Comments.class);
                }
            }

            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            final PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            final Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            android.app.Notification notif = new android.app.Notification.Builder(this)
                    .setContentIntent(resultPendingIntent)
                    .setContentTitle(title)
                    .setAutoCancel(true)
                    .setContentText(n.getData())
                    .setSound(notificationSound)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(bitmap)
                    .build();
            notificationManager.notify(n.getId() != null ? Integer.valueOf(n.getId()) : 0, notif);
        } else {
            Intent notificationIntent = new Intent("notification");
            notificationIntent.putExtra("isCustom", isCustom);
            if (!isCustom) {
                notificationIntent.putExtra("id", n.getId());
                notificationIntent.putExtra("postId", n.getPostId());
                notificationIntent.putExtra("messageData", n.getData());
                notificationIntent.putExtra("commentId", n.getCommentId());
                notificationIntent.putExtra("userId", n.getUser_id());
                notificationIntent.putExtra("action", n.getAction());
                notificationIntent.putExtra("username", n.getUsername());
                if (n.getName() != null) notificationIntent.putExtra("name", n.getName());
            } else {
                notificationIntent.putExtra("action", n.getAction());
                notificationIntent.putExtra("messageData", n.getData());
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(notificationIntent);
        }
    }

    public Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
