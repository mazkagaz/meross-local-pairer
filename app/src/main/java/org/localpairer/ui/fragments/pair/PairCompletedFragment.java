package org.localpairer.ui.fragments.pair;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import org.localpairer.R;


public class PairCompletedFragment extends Fragment {
    private ImageView mCheckIcon;
    private TextView mTextView;
    private ImageView mQuestionMarkView;
    private Button pairDoneButton;
    private boolean mVerified;
    private String mTargetUuid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVerified = getArguments().getBoolean("verified", false);
        mTargetUuid = getArguments().getString("targetUuid", null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pair_done, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTextView = view.findViewById(R.id.pairing_completed_text_view);
        mQuestionMarkView = view.findViewById(R.id.questionMarkIcon);
        pairDoneButton = view.findViewById(R.id.pairDoneButton);
        pairDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController ctrl = NavHostFragment.findNavController(PairCompletedFragment.this);
                ctrl.popBackStack(R.id.ScanDeviceFragment, false);
            }
        });

        mCheckIcon = view.findViewById(R.id.pairDoneCheckIcon);
        mCheckIcon.setVisibility(mVerified ? View.VISIBLE : View.GONE);
        mQuestionMarkView.setVisibility(!mVerified ? View.VISIBLE : View.GONE);
        String message = getString(mVerified
                ? R.string.pairing_done_verified
                : R.string.pairing_done_unverified);
        if (mTargetUuid != null && !mTargetUuid.isEmpty()) {
            message += "\n\nUUID appairé : " + mTargetUuid;
        }
        mTextView.setText(message);
    }
}
