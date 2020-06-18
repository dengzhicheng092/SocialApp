package innovativedeveloper.com.socialapp.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.dataset.Notification;

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private int lastAnimatedPosition = -1;
    private boolean animationsLocked = false;
    private ArrayList<Notification> notificationArrayList;
    private OnNotificationItemClickListener onNotificationItemClickListener;

    public NotificationAdapter(Context context, ArrayList<Notification> notificationArrayList) {
        this.context = context;
        this.notificationArrayList = notificationArrayList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationAdapter.NotificationsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {
        runEnterAnimation(viewHolder.itemView, position);
        final NotificationsViewHolder holder = (NotificationsViewHolder) viewHolder;
        Picasso.with(context)
                .load(notificationArrayList.get(holder.getAdapterPosition()).getIcon())
                .centerCrop()
                .resize(100, 100)
                .placeholder(R.drawable.ic_people)
                .error(R.drawable.ic_people)
                .into(holder.icon);
        holder.txtContent.setText(notificationArrayList.get(holder.getAdapterPosition()).getData());
        holder.txtDate.setText(AppHandler.getTimestampShort(notificationArrayList.get(holder.getAdapterPosition()).getCreation()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNotificationItemClickListener.onItemClick(v, holder.getAdapterPosition(), Integer.valueOf(notificationArrayList.get(holder.getAdapterPosition()).getAction()));
            }
        });
        holder.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNotificationItemClickListener.onCancel(view, holder.getAdapterPosition());
            }
        });
    }

    public void setOnNotificationItemClickListener(OnNotificationItemClickListener onNotificationItemClickListener) {
        this.onNotificationItemClickListener = onNotificationItemClickListener;
    }

    public interface OnNotificationItemClickListener {
        void onItemClick(View v, int position, int action);
        void onCancel(View v, int position);
    }

    private void runEnterAnimation(View view, int position) {
        if (animationsLocked) return;

        if (position > lastAnimatedPosition) {
            lastAnimatedPosition = position;
            view.setTranslationY(100);
            view.setAlpha(0.f);
            view.animate()
                    .translationY(0).alpha(1.f)
                    .setStartDelay(0)
                    .setInterpolator(new DecelerateInterpolator(2.f))
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            animationsLocked = true;
                        }
                    })
                    .start();
        }
    }

    @Override
    public int getItemCount() {
        return notificationArrayList.size();
    }

    public void updateItems() {
        notifyDataSetChanged();
    }

    public void setAnimationsLocked(boolean animationsLocked) {
        this.animationsLocked = animationsLocked;
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    private static class NotificationsViewHolder extends RecyclerView.ViewHolder {
        CircleImageView icon;
        TextView txtContent;
        TextView txtDate;
        ImageView imgAction;
        ImageButton btnCancel;

        private NotificationsViewHolder(View view) {
            super(view);
            icon = (CircleImageView) view.findViewById(R.id.icon);
            txtContent = (TextView) view.findViewById(R.id.txtContent);
            txtDate = (TextView) view.findViewById(R.id.txtDate);
            btnCancel = (ImageButton) view.findViewById(R.id.btnCancel);
        }
    }
}
