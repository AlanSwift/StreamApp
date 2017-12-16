package stream.com.streamapp.home;

/**
 * Created by WuYiQuan on 2017/12/13.
 */

import stream.com.streamapp.exception.internetError;

import android.content.ContentValues;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;

import com.bumptech.glide.request.target.DrawableImageViewTarget;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import stream.com.streamapp.constant.regex;
import stream.com.streamapp.db.Assets;
import stream.com.streamapp.db.Bills;
import stream.com.streamapp.login;


public class UpdateData {
    /*
    ** server url
     */
    static final private String mUrl = "http://47.95.245.4/query.php";
    public static String result;
    /*
    ** Return Answer
     */


    /*
    ** Wrap Thread
     */
    private static class myThread extends Thread {
        final private String url;
        final private String query;

        myThread(String url, String query) {
            this.url = url;
            this.query = query;
        }

        @Override
        public void run() {
            try {

                OkHttpClient mClient = new OkHttpClient();
                /*
                ** Request body
                 */
                RequestBody body = new FormBody.Builder()
                        .add("content", query)
                        .build();
                Log.e("fff", "query:" + query);
//                Log.e("fff",body.toString());
                /*
                ** headers, url, body should be here
                 */
                Request tRequest = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();
                Call call = mClient.newCall(tRequest);
                Response tResponse = call.execute();
//                Log.e("eee","***"+tResponse.toString());
                String ans = tResponse.body().string();
                result = ans;

//                Log.e("eee",ans);


            } catch (IOException e) {
                Log.e("fff", "1111");
                e.printStackTrace();
            } catch (Exception e) {
                Log.e("fff", "*****");
                e.printStackTrace();
            }
        }
    }


    public static void UploadBill() throws InterruptedException {
        List<Bills> bills;
        String query = "";
        //添加同步
        bills = DataSupport.where("user_id = ? and state = 1 ", String.valueOf(login.getUser_id())).find(Bills.class);
        for (int i = 0; i < bills.size(); i++) {
            query = "insert into Bills (id, user_id, amount, date, place, inOrOut, type, note, timeStamp ) values( " + bills.get(i).getId() + "," +
                    bills.get(i).getUser_id() + "," + bills.get(i).getAmount() + ", \"" + bills.get(i).getDate() + "\" ,\"" + bills.get(i).getPlace() + "\", \"" +
                    bills.get(i).getInOrOut() + "\",\"" + bills.get(i).getType() + "\", \"" + bills.get(i).getNote() + "\", \"" + bills.get(i).getTimeStamp() + "\" );";
            Log.d("query", query);
            ContentValues values = new ContentValues();
            values.put("state", 9);
            DataSupport.update(Bills.class, values, bills.get(i).getId());
            myThread tThread = new myThread(mUrl, query);
            tThread.start();
        }
        //修改同步
        bills = DataSupport.where("user_id = ? and state = 2 ", String.valueOf(login.getUser_id())).find(Bills.class);
        for (int i = 0; i < bills.size(); i++) {
            query = "UPDATE Bills set amount = " + bills.get(i).getAmount() + " ,date = '" + bills.get(i).getDate() + "' ,place = '" +
                    bills.get(i).getPlace() + "' ,inOrOut = '" + bills.get(i).getInOrOut() + "' ,type = '" + bills.get(i).getType() + "' ,note = '"
                    + bills.get(i).getNote() + "' ,timeStamp = '" + bills.get(i).getTimeStamp() + "' WHERE id = " + bills.get(i).getId() + ";";
            Log.d("query1", query);
            ContentValues values = new ContentValues();
            values.put("state", 9);
            DataSupport.update(Bills.class, values, bills.get(i).getId());
            myThread tThread = new myThread(mUrl, query);
            tThread.start();
        }
        //删除同步
        bills = DataSupport.where("user_id = ? and state = 3 ", String.valueOf(login.getUser_id())).find(Bills.class);
        for (int i = 0; i < bills.size(); i++) {
            query = "DELETE FROM Bills where id = " + bills.get(i).getId() + ";";
            Log.d("query", query);
            DataSupport.delete(Bills.class, bills.get(i).getId());
            myThread tThread = new myThread(mUrl, query);
            tThread.start();
        }

        return;
    }

    public static void downloadBill() throws InterruptedException,internetError {
        result = "";
        int count = DataSupport.where("user_id = ?", String.valueOf(login.getUser_id())).count(Bills.class);
        Log.d("userid", "" + login.getUser_id());
        String latestTimeStamp;
        String query = "";
        Log.d("count", "" + count);
        if (count == 0)
            query = "select id, user_id, amount, date, place, inOrOut, note, type, timeStamp from Bills where user_id = " + login.getUser_id() + ";";
        else {
            latestTimeStamp = DataSupport.max(Bills.class, "timeStamp", String.class);
            query = "select id, user_id, amount, date, place, inOrOut, note, type, timeStamp from Bills where user_id = " + login.getUser_id() + " and timeStamp > '" + latestTimeStamp + "';";
        }
        Log.d("gengxinquery", query);
        myThread tThread = new myThread(mUrl, query);
        tThread.start();
        tThread.join();
        if(result.equals(""))
        {
            throw new internetError("网络连接错误!");
        }
        Log.d("result:", result);
        Pattern r = Pattern.compile(regex.resultPattern);
        Matcher m = r.matcher(result);
        if (m.find()) {
            String res = m.group(0).replaceAll("<br>", "&");
            res = res.replaceAll("\t", "#");
            Log.d("res:", res);
            String[] lines = res.split("&");
            Log.d("length:", "" + lines.length);
            for (int i = 2; i < lines.length; i++) {
                String line = lines[i];
                String[] bill = line.split("#");

                Bills bills = new Bills();
                bills.setId(Integer.valueOf(bill[0]));
                bills.setUser_id(Integer.valueOf(bill[1]));
                bills.setAmount(Double.valueOf(bill[2]));
                bills.setDate(bill[3]);
                bills.setPlace(bill[4]);
                bills.setInOrOut(bill[5]);
                bills.setNote(bill[6]);
                bills.setType(bill[7]);
                bills.setTimeStamp(bill[8]);
                bills.setState(9);
                bills.save();
            }

            return;
        }
    }

    public static void UploadAssets() throws InterruptedException {
        List<Assets> assets;
        String query = "";
        assets = DataSupport.where("user_id = ? and state = 1 ", String.valueOf(login.getUser_id())).find(Assets.class);
        for (int i = 0; i < assets.size(); i++) {
            query = "insert into Assets (id, user_id, amount, date,inOrOut, type, timeStamp ) values( " + assets.get(i).getId() + "," +
                    assets.get(i).getUser_id() + "," + assets.get(i).getAmount() + ", \"" + assets.get(i).getDate() + "\", \"" +
                    assets.get(i).getInOrOut() + "\",\"" + assets.get(i).getType() + "\", \"" + assets.get(i).getTimeStamp() + "\" );";
            Log.d("query", query);
            ContentValues values = new ContentValues();
            values.put("state", 9);
            DataSupport.update(Assets.class, values, assets.get(i).getId());
            myThread tThread = new myThread(mUrl, query);
            tThread.start();
        }
        return;
    }

    public static void downloadAssets() throws InterruptedException,internetError {
        result = "";
        int count = DataSupport.where("user_id = ?", String.valueOf(login.getUser_id())).count(Assets.class);
        String latestTimeStamp;
        String query = "";
        if (count == 0)
            query = "select id, user_id, amount, date, inOrOut, type, timeStamp from Assets where user_id = " + login.getUser_id() + ";";
        else {
            latestTimeStamp = DataSupport.max(Assets.class, "timeStamp", String.class);
            query = "select id, user_id, amount, date, inOrOut, type, timeStamp from Assets where user_id = " + login.getUser_id() + " and timeStamp > '" + latestTimeStamp + "';";
        }
        myThread tThread = new myThread(mUrl, query);
        tThread.start();
        tThread.join();

        if(result.equals(""))
        {
            throw new internetError("网络连接错误");
        }
        Log.d("result:", result);
        Pattern r = Pattern.compile(regex.resultPattern);
        Matcher m = r.matcher(result);
        if (m.find()) {
            String res = m.group(0).replaceAll("<br>", "&");
            res = res.replaceAll("\t", "#");
            String[] lines = res.split("&");
            for (int i = 2; i < lines.length; i++) {
                String line = lines[i];
                String[] asset = line.split("#");

                Assets assets = new Assets();
                assets.setId(Integer.valueOf(asset[0]));
                assets.setUser_id(Integer.valueOf(asset[1]));
                assets.setAmount(Double.valueOf(asset[2]));
                assets.setDate(asset[3]);
                assets.setInOrOut(asset[5]);
                assets.setType(asset[7]);
                assets.setTimeStamp(asset[8]);
                assets.setState(9);
                assets.save();
            }

            return;
        }
    }
}