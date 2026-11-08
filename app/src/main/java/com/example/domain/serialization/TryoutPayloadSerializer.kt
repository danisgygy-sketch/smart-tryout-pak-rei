package com.example.domain.serialization

import com.example.model.BlueprintRow
import com.example.model.DimensionStat
import com.example.model.PakReiSummary
import com.example.model.QuestionItem
import com.example.model.QuestionReviewItem
import com.example.model.UserAnswerState
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

/**
 * SRP (Single Responsibility Principle):
 * Dedicated solely to JSON serialization and deserialization of Firestore payload strings.
 */
object TryoutPayloadSerializer {

    fun serializeQuestions(list: List<QuestionItem>): String {
        val arr = JSONArray()
        list.forEach { q ->
            val obj = JSONObject().apply {
                put("type", q.type)
                put("topic", q.topic)
                put("indicator", q.indicator)
                put("cognitiveLevel", q.cognitiveLevel)
                put("stimulus", q.stimulus)
                put("text", q.text)
                put("options", JSONArray(q.options))
                put("statements", JSONArray(q.statements))
                put("optionWeights", JSONArray(q.optionWeights))
                put("singleAnswer", q.singleAnswer)
                put("multiAnswer", JSONArray(q.multiAnswer))
                put("boolAnswers", JSONArray(q.boolAnswers))
                put("acceptedAnswers", JSONArray(q.acceptedAnswers))
                put("explanation", q.explanation)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeQuestions(jsonStr: String): List<QuestionItem> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val optsArr = o.optJSONArray("options") ?: JSONArray()
                val stmtsArr = o.optJSONArray("statements") ?: JSONArray()
                val weightsArr = o.optJSONArray("optionWeights") ?: JSONArray()
                val multiArr = o.optJSONArray("multiAnswer") ?: JSONArray()
                val boolArr = o.optJSONArray("boolAnswers") ?: JSONArray()
                val accArr = o.optJSONArray("acceptedAnswers") ?: JSONArray()
                QuestionItem(
                    type = o.optString("type", "pilgan"),
                    topic = o.optString("topic", "Konsep Utama"),
                    indicator = o.optString("indicator", ""),
                    cognitiveLevel = o.optString("cognitiveLevel", "Aplikasi"),
                    stimulus = o.optString("stimulus", ""),
                    text = o.optString("text", ""),
                    options = (0 until optsArr.length()).map { optsArr.getString(it) },
                    statements = (0 until stmtsArr.length()).map { stmtsArr.getString(it) },
                    optionWeights = (0 until weightsArr.length()).map { weightsArr.getInt(it) },
                    singleAnswer = o.optInt("singleAnswer", 0),
                    multiAnswer = (0 until multiArr.length()).map { multiArr.getInt(it) },
                    boolAnswers = (0 until boolArr.length()).map { boolArr.getBoolean(it) },
                    acceptedAnswers = (0 until accArr.length()).map { accArr.getString(it) },
                    explanation = o.optString("explanation", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeAnswers(list: List<UserAnswerState>): String {
        val arr = JSONArray()
        list.forEach { a ->
            val obj = JSONObject().apply {
                if (a.singleChoice != null) put("singleChoice", a.singleChoice) else put("singleChoice", JSONObject.NULL)
                put("multiChoices", JSONArray(a.multiChoices))
                val bArr = JSONArray()
                a.boolChoices.forEach { b ->
                    if (b == null) bArr.put(JSONObject.NULL) else bArr.put(b)
                }
                put("boolChoices", bArr)
                put("textAnswer", a.textAnswer)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeAnswers(jsonStr: String, targetSize: Int): List<UserAnswerState> {
        val result = MutableList(targetSize) { UserAnswerState() }
        if (jsonStr.isBlank()) return result
        return try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until min(arr.length(), targetSize)) {
                val o = arr.getJSONObject(i)
                val single = if (o.isNull("singleChoice")) null else o.optInt("singleChoice")
                val mArr = o.optJSONArray("multiChoices") ?: JSONArray()
                val bArr = o.optJSONArray("boolChoices") ?: JSONArray()
                result[i] = UserAnswerState(
                    singleChoice = single,
                    multiChoices = (0 until mArr.length()).map { mArr.getInt(it) },
                    boolChoices = (0 until bArr.length()).map { idx ->
                        if (bArr.isNull(idx)) null else bArr.getBoolean(idx)
                    },
                    textAnswer = o.optString("textAnswer", "")
                )
            }
            result
        } catch (_: Exception) {
            result
        }
    }

    fun serializeDoubts(list: List<Boolean>): String = JSONArray(list).toString()

    fun deserializeDoubts(jsonStr: String, targetSize: Int): List<Boolean> {
        val result = MutableList(targetSize) { false }
        if (jsonStr.isBlank()) return result
        return try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until min(arr.length(), targetSize)) {
                result[i] = arr.optBoolean(i, false)
            }
            result
        } catch (_: Exception) {
            result
        }
    }

    fun serializeDimensionStats(list: List<DimensionStat>): String {
        val arr = JSONArray()
        list.forEach { d ->
            arr.put(JSONObject().apply {
                put("label", d.label)
                put("got", d.got)
                put("max", d.max)
            })
        }
        return arr.toString()
    }

    fun deserializeDimensionStats(jsonStr: String): List<DimensionStat> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DimensionStat(
                    label = o.optString("label", "Dimensi"),
                    got = o.optDouble("got", 0.0),
                    max = o.optDouble("max", 1.0)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeSummaries(list: List<PakReiSummary>): String {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().apply {
                put("topic", s.topic)
                put("explanation", s.explanation)
            })
        }
        return arr.toString()
    }

    fun deserializeSummaries(jsonStr: String): List<PakReiSummary> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                PakReiSummary(
                    topic = o.optString("topic", "Rekomendasi"),
                    explanation = o.optString("explanation", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeBlueprintItems(list: List<BlueprintRow>): String {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(JSONObject().apply {
                put("domain", r.domain)
                put("indicator", r.indicator)
                put("cognitiveLevel", r.cognitiveLevel)
                put("questionType", r.questionType)
                put("count", r.count)
            })
        }
        return arr.toString()
    }

    fun deserializeBlueprintItems(jsonStr: String): List<BlueprintRow> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                BlueprintRow(
                    domain = o.optString("domain", "Materi Pokok"),
                    indicator = o.optString("indicator", "Indikator Soal"),
                    cognitiveLevel = o.optString("cognitiveLevel", "L2 - Aplikasi"),
                    questionType = o.optString("questionType", "pilgan"),
                    count = max(1, o.optInt("count", 1))
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeReviewItems(list: List<QuestionReviewItem>): String {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(JSONObject().apply {
                put("index", r.index)
                put("questionJson", serializeQuestions(listOf(r.question)))
                put("userAnswerText", r.userAnswerText)
                put("correctAnswerText", r.correctAnswerText)
                put("gotScore", r.gotScore)
                put("maxScore", r.maxScore)
                put("fullCorrect", r.fullCorrect)
            })
        }
        return arr.toString()
    }

    fun deserializeReviewItems(jsonStr: String): List<QuestionReviewItem> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val qList = deserializeQuestions(o.optString("questionJson", ""))
                val q = qList.firstOrNull() ?: return@mapNotNull null
                QuestionReviewItem(
                    index = o.optInt("index", i),
                    question = q,
                    userAnswerText = o.optString("userAnswerText", "-"),
                    correctAnswerText = o.optString("correctAnswerText", "-"),
                    gotScore = o.optDouble("gotScore", 0.0),
                    maxScore = o.optDouble("maxScore", 1.0),
                    fullCorrect = o.optBoolean("fullCorrect", false)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
