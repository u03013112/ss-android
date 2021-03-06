/*******************************************************************************
 *                                                                             *
 *  Copyright (C) 2017 by Max Lv <max.c.lv@gmail.com>                          *
 *  Copyright (C) 2017 by Mygod Studio <contact-ss-android@mygod.be>  *
 *                                                                             *
 *  This program is free software: you can redistribute it and/or modify       *
 *  it under the terms of the GNU General Public License as published by       *
 *  the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                        *
 *                                                                             *
 *  This program is distributed in the hope that it will be useful,            *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 *  GNU General Public License for more details.                               *
 *                                                                             *
 *  You should have received a copy of the GNU General Public License          *
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                             *
 *******************************************************************************/

package com.github.kxu03013112

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ShortcutManager
//import android.hardware.camera2.CameraAccessException
//import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.util.forEach
import com.crashlytics.android.Crashlytics
import com.github.kxu03013112.database.Profile
import com.github.kxu03013112.database.ProfileManager
import com.github.kxu03013112.utils.*
import com.google.android.gms.common.GoogleApiAvailability
//import com.google.android.gms.samples.vision.barcodereader.BarcodeCapture
//import com.google.android.gms.samples.vision.barcodereader.BarcodeGraphic
import com.google.android.gms.vision.Frame
//import com.google.android.gms.vision.barcode.Barcode
//import com.google.android.gms.vision.barcode.BarcodeDetector
//import xyz.belvi.mobilevisionbarcodescanner.BarcodeRetriever

class ScannerActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ScannerActivity"
        private const val REQUEST_IMPORT = 2
        private const val REQUEST_IMPORT_OR_FINISH = 3
        private const val REQUEST_GOOGLE_API = 4
    }

//    private lateinit var detector: BarcodeDetector

    private fun fallback() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                    "market://details?id=com.github.sumimakito.awesomeqrsample")))
        } catch (_: ActivityNotFoundException) { }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        detector = BarcodeDetector.Builder(this)
//                .setBarcodeFormats(Barcode.QR_CODE)
//                .build()
//        if (!detector.isOperational) {
//            val availability = GoogleApiAvailability.getInstance()
//            val dialog = availability.getErrorDialog(this, availability.isGooglePlayServicesAvailable(this),
//                    REQUEST_GOOGLE_API)
//            if (dialog == null) {
//                Toast.makeText(this, R.string.common_google_play_services_notification_ticker, Toast.LENGTH_SHORT)
//                        .show()
//                fallback()
//            } else {
//                dialog.setOnDismissListener { fallback() }
//                dialog.show()
//            }
//            return
//        }
//        if (Build.VERSION.SDK_INT >= 25) getSystemService<ShortcutManager>()!!.reportShortcutUsed("scan")
//        if (try {
//                    getSystemService<CameraManager>()?.cameraIdList?.isEmpty()
//                } catch (_: CameraAccessException) {
//                    true
//                } != false) {
//            startImport()
//            return
//        }
//        setContentView(R.layout.layout_scanner)
//        setSupportActionBar(findViewById(R.id.toolbar))
//        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
//        val capture = supportFragmentManager.findFragmentById(R.id.barcode) as BarcodeCapture
//        capture.setCustomDetector(detector)
//        capture.setRetrieval(this)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.scanner_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        R.id.action_import_clipboard -> {
            startImport(true)
            true
        }
        else -> false
    }

    /**
     * See also: https://stackoverflow.com/a/31350642/2245107
     */
    override fun shouldUpRecreateTask(targetIntent: Intent?) = super.shouldUpRecreateTask(targetIntent) || isTaskRoot

    private fun startImport(manual: Boolean = false) = startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "image/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }, if (manual) REQUEST_IMPORT else REQUEST_IMPORT_OR_FINISH)
}
