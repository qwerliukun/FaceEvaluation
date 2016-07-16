package com.liukunup.facereconevaluation;

import org.json.JSONException;
import org.json.JSONObject;

import com.faceplusplus.api.FaceDetecter;
import com.faceplusplus.api.FaceDetecter.Face;
import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {

	// FIXME 替换成申请的key
	private final static String API_KEY = "67d67eca08ab4d13033a3271832b4f52";
	private final static String API_SECRET = "r1a_LmnbFQncCyI-zYc_Ne9RdjH90-gT";

	private final static int REQUEST_GET_MODEL = 1;
	private final static int REQUEST_GET_PHOTO = 2;

	HandlerThread detectThread = null;
	Handler detectHandler = null;
	FaceDetecter detecter = null;
	HttpRequests request = null;

	ImageView imageViewModel = null;
	ImageView imageViewPhoto = null;

	private Bitmap modelBitmap = null;
	private Bitmap photoBitmap = null;

	private String faceId1 = "";
	private String faceId2 = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		detectThread = new HandlerThread("detect");
		detectThread.start();
		detectHandler = new Handler(detectThread.getLooper());

		detecter = new FaceDetecter();
		detecter.init(this, API_KEY);

		request = new HttpRequests(API_KEY, API_SECRET, true, true);

		imageViewModel = (ImageView) findViewById(R.id.imageview_model);
		imageViewPhoto = (ImageView) findViewById(R.id.imageview_photo);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_infos) {

			new AlertDialog.Builder(MainActivity.this).setTitle("关于")
					.setIcon(R.drawable.facepp_inside)
					.setMessage("Web API:Face++\nMy Blog:www.liukunup.com")
					.setPositiveButton("退出", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							MainActivity.this.finish();
						}
					}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					}).show();
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		detecter.release(this);
	}


	
	public static Bitmap getFaceInfoBitmap(Face[] faceinfos, Bitmap oribitmap) {
		Bitmap tmp;
		tmp = oribitmap.copy(Bitmap.Config.ARGB_8888, true);

		Canvas localCanvas = new Canvas(tmp);
		Paint localPaint = new Paint();
		localPaint.setColor(0xffff0000);
		localPaint.setStyle(Paint.Style.STROKE);
		for (Face localFaceInfo : faceinfos) {
			RectF rect = new RectF(oribitmap.getWidth() * localFaceInfo.left, oribitmap.getHeight() * localFaceInfo.top,
					oribitmap.getWidth() * localFaceInfo.right, oribitmap.getHeight() * localFaceInfo.bottom);
			localCanvas.drawRect(rect, localPaint);
		}
		return tmp;
	}

	public static Bitmap getScaledBitmap(String fileName, int dstWidth) {
		BitmapFactory.Options localOptions = new BitmapFactory.Options();
		localOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(fileName, localOptions);
		int originWidth = localOptions.outWidth;
		int originHeight = localOptions.outHeight;

		localOptions.inSampleSize = originWidth > originHeight ? originWidth / dstWidth : originHeight / dstWidth;
		localOptions.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(fileName, localOptions);
	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.pick_model:
			startActivityForResult(
					new Intent("android.intent.action.PICK", MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
					REQUEST_GET_MODEL);
			break;
		case R.id.pick_photo:
			startActivityForResult(
					new Intent("android.intent.action.PICK", MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
					REQUEST_GET_PHOTO);
			break;
		case R.id.detect:
			detectHandler.post(new Runnable() {
				@Override
				public void run() {

					Face[] faceinfo = detecter.findFaces(modelBitmap);// 进行人脸检测
					if (faceinfo == null) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								Toast.makeText(MainActivity.this, "第一张未发现人脸信息", Toast.LENGTH_LONG).show();
							}
						});
						return;
					}

					// 在线api交互
					try {
						JSONObject jsonObject = request.offlineDetect(detecter.getImageByteArray(),
								detecter.getResultJsonString(), new PostParameters());
						try {
							faceId1 = jsonObject.getJSONArray("face").getJSONObject(0).getString("face_id").toString();
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} catch (FaceppParseException e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
					final Bitmap bit = getFaceInfoBitmap(faceinfo, modelBitmap);
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							imageViewModel.setImageBitmap(bit);
							System.gc();
						}
					});
				}
			});
			detectHandler.post(new Runnable() {
				@Override
				public void run() {

					Face[] faceinfo = detecter.findFaces(photoBitmap);// 进行人脸检测
					if (faceinfo == null) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								Toast.makeText(MainActivity.this, "第二张未发现人脸信息", Toast.LENGTH_LONG).show();
							}
						});
						return;
					}

					// 在线api交互
					try {
						JSONObject jsonObject = request.offlineDetect(detecter.getImageByteArray(),
								detecter.getResultJsonString(), new PostParameters());
						try {
							faceId2 = jsonObject.getJSONArray("face").getJSONObject(0).getString("face_id").toString();
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} catch (FaceppParseException e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
					final Bitmap bit = getFaceInfoBitmap(faceinfo, photoBitmap);
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							imageViewPhoto.setImageBitmap(bit);
							System.gc();
						}
					});
				}
			});
			break;
		case R.id.evaluation:
			detectHandler.post(new Runnable() {
				@Override
				public void run() {
					// 在线api交互
					try {
						JSONObject jsonObject = request.recognitionCompare(
								new PostParameters().setFaceId1(faceId1).setFaceId2(faceId2).setAsync(false));
						try {
							double s = jsonObject.getDouble("similarity");
							Toast.makeText(getApplicationContext(), "similarity:" + s, Toast.LENGTH_LONG).show();
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} catch (FaceppParseException e) {
						e.printStackTrace();
					}

				}
			});
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (resultCode == RESULT_OK) {
			switch (requestCode) {

			case REQUEST_GET_MODEL: {
				if (data != null) {
					final String str;
					Uri localUri = data.getData();
					String[] arrayOfString = new String[1];
					arrayOfString[0] = "_data";
					Cursor localCursor = getContentResolver().query(localUri, arrayOfString, null, null, null);
					if (localCursor == null)
						return;
					localCursor.moveToFirst();
					str = localCursor.getString(localCursor.getColumnIndex(arrayOfString[0]));
					localCursor.close();
					if ((modelBitmap != null) && (!modelBitmap.isRecycled()))
						modelBitmap.recycle();
					modelBitmap = getScaledBitmap(str, 540);
					imageViewModel.setImageBitmap(modelBitmap);
				}
				break;
			}

			case REQUEST_GET_PHOTO: {
				if (data != null) {
					final String str;
					Uri localUri = data.getData();
					String[] arrayOfString = new String[1];
					arrayOfString[0] = "_data";
					Cursor localCursor = getContentResolver().query(localUri, arrayOfString, null, null, null);
					if (localCursor == null)
						return;
					localCursor.moveToFirst();
					str = localCursor.getString(localCursor.getColumnIndex(arrayOfString[0]));
					localCursor.close();
					if ((photoBitmap != null) && (!photoBitmap.isRecycled()))
						photoBitmap.recycle();
					photoBitmap = getScaledBitmap(str, 540);
					imageViewPhoto.setImageBitmap(photoBitmap);
				}
				break;
			}
			}

		}
	}
}