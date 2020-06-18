package innovativedeveloper.com.socialapp;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import innovativedeveloper.com.socialapp.adapter.ExploreGridAdapter;
import innovativedeveloper.com.socialapp.adapter.FeedItemAdapter;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.Feed;

public class MediaActivity extends AppCompatActivity implements ExploreGridAdapter.OnExploreItemClickListener {

    RecyclerView recyclerView;
    ProgressBar progressBar;
    ArrayList<Feed> feedItems;
    ExploreGridAdapter gridAdapter;
    View errorLayout, emptyLayout;
    String username, name;
    Toolbar toolbar;
    boolean isPhotos;
    boolean isVideos;
    AdView mAdView;

    public static void startActivity(Activity startingActivity, String username, String name, boolean isPhotos, boolean isVideos) {
        Intent intent = new Intent(startingActivity, MediaActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("name", name);
        intent.putExtra("isPhotos", isPhotos);
        intent.putExtra("isVideos", isVideos);
        startingActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        mAdView = (AdView) findViewById(R.id.adView);
        username = getIntent().getStringExtra("username");
        name = getIntent().getStringExtra("name");
        isPhotos = getIntent().getBooleanExtra("isPhotos", false);
        isVideos = getIntent().getBooleanExtra("isVideos", false);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        errorLayout = findViewById(R.id.lyt_error);
        emptyLayout = findViewById(R.id.lyt_empty);
        feedItems = new ArrayList<>();
        final StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setPadding(2, 2, 2, 2);
        gridAdapter = new ExploreGridAdapter(this, feedItems);
        recyclerView.setAdapter(gridAdapter);
        gridAdapter.setOnExploreItemClickListener(this);
        loadFeed();
        setTitle(name + "\'s Library");
        initializeAd();
    }

    private void initializeAd() {
        if (Config.ENABLE_ACTIVITY_MEDIA_BANNER) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            mAdView.setVisibility(View.VISIBLE);
        }
    }

    private void loadFeed() {
        errorLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        StringRequest request = new StringRequest(Request.Method.GET, Config.GET_MEDIA_FEED.replace(":id", username) + "?isPhotos=$p&isVideos=$v".replace("$p", isPhotos ? "1" : "0")
                .replace("$v", isVideos ? "2" : "0"), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressBar.setVisibility(View.GONE);
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        JSONArray feeds = obj.getJSONArray("feeds");
                        if (feeds.length() != 0) {
                            for (int i = 0; i < feeds.length(); i++) {
                                JSONObject feed = feeds.getJSONObject(i);
                                JSONObject post = feed.getJSONObject("post");
                                Feed f = new Feed();
                                f.setId(post.getString("postId"));
                                f.setContent(post.getString("content"));
                                f.setType(post.getInt("type"));
                                feedItems.add(f);
                            }
                            gridAdapter.updateItems(true);
                        } else {
                            emptyLayout.setVisibility(View.VISIBLE);
                        }
                    } else {
                        emptyLayout.setVisibility(View.VISIBLE);
                        Toast.makeText(MediaActivity.this, "Unknown error occured.", Toast.LENGTH_SHORT).show();
                    }
                }
                catch (JSONException ex) {
                    Log.e("MediaActivity", "JSONException: " + ex.getMessage());
                    errorLayout.setVisibility(View.VISIBLE);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                errorLayout.setVisibility(View.VISIBLE);
                Log.e("MediaActivity", "Error: " + error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }
        };
        AppHandler.getInstance().addToRequestQueue(request);
    }

    @Override
    public void onItemClick(View v, int position) {
        ActivityPost.startActivityPost(this, username, name, feedItems.get(position).getId());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
