package com.quick.completionassygt;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Created by Admin on 9/2/2017.
 */

public class RecyclerCompletion extends RecyclerView.Adapter<RecyclerCompletion.ViewHolder> {
    private Context mContext;
    private ResultSet mResult;
    final private ItemClickListener mItemClickListener;
    private Boolean mState;
    Runnable runnable;
    int delayAnimate = 300;
    int itemCount;

    Handler controlDelay = new Handler();
    int bindPos, lastBindPos, lastGetDelay;

    public RecyclerCompletion(Context context, ResultSet result, ItemClickListener listener, Boolean state) {
        mContext = context;
        mResult = result;
        mItemClickListener = listener;
        mState = false;
        mState = state;
    }

    public interface ItemClickListener {
        void OnItemClick(String jobName, String jobId, String item,  String qtyAvail, String qtyComplete);
    }

    @Override
    public int getItemCount() {
        try {
            mResult.last();
            itemCount = mResult.getRow();
            return itemCount;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.row_list_job, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        //bindPos selalu berganti tiap bind
        bindPos = position;

        //lastBindPos untuk mengetahui berapa item yang sudah di scroll down
        if (position > lastBindPos) {
            lastBindPos = bindPos;
        }

        try {
            mResult.absolute(position + 1);
            String job         = readStream(mResult.getAsciiStream(1));
            String tgl         = readStream(mResult.getAsciiStream(2));
            String qtyStart    = mResult.getString(3);
            String qtyComplete = mResult.getString(4);
            String status      = mResult.getString(6);
            String item      = readStream(mResult.getAsciiStream(8));
            String desc      =readStream(mResult.getAsciiStream(9));

            holder.tv_job.setText(job);
            holder.tv_tgl.setText(tgl);
            holder.tv_qtyStart.setText(qtyStart);
            holder.tv_qtyComplete.setText(qtyComplete);
            holder.tv_item.setText(item);
            holder.tv_desc.setText(desc);

            if(status.equals("0")){
                holder.tv_tgl.setTextColor(mContext.getResources().getColor(R.color.red));
            }else {
                holder.tv_tgl.setTextColor(mContext.getResources().getColor(R.color.font));
            }

            //animasi hanya tampil saat bind awal
            Log.d("Cursor Posisi", "" + bindPos);
            Log.d("Bind Posisi", "" + lastBindPos);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        holder.cv_parent.setCardBackgroundColor(mContext.getResources().getColor(R.color.white));
        if (mState){
            holder.cv_parent.setCardBackgroundColor(mContext.getResources().getColor(R.color.yellow));
            holder.cv_parent.setEnabled(false);
        }else {
            holder.cv_parent.setCardBackgroundColor(mContext.getResources().getColor(R.color.white));
            if (bindPos == lastBindPos && lastBindPos < itemCount) {
                holder.cv_parent.setVisibility(View.INVISIBLE);
                setAnimation(holder.cv_parent);
                controlDelay();
                if (lastBindPos + 1 == itemCount) {
                    lastBindPos += 1;
                }
            }
        }
    }

    private void setAnimation(final View view) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                Animation animation = AnimationUtils.loadAnimation(mContext, android.R.anim.slide_in_left);
                if (view != null) {
                    view.startAnimation(animation);
                    view.setVisibility(View.VISIBLE);
                    runnable = this;
                    Log.d("Animate", "running");
                }
            }
        }, delayAnimate);
        delayAnimate += 300;
    }

    //Mengembalikan delay ke 0 jika animasi tidak berjalan
    void controlDelay() {
        controlDelay.postDelayed(new Runnable() {
            @Override
            public void run() {
                runnable = this;
                if (lastGetDelay == delayAnimate) {
                    delayAnimate = 0;
                    controlDelay.removeCallbacks(runnable);
                    Log.d("ControlDelay", "OFF");
                } else {
                    controlDelay.postDelayed(runnable, 600);
                    Log.d("ControlDelay", "ON");
                    lastGetDelay = delayAnimate;
                }
            }
        }, 600);
    }

    private String readStream(InputStream data) {
        Scanner s = new Scanner(data).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    class ViewHolder extends RecyclerView.ViewHolder implements AdapterView.OnClickListener {
        CardView cv_parent;
        TextView tv_job, tv_tgl, tv_qtyStart, tv_qtyComplete, tv_item, tv_desc;

        public ViewHolder(View itemView) {
            super(itemView);
            cv_parent      = (CardView) itemView.findViewById(R.id.cv_parent);
            tv_job         = (TextView) itemView.findViewById(R.id.tv_job);
            tv_tgl         = (TextView) itemView.findViewById(R.id.tv_tgl);
            tv_qtyStart    = (TextView) itemView.findViewById(R.id.tv_qtyStart);
            tv_qtyComplete = (TextView) itemView.findViewById(R.id.tv_qtyComplete);
            tv_item         = (TextView) itemView.findViewById(R.id.tv_item);
            tv_desc         = (TextView) itemView.findViewById(R.id.tv_desc);
            cv_parent.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            try {
                mResult.absolute(getAdapterPosition() + 1);
                String jobName = readStream(mResult.getAsciiStream(1));
                String jobId = mResult.getString(7);

                String qtyAvail = mResult.getString(5);
                String qtyComplete = mResult.getString(4);
                String item      = readStream(mResult.getAsciiStream(8));

                mItemClickListener.OnItemClick(jobName, jobId, item, qtyAvail, qtyComplete);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private String readStream(InputStream data) {
            Scanner s = new Scanner(data).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }
}
