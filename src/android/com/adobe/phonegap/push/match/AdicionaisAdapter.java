package com.adobe.phonegap.push.match;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by alvaro.menezes on 05/12/2017.
 */

public class AdicionaisAdapter extends RecyclerView.Adapter<AdicionaisAdapter.ViewHolder> {

  // references to our images
  private String[] mDataset = {
    "Carrinho transportador",
    "Voluptatem",
    "Quaeu",
    "Carrinho transportador",
    "Carrinho transportador",
    "Carrinho transportador",
    "Carrinho transportador",
    "Carrinho transportador",
    "Carrinho transportador",
    "Carrinho transportador",
    "Carrinho transportador",
    "Carrinho transportador",
    "Carrinho transportador"
  };
  private Context mContext;

  public static class ViewHolder extends RecyclerView.ViewHolder {
    // each data item is just a string in this case
    public TextView mTextView;
    public ViewHolder(TextView v) {
      super(v);
      mTextView = v;
    }
  }

  public AdicionaisAdapter(Context c) {
    mContext = c;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    // create a new view
    TextView v = (TextView) new TextView(mContext);
//    LayoutInflater.from(parent.getContext())
//      .inflate(R.id.my_text_view, parent, false);

    ViewHolder vh = new ViewHolder(v);
    return vh;
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    // - get element from your dataset at this position
    // - replace the contents of the view with that element
    holder.mTextView.setText("    • " + mDataset[position]);
  }

  @Override
  public int getItemCount() {
    return mDataset.length;
  }


}
