package innovativedeveloper.com.socialapp.fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;

import innovativedeveloper.com.socialapp.ActivityPost;
import innovativedeveloper.com.socialapp.Comments;
import innovativedeveloper.com.socialapp.ItemDivider;
import innovativedeveloper.com.socialapp.MainActivity;
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.UserProfile;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.adapter.NotificationAdapter;
import innovativedeveloper.com.socialapp.dataset.Notification;

public class Notifications extends Fragment implements NotificationAdapter.OnNotificationItemClickListener {

    RecyclerView recyclerView;
    View emptyView, v;
    ArrayList<Notification> notificationArrayList = new ArrayList<>();
    NotificationAdapter notificationAdapter;
    public Notifications() {}

    public void refreshNotifications() {
        loadNotifications();
    }

    private void initialize() {
        recyclerView = (RecyclerView) v.findViewById(R.id.recyclerView);
        emptyView = v.findViewById(R.id.emptyView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_notifications, container, false);
        setHasOptionsMenu(true);
        initialize();
        notificationAdapter = new NotificationAdapter(getActivity(), notificationArrayList);
        notificationAdapter.setOnNotificationItemClickListener(this);
        notificationAdapter.setAnimationsLocked(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity()) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 300;
            }
        };
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(notificationAdapter);
        recyclerView.addItemDecoration(new ItemDivider(getActivity()));
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    notificationAdapter.setAnimationsLocked(true);
                }
            }
        });
        ItemTouchHelper.SimpleCallback touchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                AppHandler.getInstance().getDBHandler().markAsReadNotification(notificationArrayList.get(position));
                notificationArrayList.remove(position);
                notificationAdapter.notifyItemRemoved(position);
                Toast.makeText(getContext(), "Marked as read.", Toast.LENGTH_SHORT).show();
                if (notificationArrayList.size() > 0) {
                    emptyView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.VISIBLE);
                }
                updateNotificationCount();
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(touchHelperCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        loadNotifications();
        return v;
    }

    void loadNotifications() {
        notificationArrayList.clear();
        notificationArrayList.addAll(AppHandler.getInstance().getDBHandler().getNotifications());
        if (notificationArrayList.size() > 0) {
            emptyView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.VISIBLE);
        }
        notificationAdapter.updateItems();
    }

    @Override
    public void onItemClick(View v, int position, int action) {
        Notification n = notificationArrayList.get(position);
        AppHandler.getInstance().getDBHandler().markAsReadNotification(notificationArrayList.get(position));
        notificationArrayList.remove(position);
        notificationAdapter.notifyItemRemoved(position);
        if (notificationArrayList.size() > 0) {
            emptyView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.VISIBLE);
        }
        updateNotificationCount();
        if (action == 1) {
            if (!n.getCommentId().equals("0")) {
                final Intent intent = new Intent(getActivity(), Comments.class);
                intent.putExtra("postId", n.getPostId());
                startActivity(intent);
                getActivity().overridePendingTransition(0, 0);
                return;
            }
            if (n.getPostId().equals("0")) {
                UserProfile.startUserProfile(getActivity(), n.getUsername(), n.getUsername());
            } else {
                ActivityPost.startActivityPost(getActivity(), n.getUsername(), n.getUsername(), n.getPostId());
            }
        }
        if (action == 4) {
            final Intent intent = new Intent(getActivity(), Comments.class);
            intent.putExtra("postId", n.getPostId());
            intent.putExtra("commentId", n.getCommentId());
            intent.putExtra("isDisabled", false);
            intent.putExtra("isOwnPost", false);
            intent.putExtra("isReply", true);
            startActivity(intent);
            getActivity().overridePendingTransition(0, 0);
        }
    }

    @Override
    public void onCancel(View v, int position) {
        AppHandler.getInstance().getDBHandler().markAsReadNotification(notificationArrayList.get(position));
        notificationArrayList.remove(position);
        notificationAdapter.notifyItemRemoved(position);
        Toast.makeText(getContext(), "Marked as read.", Toast.LENGTH_SHORT).show();
        if (notificationArrayList.size() > 0) {
            emptyView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.VISIBLE);
        }
        updateNotificationCount();
    }

    private void updateNotificationCount() {
        ((MainActivity)getActivity()).updateNotification(AppHandler.getInstance().getDBHandler().getNotificationsCount());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_notifications, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_reset_notifications) {
            AppHandler.getInstance().getDBHandler().resetNotifications();
            loadNotifications();
            updateNotificationCount();
        }
        return super.onOptionsItemSelected(item);
    }
}
