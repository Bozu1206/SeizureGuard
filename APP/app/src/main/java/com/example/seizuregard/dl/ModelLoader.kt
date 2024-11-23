import android.content.Context
import android.content.res.Resources
import com.example.seizuregard.R
import org.jetbrains.kotlinx.dl.onnx.inference.OnnxInferenceModel
import java.io.File

class ModelLoader(private val context: Context) {

    fun loadModel(modelFileName: String, resources: Resources): OnnxInferenceModel {

        val modelBytes = resources.openRawResource(R.raw.base_pat_02).readBytes()
        val model = OnnxInferenceModel(modelBytes)

        // Load the model from the temporary file
        return  model
    }
}