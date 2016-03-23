package com.lge.pocketphoto.sample;



import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.TextView;

public final class PrintProgressAsync extends AsyncTask<Void, Integer, Void> 
{
	private final ProgressBar mProgress;
	private final TextView mTextView;	

	public PrintProgressAsync(final ProgressBar progress, final TextView textView)
	{
		this.mProgress = progress;
		this.mTextView = textView;
		progress.setMax(100);			
	}

	public void setProgress(int curr)
	{
		publishProgress(curr);			
	}


	@Override
	protected Void doInBackground(Void... params) {
		return null;
	}

	@Override
	protected void onProgressUpdate(final Integer... values) {
		mProgress.setProgress(values[0]);
	}
}

