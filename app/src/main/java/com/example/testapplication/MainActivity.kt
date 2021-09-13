package com.example.testapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.File
import java.lang.Exception
import java.net.URI
import android.widget.Toast







class MainActivity : AppCompatActivity(), View.OnClickListener {

    var send: Button? = null
    var stop: Button? = null
    var messageView: TextView? = null
    private var audiorecord: AudioRecord? = null
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    var minBufSize = AudioRecord.getMinBufferSize(SAMPLER, channelConfig, audioFormat)
    var uri = URI.create("wss://dev.staging.kvint.io/api/audio")
    private var mWebSocket: WebSocketClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        send = findViewById(R.id.start_button)
        stop = findViewById(R.id.stop_button)
        messageView = findViewById(R.id.textView1)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }else {
            audiorecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLER,
                channelConfig,
                audioFormat,
                minBufSize
            )
            send!!.setOnClickListener(this)
            stop!!.setOnClickListener(this)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.start_button -> {
                val task = AudioTask()
                task.doInBackground()
            }
            R.id.stop_button -> {
                audiorecord!!.stop()
                mWebSocket!!.close()
                Log.d("Socket Closed", "")
            }
            else -> {
            }
        }
    }

    inner class AudioTask : AsyncTask<String?, Void?, String?>() {
        public override fun doInBackground(vararg p0: String?): String? {
            try {
                mWebSocket = object : WebSocketClient(uri) {
                    override fun onOpen(handshakedata: ServerHandshake?) {
                        Log.d("ServerOpen: ", mWebSocket!!.connection.isConnecting.toString())
                        val buffer = ByteArray(minBufSize)
                        audiorecord!!.startRecording()
                        while (true) {
                            val number = audiorecord!!.read(buffer, 0, minBufSize)
                            mWebSocket!!.send(buffer)
                        }
                    }

                    override fun onMessage(message: String?) {
                        messageView!!.text = message
                        Log.d("ServerMessage: ", message!!)
                    }

                    override fun onClose(code: Int, reason: String, remote: Boolean) {
                        Log.d("ServerClose", "Code = $code, Reason: $reason")
                    }

                    override fun onError(ex: Exception) {
                        Log.d("ServerError: ", ex.toString())
                    }
                }
                (mWebSocket as WebSocketClient).connect()
            } catch (e: Exception) {
                Log.d("Server: ", e.toString())
            }
            return null
        }
    }

    companion object {
        private const val SAMPLER = 48000 //Sample Audio Rate
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                ) {
                    audiorecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLER,
                        channelConfig,
                        audioFormat,
                        minBufSize
                    )
                    send!!.setOnClickListener(this)
                    stop!!.setOnClickListener(this)
                } else {
                    Toast.makeText(
                        this,
                        "Permission denied to read your External storage",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
        }
    }
}