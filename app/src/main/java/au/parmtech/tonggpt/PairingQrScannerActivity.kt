package au.parmtech.tonggpt

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

/**
 * Fully embedded local QR reader: no third-party scanning API, browser, upload,
 * analytics, or remote server. Scanned data stays in the local Android activities.
 */
class PairingQrScannerActivity : Activity() {
    companion object {
        const val RESULT_QR_CONTENT = "tg_qr_content"
        private const val CAMERA_REQUEST = 420
    }

    private lateinit var cameraView: DecoratedBarcodeView
    private var finished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        window.statusBarColor = Color.rgb(9, 17, 28)
        window.navigationBarColor = Color.rgb(9, 17, 28)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF09121E.toInt())
        }
        setContentView(root)
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val safe = insets.getInsets(
                    WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars() or
                        WindowInsets.Type.displayCutout()
                )
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        root.requestApplyInsets()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(6))
        }
        header.addView(Button(this).apply {
            text = "← Back"
            isAllCaps = false
            minHeight = dp(48)
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = "Scan Windows pairing QR"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(dp(10), 0, 0, 0)
        })
        root.addView(header)

        root.addView(TextView(this).apply {
            text = "Point your camera at the QR code shown on the Windows screen."
            textSize = 14f
            setTextColor(0xFFB6D3DE.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(12), dp(18), dp(12))
        })
        cameraView = DecoratedBarcodeView(this).apply {
            setStatusText("QR codes are scanned locally on your phone.")
        }
        root.addView(cameraView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(TextView(this).apply {
            text = "Official ChatGPT QR → official ChatGPT pairing.\n" +
                "Tong GPT companion QR → secure Windows Codex connection."
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(0xFF90AFBD.toInt())
            setPadding(dp(20), dp(12), dp(20), dp(20))
        })
        cameraView.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                val content = result?.text ?: return
                if (finished || content.isBlank() || content.length > 8192) return
                finished = true
                cameraView.pause()
                setResult(RESULT_OK, Intent().putExtra(RESULT_QR_CONTENT, content))
                finish()
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })
        requestCamera()
    }

    private fun requestCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) {
            cameraView.resume()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                cameraView.resume()
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes.",
                    Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::cameraView.isInitialized && !finished &&
            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraView.resume()
        }
    }

    override fun onPause() {
        if (::cameraView.isInitialized) cameraView.pause()
        super.onPause()
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density + 0.5f).toInt()
}
