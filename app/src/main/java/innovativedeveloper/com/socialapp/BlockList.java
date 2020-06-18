package innovativedeveloper.com.socialapp;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

import innovativedeveloper.com.socialapp.adapter.BlockAdapter;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.User;

public class BlockList extends AppCompatActivity implements BlockAdapter.OnClickListener {

    Toolbar toolbar;
    RecyclerView recyclerView;
    ArrayList<User> blockedUsers;
    BlockAdapter blockAdapter;
    ProgressBar progressBar;
    View emptyView;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_list);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        progressDialog = new ProgressDialog(this, R.style.AppTheme_Dark_Dialog);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Setting up likes and adapter
        blockedUsers = new ArrayList<>();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 300;
            }
        };
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        blockAdapter = new BlockAdapter(this, blockedUsers);
        blockAdapter.setOnClickListener(this);
        recyclerView.setAdapter(blockAdapter);
        recyclerView.addItemDecoration(new ItemDivider(this));
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    blockAdapter.setAnimationsLocked(true);
                }
            }
        });

        LoadBlockList();
    }

    @Override
    public void RemoveBlock(final int position) {
        final User u = blockedUsers.get(position);
        new AlertDialog.Builder(BlockList.this)
                .setTitle("Unblock :user?".replace(":user", u.getUsername()))
                .setMessage("Are you sure that you want to unblock :user?".replace(":user", u.getName()))
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        progressDialog.setIndeterminate(true);
                        progressDialog.setCancelable(false);
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.setMessage("Please wait for a moment...");
                        progressDialog.show();
                        StringRequest request = new StringRequest(Request.Method.PUT, Config.UNBLOCK_USER.replace(":id", u.getId()), new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                progressDialog.dismiss();
                                try {
                                    JSONObject obj = new JSONObject(response);
                                    if (!obj.getBoolean("error")) {
                                        Toast.makeText(BlockList.this, ":user is no longer in your block list.".replace(":user", u.getName()), Toast.LENGTH_SHORT).show();
                                        blockedUsers.remove(position);
                                        blockAdapter.notifyItemRemoved(position);
                                        if (blockedUsers.size() == 0) {
                                            emptyView.setVisibility(View.VISIBLE);
                                            recyclerView.setVisibility(View.GONE);
                                            progressBar.setVisibility(View.GONE);
                                        }
                                    } else {
                                        Toast.makeText(BlockList.this, "Unable to remove :user from your block list".replace(":user", u.getName()), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException ex) {
                                    Log.e("BlockList", "JSON Parse error: " + ex.getMessage() + "\nResponse: " + response);
                                }
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                progressDialog.dismiss();
                                Log.e("BlockList", "Error: " + error.getMessage());
                                Toast.makeText(BlockList.this, "Your request was not proceed.", Toast.LENGTH_SHORT).show();
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
                }).show();
    }

    private void LoadBlockList() {
        StringRequest request = new StringRequest(Request.Method.GET, Config.LOAD_BLOCK_LIST, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressBar.setVisibility(View.GONE);
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        JSONArray blockList = obj.getJSONArray("list");
                        if (blockList.length() != 0) {
                            for (int i = 0; i < blockList.length(); i++) {
                                JSONObject user = blockList.getJSONObject(i);
                                User u = new User();
                                u.setId(user.getString("user_id"));
                                u.setName(user.getString("name"));
                                u.setUsername(user.getString("username"));
                                blockedUsers.add(u);
                            }
                            blockAdapter.updateItems();
                        } else {
                            progressBar.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        finish();
                    }
                } catch (JSONException ex) {
                    progressBar.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    Log.e("BlockList", "Unexpected error: " + ex.getMessage());
                    Toast.makeText(BlockList.this, "Couldn't load", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                Log.e("BlockList", "Error: " + error.getMessage());
                Toast.makeText(BlockList.this, "Unable to load block list.", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }
        };
        AppHandler.getInstance().addToRequestQueue(request);
    }
}
