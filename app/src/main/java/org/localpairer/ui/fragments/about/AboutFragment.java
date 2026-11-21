package org.localpairer.ui.fragments.about;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.localpairer.AppUpdateChecker;
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

        TextView updateLink = view.findViewById(R.id.aboutUpdateLink);
        if (AppUpdateChecker.isUpdateCheckDisabled(requireContext())) {
            updateLink.setVisibility(View.GONE);
        } else {
            updateLink.setVisibility(View.VISIBLE);
            updateLink.setOnClickListener(v ->
                    AppUpdateChecker.openReleasesPage(requireContext()));
        }

        view.findViewById(R.id.aboutCoffeeBanner).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.about_coffee_url))));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(requireContext(), "No browser found.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
