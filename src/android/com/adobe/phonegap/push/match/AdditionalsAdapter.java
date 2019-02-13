package com.adobe.phonegap.push.match;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by alvaro.menezes on 05/12/2017.
 */

public class AdditionalsAdapter extends RecyclerView.Adapter<AdditionalsAdapter.ViewHolder> {

  private Context mContext;
  private JSONArray mDataSet;

  public static class ViewHolder extends RecyclerView.ViewHolder {
    // each data item is just a string in this case
    public TextView mTextView;
    public ViewHolder(TextView v) {
      super(v);
      mTextView = v;
    }
  }

  public AdditionalsAdapter(JSONArray dataSet, Context c) {
    mContext = c;
    mDataSet = dataSet;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    // create a new view
    TextView v = (TextView) new TextView(mContext);
    ViewHolder vh = new ViewHolder(v);
    return vh;
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    try {
      holder.mTextView.setText("    • " + mDataSet.getString(position));
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @Override
  public int getItemCount() {
    return mDataSet.length();
  }
}
