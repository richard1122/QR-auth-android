package com.richard1993.qrauth.android;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends Activity {
	
	TextView usernameTextView, remoteTextView, keyTextView;
	EditText registerEditNameEditText = null;
	Button button = null;
	DataHelper dataHelper = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		
		dataHelper = new DataHelper(this);
		
		usernameTextView = (TextView) findViewById(R.id.register_edit_user);
		remoteTextView = (TextView) findViewById(R.id.register_edit_remote);
		keyTextView = (TextView) findViewById(R.id.register_edit_key);
		button = (Button) findViewById(R.id.register_edit_commit);
		registerEditNameEditText = (EditText) findViewById(R.id.register_edit_name);
		
		Intent intent = getIntent();
		
		Bundle bundle = intent.getExtras();
		
		String remote = bundle.getString("remote");
		String username = bundle.getString("username");
		String key = bundle.getString("key");
		
		usernameTextView.setText(username);
		remoteTextView.setText(remote);
		keyTextView.setText(key);
		button.setOnClickListener(onClickListener);
	}
	
	final View.OnClickListener onClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			String username = usernameTextView.getText().toString(), remote = remoteTextView.getText().toString(), key = keyTextView.getText().toString();
			Editable name = registerEditNameEditText.getEditableText();
			
			if (name.length() < 1) {
				Toast.makeText(RegisterActivity.this, "Please input an item name.", Toast.LENGTH_LONG).show();
				return;
			}
			
			if (dataHelper.register(username, remote, key, name.toString())) {
				Toast.makeText(RegisterActivity.this, String.format("%s Registered successfully.", name.toString()), Toast.LENGTH_LONG).show();
				finish();
				return;
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.register, menu);
		return true;
	}

}
