package science.keng42.keep.adapter;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import science.keng42.keep.R;

/**
 * Created by Keng on 2015/9/11
 */
public class SDCardRVAdapter extends Adapter<SDCardRVAdapter.SDCardViewHolder> {
    private List<String> mFileNames;
    private List<String> mInfos;
    private int mPage;
    private SDCardRVAdapterListener listener = null;

    public SDCardRVAdapter(List<String> mFileNames, List<String> mInfos, int mPage) {
        this.mFileNames = mFileNames;
        this.mInfos = mInfos;
        this.mPage = mPage;
    }

    /**
     * 设置列表项点击响应实例
     */
    public void setListener(SDCardRVAdapterListener listener) {
        this.listener = listener;
    }

    @Override
    public SDCardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_backup_item, parent, false);
        if (mPage == 1) {
            ImageView iv = (ImageView) view.findViewById(R.id.iv);
            iv.setImageResource(R.drawable.ic_backup_file_dropbox_grey600_24dp);
        }
        return new SDCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SDCardViewHolder holder, int position) {
        holder.tvFileName.setText(mFileNames.get(position));
        holder.tvTime.setText(mInfos.get(position));

        final int p = position;
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(mFileNames.get(p));
                }
            }
        });

        holder.ivMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onMoreClick(mFileNames.get(p), v);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFileNames.size();
    }

    /**
     * 列表项点击响应接口
     */
    public interface SDCardRVAdapterListener {
        void onItemClick(String filename);

        void onMoreClick(String filename, View view);

    }

    /**
     * ViewHolder
     */
    class SDCardViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName;
        TextView tvTime;
        ImageView ivMore;

        public SDCardViewHolder(View itemView) {
            super(itemView);
            tvFileName = (TextView) itemView.findViewById(R.id.tv_file_name);
            tvTime = (TextView) itemView.findViewById(R.id.tv_time);
            ivMore = (ImageView) itemView.findViewById(R.id.iv_more);
        }
    }
}
