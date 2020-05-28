package com.reacheng.simlock.provision;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class SimLockProvision extends Activity {
    private static final String TAG = "SimLockProvision";
    private final String PWD = "4600247";
    private final int TRY_TO_GET_PHONE = 100;
    private final int ADDLOCK_ICC_SML = 101;
    private final int ADDLOCK_ICC_SML_COMPLETE = 102;
    private final int UNLOCK_ICC_SML_COMPLETE = 103;
    private static boolean hasAccess;
    private ContentResolver cr;
    private PackageManager pm;
    private Phone mPhone;
    private int mAddedLockCount = 0;
    private int mFailCount = 0;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            switch (msg.what) {
                case TRY_TO_GET_PHONE:
                    synchronized (this) {
                        getPhone();
                    }
                    break;
                case ADDLOCK_ICC_SML:
                case UNLOCK_ICC_SML_COMPLETE:
                    synchronized (this) {
                        setNetworkLock(mPhone, mAddedLockCount);
                    }
                    break;
                case ADDLOCK_ICC_SML_COMPLETE:
                    synchronized (this) {
                        if (ar.exception != null) {
                            if (mFailCount++ < 10) {
                                Log.e(TAG, "add sim lock cause error, retry count:" + mFailCount);
                                sendEmptyMessage(ADDLOCK_ICC_SML);
                            } else {
                                Log.e(TAG, "add sim lock cause error more then 10 times!");
                                setSimLockFlag(false);
                            }
                        } else {
                            if (++mAddedLockCount < 4) {
                                Log.e(TAG, "add sim lock success, mAddedLockCount:" + mAddedLockCount);
                                setNetworkUnlock(mPhone);
                            } else {
                                Log.e(TAG, "add sim lock complete, disable component");
                                setSimLockFlag(true);
                                Settings.Global.putInt(cr, "simlock_provisioned", 1);
                                disableComponent();
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void setNetworkLock(Phone phone, int addedLockCount) {
        String mccmnc = "46000";
        switch (addedLockCount) {
            default:
            case 0:
                mccmnc = "46000";
                break;
            case 1:
                mccmnc = "46002";
                break;
            case 2:
                mccmnc = "46004";
                break;
            case 3:
                mccmnc = "46007";
                break;
        }
        Log.e(TAG, "setNetworkLock, lock count:" + addedLockCount + " mccmnc:" + mccmnc);
        Message addLockCallback = Message.obtain(mHandler, ADDLOCK_ICC_SML_COMPLETE);
        if (phone != null) {
            phone.getIccCard().setIccNetworkLockEnabled(0, 2, PWD,
                    mccmnc, null, null, addLockCallback);
        }
    }

    private void setNetworkUnlock(Phone phone) {
        Message unlockCallback = Message.obtain(mHandler, UNLOCK_ICC_SML_COMPLETE);
        if (phone != null) {
            phone.getIccCard().setIccNetworkLockEnabled(0, 0, PWD,
                    null, null, null, unlockCallback);
        }
    }

    private void disableComponent() {
        // remove this activity from the package manager.
        ComponentName name = new ComponentName(this, SimLockProvision.class);
        pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setGravity(Gravity.LEFT | Gravity.TOP);
        WindowManager.LayoutParams params = window.getAttributes();
        params.x = 0;
        params.y = 0;
        params.height = 1;
        params.width = 1;
        window.setAttributes(params);

        cr = getContentResolver();
        pm = getPackageManager();
        Log.d(TAG, "onCreate hasAccess:" + hasAccess);
        if (!hasAccess) {
            hasAccess = true;
            if (getSimLockFlag() == 2) {
                if (Settings.Global.getInt(cr, "simlock_provisioned", 0) == 0) {
                    Settings.Global.putInt(cr, "simlock_provisioned", 1);
                }
            } else {
                Log.d(TAG, "onCreate NumOfPhone:" + getNumOfPhone());
                mHandler.sendEmptyMessage(TRY_TO_GET_PHONE);
            }
        }
    }

    private void getPhone() {
        for (int phoneId = 0; phoneId < getNumOfPhone(); phoneId++) {
            mPhone = PhoneFactory.getPhone(phoneId);
            if (mPhone != null) break;
        }
        if (mPhone == null) mPhone = PhoneFactory.getDefaultPhone();
        if (mPhone == null) {
            Log.d(TAG, "getPhone mPhone is null, delay 500ms retry.");
            Message msg = Message.obtain();
            msg.what = TRY_TO_GET_PHONE;
            mHandler.sendMessageDelayed(msg, 500);
        } else {
            Log.d(TAG, "getPhone mPhone:" + mPhone + " send message ADDLOCK_ICC_SML");
            mHandler.sendEmptyMessage(ADDLOCK_ICC_SML);
        }
    }

    private final int MAX_PHONE_COUNT = 4;
    private int getNumOfPhone() {
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        return (phoneCount > MAX_PHONE_COUNT) ? MAX_PHONE_COUNT : phoneCount;
    }

    private final String SIMLOCK_PROVISION_FILE = "/data/nvram/APCFG/APRDEB/MIDTEST_KEY";
    private final int SIMLOCK_PROVISION_FLAG = 30;
    private int getSimLockFlag() {
        IBinder binder = ServiceManager.getService("NvRAMAgent");
        NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);
        byte[] buff = null;
        try {
            buff = agent.readFileByName(SIMLOCK_PROVISION_FILE);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "getSimLockFlag buff.length: " + buff.length);

        int flag = -1;
        if (buff.length > SIMLOCK_PROVISION_FLAG) {
            flag = (int) buff[SIMLOCK_PROVISION_FLAG];
        }
        Log.d(TAG, "getSimLockFlag buff[SIMLOCK_PROVISION_FLAG]: " + flag);
        return flag;
    }

    private void setSimLockFlag(boolean success) {
        IBinder binder = ServiceManager.getService("NvRAMAgent");
        NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);
        byte[] buff = null;
        try {
            buff = agent.readFileByName(SIMLOCK_PROVISION_FILE);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        byte result = success ? (byte)0x02 : (byte)0x01;
        if (buff.length > SIMLOCK_PROVISION_FLAG && buff[SIMLOCK_PROVISION_FLAG] != result) {
            buff[SIMLOCK_PROVISION_FLAG] = result;
            try {
                int flag = agent.writeFileByName(SIMLOCK_PROVISION_FILE, buff);
                if (flag > 0) {
                    Log.d(TAG, "setSimLockFlag writeNVRam success!");
                } else {
                    Log.d(TAG, "setSimLockFlag writeNVRam failed!");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (NvRamNative.writeNv()) {
                Log.d(TAG, "setSimLockFlag backup NVRam success!");
            } else {
                Log.e(TAG, "setSimLockFlag backup NVRam failed!");
            }
        } else {
            Log.d(TAG, "setSimLockFlag may no changes, no need writeNVRam!");
        }
    }
}

