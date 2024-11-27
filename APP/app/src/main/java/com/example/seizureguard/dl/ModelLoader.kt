import android.content.Context
import android.content.res.Resources
import com.example.seizureguard.R
import org.jetbrains.kotlinx.dl.onnx.inference.OnnxInferenceModel
import java.io.File

class ModelLoader(private val context: Context) {
    fun loadModel(modelFileName: String, resources: Resources): OnnxInferenceModel {
        val modelBytes = resources.openRawResource(R.raw.base_pat_02).readBytes()
        // Load the model from the temporary file
        val model = OnnxInferenceModel(modelBytes)
        return  model
    }
}