package com.sample.usonic;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;

/* 集音ボタンで、周辺の音を拾って、再生するアプリ */
public class MainActivity extends AppCompatActivity {
	private static final int SAMPLE_RATE = 44100;
	private static final int BLOCK_NUMBER = 300;
	private AudioRecord mAudioRecord = null;
	private AudioTrack mAudioTrack = null;
	private boolean mIsRecording = false;
	private boolean mIsStop = false;
	private short[] mPlayBuf;
	private short[] mRecordBuf;

	@Override
	protected void onDestroy() {
		mIsStop = true;
		super.onDestroy();

		if (mAudioRecord != null) {
			if (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) {
				TLog.d("cleanup mAudioRecord");
				mAudioRecord.stop();
			}
			mAudioRecord = null;
		}
		if (mAudioTrack != null) {
			if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
				TLog.d("cleanup mAudioTrack");
				mAudioTrack.stop();
				mAudioTrack.flush();
			}
			mAudioTrack = null;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		TLog.d("");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/* 権限チェック アプリにAudio権限がなければ要求する。 */
		if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
			/* RECORD_AUDIOの実行権限を要求 */
			requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 2222);
		}

		int bufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE,
				AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT);

		int bufferSizeInShort = bufferSizeInBytes / 2;
		// 録音用バッファ
		mRecordBuf = new short[bufferSizeInShort];
		// 再生用バッファ
		mPlayBuf = new short[bufferSizeInShort * BLOCK_NUMBER];
		// 録音用
		mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
				SAMPLE_RATE,
				AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				bufferSizeInBytes);
		// 再生用
		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				SAMPLE_RATE,
				AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				bufferSizeInBytes,
				AudioTrack.MODE_STREAM);

		/* 集音開始/終了処理定義 */
		findViewById(R.id.btnstartstopaudiorec).setOnClickListener(v -> {
			Button btnstartstop = (Button)v;

			if( btnstartstop.getText().equals("集音開始")) {
				TLog.d( "集音開始");
				runOnUiThread(() -> { btnstartstop.setText("集音停止"); });
				mIsRecording = true;

				new Thread(() -> {
					int count = 0;
					mAudioRecord.startRecording();

					TLog.d("集音開始");
					while (mIsRecording && !mIsStop) {
						mAudioRecord.read(mRecordBuf, 0, bufferSizeInBytes/2);
						/* 再生用バッファはリングバッファとして扱う */
						if (count * bufferSizeInBytes/2 >= mPlayBuf.length) {
							count = 0;
						}
						/* 再生用バッファへ集音したデータをアペンド */
						System.arraycopy(mRecordBuf, 0, mPlayBuf, count * bufferSizeInBytes/2, bufferSizeInBytes/2);
						count++;
					}
					TLog.d("集音終了");
					/* 集音終了 */
					mAudioRecord.stop();
					if (mIsStop) {
						return;
					}
					/* 再生 */
					TLog.d("再生開始");
					runOnUiThread(() -> { btnstartstop.setEnabled(false); });
					mAudioTrack.setPlaybackRate(SAMPLE_RATE);
					mAudioTrack.play();
					mAudioTrack.write(mPlayBuf, 0, count * bufferSizeInBytes/2);
					TLog.d("再生終了");
					mAudioTrack.stop();
					mAudioTrack.flush();
					runOnUiThread(() -> {
						btnstartstop.setEnabled(true);
						btnstartstop.setText("集音開始");
					});
				}).start();
			}
			else {
				TLog.d("集音終了");
				runOnUiThread(() -> { btnstartstop.setText("集音停止処理中"); });
				mIsRecording = false;
			}
		});
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		TLog.d("s");
		/* 権限リクエストの結果を取得する. */
		if (requestCode == 2222) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				/* RECORD_AUDIOの権限を得た */
				TLog.d("RECORD_AUDIOの実行権限を取得!! OK.");
			} else {
				ErrPopUp.create(MainActivity.this).setErrMsg("失敗しました。\n\"許可\"を押下して、このアプリにAUDIO録音の権限を与えて下さい。\n終了します。").Show(MainActivity.this);
			}
		}
		/* 知らん応答なのでスルー。 */
		else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
		TLog.d("e");
	}
}