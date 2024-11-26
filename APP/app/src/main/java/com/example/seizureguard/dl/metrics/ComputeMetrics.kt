package com.example.seizuregard.dl.metrics

object ComputeMetrics {
    fun computeMetrics(trueLabels: IntArray, predLabels: IntArray): Metrics {
        var tp = 0  // True Positives
        var tn = 0  // True Negatives
        var fp = 0  // False Positives
        var fn = 0  // False Negatives
        for (i in trueLabels.indices) {
            val trueLabel = trueLabels[i]
            val predLabel = predLabels[i]
            when {
                trueLabel == 1 && predLabel == 1 -> tp += 1
                trueLabel == 0 && predLabel == 0 -> tn += 1
                trueLabel == 0 && predLabel == 1 -> fp += 1
                trueLabel == 1 && predLabel == 0 -> fn += 1
            }
        }
        val precision = if (tp + fp > 0) tp.toDouble() / (tp + fp) else 0.0
        val recall = if (tp + fn > 0) tp.toDouble() / (tp + fn) else 0.0
        val f1 = if (precision + recall > 0) 2 * precision * recall / (precision + recall) else 0.0
        val fpr = if (fp + tn > 0) fp.toDouble() / (fp + tn) else 0.0
        return Metrics(
            precision = precision,
            recall = recall,
            f1 = f1,
            fpr = fpr
        )
    }
}