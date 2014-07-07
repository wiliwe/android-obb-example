/*
 * Copyright (C) 2011 Open Source for ALL mankind
 * 
 * This example is based on the sample code of OBB sample code :
 *         AndroidSrc-Honeycomb-3.2/development/samples/Obb 
 */

package com.example.android.obbapp;

import android.app.Activity;
import android.content.res.ObbInfo;
import android.content.res.ObbScanner;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.OnObbStateChangeListener;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * This class provides a basic demonstration of how to manage an OBB file. It
 * provides two buttons: one to mount an OBB and another to unmount an OBB. The
 * main feature is that it implements an OnObbStateChangeListener which updates
 * some text fields with relevant information.
 */
public class ObbMountActivity extends Activity {
    private static final String TAG = "ObbMountActivity";
    private boolean DEBUG = true;
    
    private static String mObbFilePath = null;    
    private static final String mObbKey = "1234";  /* This is the password(key) used when using mkobb.sh to create a OBB file. */
    private static final String mObbFileName = "obbtest.obb"; /* This is the OBB file name. */

    private TextView mStatus = null;
    private TextView mMountPath = null;
    private TextView mFileName = null;
    private TextView mFileContent = null;
    private TextView mFileAmount = null;
    private TextView mObbFile = null;
    private TextView mPackageName = null;

    private StorageManager mSM = null;
    private Button bnMount = null;
    private Button bnUnmount = null;
    private Button bnNextFile = null;
    private File[] mfiles = null;  /* store all files in the mounted OBB file. */
    private int mNumOfFiles = 0; /* Total amount of files contained in currently mounted OBB file */
    private int mIdx = 0; /* zero-based indexing for File array representing all files contained in currently mounted OBB file */
    private ObbInfo mObbInfo = null;
    
    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate our UI from its XML layout description.
        setContentView(R.layout.obb_mount_activity);

        // Hook up button presses to the appropriate event handler.
        bnMount = ((Button) findViewById(R.id.mount));
        bnMount.setOnClickListener(mMountListener);
        
        bnUnmount = ((Button) findViewById(R.id.unmount));
        bnUnmount.setOnClickListener(mUnmountListener);
        bnUnmount.setEnabled(false);
        
        bnNextFile = ((Button) findViewById(R.id.nextfile));
        bnNextFile.setOnClickListener(mNextFileListener);
        bnNextFile.setEnabled(false);
        
        // Text indications of current status
        mStatus = (TextView) findViewById(R.id.status);
        mMountPath = (TextView) findViewById(R.id.mountpath);
        mFileName = (TextView) findViewById(R.id.file);
        mFileContent = (TextView) findViewById(R.id.filecontent);
        mFileAmount = (TextView) findViewById(R.id.fileamount);
        if (mFileAmount != null)
        	mFileAmount.setText("  0");
        mObbFile = (TextView) findViewById(R.id.obbfile);
        mPackageName = (TextView) findViewById(R.id.packagename);
        
        ObbState state = (ObbState) getLastNonConfigurationInstance();

        if (state != null) {
            mSM = state.storageManager;
            mStatus.setText(state.status);
            mMountPath.setText(state.path);
        } else {
            // Get an instance of the StorageManager
            mSM = (StorageManager) getApplicationContext().getSystemService(STORAGE_SERVICE);
        }

        /* mObbFileName is the signed OBB file!!! */
        mObbFilePath = new File(Environment.getExternalStorageDirectory(), mObbFileName).getPath();
    }

    OnObbStateChangeListener mEventListener = new OnObbStateChangeListener() {
        @Override
        public void onObbStateChange(String path, int state) {
            Log.d(TAG, "path=" + path + "; state=" + state);
            
            /* To set the current status description */
            mStatus.setText(getStatusDescription(state));
            
            if (state == OnObbStateChangeListener.ERROR_ALREADY_MOUNTED) {
            	Log.e(TAG, "Already Mounted Error");
            	mSM.unmountObb(mObbFilePath, true, mEventListener);
        	}

            if (state == OnObbStateChangeListener.MOUNTED) {
            	/* To show current mount point of the OBB file  */
            	mMountPath.setText("  " + mSM.getMountedObbPath(mObbFilePath));
                
                /* Try to read all files(if there exist) in mounted OBB file. */
                File mountedObbContent = new File(mSM.getMountedObbPath(mObbFilePath));
                
                if (mountedObbContent.isDirectory() == true) {
                	if (DEBUG)
                	   Log.v(TAG, mountedObbContent + " is a folder");

                	mfiles = mountedObbContent.listFiles();

                	/* If there has more than one files in OBB file. */
                	mNumOfFiles = mfiles.length;
                	/* Set the amount of file in the OBB file. */
                    mFileAmount.setText("  " + String.valueOf(mNumOfFiles));
                                        
                	if (mNumOfFiles > 0)
                    {   
                      if (mNumOfFiles > 1)
                     	 bnNextFile.setEnabled(true);
          			
          			  /* Show the FIRST file content */
                      mIdx = 0;
                      showFile(mIdx);
                      
                	  /* For debugging purpose !!! */
                      if (DEBUG)
                    	 listFilesForDebug();
                	} // end of if (mNumOfFiles > 0)
                	
                	bnUnmount.setEnabled(true);
                	bnMount.setEnabled(false);
                } // end of if (mountedObbContent.isDirectory() == true)
            } else if(state == OnObbStateChangeListener.UNMOUNTED) {
            	/* Clear out the fields */
            	mStatus.setText("");
            	mMountPath.setText("");
                mFileAmount.setText("  0");
                mFileName.setText("");
                mFileContent.setText("");
                mObbFile.setText("");
                mPackageName.setText("");
                
                /* Reset the state of buttons. */
            	bnUnmount.setEnabled(false);
            	bnMount.setEnabled(true);
                bnNextFile.setEnabled(false);
            } else
            	mStatus.setText(getStatusDescription(state));
            
        }
    };

    /** A call-back for when the user presses the back button. */
    OnClickListener mMountListener = new OnClickListener() {
        public void onClick(View v) {
        	 File tmpFile = new File(mObbFilePath);

        	 if((tmpFile.exists()==true) && tmpFile.isFile() == true)
        	 {
        		try {
            		try {
            			mObbInfo = ObbScanner.getObbInfo(mObbFilePath);
            			if(mObbInfo != null) { 
            				mObbFile.setText(" " + mObbInfo.filename);
            				mPackageName.setText(" " + mObbInfo.packageName);
            			}
            		} catch (IllegalArgumentException e) {
            			mStatus.setText(" The OBB file could not be found!");
            			Log.d(TAG, "The OBB file could not be found");
                	} catch (IOException e) {
                		e.printStackTrace();
                		mStatus.setText(" The OBB file could not be read!");
            			Log.d(TAG, "The OBB file could not be read");
    				}                	
            	
                	// We don't need to synchronize here to avoid clobbering the
                	// content of mStatus because the callback comes to our main
                	// looper.
                	/* "mObbKey" is the key used in using "obbtool" to sign an OBB file */
                	if (mSM.mountObb(mObbFilePath, mObbKey, mEventListener)) {
                		mStatus.setText(R.string.attempting_mount);
                	} else {
                		mStatus.setText(R.string.failed_to_start_mount);
                	}
        		} catch (IllegalArgumentException e) {
        			mStatus.setText(R.string.obb_already_mounted);
        			Log.d(TAG, "OBB already mounted");
        		}
        	} else if(tmpFile.exists() == false) {
        		mStatus.setText(" The specified path is not existent!!!");
        		Log.e(TAG, "The specified path is not existent!!!");   
        	}
        	else if(tmpFile.isDirectory() == true) {
        		mStatus.setText(" The specified path is a Folder!!!");
        		Log.e(TAG, "The specified path is a Folder!!!");
        	}

        	/* To release useless objects */
        	tmpFile = null; // Has no reference to object       	
            System.gc();  // To recommend the system to do garbage collection 
        }
    };

    /** A call-back for when the user presses the clear button. */
    OnClickListener mUnmountListener = new OnClickListener() {
        public void onClick(View v) {
            try {
                if (mSM.unmountObb(mObbFilePath, false, mEventListener)) {
                    mStatus.setText(R.string.attempting_unmount);
                } else {
                    mStatus.setText(R.string.failed_to_start_unmount);
                }
            } catch (IllegalArgumentException e) {
                mStatus.setText(R.string.obb_not_mounted);
                Log.d(TAG, "OBB not mounted");
            }
        }
    };

    /** A call-back for when the user presses the clear button. */
    OnClickListener mNextFileListener = new OnClickListener() {
        public void onClick(View v) {
        	/* Show the NEXT file content */
        	if((++mIdx) >= mNumOfFiles)
        	  mIdx = 0;
        	
			showFile(mIdx);
        }
    };
    
    /** To fill OBB file containing files' name and content into the UI fields. */
    private void showFile(int idx)
    {
    	if (idx > mNumOfFiles) {
    		Log.e(TAG, "Error! The index of the file want to be shown is out of boundary");
    		return;
    	}
    	
    	File f = mfiles[idx];
  		Log.v(TAG, "File-" + (mIdx+1) + " : " + f.getName());
		
		BufferedReader br = null;
		try {
			br =  new BufferedReader(new FileReader(f));
        } catch (FileNotFoundException e) {
           e.printStackTrace();
        }
        
		StringBuilder sb = new StringBuilder();                        
        String line;
        try {
           while ((line = br.readLine()) != null) {
               sb.append(line);
               sb.append('\n');
           }
        } catch (IOException e) {
           Log.e(TAG, "Can't read File-" + (mIdx+1) + " : " + f.getName() + "\n");
           Log.e(TAG, "Exception cause --- " + e.getMessage() + "\n");
        } finally {
           Log.v(TAG, "File-" + (mIdx+1) + " : " + f.getName() + " content ==>  " + sb.toString());

		   /* Fill in the file name and file content */
		   mFileName.setText(" " + f.getName());
           mFileContent.setText(" " + sb.toString());

           try {
               br.close();
           } catch (IOException e) {
               // ignore.
           }
        } // end of finally
    }
    
    /** Display all OBB file containing files' name and content to Logcat */
    private void listFilesForDebug() {
    	int i = 1;

    	for(File f : mfiles) 
    	{
    		Log.v(TAG, "File-" + i + " : " + f.getName());

    		BufferedReader br = null;
    		try {
    			br =  new BufferedReader(new FileReader(f));
    		} catch (FileNotFoundException e) {
    			e.printStackTrace();
    		}
         
    		StringBuilder sb = new StringBuilder();                        
    		String line;                    		
    		try {
    			while ((line = br.readLine()) != null) {
    				sb.append(line);
    				sb.append('\n');
    			}
    		} catch (IOException e) {
    			Log.e(TAG, "Can't read File-" + i + " : " + f.getName() + "\n");
    			Log.e(TAG, "Exception cause --- " + e.getMessage() + "\n");
    		} finally {
    			Log.v(TAG, "File-" + i + " : " + f.getName() + " content ==>  " + sb.toString());

    			try {
    				br.close();
    			} catch (IOException e) {
    				// ignore.
    			}
    		} // end of finally

    		i++;
    	} // end of "for" block
    }
    
    /** Display all OBB file containing files' name and content to Logcat */
    private String getStatusDescription(int statusId) {
    	switch(statusId) {
    	case 1:
    		return " The OBB file is Mounted successfully";
    	case 2:
    		return " The OBB file is Unmounted successfully";
    	case 20:
    		return " Internal Error!";
    	case 21:
    		return " Could Not Mount current OBB file!";
    	case 22:
    		return " Could Not Unmount current OBB file!";
    	case 23:
    		return " The OBB file is not mounted, so it can not unmount!";
    	case 24:
    		return " Error! The OBB file is already mounted!";
    	case 25:
    		return " Error! Your Application has no permission to access current OBB file";
    	case -1:
    		return " The OBB file wanted to be mounted is Not Assigned yet!!!";
    	default:
    		return " Unknown Error!";
    	}
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        // Since our OBB mount is tied to the StorageManager, retain it
        ObbState state = new ObbState(mSM, mStatus.getText(), mMountPath.getText());
        return state;
    }

    private static class ObbState {
        public StorageManager storageManager;
        public CharSequence status;
        public CharSequence path;

        ObbState(StorageManager storageManager, CharSequence status, CharSequence path) {
            this.storageManager = storageManager;
            this.status = status;
            this.path = path;
        }
    }
}
