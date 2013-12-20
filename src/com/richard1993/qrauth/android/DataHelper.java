package com.richard1993.qrauth.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.Resources.NotFoundException;

public class DataHelper {
	private String PREFERENCE = "USER_LIST_";
	
	private Context context;
	private List<Map<String, String>> list = null;
	
	public DataHelper(Context context) {
		this.context = context;
		reload();
	}
	
	public boolean register(String username, String remote, String key, String name) {
		for (Map<String, String> map : list) {
			if (remote.equalsIgnoreCase(map.get("remote")) && username.equalsIgnoreCase(map.get("username"))) {
				map.put("key", key);
				map.put("name", name);
				save();
				return true;
			}
			
			if (remote.equalsIgnoreCase(map.get("name"))) {
				return false;
			}
		}

		HashMap<String, String> hashMap = new HashMap<String, String>();
		hashMap.put("username", username);
		hashMap.put("remote", remote);
		hashMap.put("key", key);
		hashMap.put("name", name);
		list.add(hashMap);
		save();
		return true;
	}
	
	public List<String> getUsernameListByRemote(String remote) {
		List<String> userList = new ArrayList<String>();
		for (Map<String, String> map : list) {
			if (remote.equalsIgnoreCase(map.get("remote"))) {
				userList.add(map.get("username"));
			}
		}
		return userList;
	}
	
	public String getKey (String remote, String username) {
		for (Map<String, String> map : list) {
			if (remote.equalsIgnoreCase(map.get("remote")) && username.equalsIgnoreCase(map.get("username")))
				return map.get("key");
		}
		return null;
	}
	
	private void reload() {
		this.list = new ArrayList<Map<String,String>>();
		try {
			String string = context.getSharedPreferences(PREFERENCE, 0).getString(PREFERENCE, null);
			JSONArray jsonArray = new JSONArray(string);
			for (int i = 0; i != jsonArray.length(); ++i) {
				Map<String, String> map = new HashMap<String, String>();
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				
				map.put("username", jsonObject.getString("username"));
				map.put("remote", jsonObject.getString("remote"));
				map.put("key", jsonObject.getString("key"));
				map.put("name", jsonObject.getString("name"));
				
				this.list.add(map);
			}
		} catch (Exception e) {
			
		}
	}
	
	private void save() {
		JSONArray jsonArray = new JSONArray();
		for(Map<String, String> map : list) {
			jsonArray.put(new JSONObject(map));
		}
		context.getSharedPreferences(PREFERENCE,  0)
			.edit()
			.putString(PREFERENCE, jsonArray.toString())
			.commit();
	}
	
}
