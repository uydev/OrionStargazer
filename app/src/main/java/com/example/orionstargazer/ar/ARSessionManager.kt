package com.example.orionstargazer.ar

import android.content.Context
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.sceneform.ArSceneView

class ARSessionManager(private val context: Context) {
    private var session: Session? = null

    fun getSession(): Session? {
        if (session == null) {
            try {
                val newSession = Session(context)
                val config = Config(newSession).apply {
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                }
                newSession.configure(config)
                session = newSession
            } catch (e: UnavailableException) {
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

