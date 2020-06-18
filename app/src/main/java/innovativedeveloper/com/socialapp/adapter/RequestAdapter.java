package innovativedeveloper.com.socialapp.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import innovativedeveloper.com.socialapp.dataset.User;
import innovativedeveloper.com.socialapp.R;

public class RequestAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {

    private Context context;
    private int lastAnimatedPosition = -1;
    private boolean animationsLocked = false;
    private ArrayList<User> userArrayList;
    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
        void onAddFriendClick(View v, int position);
        void onRejectClick(View v, int position);
    }

    public RequestAdapter(Context context, ArrayList<User> userArrayList) {
        this.context = context;
        this.userArrayList = userArrayList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_request, parent, false);
        return new RequestListHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        runEnterAnimation(viewHolder.itemView, position);
        final RequestListHolder holder = (RequestListHolder) viewHolder;
        Picasso.with(context)
                .load(userArrayList.get(position).getProfilePhoto())
                .placeholder(R.drawable.ic_people)
                .error(R.drawable.ic_people)
                .into(holder.icon);
        holder.txtName.setText(userArrayList.get(position).getName());
        holder.verifiedIcon.setVisibility(userArrayList.get(position).isVerified() ? View.VISIBLE : View.INVISIBLE);
        if (userArrayList.get(position).getMutualFriends() > 0) {
            holder.txtMutualFriends.setText(userArrayList.get(position).getMutualFriends() + " mutual friends");
            holder.enableLocation(false);
        } else {
            if (!userArrayList.get(position).getLocation().isEmpty()) {
                holder.txtLocation.setText(userArrayList.get(position).getLocation());
                holder.enableLocation(true);
            } else {
                holder.enableLocation(false);
                holder.txtMutualFriends.setVisibility(View.GONE);
            }
        }
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemClickListener.onItemClick(holder.view, holder.getAdapterPosition());
            }
        });
        holder.btnAddFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.btnAddFriend.setEnabled(false);
                holder.btnReject.setEnabled(false);
                onItemClickListener.onAddFriendClick(holder.view, holder.getAdapterPosition());
            }
        });
        holder.btnReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.btnAddFriend.setEnabled(false);
                holder.btnReject.setEnabled(false);
                onItemClickListener.onRejectClick(holder.view, holder.getAdapterPosition());
            }
        });
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
        return userArrayList.size();
    }

    public void updateItems() {
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    public void setAnimationsLocked(boolean animationsLocked) {
        this.animationsLocked = animationsLocked;
    }

    public static class RequestListHolder extends RecyclerView.ViewHolder {

        CircleImageView icon;
        TextView txtName;
        TextView txtMutualFriends;
        TextView txtLocation;
        ImageView verifiedIcon, imgLocation;
        View view;
        Button btnAddFriend, btnReject;

        private RequestListHolder(View view) {
            super(view);
            this.view = view;
            this.icon = (CircleImageView) view.findViewById(R.id.icon);
            this.txtName = (TextView) view.findViewById(R.id.txtName);
            this.txtMutualFriends = (TextView) view.findViewById(R.id.txtMutual);
            this.txtLocation = (TextView) view.findViewById(R.id.txtLocation);
            this.imgLocation = (ImageView) view.findViewById(R.id.imgLocation);
            this.verifiedIcon = (ImageView) view.findViewById(R.id.verifiedIcon);
            this.btnAddFriend = (Button) view.findViewById(R.id.btnAddFriend);
            this.btnReject = (Button) view.findViewById(R.id.btnRemove);
        }

        public void enableLocation(boolean v) {
            txtLocation.setVisibility(v ? View.VISIBLE : View.GONE);
            imgLocation.setVisibility(v ? View.VISIBLE : View.GONE);
            txtMutualFriends.setVisibility(!v ? View.VISIBLE : View.GONE);
        }
    }
}
