package org.localpairer.ui.fragments.about;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import org.localpairer.BuildConfig;
import org.localpairer.R;

public class AboutFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView versionView = view.findViewById(R.id.aboutVersionTextView);
        versionView.setText(getString(R.string.about_version_format, BuildConfig.VERSION_NAME));

        TextView repositoryLink = view.findViewById(R.id.aboutRepositoryLink);
        repositoryLink.setMovementMethod(LinkMovementMethod.getInstance());

        TextView upstreamLink = view.findViewById(R.id.aboutUpstreamLink);
        upstreamLink.setMovementMethod(LinkMovementMethod.getInstance());

        view.findViewById(R.id.aboutSupportButton).setOnClickListener(v ->
                NavHostFragment.findNavController(AboutFragment.this)
                        .navigate(R.id.action_about_to_support));

        view.findViewById(R.id.aboutAccountButton).setOnClickListener(v ->
                NavHostFragment.findNavController(AboutFragment.this)
                        .navigate(R.id.action_about_to_account));

        return view;
    }
}
