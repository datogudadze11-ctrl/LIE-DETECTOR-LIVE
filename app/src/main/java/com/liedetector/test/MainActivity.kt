package com.liedetector.test

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LieDetectorApp() }
    }
}

@Composable
fun LieDetectorApp() {
    var tab by remember { mutableStateOf(0) }
    var text by remember { mutableStateOf("") }
    var score by remember { mutableStateOf<Int?>(null) }
    var reasons by remember { mutableStateOf(listOf<String>()) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6C4DFF),
            background = Color(0xFFF7F7FA)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F7FA)) {
            Column(Modifier.fillMaxSize()) {
                Header()
                when (tab) {
                    0 -> TextAnalyzer(
                        text = text,
                        onText = { text = it; score = null },
                        score = score,
                        reasons = reasons,
                        onAnalyze = {
                            val result = analyze(text)
                            score = result.first
                            reasons = result.second
                        }
                    )
                    1 -> VideoAnalyzer()
                    else -> HistoryTab()
                }
                NavigationBar {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                        icon = { Text("📝") }, label = { Text("Text") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                        icon = { Text("🎥") }, label = { Text("Video") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 },
                        icon = { Text("📊") }, label = { Text("History") })
                }
            }
        }
    }
}

@Composable
fun Header() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("Lie Detector AI", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Credibility & deception analysis", color = Color.Gray, fontSize = 14.sp)
    }
}

// ---------------------------------------------------------------------------
// TEXT TAB
// ---------------------------------------------------------------------------

@Composable
fun TextAnalyzer(
    text: String,
    onText: (String) -> Unit,
    score: Int?,
    reasons: List<String>,
    onAnalyze: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Paste a message", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = text,
            onValueChange = onText,
            modifier = Modifier.fillMaxWidth().height(180.dp),
            placeholder = { Text("Paste an SMS, chat message or statement here…") },
            shape = RoundedCornerShape(16.dp)
        )
        Button(
            onClick = onAnalyze,
            enabled = text.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("ANALYZE", fontWeight = FontWeight.Bold) }

        if (score != null) {
            ResultCard(score, reasons)
        } else {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("How it works", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("This demo uses linguistic signals and contradictions to estimate how suspicious a statement sounds. It does not prove whether a person is actually lying.")
                }
            }
        }
    }
}

@Composable
fun ResultCard(score: Int, reasons: List<String>) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Deception probability", color = Color.Gray)
            Text("$score%", fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Text(
                when {
                    score >= 75 -> "Highly suspicious"
                    score >= 50 -> "Suspicious"
                    score >= 30 -> "Uncertain"
                    else -> "Mostly credible"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            Text("Why?", fontWeight = FontWeight.Bold)
            reasons.forEach { Text("• $it") }
            Spacer(Modifier.height(4.dp))
            Text(
                "⚠ This is an AI-style estimate, not proof of deception.",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

fun analyze(input: String): Pair<Int, List<String>> {
    val t = input.lowercase()
    var score = 18
    val reasons = mutableListOf<String>()

    val defensive = listOf(
        "გეფიცები", "მართლა", "ნამდვილად", "trust me", "believe me",
        "i swear", "honestly", "to be honest", "სიმართლეს გეუბნები"
    )
    val vague = listOf(
        "ალბათ", "როგორც მახსოვს", "არ ვიცი", "maybe", "probably",
        "i think", "not sure", "somewhere", "later", "some time"
    )
    val absolute = listOf(
        "არასდროს", "ყოველთვის", "never", "always", "100%"
    )

    if (defensive.any { t.contains(it) }) {
        score += 18
        reasons += "Strong reassurance / defensive wording detected."
    }
    if (vague.any { t.contains(it) }) {
        score += 12
        reasons += "Vague or uncertain wording reduces credibility."
    }
    if (absolute.any { t.contains(it) }) {
        score += 10
        reasons += "Absolute claims can be a suspicious linguistic signal."
    }
    if (input.length < 35) {
        score += 8
        reasons += "Very short statement provides limited verifiable detail."
    } else if (input.length > 300) {
        score += 5
        reasons += "Long explanation contains more opportunities for inconsistency."
    }
    val exclamations = input.count { it == '!' }
    if (exclamations >= 2) {
        score += 7
        reasons += "Repeated exclamation marks suggest heightened emphasis."
    }

    score = min(score, 96)
    if (reasons.isEmpty()) reasons += "No strong deception signals were detected."
    return score to reasons.take(4)
}

// ---------------------------------------------------------------------------
// VIDEO TAB
// ---------------------------------------------------------------------------

data class FrameMetrics(
    val smiling: Float,
    val leftEyeOpen: Float,
    val rightEyeOpen: Float,
    val headEulerY: Float,
    val headEulerZ: Float
)

data class VideoVerdict(val score: Int, val isLie: Boolean, val reasons: List<String>)

@Composable
fun VideoAnalyzer() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var verdict by remember { mutableStateOf<VideoVerdict?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val pickVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            videoUri = uri
            verdict = null
            errorMsg = null
        }
    }

    val captureVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success && pendingCaptureUri != null) {
            videoUri = pendingCaptureUri
            verdict = null
            errorMsg = null
        }
    }

    fun startRecording() {
        val dir = File(context.cacheDir, "captured_videos").apply { mkdirs() }
        val file = File(dir, "clip_${System.currentTimeMillis()}.mp4")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCaptureUri = uri
        captureVideoLauncher.launch(uri)
    }

    fun runAnalysis() {
        val uri = videoUri ?: return
        isProcessing = true
        errorMsg = null
        verdict = null
        scope.launch {
            val frames = extractFrames(context, uri)
            if (frames.isEmpty()) {
                errorMsg = "Couldn't read frames from this video. Try a different clip."
                isProcessing = false
                return@launch
            }
            val metrics = frames.mapNotNull { analyzeFrame(it) }
            if (metrics.isEmpty()) {
                errorMsg = "No face was clearly detected. Make sure the face is visible and well-lit, facing the camera."
                isProcessing = false
                return@launch
            }
            verdict = computeVerdict(metrics)
            isProcessing = false
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Video statement check", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Record or upload a short clip of someone answering a question. This experimental feature looks at eye contact, blinking and expression patterns.",
            fontSize = 13.sp,
            color = Color.Gray
        )
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))) {
            Text(
                "⚠ Not a real lie detector. Facial expressions and body language are not scientifically reliable indicators of honesty — please don't use this to make real decisions about someone.",
                Modifier.padding(14.dp),
                fontSize = 12.sp,
                color = Color(0xFF7A5B00)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { startRecording() }, modifier = Modifier.weight(1f)) {
                Text("🎥 RECORD")
            }
            OutlinedButton(onClick = { pickVideoLauncher.launch("video/*") }, modifier = Modifier.weight(1f)) {
                Text("📁 UPLOAD")
            }
        }

        if (videoUri != null) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Video ready", fontWeight = FontWeight.SemiBold)
                    Text("Tap analyze to check this clip.", color = Color.Gray, fontSize = 13.sp)
                }
            }
            Button(
                onClick = { runAnalysis() },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("ANALYZE VIDEO", fontWeight = FontWeight.Bold)
                }
            }
        }

        errorMsg?.let { msg ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Text(msg, Modifier.padding(16.dp), color = Color(0xFFB00020))
            }
        }

        verdict?.let { VideoResultCard(it) }
    }
}

@Composable
fun VideoResultCard(v: VideoVerdict) {
    val bgColor = if (v.isLie) Color(0xFFE53935) else Color(0xFF43A047)
    val label = if (v.isLie) "LIE" else "TRUE"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(label, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("${v.score}% suspicion score", color = Color.White.copy(alpha = 0.9f))
            }
        }

        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What we noticed", fontWeight = FontWeight.Bold)
                v.reasons.forEach { Text("• $it") }
                HorizontalDivider()
                Text(
                    "⚠ Body language and facial expressions are not reliable indicators of honesty. Treat this as an experimental novelty, not a real verdict about anyone.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/** Extracts a handful of evenly spaced frames from the video at [uri]. */
suspend fun extractFrames(context: Context, uri: Uri, count: Int = 6): List<Bitmap> =
    withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            if (durationMs <= 0) return@withContext frames
            val stepMs = durationMs / (count + 1)
            for (i in 1..count) {
                val timeUs = (stepMs * i) * 1000
                val bmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bmp != null) frames.add(bmp)
            }
        } catch (_: Exception) {
            // return whatever frames were collected so far
        } finally {
            retriever.release()
        }
        frames
    }

/** Runs ML Kit face detection on a single frame and extracts the relevant signals. */
suspend fun analyzeFrame(bitmap: Bitmap): FrameMetrics? {
    val options = FaceDetectorOptions.Builder()
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .build()
    val detector = FaceDetection.getClient(options)
    return try {
        val image = InputImage.fromBitmap(bitmap, 0)
        val faces = detector.process(image).await()
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: return null
        FrameMetrics(
            smiling = face.smilingProbability ?: -1f,
            leftEyeOpen = face.leftEyeOpenProbability ?: -1f,
            rightEyeOpen = face.rightEyeOpenProbability ?: -1f,
            headEulerY = face.headEulerAngleY,
            headEulerZ = face.headEulerAngleZ
        )
    } catch (_: Exception) {
        null
    } finally {
        detector.close()
    }
}

/**
 * Combines the per-frame facial signals into a heuristic 0-100 "suspicion score".
 * This is a novelty heuristic, not a validated deception-detection method.
 */
fun computeVerdict(metricsList: List<FrameMetrics>): VideoVerdict {
    var score = 22
    val reasons = mutableListOf<String>()

    val gazeAngles = metricsList.map { it.headEulerY.toDouble() }
    val gazeMean = gazeAngles.average()
    val gazeVariance = gazeAngles.map { (it - gazeMean) * (it - gazeMean) }.average()

    if (gazeVariance > 60.0) {
        score += 22
        reasons += "Noticeable head/gaze movement across the clip, rather than steady eye contact."
    } else if (gazeVariance < 8.0) {
        reasons += "Gaze stayed relatively steady throughout the clip."
    }

    val eyeOpenValues = metricsList.flatMap {
        listOfNotNull(
            it.leftEyeOpen.takeIf { v -> v >= 0f }?.toDouble(),
            it.rightEyeOpen.takeIf { v -> v >= 0f }?.toDouble()
        )
    }
    val avgEyeOpen = if (eyeOpenValues.isNotEmpty()) eyeOpenValues.average() else -1.0
    if (avgEyeOpen in 0.0..0.55) {
        score += 14
        reasons += "Lower average eye-openness, which can indicate an averted or lowered gaze."
    }

    if (eyeOpenValues.isNotEmpty()) {
        val blinkishRatio = eyeOpenValues.count { it < 0.35 }.toDouble() / eyeOpenValues.size
        if (blinkishRatio > 0.3) {
            score += 10
            reasons += "Frequent partially-closed eyes detected, sometimes linked to stress blinking."
        }
    }

    val smilingValues = metricsList.mapNotNull { it.smiling.takeIf { v -> v >= 0f }?.toDouble() }
    val avgSmiling = if (smilingValues.isNotEmpty()) smilingValues.average() else -1.0
    if (avgSmiling > 0.6 && gazeVariance > 40.0) {
        score += 8
        reasons += "Smiling combined with unsteady gaze — a mismatched-expression pattern."
    }

    val headTilt = metricsList.map { abs(it.headEulerZ.toDouble()) }.average()
    if (headTilt > 12.0) {
        score += 6
        reasons += "Noticeable head tilting detected during the response."
    }

    score = min(score, 96)
    if (reasons.isEmpty()) reasons += "No strong deception-associated signals were detected in this clip."

    return VideoVerdict(score, score >= 50, reasons.take(4))
}

// ---------------------------------------------------------------------------
// HISTORY TAB
// ---------------------------------------------------------------------------

@Composable
fun HistoryTab() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Analysis history", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("No analyses saved yet.")
                Text("Your future results will appear here.", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}
