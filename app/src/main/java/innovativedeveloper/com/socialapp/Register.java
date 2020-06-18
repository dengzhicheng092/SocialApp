package innovativedeveloper.com.socialapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.config.DataStorage;

public class Register extends AppCompatActivity {

    EditText txtName, txtUsername, txtEmail, txtPassword;
    Button btnRegister, btnLogin;
    TextInputLayout layout_username,layout_password, layout_email, layout_name;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        txtName = (EditText) findViewById(R.id.txtName);
        txtUsername  = (EditText) findViewById(R.id.txtUsername);
        txtEmail = (EditText) findViewById(R.id.txtEmail);
        txtPassword = (EditText) findViewById(R.id.txtPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        layout_username = (TextInputLayout) findViewById(R.id.input_layout_username);
        layout_password = (TextInputLayout) findViewById(R.id.input_layout_password);
        layout_email = (TextInputLayout) findViewById(R.id.input_layout_email);
        layout_name = (TextInputLayout) findViewById(R.id.input_layout_name);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Login.class));
                finish();
            }
        });
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validate()) {
                    changeControlState(true);
                    Register();
                }
            }
        });
    }

    private void Register() {
        final String name = txtName.getText().toString().trim();
        final String username = txtUsername.getText().toString().trim();
        final String email = txtEmail.getText().toString().trim();
        final String password = txtPassword.getText().toString().trim();
        StringRequest request = new StringRequest(Request.Method.POST, Config.REGISTER, new Response.Listener<String>() {
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
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                    } else {
                        int code = obj.getInt("code");
                        if (code == Config.USER_ALREADY_EXISTS) {
                            layout_username.setError("Username already exists. Please type a different one.");
                            layout_username.requestFocus();
                        } else if (code == Config.EMAIL_ALREADY_EXISTS) {
                            layout_email.setError("Email address already exists.");
                            layout_email.requestFocus();
                        } else if (code == Config.UNKNOWN_ERROR) {
                            Toast.makeText(Register.this, "Unknown error occurred while contacting server.", Toast.LENGTH_SHORT).show();
                            Log.e("Register", "Unknown error returned by server.");
                        }
                    }
                }
                catch (JSONException ex) {
                    Log.e("Register", "Error:" + ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                changeControlState(false);
                Toast.makeText(getApplicationContext(), "Unable to connect to server.", Toast.LENGTH_SHORT).show();
                Log.e(AppHandler.TAG, "" + error.getMessage());
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("name", name);
                params.put("username", username.trim().toLowerCase());
                params.put("email", email.trim().toLowerCase());
                params.put("password", password);
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

    public boolean validate() {
        boolean valid = true;
        String strName = txtPassword.getText().toString();
        String strUsername = txtUsername.getText().toString();
        String strPassword = txtPassword.getText().toString();
        String strEmail = txtPassword.getText().toString();
        if (strName.isEmpty()) {
            layout_name.setError("Please enter your real name here.");
            valid = false;
        } else {
            layout_name.setError(null);
        }
        if (strUsername.isEmpty() || strUsername.contains(" ")) {
            layout_username.setError("Please enter a valid username.");
            valid = false;
        } else {
            layout_username.setError(null);
        }
        if (strPassword.isEmpty() || strPassword.length() < 8) {
            layout_password.setError("Password must be 8 characters long.");
            valid = false;
        } else {
            layout_password.setError(null);
        }
        if (strEmail.isEmpty() || strEmail.contains(" ")) {
            layout_email.setError("Please enter a valid email.");
            valid = false;
        } else {
            layout_email.setError(null);
        }
        return valid;
    }

    private void changeControlState(boolean v) {
        if (v) {
            txtUsername.setEnabled(false);
            txtPassword.setEnabled(false);
            txtEmail.setEnabled(false);
            txtName.setEnabled(false);
            btnRegister.setVisibility(View.GONE);
            btnLogin.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            txtUsername.setEnabled(true);
            txtPassword.setEnabled(true);
            txtEmail.setEnabled(true);
            txtName.setEnabled(true);
            btnRegister.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(true);
            progressBar.setVisibility(View.GONE);
        }
    }
}
