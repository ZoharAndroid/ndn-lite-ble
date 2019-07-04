package zohar.com.ndn_liteble.utils;

import android.os.AsyncTask;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;



public class SendInterestTaskV2 extends AsyncTask<Name, Integer, Boolean> {

    private static final String TAG = "SendInterestTaskV2";

    Data comebackData = new Data();

    @Override
    protected void onPreExecute() {
        Log.i(TAG,"SendInterestTaskV2 : onPreExecute ");
    }

    @Override
    protected Boolean doInBackground(Name... names) {
        Log.i(TAG, "SendInterestTaskV2 : doInBackground");
        Interest pendingInterest =new Interest(names[0]);


        return null;
    }

    /**
     * 回来的数据包
     */
    private class IncomingData implements OnData{
        @Override
        public void onData(Interest interest, Data data) {
            Log.i(TAG,"获取数据包：" + data.getName().toUri());
            String msg = data.getContent().toString();
            if (msg.length() == 0){
                Log.i(TAG, "数据包为空");
            }else if (msg.length() > 0){
                comebackData.setContent(data.getContent());
            }
        }
    }


}
