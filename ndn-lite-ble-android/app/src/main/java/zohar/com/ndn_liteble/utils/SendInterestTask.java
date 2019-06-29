
/*
 * Copyright (C) 2018-2019 Bo Chen
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package zohar.com.ndn_liteble.utils;

import android.os.AsyncTask;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;


public class SendInterestTask extends AsyncTask<Name, Integer, Boolean> {
    private final String TAG = "SendInterestTask";
    Face face = new Face();
    Data comeBackData = new Data();

    public SendInterestTask(Callback callback) {
        this.callback = callback;
    }

    //callback function here
    interface Callback {
         void callbackData(Data data);
    }

    private Callback callback;

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        Log.i(TAG, "onPostExecute:  execute sending interest success!");
        if (this.callback != null) {
            this.callback.callbackData(comeBackData);
        }
        super.onPostExecute(aBoolean);
    }

    @Override
    protected Boolean doInBackground(Name... PendingName) {
        Log.i(TAG, "doInBackground: get into do in background");
        incomingData incomD = new incomingData();
        //String tempName=new Name(PendingName);
        Interest pendingInterest = new Interest(PendingName[0]);
//        pendingInterest.setName(PendingName);
        try {
            face.expressInterest(pendingInterest, incomD);
            face.processEvents();

            // We need to sleep for a few milliseconds so we don't use
            // 100% of

            // the CPU.

            Thread.sleep(50);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return false;
    }

    //Implementation of Ondata, OnTimeout
    private class incomingData implements OnData {
//        @Override
//        public void onNetworkNack(Interest interest, NetworkNack networkNack) {
//            Log.i(TAG, "networkNack for interest:" + interest.getName().toUri());
//            Log.i(TAG, "networkNack:" + networkNack.toString());
//        }
//
//        @Override
//        public void onTimeout(Interest interest) {
//            Log.i(TAG, "Time out for interest:" + interest.getName().toUri());
//        }

        @Override
        public void onData(Interest interest, Data data) {
            Log.i(TAG, "Got data packet with name:" + data.getName().toUri());
            String msg = data.getContent().toString();
            Log.i(TAG, "onData: " + msg);
            if (msg.length() == 0) {
                Log.i(TAG, "Data is null");
            } else if (msg.length() > 0) {
                comeBackData.setContent(data.getContent());
            }
        }
    }
}
