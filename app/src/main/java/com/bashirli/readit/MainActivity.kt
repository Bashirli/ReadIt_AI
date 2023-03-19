package com.bashirli.readit

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import android.util.Rational
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.internal.annotation.CameraExecutor
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.PermissionChecker
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale

import com.bashirli.readit.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions



class MainActivity : AppCompatActivity() {

    private lateinit var binding:ActivityMainBinding
    private lateinit var cameraExecuter:ExecutorService
    private var imageCapture: ImageCapture? = null
    private  val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.imageCaptureB2utton.setOnClickListener {
            println("a")
            takePhoto()
        }
        cameraExecuter=Executors.newSingleThreadExecutor()
        requestPermission()
    }

    private fun requestPermission(){
        requestCameraPermissionIfMissing{
            granted->
            if(granted){
                startCamera()
            }else{
                Toast.makeText(this@MainActivity,"PLease Allow the..",Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun requestCameraPermissionIfMissing(onResult:((Boolean)->Unit)){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){
            onResult(true)
        }else{
            registerForActivityResult(ActivityResultContracts.RequestPermission()){
                onResult(it)
            }.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera(){
    val cameraProvider=ProcessCameraProvider.getInstance(this)
    cameraProvider.addListener(
        {
            imageCapture = ImageCapture.Builder().build()
           val cameraProvider2=cameraProvider.get()
            val previewUseCase= Preview.Builder().setTargetAspectRatio(RATIO_4_3).build()
            cameraProvider2.unbindAll()
            previewUseCase.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            cameraProvider2.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,previewUseCase,imageCapture)

        },ContextCompat.getMainExecutor(this)
    )
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        println("1")

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    println(exc.localizedMessage)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    var result: Text?=null
                    println("2")
                    val image=InputImage.fromFilePath(this@MainActivity,output.savedUri!!)
                    val test=recognizer.process(image).addOnSuccessListener {
                        result=it
                        var elementText=""
                        var lineText=""
                        var blockText=""

                        val resultText = result!!.text
                        for (block in result!!.textBlocks) {
                            blockText = block.text
                            val blockCornerPoints = block.cornerPoints
                            val blockFrame = block.boundingBox
                            for (line in block.lines) {
                                lineText = line.text
                                val lineCornerPoints = line.cornerPoints
                                val lineFrame = line.boundingBox
                                for (element in line.elements) {
                                    elementText = element.text
                                    val elementCornerPoints = element.cornerPoints
                                    val elementFrame = element.boundingBox
                                }
                            }
                        }
                        binding.textView.setText("element : ${elementText} \n"+
                                "lineText : ${lineText} \n"+"blockText : ${blockText} \n")

                        println(it.toString())
                    }.addOnFailureListener {
                        println(it.localizedMessage)
                    }


                }
            }
        )
    }

}