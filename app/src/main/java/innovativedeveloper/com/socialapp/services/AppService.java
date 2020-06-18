package innovativedeveloper.com.socialapp.services;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import innovativedeveloper.com.socialapp.BuildConfig;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.AppHelper;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.Comment;
import innovativedeveloper.com.socialapp.dataset.Message;
import innovativedeveloper.com.socialapp.dataset.User;

import static innovativedeveloper.com.socialapp.config.Config.SESSION_EXPIRED;
import static innovativedeveloper.com.socialapp.config.Config.UNKNOWN_ERROR;
import static innovativedeveloper.com.socialapp.config.Config.UPLOAD_URI;

public class AppService {
    private OnServiceChanged onServiceChanged;
    private OnMessagingStatusChanged onMessageStatusChanged;
    private OnImageUploadStatusChanged onImageUploadStatusChanged;
    private OnCommentStatusChanged onCommentStatusChanged;
    private OnLikeChanged onLikeChanged;
    private boolean isAuthorized = false;
    private int serverResponseCode;

    public boolean IsAuthorized() {
        return isAuthorized;
    }

    public void setOnServiceChanged(OnServiceChanged onServiceChanged) {
        this.onServiceChanged = onServiceChanged;
    }

    public void setOnMessagingStatusChanged(OnMessagingStatusChanged onMessageStatusChanged) {
        this.onMessageStatusChanged = onMessageStatusChanged;
    }

    public void setOnImageUploadStatusChanged(OnImageUploadStatusChanged onImageUploadStatusChanged) {
        this.onImageUploadStatusChanged = onImageUploadStatusChanged;
    }

    public void setOnCommentStatusChanged(OnCommentStatusChanged onCommentStatusChanged) {
        this.onCommentStatusChanged = onCommentStatusChanged;
    }

    public void setOnLikeChanged(OnLikeChanged onLikeChanged) {
        this.onLikeChanged = onLikeChanged;
    }

    public interface OnServiceChanged {
        void onAuthorizedStatusChanged(JSONObject obj);
    }

    public interface OnMessagingStatusChanged {
        void onMessageStatusChanged(JSONObject obj);
    }

    public interface OnImageUploadStatusChanged {
        void onImageUploadStatusChanged(JSONObject obj);
    }

    public interface OnLikeChanged {
        void onLikeChanged(String response);
        void onLikeErrorResponse(VolleyError error);
    }

    public interface OnCommentStatusChanged {
        void onCommentChanged(String response, Comment c);
        void onCommentErrorResponse(VolleyError error, Comment c);
    }

    public void Authorize() {
        StringRequest request = new StringRequest(Request.Method.POST, Config.AUTHORIZE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        JSONObject user = obj.getJSONObject("user");
                        User u = new User();
                        u.setId(user.getString("id"));
                        u.setUsername(user.getString("username"));
                        u.setEmail(user.getString("email"));
                        u.setName(user.getString("name"));
                        u.setRelationship(user.getInt("relationship"));
                        u.setGender(user.getInt("gender"));
                        u.setVerified(user.getInt("isVerified") == 1);
                        u.setIsDisabled(user.getInt("isDisabled"));
                        u.setLocation(user.getString("location"));
                        u.setProfilePhoto(user.getString("profilePhoto"));
                        u.setDescription(user.getString("description"));
                        u.totalPosts = user.getInt("totalPosts");
                        u.totalPhotos = user.getInt("totalPhotos");
                        u.totalVideos = user.getInt("totalVideos");
                        u.totalFriends = user.getInt("totalFriends");
                        AppHandler.getInstance().updateUser(u);
                        isAuthorized = true;
                        onServiceChanged.onAuthorizedStatusChanged(obj);
                    }
                    else {
                        isAuthorized = false;
                        onServiceChanged.onAuthorizedStatusChanged(obj);
                    }
                }
                catch (JSONException ex) {
                    isAuthorized = false;
                    Log.e("Authorization", "Unexpected error: " + ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isAuthorized = false;
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("error", true);
                    obj.put("code", UNKNOWN_ERROR);
                    onServiceChanged.onAuthorizedStatusChanged(obj);
                } catch (JSONException ex) {
                    Log.e(AppHandler.TAG, ex.getMessage());
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("registration_id", FirebaseInstanceId.getInstance().getToken());
                params.put("version", BuildConfig.VERSION_NAME);
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

    public void updateLike(String id, final int action) {
        StringRequest request = new StringRequest(Request.Method.PUT, Config.UPDATE_LIKE.replace(":id", id), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                AppHandler.getInstance().getAppService().onLikeChanged.onLikeChanged(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                AppHandler.getInstance().getAppService().onLikeChanged.onLikeErrorResponse(error);
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("action", String.valueOf(action));
                return params;
            }

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

    public void sendMessage(final Message m) {
        StringRequest request = new StringRequest(Request.Method.POST, Config.MESSAGE_USER.replace(":id", m.getUsername()), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    onMessageStatusChanged.onMessageStatusChanged(obj);
                } catch (JSONException ex) {
                    Log.e(AppHandler.TAG, ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("AppService", "Unexpected error: " + error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("msg_type", String.valueOf(m.getType()));
                params.put("message", m.getMessage());
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

    public void uploadImage(String tag, final String image) {
        StringRequest request = new StringRequest(Request.Method.POST, Config.IMAGE_UPLOAD_URI, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    AppHandler.getInstance().getAppService().onImageUploadStatusChanged.onImageUploadStatusChanged(obj);
                } catch (JSONException ex) {
                    Log.e(AppHandler.TAG, ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("error", true);
                    AppHandler.getInstance().getAppService().onImageUploadStatusChanged.onImageUploadStatusChanged(obj);
                } catch (JSONException ex) {
                    Log.e(AppHandler.TAG, ex.getMessage());
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("image", image);
                return params;
            }
        };
        int socketTimeout = 0;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        request.setRetryPolicy(policy);
        AppHandler.getInstance().addToRequestQueue(tag, request);
    }

    public String uploadVideo(String file) {

        String fileName = file;
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        File sourceFile = new File(file);
        if (!sourceFile.isFile()) {
            Log.e("AppService", "Source File Does not exist");
            return null;
        }

        try {
            FileInputStream fileInputStream = new FileInputStream(sourceFile);

            URL url = new URL(UPLOAD_URI);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setChunkedStreamingMode(128 * 1024);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCRYPT", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setRequestProperty("myFile", fileName);
            conn.setRequestProperty("fileType", getFileExt(fileName));
            conn.setRequestProperty("Authorization", AppHandler.getInstance().getAuthorization().get("Authorization"));
            dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"myFile\"; filename=\"" + fileName + "\"" + lineEnd);
            dos.writeBytes(lineEnd);

            bytesAvailable = fileInputStream.available();

            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            }

            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            serverResponseCode = conn.getResponseCode();

            fileInputStream.close();
            dos.flush();
            dos.close();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (serverResponseCode == 200) {
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                rd.close();
            } catch (IOException ex) {
                Log.e("AppService", "IOException: " + ex.getMessage());
            }
            return sb.toString();
        } else {
            return String.valueOf(serverResponseCode);
        }
    }

    public static String getFileExt(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."), fileName.length());
    }

    public void addReply(final String comment, final String postId, final Comment c) {
        StringRequest request = new StringRequest(Request.Method.POST, Config.ADD_REPLY.replace(":commentId", c.getCommentId()), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                AppHandler.getInstance().getAppService().onCommentStatusChanged.onCommentChanged(response, c);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                AppHandler.getInstance().getAppService().onCommentStatusChanged.onCommentErrorResponse(error, c);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("reply", comment);
                params.put("postId", postId);
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

    public void addComment(final String comment, String postId, final Comment c) {
        StringRequest request = new StringRequest(Request.Method.POST, Config.ADD_COMMENT.replace(":id", postId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                AppHandler.getInstance().getAppService().onCommentStatusChanged.onCommentChanged(response, c);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                AppHandler.getInstance().getAppService().onCommentStatusChanged.onCommentErrorResponse(error, c);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("comment", comment);
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

    static Bitmap retrieveVideoFrameFromVideo(String videoPath) throws Throwable
    {
        Bitmap bitmap = null;
        MediaMetadataRetriever mediaMetadataRetriever = null;
        try
        {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            if (Build.VERSION.SDK_INT >= 14)
                mediaMetadataRetriever.setDataSource(videoPath, new HashMap<String, String>());
            else
                mediaMetadataRetriever.setDataSource(videoPath);
            bitmap = mediaMetadataRetriever.getFrameAtTime();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Throwable("Exception in retrieveVideoFrameFromVideo(String videoPath)" + e.getMessage());
        } finally {
            if (mediaMetadataRetriever != null) {
                mediaMetadataRetriever.release();
            }
        }
        return bitmap;
    }

    public static void addVideoThumbnail(final String videoPath, final ImageView imageView) {
        class videoThumbnailTask extends AsyncTask<Void, Void, String> {
            Bitmap bitmap;
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                }

                @Override
                protected void onPostExecute(String s) {
                    super.onPostExecute(s);
                    imageView.setImageBitmap(bitmap);
                }

                @Override
                protected String doInBackground(Void... params) {
                    try {
                        bitmap = retrieveVideoFrameFromVideo(videoPath);
                    }
                    catch (Throwable ex) {
                        Log.e("FeedItemAdapter", "error: "  + ex.getMessage());
                    }
                    return "Done";
                }
            }
            videoThumbnailTask vt = new videoThumbnailTask();
            vt.execute();
    }
}
