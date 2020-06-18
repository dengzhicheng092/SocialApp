package innovativedeveloper.com.socialapp;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.volokh.danylo.hashtaghelper.HashTagHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.services.AppService;

public class UpdatePost extends AppCompatActivity implements Button.OnClickListener, AppService.OnImageUploadStatusChanged {

    View audienceLayout;
    Toolbar toolbar;
    ProgressDialog progressDialog;
    EditText txtDescription;
    TextView txtAudience;
    int audience, type;
    String content;
    private String selectedPath;
    Uri imagePath;
    private static final int SELECT_IMAGE = 1;
    private static final int SELECT_VIDEO = 2;
    Bitmap bitmap;
    Button btnUpload;
    ImageView preview_photo;
    ProgressDialog alertDialogLoader;
    boolean isCancelEnable = false;
    HashTagHelper hashTagHelper;
    private AdView mAdView;
    boolean isPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_post);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mAdView = (AdView) findViewById(R.id.adView);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        txtDescription = (EditText) findViewById(R.id.txtDescription);
        preview_photo = (ImageView) findViewById(R.id.photo);
        txtAudience = (TextView) findViewById(R.id.txtAudience);
        txtDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                invalidateOptionsMenu();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        audienceLayout = findViewById(R.id.audienceLayout);
        btnUpload = (Button) findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(this);
        audienceLayout.setOnClickListener(this);
        progressDialog = new ProgressDialog(this, R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage("Uploading...");
        hashTagHelper = HashTagHelper.Creator.create(getResources().getColor(R.color.colorPrimary), null);
        hashTagHelper.handle(txtDescription);

        content = "";
        type = 0;
        setAudience(0);

        if (Config.ENABLE_ACTIVITY_UPLOAD_BANNER) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            mAdView.setVisibility(View.VISIBLE);
        }
        requestPermissions();
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.GET_TASKS)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            isPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.INTERNET, android.Manifest.permission.GET_TASKS,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isPermissionGranted = true;
            } else {
                Toast.makeText(getApplicationContext(), "Permission denied for certain usages within the app.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnUpload) {
            if (isCancelEnable) {
                if (bitmap != null) {
                    bitmap.recycle();
                    bitmap = null;
                }
                content = "";
                preview_photo.setVisibility(View.GONE);
                updateUploadCancellation(false);
                type = 0;
                return;
            }
            if (!isPermissionGranted) {
                requestPermissions();
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Upload?");
            String[] uploadType = {"Image", "Video"};
            builder.setItems(uploadType, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: choosePicture(); break;
                        case 1: chooseVideo(); break;
                    }
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else if (v.getId() == R.id.audienceLayout) {
            final CharSequence[] items = {" Friends Only ", " Friends and followers ", " Public "};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Audience");
            builder.setSingleChoiceItems(items, audience, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch(item)
                    {
                        case 0:
                            setAudience(0);
                            break;
                        case 1:
                            setAudience(1);
                            break;
                        case 2:
                            setAudience(2);
                            break;
                    }
                    dialog.dismiss();
                }
            }).show();
        }
    }

    private void updateUploadCancellation(boolean c) {
        isCancelEnable = c;
        if (c) {
            btnUpload.setText("Cancel");
        } else {
            btnUpload.setText("Upload Video/Photo");
        }
    }

    private void setAudience(int a) {
        audience = a;
        if (a == 0) {
            txtAudience.setText("Friends Only");
        } else if (a == 1) {
            txtAudience.setText("Friends and followers");
        } else {
            txtAudience.setText("Public");
        }
    }

    void updatePost(final String description, final String content) {
        progressDialog.show();
        StringRequest request = new StringRequest(Request.Method.POST, Config.ADD_POST, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressDialog.dismiss();
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        Toast.makeText(UpdatePost.this, "Successfully posted.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(UpdatePost.this, "Unable to upload your post. Please restart this app.", Toast.LENGTH_SHORT).show();
                    }
                }
                catch (JSONException ex) {
                    Log.e("UpdatePost", "Unexpected error: " + ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                Log.e("UpdatePost", "Unexpected error: " + error.getMessage());
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("description", description);
                params.put("content", content);
                params.put("audience", String.valueOf(audience));
               params.put("type", String.valueOf(type));
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        menu.findItem(R.id.action_save).setEnabled(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            updatePost(txtDescription.getText().toString(), content);
        }
        return super.onOptionsItemSelected(item);
    }

    private void chooseVideo() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a Video"), SELECT_VIDEO);
    }

    private void choosePicture() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_IMAGE);
    }

    public String getStringImage(Bitmap bmp){
        ByteArrayOutputStream bAos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bAos);
        byte[] imageBytes = bAos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_VIDEO) {
                Uri selectedVideoUri = data.getData();
                selectedPath = getPath(selectedVideoUri);
                if (selectedPath != null) {
                    Log.d("UpdatePOst", selectedPath.toString());
                    uploadVideo();
                }
            } else if (requestCode == SELECT_IMAGE) {
                imagePath = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imagePath);
                    final int maxSize = 960;
                    int outWidth;
                    int outHeight;
                    int inWidth = bitmap.getWidth();
                    int inHeight = bitmap.getHeight();
                    if(inWidth > inHeight){
                        outWidth = maxSize;
                        outHeight = (inHeight * maxSize) / inWidth;
                    } else {
                        outHeight = maxSize;
                        outWidth = (inWidth * maxSize) / inHeight;
                    }
                    bitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false);
                    uploadPicture();
                    updateUploadCancellation(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getPath(Uri uri) {
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            String document_id = cursor.getString(0);
            document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
            cursor.close();

            cursor = getContentResolver().query(
                    android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
            cursor.moveToFirst();
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
            cursor.close();

            return path;
        }
        catch (Exception ex) {
            Toast.makeText(this, "Unknown error", Toast.LENGTH_SHORT).show();
            Log.d("UpdatePost", "error: " + ex.getMessage());
            return null;
        }
    }

    private void loadPhoto() {
        preview_photo.setScaleX(0);
        preview_photo.setScaleY(0);
        Picasso.with(this)
                .load(imagePath)
                .into(preview_photo, new Callback() {
                    @Override
                    public void onSuccess() {
                        preview_photo.animate()
                                .scaleX(1.f).scaleY(1.f)
                                .setInterpolator(new OvershootInterpolator())
                                .setDuration(400)
                                .setStartDelay(200)
                                .start();
                    }

                    @Override
                    public void onError() {
                    }
                });
    }

    private void uploadPicture() {
        alertDialogLoader = new ProgressDialog(UpdatePost.this);
        AppHandler.getInstance().getAppService().setOnImageUploadStatusChanged(this);
        AppHandler.getInstance().getAppService().uploadImage("image", getStringImage(bitmap));
        alertDialogLoader.setTitle("Uploading Image");
        alertDialogLoader.setMessage("Please wait...");
        alertDialogLoader.setCancelable(false);
        alertDialogLoader.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cancelUploadPicture();
                Toast.makeText(UpdatePost.this, "Uploading cancelled.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        alertDialogLoader.show();
    }

    private void cancelUploadPicture() {
        AppHandler.getInstance().cancelPendingRequests("image");
        Toast.makeText(this, "Uploading cancel", Toast.LENGTH_SHORT).show();
        bitmap.recycle();
        alertDialogLoader.dismiss();
        updateUploadCancellation(false);
    }

    @Override
    public void onImageUploadStatusChanged(JSONObject obj) {
        alertDialogLoader.dismiss();
        try {
            if (!obj.getBoolean("error")) {
                content = obj.getString("image_path");
                type = 1;
                preview_photo.setVisibility(View.VISIBLE);
                loadPhoto();
            } else {
                preview_photo.setVisibility(View.GONE);
            }
        } catch (JSONException ex) {
            Log.e(AppHandler.TAG, ex.getMessage());
        }
    }

    ProgressDialog uploading;
    private void uploadVideo() {
        class UploadVideo extends AsyncTask<Void, Void, String> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                uploading = new ProgressDialog(UpdatePost.this);
                uploading.setTitle("Uploading Video");
                uploading.setMessage("Please wait...");
                uploading.setCancelable(false);
                uploading.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UploadVideo.this.cancel(true);
                        Toast.makeText(UpdatePost.this, "Uploading cancelled.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
                uploading.show();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                uploading.dismiss();
                if (!s.equals("400") || !s.equals("201")) {
                    if (s.equals("")) {
                        Toast.makeText(UpdatePost.this, "Unable to upload this video.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    content = s;
                    type = 2;
                    updateUploadCancellation(true);
                } else {
                    Toast.makeText(UpdatePost.this, "Unable to upload this video.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            protected String doInBackground(Void... params) {
                return AppHandler.getInstance().getAppService().uploadVideo(selectedPath);
            }
        }
        UploadVideo uv = new UploadVideo();
        uv.execute();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (txtDescription.getText().length() > 5 || bitmap != null || type == 2) {
            menu.findItem(R.id.action_save).setEnabled(true);
        } else {
            menu.findItem(R.id.action_save).setEnabled(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
