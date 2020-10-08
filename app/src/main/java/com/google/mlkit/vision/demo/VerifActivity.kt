package com.google.mlkit.vision.demo

import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_verif.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class VerifActivity : AppCompatActivity() {
    private var TAG = "VerifyActivity"
    private var MODEL_PATH = "facenet_minify_512.tflite"
    private val FILE_NAME_ARRAY = "array1.txt"
    private lateinit var tfliteModel : MappedByteBuffer
    private lateinit var interpreter : Interpreter
    private lateinit var tImage : TensorImage
    private lateinit var tBuffer : TensorBuffer
    var inputImageHeight = 160
    var inputImageWidth = 160
    private val IMAGE_MEAN = 127.5f
    private val IMAGE_STD = 128f
    private lateinit var textview : TextView
    private lateinit var float_array : FloatArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verif)
        textview = findViewById(R.id.text1)

        val intent = intent
        val image_bitmap = intent.getParcelableExtra<Parcelable>("BitmapImage") as Bitmap

        initializeModel()
        getEmbedding(image_bitmap)
        textview.setText("berhasil input image 1, silakan klik tombol next untuk input image 2")

        var fos_array: FileOutputStream? = null
        try {

            fos_array = applicationContext.openFileOutput(FILE_NAME_ARRAY, Context.MODE_PRIVATE)
            fos_array.write(float_array.joinToString().toByteArray())
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (fos_array != null) {
                try {
                    fos_array.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        next.setOnClickListener {
            val new_intent = Intent(this, StillImageActivity2::class.java)
            // start your next activity
            startActivity(new_intent)
        }

    }

    private fun initializeModel(){
        try{
            tfliteModel = loadModelFile()

//            val delegate = GpuDelegate(GpuDelegate.Options().setQuantizedModelsAllowed(true))
//            val options = (Interpreter.Options()).addDelegate(delegate)

            @Suppress("DEPRECATION")
            interpreter = Interpreter(tfliteModel)

            val probabilityTensorIndex = 0
            val probabilityShape = interpreter.getOutputTensor(probabilityTensorIndex).shape() // {1, EMBEDDING_SIZE}
            val probabilityDataType = interpreter.getOutputTensor(probabilityTensorIndex).dataType()

            // Creates the input tensor
            tImage = TensorImage(DataType.FLOAT32)

            // Creates the output tensor and its processor
            tBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)

            Log.d(TAG, "Model loaded successful")
        } catch (e : IOException){
            Log.e(TAG, "Error reading model", e)
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun getEmbedding(bitmap : Bitmap) : FloatArray {
        tImage = loadImage(bitmap)

        interpreter.run(tImage.buffer, tBuffer.buffer.rewind())

        float_array = tBuffer.floatArray

        return float_array
    }

    private fun loadImage(bitmap : Bitmap) : TensorImage {
        // Loads bitmap into a TensorImage
        tImage.load(bitmap)

        val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                .build()
        return imageProcessor.process(tImage)
    }

//    fun next(view: View) {}
//    val changepage = Intent(applicationContext, StillImageActivity2::class.java)
//    override fun getApplicationContext(): Context {
//        return super.getApplicationContext()
//        startActivity(changepage)
//    }
}
