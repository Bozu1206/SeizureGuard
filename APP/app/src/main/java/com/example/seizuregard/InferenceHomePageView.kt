package com.example.seizuregard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.seizuregard.dl.metrics.Metrics
import com.example.seizuregard.ui.theme.SeizuregardTheme

@Composable
fun InferenceHomePage(
    metrics: Metrics,
    onPerformInference: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val (titleText, performInferenceButton, metricsColumn, notComputedText) = createRefs()

        Text(text = "Inference Dashboard", modifier = Modifier.constrainAs(titleText) {
            top.linkTo(parent.top, margin = 16.dp)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        },
            style = MaterialTheme.typography.headlineLarge,
        )

        Button(
            onClick = onPerformInference,
            modifier = Modifier
                .constrainAs(performInferenceButton) {
                    top.linkTo(titleText.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .fillMaxWidth(0.8f)  // Button fills 80% of the width
        ) {
            Text(text = "Perform Inference")
        }

        if (metrics.f1 != -1.0) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.constrainAs(metricsColumn) {
                    top.linkTo(performInferenceButton.bottom, margin = 24.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            ) {
                Text(text = "F1 Score: ${"%.4f".format(metrics.f1)}")
                Text(text = "Precision: ${"%.4f".format(metrics.precision)}")
                Text(text = "Recall: ${"%.4f".format(metrics.recall)}")
                Text(text = "FPR: ${"%.4f".format(metrics.fpr)}")
            }
        } else {
            Text(
                text = "Not yet computed",
                modifier = Modifier.constrainAs(notComputedText) {
                    top.linkTo(performInferenceButton.bottom, margin = 24.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InferenceHomePagePreview() {
    SeizuregardTheme {
        InferenceHomePage( Metrics(0.0,0.0,0.0,0.0), {})
    }
}
