package com.bameng.ui.business;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bameng.BaseApplication;
import com.bameng.R;
import com.bameng.R2;
import com.bameng.config.Constants;
import com.bameng.model.CloseEvent;
import com.bameng.model.OrderDetailOutputModel;
import com.bameng.model.OrderModel;
import com.bameng.model.OrderOutputModel;
import com.bameng.model.PostModel;
import com.bameng.model.UserData;
import com.bameng.service.ApiService;
import com.bameng.service.ZRetrofitUtil;
import com.bameng.ui.base.BaseActivity;
import com.bameng.ui.login.PhoneLoginActivity;
import com.bameng.utils.ActivityUtils;
import com.bameng.utils.AuthParamUtils;
import com.bameng.utils.DateUtils;
import com.bameng.utils.DensityUtils;
import com.bameng.utils.FileUtils;
import com.bameng.utils.SystemTools;
import com.bameng.utils.ToastUtils;
import com.bameng.utils.Util;
import com.bameng.widgets.PhoteZoomView;
import com.bameng.widgets.UserInfoView;
import com.bameng.widgets.custom.FrescoControllerListener;
import com.bameng.widgets.custom.FrescoDraweeController;
import com.facebook.drawee.view.SimpleDraweeView;

import org.greenrobot.eventbus.EventBus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.relex.photodraweeview.PhotoDraweeView;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.bameng.config.Constants.url;


/***
 * 订单详情 界面
 */
public class OrderDetailsActivity extends BaseActivity
        implements UserInfoView.OnUserInfoBackListener, FrescoControllerListener.ImageCallback {

    @BindView(R2.id.titleText)
    TextView titleText;
    @BindView(R2.id.titleLeftImage)
    ImageView titleLeftImage;
    @BindView(R2.id.tvOrderNo)
    TextView tvOrderNo;
    @BindView(R2.id.tvOrderTime)
    TextView tvOrderTime;
    @BindView(R2.id.tvName)
    TextView tvName;
    @BindView(R2.id.tvPhone)
    TextView tvPhone;
    @BindView(R2.id.tvAddress)
    TextView tvAddress;
    @BindView(R2.id.tvRemarks)
    TextView tvRemark;
    @BindView(R2.id.tvOrderStatus)
    TextView tvOrderStatus;
    @BindView(R2.id.ivPicture)
    SimpleDraweeView ivPicture;
    @BindView(R2.id.btnSave)
    Button btnSave;
    @BindView(R2.id.btnUpload)
    Button btnUpload;
    @BindView(R.id.line_coupon)
    View lineCoupon;
    @BindView(R.id.layCoupon)
    LinearLayout layCoupon;
    @BindView(R.id.tvCouponMoney)
    TextView tvCouponMoney;
    @BindView(R.id.tvOrderMoney)
    TextView tvOrderMoney;

    ProgressDialog progressDialog;
    UserInfoView userInfoView;

    String orderId;
    OrderModel orderModel;
    //Bitmap bitmap;
    final  int REQUEST_CODE_UPLOAD=100;
    String bitmapPath;

    PhoteZoomView photeZoomView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);
        ButterKnife.bind(this);
        initView();
        orderId = getIntent().getStringExtra("orderid");
        StartApi();
    }

    @Override
    protected void initView() {
        titleText.setText("订单详情");
        titleLeftImage.setVisibility(View.VISIBLE);
        //Drawable leftDraw = ContextCompat.getDrawable( this , R.mipmap.ic_back);
        //SystemTools.loadBackground(titleLeftImage, leftDraw);
        titleLeftImage.setBackgroundResource(R.drawable.title_left_back);
        titleLeftImage.setImageResource(R.mipmap.ic_back);


    }

    @Override
    protected void StartApi() {
        if(progressDialog==null){
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setMessage("正在拉取订单详情");
        progressDialog.show();

        String timestamp = String.valueOf(System.currentTimeMillis());
        Map<String,String> map = new HashMap<>();
        map.put("version", BaseApplication.getAppVersion());
        map.put("timestamp", timestamp );
        map.put("os", "android");
        map.put("id", orderId);

        String sign = AuthParamUtils.getSign(map);
        map.put("sign",sign);

        ApiService apiService = ZRetrofitUtil.getApiService();
        String token = BaseApplication.readToken();
        Call<OrderDetailOutputModel> call = apiService.orderdetails(token , map);
        call.enqueue(new Callback<OrderDetailOutputModel>() {
            @Override
            public void onResponse(Call<OrderDetailOutputModel> call, Response<OrderDetailOutputModel> response) {
                if(progressDialog!=null)progressDialog.dismiss();
                if(response.code() !=200 || response.body()==null ){
                    ToastUtils.showLongToast(response.message()==null?"服务器开小差了":response.message());
                    return;
                }
                if (response.body().getStatus() == Constants.STATUS_70035) {
                    ToastUtils.showLongToast(response.body().getStatusText());
                    EventBus.getDefault().post(new CloseEvent());
                    ActivityUtils.getInstance().skipActivity(OrderDetailsActivity.this, PhoneLoginActivity.class);
                    return;
                }

                if(response.body().getStatus()!=200){
                    ToastUtils.showLongToast(response.body().getStatusText());
                    return;
                }
                if( response.body().getData() ==null){
                    ToastUtils.showLongToast("返回的数据为空");
                    return;
                }

                orderModel = response.body().getData();
                tvOrderNo.setText( orderModel.getOrderId() );
                tvOrderTime.setText(DateUtils.formatDate( orderModel.getOrderTime() ));
                tvAddress.setText( orderModel.getAddress());
                tvName.setText(orderModel.getUserName());
                tvPhone.setText(orderModel.getMobile());
                tvRemark.setText(orderModel.getRemark());
                tvOrderStatus.setText( orderModel.getStatus() == Constants.ORDER_DEAL ? getString(R.string.deal) : orderModel.getStatus() ==Constants.ORDER_BACK ? getString(R.string.backorder ) : getString(R.string.noDeal));

                BigDecimal zero = new BigDecimal(0);
                if(orderModel.getCashcouponmoney().compareTo(zero)>0){
                    lineCoupon.setVisibility(View.VISIBLE);
                    layCoupon.setVisibility(View.VISIBLE);
                    tvCouponMoney.setText( String.valueOf( orderModel.getCashcouponmoney() ) );
                }else{
                    lineCoupon.setVisibility(View.GONE);
                    layCoupon.setVisibility(View.GONE);
                }
                tvOrderMoney.setText( String.valueOf(orderModel.getFianlamount()) );

//                tvOrderStatus.setEnabled( orderModel.getStatus() == Constants.ORDER_NODEAL  );
//                btnSave.setVisibility(View.GONE);
//                btnUpload.setVisibility(View.GONE);
//                int wpx = DensityUtils.getScreenW(OrderDetailsActivity.this);
//                if( orderModel.getStatus() == Constants.ORDER_DEAL){
//                    FrescoDraweeController.loadImage( ivPicture , wpx , orderModel.getSuccessUrl() , 0 , OrderDetailsActivity.this );
//                }else {
//                    FrescoDraweeController.loadImage(ivPicture, wpx, orderModel.getPictureUrl(), 0 , OrderDetailsActivity.this);
//                }

                setReadMode(orderModel);
            }

            @Override
            public void onFailure(Call<OrderDetailOutputModel> call, Throwable t) {
                if(progressDialog!=null)progressDialog.dismiss();
                Snackbar.make(getWindow().getDecorView(), t.getMessage()==null?"error":t.getMessage(),Snackbar.LENGTH_LONG);
            }
        });
    }

    /***
     * 如果当前用户是盟友，则设置 订单详情 只读模式
     */
    void setReadMode( OrderModel orderModel ){
        if( BaseApplication.UserData().getUserIdentity() == Constants.MENG_ZHU ){
            tvOrderStatus.setEnabled( orderModel.getStatus() == Constants.ORDER_NODEAL  );
            btnSave.setVisibility(View.GONE);
            btnUpload.setVisibility(View.GONE);
            int wpx = DensityUtils.getScreenW(OrderDetailsActivity.this);
            int swid = DensityUtils.dip2px(OrderDetailsActivity.this, 20);
            if( orderModel.getStatus() == Constants.ORDER_DEAL){
                FrescoDraweeController.loadImage( ivPicture , wpx - swid , orderModel.getSuccessUrl() , 0 , OrderDetailsActivity.this );
                //ivPicture.setPhotoUri( Uri.parse( orderModel.getSuccessUrl() ) );
                ivPicture.setTag( orderModel.getSuccessUrl() );
            }else {
                FrescoDraweeController.loadImage(ivPicture, wpx - swid , orderModel.getPictureUrl(), 0 , OrderDetailsActivity.this);
                //ivPicture.setPhotoUri(Uri.parse( orderModel.getPictureUrl() ));
                ivPicture.setTag( orderModel.getPictureUrl() );
            }
        }else {
            tvOrderStatus.setEnabled(false);
            btnUpload.setVisibility(View.GONE);
            btnSave.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @OnClick({R.id.tvOrderStatus,R.id.btnUpload,R.id.btnSave,R.id.ivPicture})
    void onClick(View view ){
        if( view.getId() == R.id.tvOrderStatus){
            setOrderStatus();
        }else if(view.getId() == R.id.btnUpload){
            upload();
        }else if(view.getId() == R.id.btnSave){

            //save();
            String statusstr = tvOrderStatus.getText().toString();
            if( statusstr.equals( getString(R.string.backorder)) ){
                save();
            }else {
                if( bitmapPath==null || bitmapPath.isEmpty() ) {
                    ToastUtils.showLongToast("请上传成交凭证");
                    return;
                }
                uploadData();
            }
        }else if(view.getId()==R.id.ivPicture){
            Object obj = ivPicture.getTag();
            if(obj==null || obj.toString().isEmpty() ) return;
            if(photeZoomView==null){
                photeZoomView=new PhoteZoomView(this);
            }
            photeZoomView.show(  obj.toString() );
        }
    }

    void save(){
        String statusstr = tvOrderStatus.getText().toString();
        int status=0;
        if(statusstr.equals(getString(R.string.deal))){
            status = 1;
        }else if(statusstr.equals(getString(R.string.noDeal))){
            status = 0;
        }else if(statusstr.equals(getString(R.string.backorder))){
            status=2;
        }

        if(progressDialog==null){
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setMessage("正在上传订单数据");
        progressDialog.show();

        String timestamp = String.valueOf(System.currentTimeMillis());
        Map<String,String> map = new HashMap<>();
        map.put("version", BaseApplication.getAppVersion());
        map.put("timestamp", timestamp );
        map.put("os", "android");
        map.put("orderId", orderId);
        map.put("status", String.valueOf(status));

        String sign = AuthParamUtils.getSign(map);
        map.put("sign",sign);
        String token = BaseApplication.readToken();
        ApiService apiService = ZRetrofitUtil.getApiService();
        Call<PostModel> call = apiService.orderUpdate( token , map );
        call.enqueue(new Callback<PostModel>() {
            @Override
            public void onResponse(Call<PostModel> call, Response<PostModel> response) {
                if(progressDialog!=null){
                    progressDialog.dismiss();
                }

                if(response.code() !=200){
                    ToastUtils.showLongToast(response.message());
                    return;
                }
                if(response.body()==null){
                    ToastUtils.showLongToast("返回数据空");
                    return;
                }
                if (response.body().getStatus() == Constants.STATUS_70035) {
                    ToastUtils.showLongToast(response.body().getStatusText());
                    EventBus.getDefault().post(new CloseEvent());
                    ActivityUtils.getInstance().skipActivity(OrderDetailsActivity.this, PhoneLoginActivity.class);
                    return;
                }

                ToastUtils.showLongToast(response.body().getStatusText());
                if(response.body().getStatus() == 200) {
                    OrderDetailsActivity.this.finish();
                }
            }

            @Override
            public void onFailure(Call<PostModel> call, Throwable t) {
                if(progressDialog!=null){
                    progressDialog.dismiss();
                }
                ToastUtils.showLongToast("请求失败"+ t.getMessage()==null?"":t.getMessage());
            }
        });

    }

    void uploadData(){

        if(progressDialog==null){
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setMessage("正在上传成交凭证中...");
        progressDialog.show();

        String customer = orderModel.getUserName();
        String phone = orderModel.getMobile();
        String price = orderModel.getMoney();
        String remarks = orderModel.getRemark();

        String timestamp = String.valueOf(System.currentTimeMillis());
        Map<String,String> map = new HashMap<>();
        map.put("version", BaseApplication.getAppVersion());
        map.put("timestamp", timestamp );
        map.put("os", "android");
        map.put("orderId", orderModel.getOrderId());
        map.put("customer",customer);
        map.put("mobile",phone );
        map.put("price",price);
        map.put("memo",remarks);

        AuthParamUtils authParamUtils = new AuthParamUtils();
        String sign = authParamUtils.getSign(map);

        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        RequestBody requestBody = RequestBody.create( MediaType.parse("text/plain") , timestamp );
        requestBodyMap.put("timestamp",requestBody);
        requestBody=RequestBody.create(MediaType.parse("text/plain"), BaseApplication.getAppVersion());
        requestBodyMap.put("version",requestBody);
        requestBody=RequestBody.create(MediaType.parse("text/plain"), "android");
        requestBodyMap.put("os",requestBody);

        requestBody=RequestBody.create(MediaType.parse("text/plain"), orderModel.getOrderId() );
        requestBodyMap.put("orderId",requestBody);

        requestBody=RequestBody.create(MediaType.parse("text/plain"), customer );
        requestBodyMap.put("customer",requestBody);

        requestBody=RequestBody.create(MediaType.parse("text/plain"), phone );
        requestBodyMap.put("mobile",requestBody);

        requestBody=RequestBody.create(MediaType.parse("text/plain"), price );
        requestBodyMap.put("price",requestBody);

        requestBody=RequestBody.create(MediaType.parse("text/plain"), remarks );
        requestBodyMap.put("memo",requestBody);

        requestBody=RequestBody.create(MediaType.parse("text/plain"), sign);
        requestBodyMap.put("sign",requestBody);


        byte[] buffer = Util.File2byte( bitmapPath );

        requestBody = RequestBody.create(MediaType.parse("image/*"), buffer );
        requestBodyMap.put("image\"; filename=\"" + timestamp + "\"", requestBody);


        String token = BaseApplication.readToken();
        ApiService apiService = ZRetrofitUtil.getApiService();
        Call<PostModel> call = apiService.UploadSuccessVoucher( token , requestBodyMap );
        call.enqueue(new Callback<PostModel>() {
            @Override
            public void onResponse(Call<PostModel> call, Response<PostModel> response) {
                if(progressDialog!=null)progressDialog.dismiss();
                if(response.code() !=200){
                    ToastUtils.showLongToast(response.message());
                    return;
                }
                if(response.body()==null){
                    ToastUtils.showLongToast("返回数据空");
                    return;
                }
                if (response.body().getStatus() == Constants.STATUS_70035) {
                    ToastUtils.showLongToast(response.body().getStatusText());
                    EventBus.getDefault().post(new CloseEvent());
                    ActivityUtils.getInstance().skipActivity(OrderDetailsActivity.this, PhoneLoginActivity.class);
                    return;
                }

                if(response.body().getStatus()!=200){
                    ToastUtils.showLongToast(response.body().getStatusText());
                    return;
                }
                save();
            }

            @Override
            public void onFailure(Call<PostModel> call, Throwable t) {
                if(progressDialog!=null)progressDialog.dismiss();
                ToastUtils.showLongToast("请求失败"+ t.getMessage()==null?"":t.getMessage());
            }
        });

    }

    void upload(){
        Intent intent = new Intent(this,UploadDocumentsActivity.class);
        Bundle bd = new Bundle();
        bd.putSerializable("order",(Serializable) orderModel);
        intent.putExtras(bd);
        intent.putExtra("bitmapPath",bitmapPath);
        ActivityUtils.getInstance().showActivityForResult(this , REQUEST_CODE_UPLOAD , intent );
    }

    void setOrderStatus(){
        if(userInfoView==null){
            userInfoView = new UserInfoView(this);
            userInfoView.setOnUserInfoBackListener(this);
        }
        userInfoView.show(UserInfoView.Type.OrderStatus , tvOrderStatus.getText().toString().trim() );
    }

    @Override
    public void onUserInfoBack(UserInfoView.Type type, String value) {
        if(type==null) return;

        tvOrderStatus.setText( value );

        btnSave.setVisibility( value.equals( getString(R.string.noDeal) ) ? View.GONE : View.VISIBLE );

        btnUpload.setVisibility( value.equals( getString(R.string.deal) ) ? View.VISIBLE: View.GONE );

    }

    @Override
    public void imageCallback(int position, int width, int height) {

        if( ivPicture==null) return;

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width,height);
        ivPicture.setLayoutParams(layoutParams);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK)return;

        if(requestCode == REQUEST_CODE_UPLOAD){
            OrderModel temp = (OrderModel) data.getExtras().getSerializable("order");
            orderModel.setUserName(temp.getUserName());
            tvName.setText( temp.getUserName() );
            orderModel.setMobile(temp.getMobile());
            tvPhone.setText( temp.getMobile() );
            orderModel.setRemark(temp.getRemark());
            tvRemark.setText( temp.getRemark() );
            orderModel.setMoney(temp.getMoney());

            tvOrderMoney.setText( temp.getMoney() );

            bitmapPath = data.getStringExtra("bitmapPath");

            String imagePath = "file://"+ bitmapPath;

            ivPicture.setImageURI( imagePath );

            ivPicture.setTag(  imagePath );

        }
    }
}
