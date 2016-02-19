package org.masonapps.autoapp.web;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by Bob on 11/28/2015.
 */
public class VolleySingleton {
    private static VolleySingleton instance = null;
    private final Context context;
    private RequestQueue requestQueue = null;

    private VolleySingleton(Context context) {
        this.context = context;
        requestQueue = getRequestQueue();
    }

    public RequestQueue getRequestQueue() {
        if(requestQueue == null){
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }
    
    public <T> void addToRequestQueue(Request<T> request){
        getRequestQueue().add(request);
    }

    public static synchronized VolleySingleton getInstance(Context context) {
        if(instance == null){
            instance = new VolleySingleton(context);
        }
        return instance;
    }
}
