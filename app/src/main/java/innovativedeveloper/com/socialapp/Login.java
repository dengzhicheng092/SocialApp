package innovativedeveloper.com.socialapp;

import android.*;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.config.DataStorage;

public class Login extends AppCompatActivity {

    Button btnLogin;
    Button btnRegister;
    EditText txtUsername;
    EditText txtPassword;
    ProgressBar progressBar;
    TextInputLayout layout_username;
    TextInputLayout layout_password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        txtUsername = (EditText) findViewById(R.id.txtEmail);
        txtPassword = (EditText) findViewById(R.id.txtPassword);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), Register.class));
                finish();
            }
        });
        layout_username = (TextInputLayout) findViewById(R.id.input_layout_email);
        layout_password = (TextInputLayout) findViewById(R.id.input_layout_password);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validate()) {
                    changeControlState(true);
                    Login();
                }
            }
        });
        requestPermissions();
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.GET_TASKS)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.INTERNET, android.Manifest.permission.GET_TASKS,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    private void Login() {
        final String username = txtUsername.getText().toString().trim();
        final String password = txtPassword.getText().toString().trim();
        StringRequest request = new StringRequest(Request.Method.POST, Config.LOGIN, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                changeControlState(false);
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        JSONObject accountObj = obj.getJSONObject("account");
                        DataStorage d = AppHandler.getInstance().getDataManager();
                        d.setString("id", accountObj.getString("id"));
                        d.setString("username", accountObj.getString("username"));
                        d.setString("name", accountObj.getString("name"));
                        d.setString("email", accountObj.getString("email"));
                        d.setString("api", obj.getString("api"));
                        d.setString("profilePhoto", accountObj.getString("profilePhoto"));
                        d.setString("description", accountObj.getString("description"));
                        d.setString("created_At", accountObj.getString("created_At"));
                        d.setString("location", accountObj.getString("location"));
                        d.setInt("isVerified", accountObj.getInt("isVerified"));
                        d.setInt("gender", accountObj.getInt("gender"));
                        d.setInt("relationship", accountObj.getInt("relationship"));

                        MainActivity.startActivity(Login.this);
                        finish();
                    } else {
                        int code = obj.getInt("code");
                        if (code == Config.USER_INVALID) {
                            layout_username.setError("Username or email address is not valid.");
                            txtUsername.requestFocus();
                        } else if (code == Config.PASSWORD_INCORRECT) {
                            layout_password.setError("Password incorrect.");
                            txtPassword.requestFocus();
                        } else if (code == Config.ACCOUNT_DISABLED) {
                            new AlertDialog.Builder(Login.this)
                                    .setTitle("Account Disabled")
                                    .setMessage("Your account is disabled/suspended for %reason.".replace("%reason", obj.getString("reason")))
                                    .setPositiveButton("OKAY", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    }).show();
                        } else if (code == Config.UNKNOWN_ERROR) {
                            Toast.makeText(Login.this, "There is an unknown server error while making contact.", Toast.LENGTH_SHORT).show();
                            Log.e("Login", "Unknown error returned by server.");
                        }
                    }
                } catch (JSONException ex) {
                    Toast.makeText(Login.this, "There is an unknown server error while making contact.", Toast.LENGTH_SHORT).show();
                    Log.e("Login", "Error:" + ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                changeControlState(false);
                Toast.makeText(getApplicationContext(), "Unable to connect to server.", Toast.LENGTH_SHORT).show();
                Log.e("Login", ""+error.getMessage());
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("username", username.trim().toLowerCase());
                params.put("password", password);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return super.getHeaders();
            }
        };
        AppHandler.getInstance().addToRequestQueue(request);
    }

    public boolean validate() {
        boolean valid = true;
        String strUsername = txtUsername.getText().toString();
        String strPassword = txtPassword.getText().toString();
        if (strUsername.isEmpty()) {
            layout_username.setError("Please enter a valid username.");
            layout_username.setErrorEnabled(true);
            valid = false;
        } else {
            layout_username.setError(null);
        }
        if (strPassword.isEmpty() || strPassword.length() < 8) {
            layout_password.setError("Password incorrect.");
            layout_password.setErrorEnabled(true);
            valid = false;
        } else {
            layout_password.setError(null);
        }
        return valid;
    }

    private void changeControlState(boolean v) {
        if (v) {
            txtUsername.setEnabled(false);
            txtPassword.setEnabled(false);
            btnLogin.setVisibility(View.GONE);
            btnRegister.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            txtUsername.setEnabled(true);
            txtPassword.setEnabled(true);
            btnLogin.setVisibility(View.VISIBLE);
            btnRegister.setEnabled(true);
            progressBar.setVisibility(View.GONE);
        }
    }
}
