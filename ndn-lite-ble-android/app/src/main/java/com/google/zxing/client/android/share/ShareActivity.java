/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android.share;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.ContactsContract;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.Intents;

import zohar.com.ndn_liteble.MainActivity;
import zohar.com.ndn_liteble.R;
import zohar.com.ndn_liteble.utils.Constant;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.client.android.clipboard.ClipboardInterface;

/**
 * Barcode Scanner can share data like contacts and bookmarks by displaying a QR Code on screen,
 * such that another user can scan the barcode with their phone.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ShareActivity extends Activity {

  private View clipboardButton;
  private Button mCreateQRView;

  private String text;

  private final View.OnClickListener clipboardListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      // Should always be true, because we grey out the clipboard button in onResume() if it's empty
       text = ClipboardInterface.getText(ShareActivity.this).toString();
        requestPermissionStorageOrLaunch();
    }
  };

  private final View.OnKeyListener textListener = new View.OnKeyListener() {
    @Override
    public boolean onKey(View view, int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
        text = ((TextView) view).getText().toString();
        requestPermissionStorageOrLaunch();
        return true;
      }
      return false;
    }
  };

  /**
   * 判断是否请求过该权限，如果请求该权限就运行下一步操作
   */
  private void requestPermissionStorageOrLaunch(){
    if (ContextCompat.checkSelfPermission(ShareActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
      ActivityCompat.requestPermissions(ShareActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constant.PERMISSION_WIRTE_STORAGE);
    }else{
      if (!text.isEmpty() && !text.equals(" ")) {
        launchSearch(text);
      }else{
        Toast.makeText(ShareActivity.this,"输入的内容不能为空",Toast.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,  String[] permissions, int[] grantResults) {
    switch (requestCode){
      case Constant.PERMISSION_WIRTE_STORAGE:
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
          launchSearch(text);
        }else{
          Toast.makeText(ShareActivity.this, "权限授予失败",Toast.LENGTH_SHORT).show();
        }
        break;
    }
  }

  /**
   * 启动二维码显示界面
   *
   * @param text
   */
  private void launchSearch(String text) {
    Intent intent = new Intent(Intents.Encode.ACTION);
    intent.addFlags(Intents.FLAG_NEW_DOC);
    intent.putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
    intent.putExtra(Intents.Encode.DATA, text);
    intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());
    startActivity(intent); // 这里会跳转到EncodeActivity界面显示二维码
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.share);

    mCreateQRView = findViewById(R.id.btn_create_qr);

    clipboardButton = findViewById(R.id.share_clipboard_button);
    clipboardButton.setOnClickListener(clipboardListener);
    final EditText editText = findViewById(R.id.share_text_view);
    editText.setOnKeyListener(textListener);


    mCreateQRView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        text = editText.getText().toString();
        requestPermissionStorageOrLaunch();
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    clipboardButton.setEnabled(ClipboardInterface.hasText(this));
  }


}
