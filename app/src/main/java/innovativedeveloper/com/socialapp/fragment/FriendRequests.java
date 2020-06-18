package innovativedeveloper.com.socialapp.fragment;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
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

import innovativedeveloper.com.socialapp.ItemDivider;
import innovativedeveloper.com.socialapp.MainActivity;
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.UserProfile;
import innovativedeveloper.com.socialapp.adapter.FeedItemAnimator;
import innovativedeveloper.com.socialapp.adapter.RequestAdapter;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.User;

public class FriendRequests extends Fragment implements RequestAdapter.OnItemClickListener {

    RecyclerView recyclerView;
    ProgressBar progressBar;
    View empty_Feed;
    ArrayList<User> requestsList;
    RequestAdapter requestAdapter;
    public FriendRequests() {
    }

    public void refreshRequests() {
        loadRequests();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_friend_requests, container, false);
        recyclerView = (RecyclerView) v.findViewById(R.id.recyclerView);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        empty_Feed = v.findViewById(R.id.empty_Feed);
        requestsList = new ArrayList<>();
        requestAdapter = new RequestAdapter(getContext(), requestsList);
        requestAdapter.setOnItemClickListener(this);
        requestAdapter.setAnimationsLocked(true);
        requestAdapter.setHasStableIds(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity()) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 300;
            }
        };
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(requestAdapter);
        recyclerView.addItemDecoration(new ItemDivider(getActivity()));
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    requestAdapter.setAnimationsLocked(true);
                }
            }
        });
        loadRequests();
        return v;
    }

    @Override
    public void onItemClick(View v, int position) {
        UserProfile.startUserProfile(getActivity(), requestsList.get(position).getUsername(), requestsList.get(position).getName());
    }

    @Override
    public void onAddFriendClick(View v, final int position) {
        StringRequest request = new StringRequest(Request.Method.POST, Config.CONFIRM_FRIEND.replace(":id", requestsList.get(position).getId()), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        requestsList.remove(position);
                        requestAdapter.notifyItemRemoved(position);
                        updateRequests();
                    } else {
                        requestAdapter.notifyItemChanged(position);
                    }
                } catch (JSONException ex) {
                    Log.e("FriendRequests", "Unexpected error: " + ex.getMessage());
                    Toast.makeText(getActivity(), "Unable to process your request.", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                requestAdapter.notifyItemChanged(position);
                Log.e("FriendRequests", "Unexpected error: " + error.getMessage());
                Toast.makeText(getActivity(), "Unable to process your request.", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onRejectClick(View v, final int position) {
        StringRequest request = new StringRequest(Request.Method.POST, Config.REMOVE_FRIEND.replace(":id", requestsList.get(position).getId()), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        requestsList.remove(position);
                        requestAdapter.notifyItemRemoved(position);
                        Toast.makeText(getActivity(), "Request rejected.", Toast.LENGTH_SHORT).show();
                        updateRequests();
                    } else {
                        requestAdapter.notifyItemChanged(position);
                    }
                } catch (JSONException ex) {
                    Log.e("FriendRequests", "Unexpected error: " + ex.getMessage());
                    Toast.makeText(getActivity(), "Unable to process your request.", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                requestAdapter.notifyItemChanged(position);
                Log.e("FriendRequests", "Unexpected error: " + error.getMessage());
                Toast.makeText(getActivity(), "Unable to process your request.", Toast.LENGTH_SHORT).show();
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

    public void loadRequests() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        requestsList.clear();
        StringRequest request = new StringRequest(Request.Method.GET, Config.RETRIEVE_REQUESTS_LIST, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressBar.setVisibility(View.GONE);
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        JSONArray requests = obj.getJSONArray("requests");
                        if (requests.length() != 0) {
                            for (int i = 0; i < requests.length(); i++) {
                                JSONObject request = requests.getJSONObject(i);
                                User r = new User();
                                r.setId(request.getString("id"));
                                r.setName(request.getString("name"));
                                r.setUsername(request.getString("username"));
                                r.setProfilePhoto(request.getString("icon"));
                                r.setCreation(request.getString("creation"));
                                r.setLocation(request.getString("location"));
                                r.setVerified(request.getInt("isVerified") == 1);
                                r.setMutualFriends(0);
                                requestsList.add(r);
                            }
                            recyclerView.setVisibility(View.VISIBLE);
                            requestAdapter.updateItems();
                            empty_Feed.setVisibility(View.GONE);
                            ((MainActivity)getActivity()).updateRequests(requestsList.size());
                        } else {
                            if (requestAdapter.getItemCount() < 1) {
                                empty_Feed.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                                progressBar.setVisibility(View.GONE);
                            } else {
                                empty_Feed.setVisibility(View.GONE);
                            }
                        }
                    } else {

                    }
                }
                catch (JSONException ex) {
                    Log.e("FriendRequests", "Unexpected error: " + ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                Log.e("FriendRequests", "Unexpected error: " + error.getMessage());
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

    public void updateRequests() {
        ((MainActivity)getActivity()).updateRequests(requestsList.size());
        if (requestAdapter.getItemCount() < 1) {
            empty_Feed.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        } else {
            empty_Feed.setVisibility(View.GONE);
        }
    }
}
