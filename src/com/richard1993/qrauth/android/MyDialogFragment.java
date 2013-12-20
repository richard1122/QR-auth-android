package com.richard1993.qrauth.android;

import java.util.List;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MyDialogFragment extends DialogFragment {
	String remote = null, session = null;
	
	static MyDialogFragment newInstance (String remote, String session) {
		MyDialogFragment f = new MyDialogFragment();
		
		Bundle bundle = new Bundle();
		bundle.putString("remote", remote);
		bundle.putString("session", session);
		f.setArguments(bundle);
		
		return f;
	}
	
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.remote = getArguments().getString("remote");
		this.session = getArguments().getString("session");
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		getDialog().setTitle("«Î—°‘Ò”√ªß");
		View view = inflater.inflate(R.layout.dialog_fragment_layout, null);
		DataHelper dataHelper = new DataHelper(getActivity());
		List<String> userList = dataHelper.getUsernameListByRemote(this.remote);
		if (userList.size() == 0) {
			//no userName.
			Toast.makeText(getActivity(), "You haven't registered this website yet.", Toast.LENGTH_LONG).show();
			getDialog().cancel();
			return null;
		}
		
		for (String string : userList) {
			//Initial all users and binding click listener
			LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_fragment_item, null);
			((TextView) layout.findViewById(R.id.dialog_fragment_text))
				.setText(string);
			layout.setOnClickListener(onClickListener);
			((LinearLayout) view.findViewById(R.id.dialog_fragment_linear))
				.addView(layout);
		}
		
		return view;
	}
	
	/**
	 * When an user is clicked, select it by calling CameraTestActivity onUserNameSelect function.
	 */
	private final View.OnClickListener onClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			String string = ((TextView) v.findViewById(R.id.dialog_fragment_text)).getText().toString();
			((CameraTestActivity) getActivity()).onUsernameSelect(string, remote, session);
		}
	}; 

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		((CameraTestActivity) getActivity())
			.onUsernameSelect(null, null, null);
	}
}
