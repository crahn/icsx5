package at.bitfire.icsdroid.ui;

import android.app.Dialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Base64;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import at.bitfire.ical4android.Event;
import at.bitfire.ical4android.InvalidCalendarException;
import at.bitfire.icsdroid.R;
import lombok.Cleanup;

public class AddCalendarValidationFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<ResourceInfo> {
    AddCalendarActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AddCalendarActivity)getActivity();
        Loader<ResourceInfo> loader = getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog progress = new ProgressDialog(getActivity());
        progress.setCancelable(false);
        progress.setMessage(getString(R.string.please_wait));
        return progress;
    }


    // loader callbacks

    @Override
    public Loader<ResourceInfo> onCreateLoader(int id, Bundle args) {
        return new ResourceInfoLoader(activity);
    }

    @Override
    public void onLoadFinished(Loader<ResourceInfo> loader, ResourceInfo info) {
        getDialog().dismiss();

        String errorMessage = null;
        if (info.exception != null)
            errorMessage = info.exception.getMessage();
        else if (info.statusCode != 200)
            errorMessage = info.statusCode + " " + info.statusMessage;
        
        if (errorMessage == null)
            // success, proceed to CreateCalendarFragment
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AddCalendarDetailsFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        else
            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoaderReset(Loader<ResourceInfo> loader) {
    }


    // loader

    protected static class ResourceInfoLoader extends AsyncTaskLoader<ResourceInfo> {
        ResourceInfo info;
        boolean started;

        public ResourceInfoLoader(AddCalendarActivity activity) {
            super(activity);

            info = new ResourceInfo(activity.url, activity.authRequired, activity.username, activity.password);
        }

        @Override
        protected void onStartLoading() {
            synchronized(this) {
                if (started == false)
                    started = true;
                    forceLoad();
                }
        }

        @Override
        public ResourceInfo loadInBackground() {
            HttpURLConnection conn;
            try {
                conn = (HttpURLConnection) info.url.openConnection();
                if (info.authRequired) {
                    String basicCredentials = info.username + ":" + info.password;
                    conn.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(basicCredentials.getBytes(), 0));
                }

                info.statusCode = conn.getResponseCode();
                info.statusMessage = conn.getResponseMessage();

                if (info.statusCode == 200) {
                    @Cleanup InputStream is = conn.getInputStream();
                    Event[] events = Event.fromStream(is, null);
                    info.eventsFound = events.length;
                }

            } catch (IOException|InvalidCalendarException e) {
                info.exception = e;
            }
            return info;
        }
    }
}
