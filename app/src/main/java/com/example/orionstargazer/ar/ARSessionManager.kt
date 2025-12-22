package com.example.orionstargazer.ar

import android.content.Context
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.ArSceneView

class ARSessionManager(private val context: Context) {
    private var session: Session? = null
    @Volatile private var lastError: String? = null

    fun lastErrorMessage(): String? = lastError

    fun getSession(): Session? {
        if (session == null) {
            try {
                val availability = ArCoreApk.getInstance().checkAvailability(context)
                if (!availability.isSupported) {
                    lastError = "ARCore not supported on this device."
                    return null
                }
                val newSession = Session(context)
                val config = Config(newSession).apply {
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                }
                newSession.configure(config)
                session = newSession
                lastError = null
            } catch (e: UnavailableException) {
                lastError = "${e.javaClass.simpleName}: ${e.message ?: "ARCore unavailable"}"
                e.printStackTrace()
            }
        }
        return session
    }

    fun attachSceneView(sceneView: ArSceneView) {
        getSession()?.let {
            sceneView.setupSession(it)
        }
    }

    fun pause() {
        session?.pause()
    }

    fun resume() {
        session?.resume()
    }

    fun destroy() {
        session?.close()
        session = null
    }
}

