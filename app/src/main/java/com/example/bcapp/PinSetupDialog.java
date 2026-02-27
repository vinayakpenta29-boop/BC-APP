package com.example.bcapp;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.EditText;

public class PinSetupDialog {

    public interface PinCallback{
        void onPinSet(String pin);
    }

    public static void show(Context c, PinCallback callback){

        EditText pin = new EditText(c);
        pin.setHint("Enter 4 digit MPIN");
        pin.setInputType(18);

        new AlertDialog.Builder(c)
                .setTitle("Set MPIN")
                .setView(pin)
                .setPositiveButton("Save",(d,w)->{

                    String p = pin.getText().toString();

                    if(p.length()==4)
                        callback.onPinSet(p);

                }).show();
    }
}
