package com.example.ariespaydemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.transsion.manager.AriesPayManager;
import com.transsion.manager.GlobalCache;
import com.transsion.manager.entity.OrderEntity;
import com.transsion.manager.entity.PriceEntity;
import com.transsion.manager.entity.SupportPayInfoEntity;
import com.transsion.manager.inter.OrderQueryCallBack;
import com.transsion.manager.inter.QueryBillingPointCallBack;
import com.transsion.manager.inter.RequestDataCallBack;
import com.transsion.pay.PayUtil;

import java.util.ArrayList;
import java.util.List;

import static com.transsion.manager.AriesPayManager.FAIL_IS_MAIN_THREAD;
import static com.transsion.manager.AriesPayManager.INVAILD_API_KEY;
import static com.transsion.manager.AriesPayManager.INVAILD_AP_ID;
import static com.transsion.manager.AriesPayManager.INVAILD_CONFIGURE;
import static com.transsion.manager.AriesPayManager.INVAILD_CP_ID;
import static com.transsion.manager.AriesPayManager.INVAILD_IMSI;
import static com.transsion.manager.AriesPayManager.INVAILD_MONEY;
import static com.transsion.manager.AriesPayManager.LAST_PAY_NOT_DONE;
import static com.transsion.manager.AriesPayManager.NOT_PERMISSION_SEND_SMS;
import static com.transsion.manager.AriesPayManager.OVER_DAY_OR_MONTH_LIMIT;
import static com.transsion.manager.AriesPayManager.PAY_CANCEL;
import static com.transsion.manager.AriesPayManager.PAY_CONFIG_WRONG;
import static com.transsion.manager.AriesPayManager.PAY_OVERTIME;
import static com.transsion.manager.AriesPayManager.PAY_TOO_OFTEN;

public class TestActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String API_KEY="7360000";
    public static final String AP_ID="1340000";
    public static final String CP_ID="2110000";

    private static final String TAG = "MainActivity";

    private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS=103;

    private TextView payLog;
    private List<SupportPayInfoEntity> supportPayInfoEntities;

    private OrderEntity orderEntity = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        findViewById(R.id.ghanaPay).setOnClickListener(this);
        payLog = findViewById(R.id.textLog);
        GlobalCache.getInstance().setPaynicornDebug(true);
        init();
        findViewById(R.id.queryOrder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AriesPayManager.getsInstance().queryOrderStatus(orderEntity, new OrderQueryCallBack() {
                    @Override
                    public void onSucess(OrderEntity orderEntity) {
                        showLog("queryOrderStatus:success" + ((null == orderEntity)?"":orderEntity.orderNum));
                    }

                    @Override
                    public void onFail(OrderEntity orderEntity) {
                        showLog("queryOrderStatus:fail:" + ((null == orderEntity)?"":orderEntity.orderNum));
                    }

                    @Override
                    public void onPaying(OrderEntity orderEntity) {
                        showLog("queryOrderStatus:onPaying" + ((null == orderEntity)?"":orderEntity.orderNum));
                    }

                    @Override
                    public void notExit(OrderEntity orderEntity) {
                        showLog("queryOrderStatus:notExit" + ((null == orderEntity)?"":orderEntity.orderNum));
                    }

                    @Override
                    public void netError() {
                        showLog("queryOrderStatus:netError");
                    }
                });
            }
        });
    }

    private void init(){
        //设置测试模式
        PayUtil.setTestMode(true);
        PayUtil.setTestMCCMNC("63903");
        PayUtil.setTestMCCMNC2("405880");
        PayUtil.setTestSendPhone("10086");


        AriesPayManager.getsInstance()
                .initAriesPay(this,AP_ID,CP_ID,
                        API_KEY,
                        new QueryBillingPointCallBack() {
                            @Override
                            public void initPayCofigureResult(int result) {
                                showLog("initPayCofigureResult: result--"+result);
                                supportPayInfoEntities = AriesPayManager.getsInstance().getCurrentSupportPayInfo();
                                if(null == supportPayInfoEntities || supportPayInfoEntities.size() <= 0){
                                    showLog("no support pruducts:" + supportPayInfoEntities);
                                }
                                for (SupportPayInfoEntity supportPayInfoEntity : supportPayInfoEntities) {
                                    showLog("supportPayInfoEntities:" + supportPayInfoEntity.mcc + supportPayInfoEntity.mnc + ",tag:" + supportPayInfoEntity.tag);
                                    List<PriceEntity> priceEntities = supportPayInfoEntity.priceEntities;
                                    for (PriceEntity priceEntity : priceEntities) {
                                        showLog("     supportPayInfoEntities--price:" + priceEntity.price + priceEntity.currency);
                                    }
                                }
                            }
                        });
        checkPermission();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.ghanaPay:
//                AriesPayManager.getsInstance().
//                        startRequestPay(TestActivity.this,AP_ID,CP_ID,API_KEY,0.45,"232fdf3434",callBack);
                showPayDialog();
                break;
            default:
                Toast.makeText(this,"no waiting",Toast.LENGTH_SHORT).show();
        }
    }

    private RequestDataCallBack callBack=new RequestDataCallBack() {

        @Override
        public void payFail(int result) {
            Log.i(TAG, "payFail: "+result);
            hidePayingDialog();
            String errorMsg = showPayResultToast(result);
            showLog(errorMsg);
        }

        @Override
        public void sendSmsSuccess(OrderEntity orderEntity) {
            Log.i(TAG, "sendSmsSuccess: ");
            hidePayingDialog();
            TestActivity.this.orderEntity = orderEntity;
            showLog(getResources().getString(R.string.pay_success) + ":" + orderEntity.orderNum);
//            Toast.makeText(TestActivity.this,R.string.pay_success,Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaying(OrderEntity orderEntity) {
            Log.i(TAG, "sendSmsSuccess: ");
            hidePayingDialog();
            TestActivity.this.orderEntity = orderEntity;
            showLog("onPaying:" + orderEntity.orderNum);
//            Toast.makeText(TestActivity.this,R.string.pay_success,Toast.LENGTH_SHORT).show();
        }
    };


    private void showLog(final String log){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(null == payLog) payLog = findViewById(R.id.textLog);
                payLog.append(log + "\n");
            }
        });
    }


    /**
     * 权限申请部分
     */
    @TargetApi(23)
    private void checkPermission(){
        List<String> permissionsList = new ArrayList<String>();
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.SEND_SMS);
        }
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.RECEIVE_SMS);
        }
        if(permissionsList.size() > 0) {
            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS://得到了部分或全部权限，如果有权限必须全部获取之后才能运行，建议二次i检查
//                ...
                break;
        }
    }

    private String showPayResultToast(int code){

        String errorMsg = "";
        switch (code) {
            case FAIL_IS_MAIN_THREAD://不在主线程						100
                errorMsg = "Not in main thread";
                break;
            case OVER_DAY_OR_MONTH_LIMIT://支付次数超过日限或者月限		101
                errorMsg = "The number of payments exceeds the daily limit or monthly limit";
                break;
            case INVAILD_IMSI://无效的IMSI（SIM卡无效或者未插入SIM卡等）	102
                errorMsg = "Invalid imsi";
                break;
            case INVAILD_AP_ID://appid为空								103
                errorMsg = "Appid is null";
                break;
            case INVAILD_CP_ID://cpid为空								104
                errorMsg = "Cpid is null";
                break;
            case INVAILD_API_KEY://apikey为空							105
                errorMsg = "Apikey is null";
                break;
            case INVAILD_CONFIGURE://没有匹配的国家，mcc+mnc				106
                errorMsg = "No matching SIM card";
                break;
            case INVAILD_MONEY://没有匹配的金额							107
                errorMsg = "No matching money";
                break;
            case LAST_PAY_NOT_DONE://上一笔还没有结束					108
                errorMsg = "Previous payment is not finish";
                break;
            case NOT_PERMISSION_SEND_SMS://没有短信权限					111
                errorMsg = "Need send SMS permission";
                break;
            case PAY_CANCEL://用户取消了支付								114
                errorMsg = "Payment cancel";
                break;
            case PAY_OVERTIME://短信发送超时								115
                errorMsg = "Send msg timeOut";
                break;
            case PAY_CONFIG_WRONG://支付配置出错							116
                errorMsg = "Pay config wrong";
                break;
            case PAY_TOO_OFTEN://支付过于频繁							117
                errorMsg = "Payment too often";
                break;

            case SmsManager.RESULT_ERROR_GENERIC_FAILURE://				1
                errorMsg = "Generic failure";
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF://					2
                errorMsg = "Radio off";
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU://					3
                errorMsg = "Null PDU";
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE://					4
                errorMsg = "No SIM service";
                break;
            case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED://				5
                errorMsg = "Reached the sending queue limit";
                break;
            case SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED://		7
                errorMsg = "Premium short codes denied1";
                break;
            case SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED://	8
                errorMsg = "Premium short codes denied2";
                break;

            default:
                errorMsg = "Unknow wrong:" + code;//未知错误
                break;
        }
        return errorMsg;
    }

    private AlertDialog confirmPayDialog;
    private ProgressDialog mProgressDialog;

    private void showPayDialog(){
        //这里的价格在实际项目中需要自己获取，这里做示范写死一个展示
        double pricePre = 0.45;
        String precurrency = "GHC";
        if(null == supportPayInfoEntities || supportPayInfoEntities.size() <= 0) {
            pricePre = 0.45;
            precurrency = "GHC";
        }else{
            pricePre =  supportPayInfoEntities.get(0).priceEntities.get(0).price;
            precurrency =  supportPayInfoEntities.get(0).priceEntities.get(0).currency;
//            pricePre = 0;
//            precurrency = "GHC";
        }
        final double price = pricePre;
        final String currency = precurrency;
        confirmPayDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_payment_title)
                .setMessage(getString(R.string.dialog_payment_msg,price+currency+""))
                .setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if(null != confirmPayDialog)confirmPayDialog.dismiss();
                        hidePayingDialog();
                        showPayingDialog();
                        confirmPayDialog = null;

                        AriesPayManager.getsInstance().
                                startRequestPay(TestActivity.this, AP_ID, CP_ID, API_KEY, price, "232fdf3434", callBack);

//                            AriesPayManager.getsInstance().
//                                    startRequestPay(TestActivity.this, AP_ID, CP_ID, API_KEY, supportPayInfoEntities.get(0).priceEntities.get(0).price, System.currentTimeMillis()+"", callBack);

                    }
                })
                .setNegativeButton(R.string.dialog_cancle, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(null != confirmPayDialog)confirmPayDialog.dismiss();
                        confirmPayDialog = null;
                    }
                })
                .create();
        confirmPayDialog.show();
    }


    private void showPayingDialog(){
        if(null == mProgressDialog) {
            mProgressDialog = new ProgressDialog(TestActivity.this);
            mProgressDialog.setTitle("");
            mProgressDialog.setMessage(getString(R.string.payment_in_doing));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelable(true);
        }
        mProgressDialog.show();
    }

    private void hidePayingDialog(){
        if(null == mProgressDialog)return;
        mProgressDialog.dismiss();
    }
}