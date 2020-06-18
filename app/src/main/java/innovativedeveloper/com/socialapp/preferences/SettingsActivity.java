package innovativedeveloper.com.socialapp.preferences;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.google.firebase.iid.FirebaseInstanceId;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import innovativedeveloper.com.socialapp.BlockList;
import innovativedeveloper.com.socialapp.MainActivity;
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.AppHelper;
import innovativedeveloper.com.socialapp.config.Config;
import jp.wasabeef.blurry.Blurry;
import jp.wasabeef.blurry.internal.Blur;

public class SettingsActivity extends AppCompatActivity implements Button.OnClickListener {

    Toolbar toolbar;
    EditText txtUsername;
    EditText txtEmail;
    TextView txtRelation, txtGender, txtLocation, txtBio, txtName, txtPassword;
    Button btnLogout, btnDeactivate, btnBlockList;
    CircleImageView icon, preview_icon;
    ProgressDialog progressDialog;
    Bitmap bitmap;
    Uri photoUri;
    View previewLayout;
    ImageView btnDone, btnCancel;
    int relationship, gender;
    String location, about, profileName, password = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        txtName = (TextView) findViewById(R.id.txtName);
        txtUsername = (EditText) findViewById(R.id.txtUsername);
        txtEmail = (EditText) findViewById(R.id.txtEmail);
        txtPassword = (TextView) findViewById(R.id.txtPassword);
        txtBio = (TextView) findViewById(R.id.txtBio);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnDeactivate = (Button) findViewById(R.id.btnDeactivate);
        btnBlockList = (Button) findViewById(R.id.btnBlockedList);
        icon = (CircleImageView) findViewById(R.id.icon);
        preview_icon = (CircleImageView) findViewById(R.id.preview_image);
        previewLayout = findViewById(R.id.preview_layout);
        progressDialog = new ProgressDialog(this, R.style.AppTheme_Dark_Dialog);
        txtLocation = (TextView) findViewById(R.id.txtLocation);
        btnDone = (ImageView) findViewById(R.id.btnDone);
        btnCancel = (ImageView) findViewById(R.id.btnCancel);
        txtRelation = (TextView) findViewById(R.id.txtRelationship);
        txtGender = (TextView) findViewById(R.id.txtGender);
        btnDone.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        loadSettings();
    }

    @TargetApi(23)
    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.GET_TASKS)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            showFileChooser();
        } else {
            requestPermissions(new String[]{android.Manifest.permission.INTERNET, android.Manifest.permission.GET_TASKS, android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showFileChooser();
            } else {
                Toast.makeText(getApplicationContext(), "Permission denied for certain usages within the app.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void loadSettings() {
        setAbout(AppHandler.getInstance().getUser().getDescription());
        txtUsername.setText(AppHandler.getInstance().getUser().getUsername());
        txtEmail.setText(AppHandler.getInstance().getUser().getEmail());
        setProfileName(AppHandler.getInstance().getUser().getName());
        setRelationship(AppHandler.getInstance().getUser().getRelationship());
        setLocation(AppHandler.getInstance().getUser().getLocation());
        setGender(AppHandler.getInstance().getUser().getGender());
        Picasso.with(this).load(AppHandler.getInstance().getUser().getProfilePhoto()).placeholder(R.drawable.ic_people).into(icon);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setMessage("Updating...");
            progressDialog.show();
            StringRequest request = new StringRequest(Request.Method.POST, Config.UPDATE_SETTINGS, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    progressDialog.dismiss();
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (!obj.getBoolean("error")) {
                            if (obj.has("name")) AppHandler.getInstance().getDataManager().setString("name", obj.getString("name"));
                            if (obj.has("description")) AppHandler.getInstance().getDataManager().setString("description", obj.getString("description"));
                            if (obj.has("location")) AppHandler.getInstance().getDataManager().setString("location", obj.getString("location"));
                            if (obj.has("relationship")) AppHandler.getInstance().getDataManager().setInt("relationship", obj.getInt("relationship"));
                            if (obj.has("gender")) AppHandler.getInstance().getDataManager().setInt("gender", obj.getInt("gender"));
                            Toast.makeText(SettingsActivity.this, "Settings updated.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SettingsActivity.this, "Unable to save your settings. Please restart the app.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException ex) {
                        Log.e("Settings", "JSON Parse error: " + ex.getMessage() + "\nResponse: " + response);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    progressDialog.dismiss();
                    Log.e("Settings", "Error: " + error.getMessage());
                    Toast.makeText(SettingsActivity.this, "Unable to update your settings. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    return AppHandler.getInstance().getAuthorization();
                }

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    if (!profileName.equals(AppHandler.getInstance().getUser().getName())) {
                        params.put("name", profileName);
                    }
                    if (!about.equals(AppHandler.getInstance().getUser().getDescription())) {
                        params.put("about", about);
                    }
                    if (!password.equals("")) {
                        params.put("password", password);
                    }
                    if (relationship != AppHandler.getInstance().getUser().getRelationship()) {
                        params.put("relationship", String.valueOf(relationship));
                    }
                    if (gender != AppHandler.getInstance().getUser().getGender()) {
                        params.put("gender", String.valueOf(gender));
                    }
                    if (!location.equals(AppHandler.getInstance().getUser().getLocation())) {
                        params.put("location", location);
                    }
                    Log.d("Settings", params.toString());
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnDone: {
                uploadProfilePhoto();
                break;
            }
            case R.id.btnCancel: {
                previewLayout.setVisibility(View.GONE);
                bitmap = null;
                break;
            }
        }
    }

    public void actionClick(final View view) {
        switch (view.getId()){
            case R.id.btnLogout: {
                new AlertDialog.Builder(SettingsActivity.this, R.style.AppTheme_Dark_Dialog)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout from this account?")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AppHandler.getInstance().getDBHandler().resetDatabase();
                                AppHandler.getInstance().getDataManager().clear();
                                Intent loginIntent = new Intent(getApplicationContext(), AppHelper.class);
                                loginIntent.putExtra("isLogin", true);
                                startActivity(loginIntent);
                                Toast.makeText(SettingsActivity.this, "Session expired.", Toast.LENGTH_SHORT).show();
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
                }).show();
                break;
            }
            case R.id.btnBlockedList: {
                startActivity(new Intent(SettingsActivity.this, BlockList.class));
                break;
            }
            case R.id.btnDeactivate: {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Delete your account?")
                        .setMessage("Are you sure you want to delete your account? Please note that this action cannot be undone and all your data (including photos, comments, likes) will be deleted.")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                progressDialog.setIndeterminate(true);
                                progressDialog.setCancelable(false);
                                progressDialog.setCanceledOnTouchOutside(false);
                                progressDialog.setMessage("Deleting your account...");
                                progressDialog.show();
                                StringRequest request = new StringRequest(Request.Method.POST, Config.DELETE_ACCOUNT, new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        progressDialog.dismiss();
                                        try {
                                            JSONObject obj = new JSONObject(response);
                                            if (!obj.getBoolean("error")) {
                                                Toast.makeText(SettingsActivity.this, "Your account is successfully deleted.", Toast.LENGTH_LONG).show();
                                                AppHandler.getInstance().getDBHandler().resetDatabase();
                                                AppHandler.getInstance().getDataManager().clear();
                                                startActivity(new Intent(SettingsActivity.this, AppHelper.class));
                                                Toast.makeText(SettingsActivity.this, "Session expired.", Toast.LENGTH_SHORT).show();
                                                try {
                                                    FirebaseInstanceId.getInstance().deleteInstanceId();
                                                } catch (IOException ex) {
                                                    Log.e("MainActivity", "Unable to delete instance id.");
                                                }
                                            } else {
                                                Log.e("Settings", "Server response: " + obj.getString("code"));
                                                Toast.makeText(SettingsActivity.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                            }
                                        } catch (JSONException ex) {
                                            Log.e("Settings", "Unexpected error: " + ex.getMessage());
                                            Toast.makeText(SettingsActivity.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        progressDialog.dismiss();
                                        Log.e("Settings", "Unexpected error: " + error.getMessage());
                                        Toast.makeText(SettingsActivity.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
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
                        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
                break;
            }
            case R.id.icon: {
                requestPermissions();
                break;
            }
            case R.id.itemRelationship: {
                final CharSequence[] items = {" Single ", " In a relationship ", " Married ", " Engaged "};
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Relationship status");
                builder.setSingleChoiceItems(items, getRelationship(), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        switch(item)
                        {
                            case 0:
                                setRelationship(1);
                                break;
                            case 1:
                                setRelationship(2);
                                break;
                            case 2:
                                setRelationship(3);
                                break;
                            case 3:
                                setRelationship(4);
                                break;
                        }
                        dialog.dismiss();
                    }
                }).show();
                break;
            }
            case R.id.itemGender: {
                final CharSequence[] items = {" Male ", " Female "};
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Gender");
                builder.setSingleChoiceItems(items, getGender(), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        switch(item)
                        {
                            case 0:
                                setGender(1);
                                break;
                            case 1:
                                setGender(2);
                                break;
                        }
                        dialog.dismiss();
                    }
                }).show();
                break;
            }
            case R.id.itemLocation: {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                LayoutInflater inflater = this.getLayoutInflater();
                final View dialogView = inflater.inflate(R.layout.custom_dialog, null);
                dialogBuilder.setView(dialogView);
                final EditText editText = (EditText) dialogView.findViewById(R.id.editText);
                editText.setText(location);
                editText.setSelection(location.length());
                dialogBuilder.setTitle("Location");
                dialogBuilder.setMessage("Enter your location below:");
                dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        setLocation(editText.getText().toString());
                        dialog.dismiss();
                    }
                });
                dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
                AlertDialog b = dialogBuilder.create();
                b.show();
                break;
            }
            case R.id.itemAbout: {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                LayoutInflater inflater = this.getLayoutInflater();
                final View dialogView = inflater.inflate(R.layout.custom_dialog, null);
                dialogBuilder.setView(dialogView);
                final EditText editText = (EditText) dialogView.findViewById(R.id.editText);
                editText.setText(about);
                editText.setSelection(about.length());
                dialogBuilder.setTitle("About");
                dialogBuilder.setMessage("Enter your about below:");
                dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        setAbout(editText.getText().toString());
                    }
                });
                dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
                AlertDialog b = dialogBuilder.create();
                b.show();
                break;
            }
            case R.id.itemName: {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                LayoutInflater inflater = this.getLayoutInflater();
                final View dialogView = inflater.inflate(R.layout.custom_dialog, null);
                dialogBuilder.setView(dialogView);
                final EditText editText = (EditText) dialogView.findViewById(R.id.editText);
                editText.setText(profileName);
                editText.setSelection(profileName.length());
                dialogBuilder.setTitle("Profile name");
                dialogBuilder.setMessage("Enter your profile name below:");
                dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        setProfileName(editText.getText().toString());
                    }
                });
                dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
                AlertDialog b = dialogBuilder.create();
                b.show();
                break;
            }
            case R.id.itemPassword: {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                LayoutInflater inflater = this.getLayoutInflater();
                final View dialogView = inflater.inflate(R.layout.custom_dialog, null);
                dialogBuilder.setView(dialogView);
                final EditText editText = (EditText) dialogView.findViewById(R.id.editText);
                editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                dialogBuilder.setTitle("Password");
                dialogBuilder.setMessage("Enter your new password below:");
                dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (editText.length() > 6) {
                            setPassword(editText.getText().toString());
                        } else {
                            Toast.makeText(SettingsActivity.this, "Minimum password length should be 6", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
                AlertDialog b = dialogBuilder.create();
                b.show();
                break;
            }
        }
    }

    private void setGender(int g) {
        gender = g;
        if (g == 2) {
            txtGender.setText(getResources().getString(R.string.gender_female));
        } else {
            txtGender.setText(getResources().getString(R.string.gender_male));
        }
    }

    private void setRelationship(int r) {
        relationship = r;
        if (r == 2) {
            txtRelation.setText(getResources().getString(R.string.relation_in));
        } else if (r == 3) {
            txtRelation.setText(getResources().getString(R.string.relation_married));
        } else if (r == 4) {
            txtRelation.setText(getResources().getString(R.string.relation_engaged));
        } else {
            txtRelation.setText(getResources().getString(R.string.relation_single));
        }
    }

    private int getRelationship() {
        if (relationship == 2) {
            return 1;
        } else if (relationship == 3) {
            return 2;
        } else if (relationship == 4) {
            return 3;
        } else {
            return 0;
        }
    }

    private int getGender() {
        if (gender == 2) {
            return 1;
        } else if (gender == 1) {
            return 0;
        } else {
            return -1;
        }
    }

    private void setLocation(String l) {
        location = l;
        txtLocation.setText(l);
    }

    private void setAbout(String a) {
        about = a;
        txtBio.setText(a);
    }

    private void setProfileName(String name) {
        profileName = name;
        txtName.setText(name);
    }

    private void setPassword(String p) {
        password = p;
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri filePath = data.getData();
            photoUri = filePath;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                showPreview();
                invalidateOptionsMenu();
            }
            catch (IOException ex) {
                finish();
            }
        }
    }

    void showPreview() {
        preview_icon.setImageBitmap(bitmap);
        previewLayout.setVisibility(View.VISIBLE);
    }

    public String getStringImage(Bitmap bmp){
        ByteArrayOutputStream bAos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bAos);
        byte[] imageBytes = bAos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    void uploadProfilePhoto() {
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Uploading photo...");
        progressDialog.show();
        StringRequest request = new StringRequest(Request.Method.POST, Config.PROFILE_PHOTO_UPLOAD, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                previewLayout.setVisibility(View.GONE);
                bitmap = null;
                progressDialog.dismiss();
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        AppHandler.getInstance().getDataManager().setString("profilePhoto", obj.getString("profilePhoto"));
                        loadSettings();
                    } else {
                        Toast.makeText(SettingsActivity.this, "Unable to set your profile picture.", Toast.LENGTH_SHORT).show();
                    }
                }
                catch (JSONException ex) {
                    Log.e("SettingsActivity", "Error: " + ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                previewLayout.setVisibility(View.GONE);
                bitmap = null;
                progressDialog.dismiss();
                Log.e("SettingsActivity", "Error: " + error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("image", getStringImage(bitmap));
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
