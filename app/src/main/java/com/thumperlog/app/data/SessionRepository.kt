package com.thumperlog.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object SessionRepository {

    private fun sessionsDir(context: Context): File {
        val dir = File(context.filesDir, "sessions")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun exportsDir(context: Context): File {
        val dir = File(context.filesDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveSession(context: Context, session: Session) {
        val file = File(sessionsDir(context), "${session.id}.json")
        file.writeText(toJson(session).toString())
    }

    fun listSessions(context: Context): List<Session> {
        val files = sessionsDir(context).listFiles { f -> f.extension == "json" } ?: emptyArray()
        return files.mapNotNull { f ->
            runCatching { fromJson(JSONObject(f.readText())) }.getOrNull()
        }.sortedByDescending { it.startTime }
    }

    fun getSession(context: Context, id: String): Session? {
        val file = File(sessionsDir(context), "$id.json")
        if (!file.exists()) return null
        return runCatching { fromJson(JSONObject(file.readText())) }.getOrNull()
    }

    fun deleteSession(context: Context, id: String) {
        File(sessionsDir(context), "$id.json").delete()
    }

    fun updateLabel(context: Context, id: String, newLabel: String) {
        val s = getSession(context, id) ?: return
        saveSession(context, s.copy(label = newLabel))
    }

    fun exportCsv(context: Context, session: Session): File {
        val file = File(exportsDir(context), "thumperlog_${session.id}.csv")
        file.bufferedWriter().use { w ->
            w.write("time_ms,magnitude_g\n")
            session.samples.forEach { s ->
                w.write("${s.tMs},${"%.4f".format(s.mag)}\n")
            }
        }
        return file
    }

    private fun toJson(s: Session): JSONObject {
        val arr = JSONArray()
        s.samples.forEach { sample ->
            val o = JSONObject()
            o.put("t", sample.tMs)
            o.put("m", sample.mag.toDouble())
            arr.put(o)
        }
        val o = JSONObject()
        o.put("id", s.id)
        o.put("label", s.label)
        o.put("startTime", s.startTime)
        o.put("duration", s.duration)
        o.put("peak", s.peak.toDouble())
        o.put("avg", s.avg.toDouble())
        o.put("samples", arr)
        return o
    }

    private fun fromJson(o: JSONObject): Session {
        val arr = o.getJSONArray("samples")
        val samples = (0 until arr.length()).map { i ->
            val so = arr.getJSONObject(i)
            Sample(so.getLong("t"), so.getDouble("m").toFloat())
        }
        return Session(
            id = o.getString("id"),
            label = o.getString("label"),
            startTime = o.getLong("startTime"),
            duration = o.getLong("duration"),
            peak = o.getDouble("peak").toFloat(),
            avg = o.getDouble("avg").toFloat(),
            samples = samples
        )
    }
}
