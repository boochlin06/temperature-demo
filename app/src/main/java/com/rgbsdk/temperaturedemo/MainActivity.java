package com.rgbsdk.temperaturedemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android_serialport_api.SerialPort;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private TextView mTxtComAddress;
    private TextView mTxtComBaudRate;
    private Button mBtnSwitch;
    private Button mBtnClear;
    private EditText mEdtList;

    private boolean isRun = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化控件
        mTxtComAddress = findViewById(R.id.txtComAddress);
        mTxtComBaudRate = findViewById(R.id.txtComBaudRate);

        mBtnSwitch = findViewById(R.id.btnSwitch);
        mBtnSwitch.setOnClickListener(this);

        mBtnClear = findViewById(R.id.btnClear);
        mBtnClear.setOnClickListener(this);

        mEdtList = findViewById(R.id.edtList);

        mEdtList.setText("");
        readTemperatureThread();

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSwitch:  //打开与关闭
            {
                if(mBtnSwitch.getText().toString().equals("打开")){
                    isRun = true;
                    mBtnSwitch.setText("关闭");
                    readTemperatureThread();
                }else{
                    isRun = false;
                    mBtnSwitch.setText("打开");
                }
                break;
            }
            case R.id.btnClear:  //清空记录
            {
                mEdtList.setText("");
                break;
            }
            default:
                break;
        }
    }

    //读取体温数据
    public void  updateEtdList(String msg){
        final String txt = msg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEdtList.setText(txt);
                mEdtList.setMovementMethod(ScrollingMovementMethod.getInstance());
                mEdtList.setSelection(txt.length(), txt.length());
            }
        });
    }

    public String getStringDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    public void readTemperatureThread() {
        new Thread() {
            @Override
            public void run() {
                SerialPort serialPort = null;
                OutputStream outputStream = null;
                InputStream inputStream = null;
                String result = "";
                try {
                    serialPort = new SerialPort(new File(mTxtComAddress.getText().toString()), Integer.valueOf(mTxtComBaudRate.getText().toString()), 0);
                    inputStream = serialPort.getInputStream();
                    outputStream = serialPort.getOutputStream();
                    result = "串口地址：" + mTxtComAddress.getText().toString() + ", 波特率：" + mTxtComBaudRate.getText().toString() + ", 打开成功\n";
                } catch (Exception ex) {
                    result = "串口地址：" + mTxtComAddress.getText().toString() + ", 波特率：" + mTxtComBaudRate.getText().toString() + ", 打开失败，失败原因："+ex.getMessage() + "\n" ;
                }

                updateEtdList(result);

                byte[] data = new byte[120];
                if (serialPort != null && inputStream != null) {
                    result += "开始接收模块传递的体温数据，格式如下\n 时间|原始数据|环境温度|体温数据 \n" ;
                    updateEtdList(result);

                    while (isRun) {
                        try {
                            int datalen = inputStream.read(data);
                            if (datalen == 6) {
                                TemperatureInfo temperatureInfo = new TemperatureInfo();
                                temperatureInfo.rawData = String.format("%x, %x, %x, %x, %x, %x", data[0], data[1], data[2], data[3], data[4], data[5]);
                                temperatureInfo.ambientTemp = getTemp(data[2], 25.0f);
                                temperatureInfo.bodyTemp = getTemp(data[3], 36.0f);
                                Log.d("Temperature", temperatureInfo.rawData + "|" + temperatureInfo.ambientTemp + "|" + temperatureInfo.bodyTemp);
                                result += getStringDate() + "|" + temperatureInfo.rawData + "|" + temperatureInfo.ambientTemp + "|" + temperatureInfo.bodyTemp + "\n";
                                updateEtdList(result);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    result = "停止接受数据\n" ;
                    updateEtdList(result);
                }

            }
        }.start();
    }

    public static float getTemp(byte data, float benchmark){
        int value = data & 0xFF;
        float temp = 0.0f;

        if(value> 127){
            temp =  ((value - 0xFF) * 1.0f) / 10;
        }else{
            temp =  (value * 1.0f) / 10;
        }
        return benchmark + temp;
    }
}
