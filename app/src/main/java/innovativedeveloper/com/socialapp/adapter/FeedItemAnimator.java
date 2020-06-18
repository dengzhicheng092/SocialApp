package innovativedeveloper.com.socialapp.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.Utils;

public class FeedItemAnimator extends DefaultItemAnimator {
    private static final DecelerateInterpolator DECCELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final AccelerateInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final OvershootInterpolator OVERSHOOT_INTERPOLATOR = new OvershootInterpolator(4);

    Map<RecyclerView.ViewHolder, AnimatorSet> likeAnimationsMap = new HashMap<>();
    Map<RecyclerView.ViewHolder, AnimatorSet> heartAnimationsMap = new HashMap<>();

    private int lastAddAnimatedItem = -2;

    @Override
    public boolean canReuseUpdatedViewHolder(RecyclerView.ViewHolder viewHolder) {
        return true;
    }

    @NonNull
    @Override
    public ItemHolderInfo recordPreLayoutInformation(@NonNull RecyclerView.State state,
                                                     @NonNull RecyclerView.ViewHolder viewHolder,
                                                     int changeFlags, @NonNull List<Object> payloads) {
        if (changeFlags == FLAG_CHANGED) {
            for (Object payload : payloads) {
                if (payload instanceof String) {
                    return new FeedItemHolderInfo((String) payload);
                }
            }
        }

        return super.recordPreLayoutInformation(state, viewHolder, changeFlags, payloads);
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder.getItemViewType() == Config.FEED_TYPE_DEFAULT) {
            if (viewHolder.getLayoutPosition() > lastAddAnimatedItem) {
                lastAddAnimatedItem++;
                runEnterAnimation((FeedItemAdapter.CellFeedViewHolder) viewHolder);
                return false;
            }
        }

        dispatchAddFinished(viewHolder);
        return false;
    }

    private void runEnterAnimation(final FeedItemAdapter.CellFeedViewHolder holder) {
        final int screenHeight = Utils.getScreenHeight(holder.itemView.getContext());
        holder.itemView.setTranslationY(screenHeight);
        holder.itemView.animate()
                .translationY(0)
                .setInterpolator(new DecelerateInterpolator(3.f))
                .setDuration(700)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        dispatchAddFinished(holder);
                    }
                })
                .start();
    }

    @Override
    public boolean animateChange(@NonNull RecyclerView.ViewHolder oldHolder,
                                 @NonNull RecyclerView.ViewHolder newHolder,
                                 @NonNull ItemHolderInfo preInfo,
                                 @NonNull ItemHolderInfo postInfo) {
        cancelCurrentAnimationIfExists(newHolder);

        if (preInfo instanceof FeedItemHolderInfo) {
            FeedItemAdapter.CellFeedViewHolder holder = (FeedItemAdapter.CellFeedViewHolder) newHolder;
            animateHeartButton(holder);
        }

        return false;
    }

    private void cancelCurrentAnimationIfExists(RecyclerView.ViewHolder item) {
        if (likeAnimationsMap.containsKey(item)) {
            likeAnimationsMap.get(item).cancel();
        }
        if (heartAnimationsMap.containsKey(item)) {
            heartAnimationsMap.get(item).cancel();
        }
    }

    private void animateHeartButton(final FeedItemAdapter.CellFeedViewHolder holder) {
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator bounceAnimX = ObjectAnimator.ofFloat(holder.btnLike, "scaleX", 0.2f, 1f);
        bounceAnimX.setDuration(300);
        bounceAnimX.setInterpolator(OVERSHOOT_INTERPOLATOR);

        ObjectAnimator bounceAnimY = ObjectAnimator.ofFloat(holder.btnLike, "scaleY", 0.2f, 1f);
        bounceAnimY.setDuration(300);
        bounceAnimY.setInterpolator(OVERSHOOT_INTERPOLATOR);
        bounceAnimY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                holder.viewLike.setImageResource(!holder.getFeedItem().isLiked() ? R.drawable.ic_like : R.drawable.ic_heart_red);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                heartAnimationsMap.remove(holder);
                dispatchChangeFinishedIfAllAnimationsEnded(holder);
            }
        });

        animatorSet.play(bounceAnimX).with(bounceAnimY);
        animatorSet.start();

        heartAnimationsMap.put(holder, animatorSet);
    }

    private void dispatchChangeFinishedIfAllAnimationsEnded(FeedItemAdapter.CellFeedViewHolder holder) {
        if (likeAnimationsMap.containsKey(holder) || heartAnimationsMap.containsKey(holder)) {
            return;
        }

        dispatchAnimationFinished(holder);
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        super.endAnimation(item);
        cancelCurrentAnimationIfExists(item);
    }

    @Override
    public void endAnimations() {
        super.endAnimations();
        for (AnimatorSet animatorSet : likeAnimationsMap.values()) {
            animatorSet.cancel();
        }
    }

    public static class FeedItemHolderInfo extends ItemHolderInfo {
        public String updateAction;

        public FeedItemHolderInfo(String updateAction) {
            this.updateAction = updateAction;
        }
    }
}
