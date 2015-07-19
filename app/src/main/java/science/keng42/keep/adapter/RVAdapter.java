package science.keng42.keep.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import science.keng42.keep.HomeActivity;
import science.keng42.keep.R;
import science.keng42.keep.bean.Card;
import science.keng42.keep.model.EntryCard;
import science.keng42.keep.util.BitmapLoaderTask;
import science.keng42.keep.util.DisplayUtil;

/**
 * Created by Keng on 2015/6/3
 */
public class RVAdapter extends RecyclerView.Adapter<RVAdapter.RVViewHolder> {

    public static final int MAX_IMAGE_NUMBER = 5;
    private Context mContext;
    private List<EntryCard> mDataSet;
    // 图片区域的最小高度
    private final int mCommonHeight;
    private final Bitmap mPlaceHolderBitmap;

    public RVAdapter(Context mContext, List<EntryCard> mDataSet) {
        this.mContext = mContext;
        this.mDataSet = mDataSet;
        this.mCommonHeight = getImageHeight();
        setHasStableIds(true);

        mPlaceHolderBitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.ic_photo_placeholder_dark);
    }

    @Override
    public RVViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_item, parent, false);
        return new RVViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        // 根据图片数量来决定 ViewType
        EntryCard card = mDataSet.get(position);
        final List<String> names = card.getNames();
        return names.size() > MAX_IMAGE_NUMBER ? MAX_IMAGE_NUMBER : names.size();
    }

    @Override
    public long getItemId(int position) {
        // DONE 解决卡片复用混乱问题
        return position;
    }

    @Override
    public void onBindViewHolder(RVViewHolder vh, int i) {
        final int position = i;
        EntryCard card = mDataSet.get(i);
        String title = card.getTitle();
        String text = card.getText();
        int color = card.getColor();
        final long entryId = card.getId();
        final List<String> names = card.getNames();
        bindImageView(vh, names);
        vh.mTvTitle.setText(title);
        if (title.equals("")) {
            vh.mTvTitle.setVisibility(View.GONE);
        } else {
            vh.mTvTitle.setVisibility(View.VISIBLE);
        }
        vh.mTvText.setText(text);
        if ("".equals(text)) {
            vh.mTvText.setVisibility(View.GONE);
        } else {
            vh.mTvText.setVisibility(View.VISIBLE);
        }
        // 向下兼容 API16
        // vh.itemView.setBackgroundColor(color);
        ((CardView) vh.itemView).setCardBackgroundColor(color);

        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onClick(entryId, position, v);
                }
            }
        });

        vh.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onLongClick(entryId, position);
                }
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    /**
     * ViewHolder 点击事件监听对象
     */
    private OnItemClickListener mOnItemClickListener = null;

    /**
     * 设置 ViewHolder 点击事件监听对象，由 Activity 调用
     *
     * @param onItemClickListener ViewHolder 点击事件监听对象
     */
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    /**
     * 绑定图片控件的数据
     * 动态设置尺寸（根据 mCommonHeight）和数量
     */
    private void bindImageView(RVViewHolder vh, List<String> names) {
        int w, h;
        ViewGroup.LayoutParams lpTop = vh.mIvTopLeft.getLayoutParams();
        ViewGroup.LayoutParams lpMiddle = vh.mIvMiddleRight.getLayoutParams();
        switch (names.size()) {
            case 0:
                break;
            case 1:
                w = mCommonHeight * 3;
                h = mCommonHeight * 2;
                lpTop.height = h;
                vh.mIvTopRight.setVisibility(View.GONE);

                vh.mIvTopLeft.setTag(names.get(0));
                loadBitmap(vh.mIvTopLeft, names.get(0), w, h);
                break;
            case 2:
                w = (int) (mCommonHeight * 1.5);
                h = (int) (mCommonHeight * 1.5);
                lpTop.height = h;

                vh.mIvTopLeft.setTag(names.get(0));
                loadBitmap(vh.mIvTopLeft, names.get(0), w, h);

                vh.mIvTopRight.setTag(names.get(1));
                loadBitmap(vh.mIvTopRight, names.get(1), w, h);
                break;
            case 3:
                w = mCommonHeight;
                h = mCommonHeight;
                lpMiddle.height = h;

                vh.mIvMiddleLeft.setTag(names.get(0));
                loadBitmap(vh.mIvMiddleLeft, names.get(0), w, h);

                vh.mIvMiddleMiddle.setTag(names.get(1));
                loadBitmap(vh.mIvMiddleMiddle, names.get(1), w, h);

                vh.mIvMiddleRight.setTag(names.get(2));
                loadBitmap(vh.mIvMiddleRight, names.get(2), w, h);
                break;
            case 4:
                w = mCommonHeight;
                h = mCommonHeight;

                vh.mIvMiddleLeft.setTag(names.get(1));
                loadBitmap(vh.mIvMiddleLeft, names.get(1), w, h);

                vh.mIvMiddleMiddle.setTag(names.get(2));
                loadBitmap(vh.mIvMiddleMiddle, names.get(2), w, h);

                vh.mIvMiddleRight.setTag(names.get(3));
                loadBitmap(vh.mIvMiddleRight, names.get(3), w, h);

                lpMiddle.height = h;

                w = 3 * w;
                h = (int) (1.5 * h);
                lpTop.height = h;

                vh.mIvTopRight.setVisibility(View.GONE);

                vh.mIvTopLeft.setTag(names.get(0));
                loadBitmap(vh.mIvTopLeft, names.get(0), w, h);

                break;
            default:
                // five images
                w = mCommonHeight;
                h = mCommonHeight;
                lpMiddle.height = h;

                vh.mIvMiddleLeft.setTag(names.get(2));
                loadBitmap(vh.mIvMiddleLeft, names.get(2), w, h);

                vh.mIvMiddleMiddle.setTag(names.get(3));
                loadBitmap(vh.mIvMiddleMiddle, names.get(3), w, h);

                vh.mIvMiddleRight.setTag(names.get(4));
                loadBitmap(vh.mIvMiddleRight, names.get(4), w, h);

                w = (int) (1.5 * h);
                h = w;
                lpTop.height = h;

                vh.mIvTopLeft.setTag(names.get(0));
                loadBitmap(vh.mIvTopLeft, names.get(0), w, h);

                vh.mIvTopRight.setTag(names.get(1));
                loadBitmap(vh.mIvTopRight, names.get(1), w, h);
                break;
        }
    }

    /**
     * 根据手机屏幕设置图片区域的最小高度
     * 用于适配图片的高度（1x,1.5x,2x）
     */
    private int getImageHeight() {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int span = DisplayUtil.dip2px(mContext, 8);
        int cardWidth;

        if (HomeActivity.mSpanCount == 2) {
            cardWidth = (screenWidth - span * 3) / 2;
        } else {
            cardWidth = (screenWidth - span * 4) / 3;
        }
        return cardWidth / 3;
    }

    /**
     * 根据文件名，绑定图片到 ImageView
     *
     * @param imageView 目标 ImageView
     * @param name      图片文件名
     * @param w         目标宽度
     * @param h         目标高度
     */
    public void loadBitmap(ImageView imageView, String name, int w, int h) {
        String path = mContext.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES) + File.separator;
        if (mContext instanceof HomeActivity) {
            HomeActivity activity = (HomeActivity) mContext;
            Bitmap bitmap = activity.getBitmapFromMemCache(path + name);
            if (bitmap == null) {
                bitmap = activity.getBitmapFromDisk(path + name);
            }
            if (bitmap != null) {
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setImageBitmap(bitmap);
                return;
            }
        }
        if (BitmapLoaderTask.cancelPotentialWork(name, imageView)) {
            BitmapLoaderTask task = new BitmapLoaderTask(imageView, mContext);
            BitmapLoaderTask.AsyncDrawable asyncDrawable =
                    new BitmapLoaderTask.AsyncDrawable(mContext.getResources(), mPlaceHolderBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(path + name, w, h);
        }
    }

    /**
     * ViewHolder 点击事件接口，由 Activity 实现
     */
    public interface OnItemClickListener {
        void onClick(long entryId, int position, View view);

        void onLongClick(long entryId, int position);
    }

    /**
     * Recycler View Holder
     */
    public static class RVViewHolder extends RecyclerView.ViewHolder {
        public ImageView mIvTopLeft;
        public ImageView mIvTopRight;
        public ImageView mIvMiddleLeft;
        public ImageView mIvMiddleMiddle;
        public ImageView mIvMiddleRight;

        public TextView mTvTitle;
        public TextView mTvText;

        public LinearLayout mLlLabel;
        public LinearLayout mLlTop;
        public LinearLayout mLlMiddle;

        public RVViewHolder(View itemView) {
            super(itemView);

            mIvTopLeft = (ImageView) itemView.findViewById(R.id.iv_top_left);
            mIvTopRight = (ImageView) itemView.findViewById(R.id.iv_top_right);
            mIvMiddleLeft = (ImageView) itemView.findViewById(R.id.iv_middle_left);
            mIvMiddleMiddle = (ImageView) itemView.findViewById(R.id.iv_middle_middle);
            mIvMiddleRight = (ImageView) itemView.findViewById(R.id.iv_middle_right);

            mTvTitle = (TextView) itemView.findViewById(R.id.tv_title);
            mTvText = (TextView) itemView.findViewById(R.id.tv_text);

            mLlLabel = (LinearLayout) itemView.findViewById(R.id.ll_label);
            mLlTop = (LinearLayout) itemView.findViewById(R.id.ll_top);
            mLlMiddle = (LinearLayout) itemView.findViewById(R.id.ll_middle);
        }
    }
}
