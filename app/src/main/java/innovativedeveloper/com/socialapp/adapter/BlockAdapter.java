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
import android.widget.TextView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.dataset.User;

public class BlockAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private int lastAnimatedPosition = -1;
    private boolean animationsLocked = false;
    private ArrayList<User> blockUser;
    private OnClickListener onClickListener;

    public BlockAdapter(Context context, ArrayList<User> blockUser) {
        this.context = context;
        this.blockUser = blockUser;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public interface OnClickListener {
        void RemoveBlock(int position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_block_user, parent, false);
        return new BlockAdapter.BlockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, int position) {
        runEnterAnimation(viewHolder.itemView, position);
        BlockViewHolder holder = (BlockViewHolder) viewHolder;
        holder.txtName.setText(blockUser.get(position).getName());
        holder.txtUsername.setText(blockUser.get(position).getUsername());
        holder.btnUnBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickListener.RemoveBlock(viewHolder.getAdapterPosition());
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
        return blockUser.size();
    }

    public void updateItems() {
        notifyDataSetChanged();
    }

    public void setAnimationsLocked(boolean animationsLocked) {
        this.animationsLocked = animationsLocked;
    }

    private static class BlockViewHolder extends RecyclerView.ViewHolder {
        CircleImageView icon;
        TextView txtName;
        TextView txtUsername;
        Button btnUnBlock;

        private BlockViewHolder(View view) {
            super(view);
            icon = (CircleImageView) view.findViewById(R.id.icon);
            txtUsername = (TextView) view.findViewById(R.id.txtUsername);
            txtName = (TextView) view.findViewById(R.id.txtName);
            btnUnBlock = (Button) view.findViewById(R.id.btnUnBlock);
        }
    }
}
