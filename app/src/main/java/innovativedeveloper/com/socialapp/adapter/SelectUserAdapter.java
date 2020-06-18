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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.dataset.User;

public class SelectUserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private int lastAnimatedPosition = -1;
    private boolean animationsLocked = false;
    private ArrayList<User> userArrayList;
    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onFeedItemClickListener) {
        this.onItemClickListener = onFeedItemClickListener;
    }

    public interface OnItemClickListener {
        void onMessageClick(View v, int position);
    }

    public SelectUserAdapter(Context context, ArrayList<User> userArrayList) {
        this.context = context;
        this.userArrayList = userArrayList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_select_friend, parent, false);
        return new UserListHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        runEnterAnimation(viewHolder.itemView, position);
        final UserListHolder holder = (UserListHolder) viewHolder;
        Picasso.with(context).load(userArrayList.get(position).getProfilePhoto())
                .placeholder(R.drawable.ic_people)
                .error(R.drawable.ic_people)
                .into(holder.icon);
        holder.txtName.setText(userArrayList.get(position).getName());
        holder.verifiedIcon.setVisibility(userArrayList.get(position).isVerified() ? View.VISIBLE : View.INVISIBLE);
        holder.btnMessage.setVisibility(View.VISIBLE);
        holder.btnMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onMessageClick(v, holder.getAdapterPosition());
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

    public void setAnimationsLocked(boolean animationsLocked) {
        this.animationsLocked = animationsLocked;
    }

    private static class UserListHolder extends RecyclerView.ViewHolder {
        CircleImageView icon;
        TextView txtName;
        ImageView verifiedIcon;
        ImageButton btnMessage;

        public UserListHolder(View itemView) {
            super(itemView);
            this.icon = (CircleImageView) itemView.findViewById(R.id.icon);
            this.txtName = (TextView) itemView.findViewById(R.id.txtName);
            this.verifiedIcon = (ImageView) itemView.findViewById(R.id.verifiedIcon);
            this.btnMessage = (ImageButton) itemView.findViewById(R.id.btnMessage);
        }
    }
}
