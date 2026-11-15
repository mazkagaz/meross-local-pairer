package org.localpairer.ui.fragments.support_this_app;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.localpairer.R;

public class SupportThisApp extends Fragment {

    public SupportThisApp() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_support_this_app, container, false);
        TextView repositoryLink = view.findViewById(R.id.repositoryLinkTextView);
        repositoryLink.setMovementMethod(LinkMovementMethod.getInstance());
        return view;
    }
}
