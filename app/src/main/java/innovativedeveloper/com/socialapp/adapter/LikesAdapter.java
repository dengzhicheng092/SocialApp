package innovativedeveloper.com.socialapp.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import innovativedeveloper.com.socialapp.dataset.Like;
import innovativedeveloper.com.socialapp.R;

public class LikesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private int lastAnimatedPosition = -1;
    private boolean animationsLocked = false;
    private ArrayList<Like> likes;
    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onFeedItemClickListener) {
        this.onItemClickListener = onFeedItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    public LikesAdapter(Context context, ArrayList<Like> likes) {
        this.context = context;
        this.likes = likes;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_like, parent, false);
        return new LikesAdapter.LikeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int position) {
        runEnterAnimation(viewHolder.itemView, position);
        final LikeViewHolder holder = (LikeViewHolder) viewHolder;
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemClickListener.onItemClick(holder.view, viewHolder.getAdapterPosition());
            }
        });
        holder.txtName.setText(likes.get(position).getUser().getName());
        if (likes.get(position).getUser().getMutualFriends() > 0) {
            holder.txtMutualFriends.setText(likes.get(position).getUser().getMutualFriends() + " mutual friends");
        } else {
            holder.txtMutualFriends.setVisibility(View.GONE);
        }
        holder.verifiedIcon.setVisibility(likes.get(position).getUser().isVerified() ? View.VISIBLE : View.INVISIBLE);
        Picasso.with(context)
                .load(likes.get(position).getIcon())
                .placeholder(R.drawable.ic_people)
                .error(R.drawable.ic_people)
                .into(holder.icon);
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
        return likes.size();
    }

    public void updateItems() {
        notifyDataSetChanged();
    }

    public void setAnimationsLocked(boolean animationsLocked) {
        this.animationsLocked = animationsLocked;
    }

    private static class LikeViewHolder extends RecyclerView.ViewHolder {
        CircleImageView icon;
        TextView txtName;
        TextView txtMutualFriends;
        ImageView verifiedIcon;
        View view;

        private LikeViewHolder(View view) {
            super(view);
            this.view = view;
            icon = (CircleImageView) view.findViewById(R.id.icon);
            txtName = (TextView) view.findViewById(R.id.txtName);
            txtMutualFriends = (TextView) view.findViewById(R.id.txtMutual);
            verifiedIcon = (ImageView) view.findViewById(R.id.verifiedIcon);
        }
    }
}

