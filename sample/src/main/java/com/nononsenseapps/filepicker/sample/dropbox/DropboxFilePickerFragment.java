/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.nononsenseapps.filepicker.sample.dropbox;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderResult;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.nononsenseapps.filepicker.AbstractFilePickerFragment;
import com.nononsenseapps.filepicker.sample.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("ValidFragment")
public class DropboxFilePickerFragment extends AbstractFilePickerFragment<Metadata> {
    private static final String TAG = "DbxFilePickerFragment";
    private final DbxClientV2 dropboxClient;
    private ProgressBar progressBar;

    @SuppressLint("ValidFragment")
    public DropboxFilePickerFragment(final DbxClientV2 api) {
        super();
        if (api == null) {
            throw new IllegalArgumentException("Must be authenticated with Dropbox");
        }

        this.dropboxClient = api;
    }

    @Override
    protected View inflateRootView(LayoutInflater inflater, ViewGroup container) {
        // Load the specific layout we created for dropbox/ftp
        View view = inflater.inflate(R.layout.fragment_loading_filepicker, container, false);
        // And bind the progress bar
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        return view;
    }

    /**
     * If we are loading, then hide the list and show the progress bar instead.
     *
     * @param nextPath path to list files for
     */
    @Override
    protected void refresh(@NonNull Metadata nextPath) {
        super.refresh(nextPath);
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Once loading has finished, show the list and hide the progress bar.
     */
    @Override
    public void onLoadFinished(Loader<List<Metadata>> loader, List<Metadata> data) {
        progressBar.setVisibility(View.INVISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
        super.onLoadFinished(loader, data);
    }

    /**
     * Once loading has finished, show the list and hide the progress bar.
     */
    @Override
    public void onLoaderReset(Loader<List<Metadata>> loader) {
        progressBar.setVisibility(View.INVISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
        super.onLoaderReset(loader);
    }

    @Override
    public void onNewFolder(@NonNull final String name) {
        File folder = new File(mCurrentPath.getPathDisplay(), name);
        new FolderCreator().execute(folder.getPath());
    }

    @Override
    public boolean isDir(@NonNull final Metadata file) {
        return file instanceof FolderMetadata;
    }

    @NonNull
    @Override
    public Metadata getParent(@NonNull final Metadata from) {
        String fromPath = from.getPathLower();
        int lastSeparatorIndex = from.getPathLower().lastIndexOf('/');

        String parentPath = "";

        if (lastSeparatorIndex > 0) {
            parentPath = fromPath.substring(0, lastSeparatorIndex);
        }

        return getPath(parentPath);
    }

    @NonNull
    @Override
    public Metadata getPath(@NonNull String path) {
        return FolderMetadata.newBuilder(path, "id")
                .withPathLower(path)
                .build();
    }

    @NonNull
    @Override
    public String getFullPath(@NonNull final Metadata file) {
        return file.getPathDisplay();
    }

    @NonNull
    @Override
    public String getName(@NonNull final Metadata file) {
        return file.getName();
    }

    @NonNull
    @Override
    public Metadata getRoot() {
        return getPath("");
    }

    @NonNull
    @Override
    public Uri toUri(@NonNull final Metadata file) {
        return new Uri.Builder().scheme("dropbox").authority("").path(file.getPathDisplay()).build();
    }

    @NonNull
    @Override
    public Loader<List<Metadata>> getLoader() {
        return new DropboxAsyncTaskLoader(this, DropboxFilePickerFragment.this.getContext());
    }

    /**
     * Dropbox requires stuff to be done in a background thread. Refreshing has to be done on the
     * UI thread however (it restarts the loader so actual work is done in the background).
     */
    private class FolderCreator extends AsyncTask<String, Void, Metadata> {
        @Override
        protected void onPreExecute() {
            // Switch to progress bar before starting work
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
        }

        @Override
        protected Metadata doInBackground(final String... paths) {
            if (paths.length == 0) {
                return null;
            }

            String path = paths[0];
            try {
                CreateFolderResult createFolderResult =  dropboxClient.files().createFolderV2(path);
                return createFolderResult.getMetadata();
            } catch (DbxException e) {
                Log.d(TAG, getString(com.nononsenseapps.filepicker.R.string.nnf_create_folder_error), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(@Nullable Metadata path) {
            if (path != null) {
                goToDir(path);
            } else {
                progressBar.setVisibility(View.INVISIBLE);
                recyclerView.setVisibility(View.VISIBLE);
                Toast.makeText(getActivity(), com.nononsenseapps.filepicker.R.string.nnf_create_folder_error,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static class DropboxAsyncTaskLoader extends AsyncTaskLoader<List<Metadata>> {

        private final DropboxFilePickerFragment dropboxFilePickerFragment;

        public DropboxAsyncTaskLoader(DropboxFilePickerFragment dropboxFilePickerFragment, Context context) {
            super(context);
            this.dropboxFilePickerFragment = dropboxFilePickerFragment;
        }

        @Override
        public List<Metadata> loadInBackground() {
            List<Metadata> files = new ArrayList<>();
            try {
                if (!(dropboxFilePickerFragment.mCurrentPath instanceof FolderMetadata)) {
                    dropboxFilePickerFragment.mCurrentPath = dropboxFilePickerFragment.getRoot();
                }

                String pathToList = dropboxFilePickerFragment.mCurrentPath.getPathLower();
                ListFolderResult listDirResult = dropboxFilePickerFragment.dropboxClient.files().listFolder(pathToList);
                List<Metadata> dirContents = listDirResult.getEntries();

                for (Metadata entry : dirContents) {
                    if ((dropboxFilePickerFragment.mode == MODE_FILE ||
                            dropboxFilePickerFragment.mode == MODE_FILE_AND_DIR) ||
                            entry instanceof FolderMetadata) {
                        files.add(entry);
                    }
                }

            } catch (DbxException ignored) {
                Log.d(TAG, "Failed to list Dropbox folder", ignored);
                ignored.getMessage();
            }

            return files;
        }

        /**
         * Handles a request to start the Loader.
         */
        @Override
        protected void onStartLoading() {
            super.onStartLoading();

            if (dropboxFilePickerFragment.mCurrentPath == null ||
                    !(dropboxFilePickerFragment.mCurrentPath instanceof FolderMetadata)) {
                dropboxFilePickerFragment.mCurrentPath = dropboxFilePickerFragment.getRoot();
            }

            forceLoad();
        }

        /**
         * Handles a request to completely reset the Loader.
         */
        @Override
        protected void onReset() {
            super.onReset();
        }
    }
}
