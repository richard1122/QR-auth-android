/*
 * Basic no frills app which integrates the ZBar barcode scanner with
 * the camera.
 * 
 * Created by lisah0 on 2012-02-24
 */
package com.richard1993.qrauth.android;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.richard1993.qrauth.android.CameraPreview;

import android.R.layout;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.Toast;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.widget.TextView;
import android.graphics.ImageFormat;

/* Import ZBar Class files */
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import net.sourceforge.zbar.Config;

public class CameraTestActivity extends Activity
{
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;

    ImageScanner scanner;

    private boolean barcodeScanned = false;
    private boolean previewing = true;

    static {
        System.loadLibrary("iconv");
    }
    
    final Handler handler = new Handler();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        FrameLayout preview = (FrameLayout)findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
    }

    public void onStop() {
        super.onStop();
        Log.i("", "onStop");
        releaseCamera();
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e){
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
            public void run() {
                if (previewing)
                    mCamera.autoFocus(autoFocusCB);
            }
        };

    PreviewCallback previewCb = new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                Camera.Parameters parameters = camera.getParameters();
                Size size = parameters.getPreviewSize();

                Image barcode = new Image(size.width, size.height, "Y800");
                barcode.setData(data);

                int result = scanner.scanImage(barcode);
                
                if (result >= 1 && !barcodeScanned) {
                    SymbolSet syms = scanner.getResults();
                    for (Symbol sym : syms) {
                    	Log.i("", sym.getData() + "Type: " + sym.getType());
                    	if (sym.getType() != Symbol.QRCODE) 
                    		return;
                    	if (processData(sym.getData())) {
                    		barcodeScanned = true;
                    	}
                    }
                }
            }
        };

    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
            public void onAutoFocus(boolean success, Camera camera) {
                autoFocusHandler.postDelayed(doAutoFocus, 1000);
            }
        };
        
    DialogFragment dialogFragment = null;
    private boolean processData(String data) {
    	if (data == null)
    		return false;
    	try {
			JSONObject jsonObject = new JSONObject(data);
			String remote = jsonObject.getString("remote");
			
			if (!jsonObject.isNull("key")) {
				//register mode
				String key = jsonObject.getString("key");
				String username = jsonObject.getString("username");
				
				Intent intent = new Intent(this, RegisterActivity.class);
				intent.putExtra("remote", remote);
				intent.putExtra("username", username);
				intent.putExtra("key", key);
				startActivity(intent);
				finish();
				return true;
			}
			if (!jsonObject.isNull("session")) {
				//login mode
				String session = jsonObject.getString("session");

				DataHelper dataHelper = new DataHelper(CameraTestActivity.this);
				
				if (dataHelper.getUsernameListByRemote(remote).size() == 0) {
					//If no user, toast it.
					Toast.makeText(CameraTestActivity.this, "You haven't registered this website yet.", Toast.LENGTH_LONG).show();
					return false;
				}
				
				if (dataHelper.getUsernameListByRemote(remote).size() == 1) {
					//When only one user registered into this remote address, auto login.
					onUsernameSelect(dataHelper.getUsernameListByRemote(remote).get(0), remote, session);
					return true;
				}
				
				dialogFragment = MyDialogFragment.newInstance(remote, session);
				dialogFragment.show(getFragmentManager(), null);
				return true;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
    }
    
    public void onUsernameSelect(final String username, final String remote, final String session) {
    	if (dialogFragment != null)
    		dialogFragment.dismiss();
    	if (username != null) {
    		Toast.makeText(CameraTestActivity.this, "An user selected and ready to send request to server, please wait.", Toast.LENGTH_LONG).show();
    		new Thread(new Runnable() {
				@Override
				public void run() {
					try {
		    			DataHelper dataHelper = new DataHelper(CameraTestActivity.this);
						HttpClient httpClient = new DefaultHttpClient();
						HttpPost httpPost = new HttpPost(remote);
						
						List<NameValuePair> params = new ArrayList<NameValuePair>();
						params.add(new BasicNameValuePair("username", username));
						params.add(new BasicNameValuePair("session", session));
						
						long t = System.currentTimeMillis() / 1000 / 30;
						
						//Use HMAC SHA512 algorithm , use key(get from register).
						//userName , session, timeStamp / 30 append together as message
						byte[] diagist = TOTP.hmac_sha("HmacSHA512", dataHelper.getKey(remote, username).getBytes(), (username + session + t).getBytes());
						
						StringBuilder sb = new StringBuilder();
					    for (byte b : diagist) {
					        sb.append(String.format("%02x", b));
					    }
					    
						params.add(new BasicNameValuePair("hash", sb.toString()));
						params.add(new BasicNameValuePair("timestamp", String.valueOf(t)));
						Log.i("", sb.toString());
						
						UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params);
						
						httpPost.setEntity(formEntity);
						HttpResponse httpResponse = httpClient.execute(httpPost);
						final JSONObject jsonObject = new JSONObject(EntityUtils.toString(httpResponse.getEntity()));
						final String status = jsonObject.getString("status");
						
						handler.post(new Runnable() {
							@Override
							public void run() {
								if (status.equalsIgnoreCase("success")) {
									//Login success
									Toast.makeText(CameraTestActivity.this, String.format("Login with user %s success.", username), Toast.LENGTH_LONG).show();
									finish();
									return;
								}
								if (status.equalsIgnoreCase("error")) {
									//Login failure
									try {
										Toast.makeText(CameraTestActivity.this, jsonObject.getString("message"), Toast.LENGTH_LONG).show();
										finish();
										return;
									} catch (JSONException e) {
										e.printStackTrace();
									}
								}
								Toast.makeText(CameraTestActivity.this, "Unexpected errror happened", Toast.LENGTH_LONG).show();
								return;
							}
						});
						
					} catch (Exception e) {
						e.printStackTrace();
						handler.post(new Runnable() {

							@Override
							public void run() {
								Toast.makeText(CameraTestActivity.this, "Network connection failed!", Toast.LENGTH_LONG).show();
								
							}
							
						});
					}
				}
			}).start();
    	}
    	barcodeScanned = false;
    }
}
