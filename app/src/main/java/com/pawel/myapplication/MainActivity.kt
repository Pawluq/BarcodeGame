package com.pawel.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.opengl.Visibility
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDateTime

import java.util.*
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var svBarcode: SurfaceView
    private lateinit var tvBarcode: TextView
    private lateinit var tvNextLoc: TextView
    private lateinit var tvScanned: TextView
    private lateinit var tvCounter: TextView
    private lateinit var startButton: Button
    private lateinit var detector: BarcodeDetector
    private lateinit var cameraSource: CameraSource
    private lateinit var tvTimer: TextView
    private lateinit var barcodeRegistry: Map<String, String>
    private lateinit var bluetooth: ArduinoCommunication
    private lateinit var lastColorSent: String
    private lateinit var startTime: Date
    var startTimeScanned: Int = 0
    var counter: Int = 0
    var timerConst: Int = 100
    var gameRunning: Boolean = false
    var gameStateWaitingForITF: Boolean = false
   private lateinit var vibrator: Vibrator
    var tonG :ToneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 70)


    //  S E T U P

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        barcodeRegistry = mapOf(
            "11274524319022" to "RED",
            "47852967365628" to "GREEN",
            "98357673456313" to "YELLOW",
            "76579296745464" to "PINK",
            "56279524312558" to "ORANGE",
            "47839688278570" to "BLUE"
        )

        svBarcode = findViewById(R.id.sv_barcode)
        vibrator  = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        tvNextLoc = findViewById(R.id.nextLocation)
        tvBarcode = findViewById(R.id.tv_barcode)
        tvCounter = findViewById(R.id.counter)
        tvScanned = findViewById(R.id.tvScanned)
        tvScanned.visibility = View.GONE
        tvTimer = findViewById(R.id.timer)
        bluetooth = ArduinoCommunication()
        bluetooth.initialize()
        lastColorSent = ""
        startButton = findViewById(R.id.startButton)
        startButton.text = "START"
        tvTimer.text = "Timer: 0"
        startTime = Calendar.getInstance().time
        tvCounter.text = "Pakete: 0 / 15"

        startButton.setOnClickListener {
            tvNextLoc.text = "Paket scannen"
            startButton.visibility = View.GONE
            tvTimer.text = "Timer: $startTime"
            counter = 0
            gameRunning = true
            startTime = Calendar.getInstance().time
            tvTimer.visibility = View.VISIBLE
            tvCounter.text = "Pakete: 0 / 15"
        }

        scan()
    }


    fun scan() {


        detector = BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ITF or Barcode.EAN_13).build()
        detector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}

            override fun receiveDetections(detections: Detector.Detections<Barcode>?) {
                if (gameRunning) {
                    val currentTime = Calendar.getInstance().time
                    val currentTimeInSec = currentTime.time / 1000
                    val startTimeInSec = startTime.time / 1000
                    tvTimer.text = "Timer: " + (currentTimeInSec - startTimeInSec).toString()

                    if (startTimeScanned + 300 < (Calendar.getInstance().time.time / 100)) {
                        tvScanned.post {
                            tvScanned.visibility = View.GONE
                        }
                    }

                    val barcodes = detections?.detectedItems
                    if (barcodes!!.size() > 0) {
                        // IF PRODUCT BARCODE
                        if (barcodes.valueAt(0).valueFormat == 5 && !gameStateWaitingForITF) {
                            gameStateWaitingForITF = true
                            tonG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                            vibrator.vibrate(100)
                            tvNextLoc.post{
                                tvNextLoc.text = "Regalfach scannen"
                            }
                            tvScanned.post {
                                tvScanned.visibility = View.VISIBLE
                                val now = Calendar.getInstance().time
                                startTimeScanned = (now.time / 100).toInt()
                            }
                        }


                        tvBarcode.post {
                            tvBarcode.text = barcodes.valueAt(0).displayValue

                            if (gameStateWaitingForITF) {


                                processBarcode(barcodes.valueAt(0))
                            }
                        }
                    }
                }
            }
        }


        )

        cameraSource = CameraSource.Builder(this, detector).setRequestedPreviewSize(1024, 768)
            .setRequestedFps(25f).setAutoFocusEnabled(true).build()

        svBarcode.holder.addCallback(object : SurfaceHolder.Callback2 {
            override fun surfaceRedrawNeeded(holder: SurfaceHolder?) {
            }

            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                cameraSource.stop()
            }

            override fun surfaceCreated(holder: SurfaceHolder?) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    cameraSource.start(holder)
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(android.Manifest.permission.CAMERA),
                        123
                    )
                }
            }
        })
    }


    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cameraSource.start(svBarcode.holder)
            } else {
                Toast.makeText(this, "Scanner won't work without permission", Toast.LENGTH_SHORT)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetooth.disconnect()
        detector.release()
        cameraSource.stop()
        cameraSource.release()
    }


    // L O G I K

    fun processBarcode(barcode: Barcode) {
        if (barcodeRegistry.containsKey(barcode.displayValue)) {
            val dummyCode = barcodeRegistry.get(barcode.displayValue)
            Log.wtf("read barcode:", barcode.displayValue)
            tvNextLoc.text = "Paket scannen"


            tonG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
            vibrator.vibrate(100)

            tvScanned.visibility = View.VISIBLE
            val now = Calendar.getInstance().time
            startTimeScanned = (now.time / 100).toInt()

            counter++
            tvCounter.text = "Pakete: $counter / 15"


            if (counter > 14) {
                startButton.visibility = View.VISIBLE
                gameRunning = false
            }


            gameStateWaitingForITF = false
            sendColorcode(dummyCode!!)
        }
    }

    fun sendColorcode(colorCode: String) {
        // log for testing:
        Log.wtf("read colorCode:", colorCode)

        if (!colorCode.equals(lastColorSent)) {
            bluetooth.send(colorCode);
        }

        lastColorSent = colorCode
    }
}
