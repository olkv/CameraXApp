package com.example.cameraxapp

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.view.CameraView
import androidx.camera.view.PreviewView
import androidx.core.content.PermissionChecker
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private var imageCapture:ImageCapture ?= null
    private lateinit var outputDirectory: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        outputDirectory = getOutputDirectory()

        if(allPermissionGranded()) {
            //Toast.makeText(this, "Camera Granted", Toast.LENGTH_SHORT).show()
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                Constants.REQUIRED_PERMISSINS,
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }

        val btnCapture_image = findViewById<Button>(R.id.capture_image)

        btnCapture_image.setOnClickListener {
            takeFoto()
        }

    }


    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {mFile->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }

        return if(mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun takeFoto() {
        val imageCapture = imageCapture?: return
        val photoFile = File(
                outputDirectory,
                SimpleDateFormat(
                        Constants.FILE_NMAE_FORMAT,
                        Locale.getDefault()).format(System.currentTimeMillis())+".jpg")

        val outputOptions = ImageCapture.
                                    OutputFileOptions.
                                    Builder(photoFile).
                                    build()

        Log.d(Constants.TAG, photoFile.name.toString())

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo Saved"

                    Toast.makeText(this@MainActivity, "$msg $savedUri", Toast.LENGTH_LONG).show()

                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d(Constants.TAG, "Error ${exception.message}", exception)

                }

            }
        )


    }

    private fun startCamera() {
        val cameraProvideFuture = ProcessCameraProvider.getInstance(this)

        val camera_view = findViewById<PreviewView>(R.id.camera_view)

        cameraProvideFuture.addListener({

            val cameraProvider : ProcessCameraProvider =  cameraProvideFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { mPreview ->
                    mPreview.setSurfaceProvider(
                        camera_view.surfaceProvider
                    )
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector,
                    preview,
                    imageCapture
                )

            } catch (e:Exception) {
                Log.d(Constants.TAG, "starting Camera fail ${e.message}")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode==Constants.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranded()) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permissions NOT Granted", Toast.LENGTH_SHORT).show()
            }
        }

    }


    private fun allPermissionGranded() =
        Constants.REQUIRED_PERMISSINS.all {
            ContextCompat.checkSelfPermission(
                baseContext,
                it
            )==PackageManager.PERMISSION_GRANTED
        }


}