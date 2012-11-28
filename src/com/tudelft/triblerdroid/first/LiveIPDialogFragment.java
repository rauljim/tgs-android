package com.tudelft.triblerdroid.first;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import me.ppsp.test.R;

public class LiveIPDialogFragment extends DialogFragment {

	private EditText edittext_ = null;
	private String tracker_ = null;
	
    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface LiveIPDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }
    
    // Use this instance of the interface to deliver action events
    LiveIPDialogListener mListener;
    
    // Override the Fragment.onAttach() method to instantiate the LiveIPDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the LiveIPDialogListener so we can send events to the host
            mListener = (LiveIPDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement LiveIPDialogListener");
        }
    }
    
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Build the dialog and set up the button click handlers
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        
        View view = inflater.inflate(R.layout.live_dialog, null);
        edittext_ = (EditText) view.findViewById(R.id.editIP);
        
        builder.setView(view);

        builder.setMessage(R.string.enteripprompt)
               .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   
            		   tracker_ = edittext_.getText().toString();
            		   tracker_ += ":6778";
                	   
                       // Send the positive button event back to the host activity
                       mListener.onDialogPositiveClick(LiveIPDialogFragment.this);
                   }
               })
               .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // Send the negative button event back to the host activity
                       mListener.onDialogPositiveClick(LiveIPDialogFragment.this);
                   }
               });
        return builder.create();
    }
    
    public String getTracker()
    {
    	return tracker_;
    }
}