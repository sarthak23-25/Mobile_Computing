package com.example.mc_a32

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mc_a32.ui.theme.MC_A32Theme
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : ComponentActivity() {
    private lateinit var labels: List<String>
    private lateinit var interpreter: Interpreter
    private lateinit var inputImageBuffer: ByteBuffer
    private lateinit var imageUris: List<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        labels = readLabelsFromAsset(this, "labels.txt")
        setContent {
            MC_A32Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RequestContentPermission()
                }
            }
        }
        interpreter = Interpreter(loadModelFile())
    }

    private fun loadLabels(): List<String> {
        try {
            assets.open("labels.txt").use { labelsInput ->
                val reader = labelsInput.bufferedReader()
                val labelsList = mutableListOf<String>()
                reader.useLines { lines -> lines.forEach { labelsList.add(it) } }
                return labelsList
            }
        } catch (e: IOException) {
            throw RuntimeException("Error loading labels file", e)
        }
    }

    private fun loadModelFile(): ByteBuffer {
        try {
            assets.open("mobilenet_v1_1.0_224_quant.tflite").use { modelInputStream ->
                val modelBytes = modelInputStream.readBytes()

                val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size)
                modelBuffer.order(ByteOrder.nativeOrder())
                modelBuffer.put(modelBytes)
                modelBuffer.rewind()

                return modelBuffer
            }
        } catch (e: IOException) {
            throw RuntimeException("Error loading model file", e)
        }
    }

    private fun runInference(inputImage: ByteBuffer): FloatArray {
        val outputArray = Array(1) { ByteArray(1001) } // Update 1001 to match the number of classes
        interpreter.run(inputImage, outputArray)

        // Convert outputArray from ByteArray to FloatArray
        val floatOutputArray = FloatArray(outputArray[0].size)
        for (i in outputArray[0].indices) {
            floatOutputArray[i] = (outputArray[0][i].toInt() and 0xFF) * 0.00390625f // Apply quantization scale
        }

        return floatOutputArray
    }

    private fun getPredictedClass(outputData: FloatArray): Int {
        var maxIdx = 0
        for (i in 1 until outputData.size) {
            if (outputData[i] > outputData[maxIdx]) {
                maxIdx = i
            }
        }
        return maxIdx
    }

    private fun loadInputImageFromUri(uri: Uri): ByteBuffer {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        // Resize the image to match the input size of your TensorFlow Lite model (224x224)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        // ByteBuffer size should match the expected input size of the model (224x224x3 for uint8 data)
        val byteBuffer = ByteBuffer.allocateDirect(224 * 224 * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        // Assuming the model expects RGB data (3 channels) and uint8 data type
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val px = resizedBitmap.getPixel(x, y)

                // Add RGB values to ByteBuffer
                byteBuffer.put((px shr 16 and 0xFF).toByte())  // Red channel
                byteBuffer.put((px shr 8 and 0xFF).toByte())   // Green channel
                byteBuffer.put((px and 0xFF).toByte())        // Blue channel
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    private fun showToast(context: android.content.Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @Composable
    fun RequestContentPermission() {
        var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
        val context = LocalContext.current
        val bitmaps by remember { mutableStateOf(mutableListOf<Bitmap?>()) }
        var predictedLabels by remember { mutableStateOf<List<String>>(emptyList()) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents()
        ) { uris: List<Uri>? ->
            uris?.let {
                imageUris = it
                loadBitmaps(it, context, bitmaps)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Button(
                onClick = {
                    launcher.launch("image/*")
                },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(text = "Pick Images")
            }

            Button(
                onClick = {
                    val predictions = mutableListOf<String>()

                    // Iterate over each image URI
                    for (uri in imageUris) {
                        // Load and process input image from URI
                        inputImageBuffer = loadInputImageFromUri(uri)

                        // Run inference
                        val outputData = runInference(inputImageBuffer)
                        val predictedClass = getPredictedClass(outputData)

                        // Get the label corresponding to the predicted class
                        val predictedLabel = labels[predictedClass]
                        predictions.add(predictedLabel)
                        println(predictedLabel)
                        println(predictions)
                    }

                    predictedLabels = predictions
                    showToast(context, "Images classified successfully")
                }
            ) {
                Text(text = "Predict Label")
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(imageUris) { index, uri ->
                    val bitmap = bitmaps.getOrNull(index)
                    val predictedLabel = predictedLabels.getOrNull(index)

                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(400.dp)
                                .padding(bottom = 16.dp)
                        )
                    }

                    predictedLabel?.let {
                        Text(
                            text = "Predicted Label: $it",
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }

    private fun readLabelsFromAsset(context: Context, fileName: String): List<String> {
        val labelsList = mutableListOf<String>()

        try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // Assuming each line in the file contains a label
                line?.let { labelsList.add(it) }
            }
            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return labelsList
    }

    private fun loadBitmaps(uris: List<Uri>, context: android.content.Context, bitmaps: MutableList<Bitmap?>) {
        bitmaps.clear()
        for (uri in uris) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    bitmaps.add(bitmap)
                    inputStream.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                bitmaps.add(null)
            }
        }
    }
}

